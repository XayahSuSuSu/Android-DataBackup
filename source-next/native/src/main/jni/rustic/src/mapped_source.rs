use std::cmp::Reverse;
use std::collections::BinaryHeap;
use std::io::ErrorKind;
use std::path::{Component, Path, PathBuf};

use rustic_core::repofile::Node;
use rustic_core::{
    Excludes, LocalSource, LocalSourceSaveOptions, ReadSource, ReadSourceEntry, RusticResult,
};

use crate::Result;

/// A physical source and its destination relative to the snapshot root (a leading slash is allowed).
#[derive(Clone, Debug)]
pub struct SourceMapping {
    pub source_path: String,
    pub snapshot_path: String,
}

struct MappedRoot {
    source: PathBuf,
    destination: PathBuf,
    reader: LocalSource,
    directory_node: Option<Node>,
    excluded: Vec<PathBuf>,
}

type Entry = RusticResult<ReadSourceEntry<<LocalSource as ReadSource>::Open>>;
type Entries = Box<dyn Iterator<Item = Entry> + Send>;

/// Applies source-specific mappings without copying content or changing where files are opened.
pub(crate) struct MappedSource {
    roots: Vec<MappedRoot>,
}

/// Escapes glob metacharacters so a physical path is matched literally by Rustic's exclude globs.
fn escape_glob(path: &Path) -> String {
    let mut escaped = String::new();
    for character in path.to_string_lossy().chars() {
        if matches!(character, '\\' | '*' | '?' | '[' | ']' | '{' | '}' | '!') {
            escaped.push('\\');
        }
        escaped.push(character);
    }
    escaped
}

impl MappedSource {
    #[cfg(test)]
    pub fn new(mappings: &[SourceMapping]) -> Result<Self> {
        Self::new_excluding(mappings, &[])
    }

    /// Creates a mapped source that omits the given physical subtrees.
    ///
    /// Each excluded path must lie strictly below a mapped source directory. Missing paths and paths outside
    /// every source are ignored, so callers can pass optional directories such as app caches without checking them.
    pub fn new_excluding(mappings: &[SourceMapping], excluded_paths: &[String]) -> Result<Self> {
        if mappings.is_empty() {
            return Err("At least one source mapping is required".into());
        }
        let mut paths: Vec<(PathBuf, PathBuf)> = Vec::new();
        for mapping in mappings {
            let source = Path::new(&mapping.source_path).canonicalize()?;
            let path = Path::new(&mapping.snapshot_path);
            if mapping.snapshot_path.is_empty()
                || path.components().any(|part| {
                    matches!(
                        part,
                        Component::ParentDir | Component::CurDir | Component::Prefix(_)
                    )
                })
            {
                return Err(
                    "Snapshot paths must not be empty or contain traversal components".into(),
                );
            }
            let destination = path
                .components()
                .fold(PathBuf::from("/"), |mut path, component| {
                    if let Component::Normal(name) = component {
                        path.push(name);
                    }
                    path
                });
            if destination == Path::new("/") && !source.is_dir() {
                return Err("A file cannot be mapped to the snapshot root".into());
            }
            if let Some((_, existing)) = paths.iter().find(|(previous, _)| *previous == source) {
                if *existing != destination {
                    return Err("A physical source has conflicting snapshot mappings".into());
                }
                continue;
            }
            paths.push((source, destination));
        }
        paths.sort();
        // Discard child mappings that are identical to their parent's inherited mapping.
        let mut retained: Vec<(PathBuf, PathBuf)> = Vec::new();
        for (source, destination) in paths {
            let parent = retained
                .iter()
                .filter(|(root, _)| source.starts_with(root))
                .max_by_key(|(root, _)| root.components().count());
            if parent.is_some_and(|(root, mapped)| {
                mapped.join(source.strip_prefix(root).unwrap()) == destination
            }) {
                continue;
            }
            retained.push((source, destination));
        }
        let exclusions: Vec<Vec<PathBuf>> = retained
            .iter()
            .map(|(source, _)| {
                retained
                    .iter()
                    .filter(|(child, _)| child != source && child.starts_with(source))
                    .map(|(child, _)| child.clone())
                    .collect()
            })
            .collect();
        // Caller exclusions only prune content; they never take part in overlap validation below.
        let mut requested_exclusions: Vec<Vec<PathBuf>> = vec![Vec::new(); retained.len()];
        for excluded in excluded_paths {
            let excluded = match Path::new(excluded).canonicalize() {
                Ok(path) => path,
                Err(error) if error.kind() == ErrorKind::NotFound => continue,
                Err(error) => return Err(error.into()),
            };
            for (index, (source, _)) in retained.iter().enumerate() {
                if excluded != *source && excluded.starts_with(source) {
                    requested_exclusions[index].push(excluded.clone());
                }
            }
        }
        // Destination roots may nest only when they do not overwrite content from another source.
        for (index, (source, destination)) in retained.iter().enumerate() {
            for (other, (_, nested)) in retained.iter().enumerate() {
                if index == other {
                    continue;
                }
                if let Ok(relative) = nested.strip_prefix(destination) {
                    if relative.as_os_str().is_empty() || !source.symlink_metadata()?.is_dir() {
                        return Err("Source mappings overlap at a snapshot destination".into());
                    }
                    let target = source.join(relative);
                    let mut physical = source.clone();
                    for component in relative.components() {
                        physical.push(component);
                        // An excluded subtree contributes no nodes to this destination.
                        if exclusions[index]
                            .iter()
                            .any(|path| physical.starts_with(path))
                        {
                            break;
                        }
                        match physical.symlink_metadata() {
                            Ok(metadata) if !metadata.is_dir() || physical == target => {
                                return Err(
                                    "Source mappings overlap at a snapshot destination".into()
                                );
                            }
                            Ok(_) => {}
                            Err(error) if error.kind() == ErrorKind::NotFound => break,
                            Err(error) => return Err(error.into()),
                        }
                    }
                }
            }
        }
        let roots = retained
            .into_iter()
            .zip(exclusions)
            .zip(requested_exclusions)
            .map(|(((source, destination), mut excluded), requested)| {
                // Negated absolute globs stop the walker from descending into excluded directories.
                // The prefix filter in `entries` still drops them in case a glob cannot match.
                let excludes = Excludes::default().globs(
                    requested
                        .iter()
                        .map(|path| format!("!{}", escape_glob(path)))
                        .collect::<Vec<_>>(),
                );
                excluded.extend(requested);
                // LocalSource skips directory roots. Use its own metadata mapper so
                // empty roots, permissions, ownership and xattrs survive mapping too.
                // The snapshot root itself has no named node in Rustic's tree format.
                let directory_node =
                    if source.symlink_metadata()?.is_dir() && destination != Path::new("/") {
                        let entry = ignore::WalkBuilder::new(&source)
                            .max_depth(Some(0))
                            .build()
                            .next()
                            .ok_or("Source directory walk returned no root entry")??;
                        Some(LocalSourceSaveOptions::default().map_entry(entry)?.node)
                    } else {
                        None
                    };
                let reader = LocalSource::new(
                    Default::default(),
                    &excludes,
                    &Default::default(),
                    std::slice::from_ref(&source),
                )?;
                Ok(MappedRoot {
                    source,
                    destination,
                    reader,
                    directory_node,
                    excluded,
                })
            })
            .collect::<Result<Vec<_>>>()?;
        Ok(Self { roots })
    }

    pub fn snapshot_paths(&self) -> Vec<PathBuf> {
        let mut paths: Vec<_> = self
            .roots
            .iter()
            .map(|root| root.destination.clone())
            .collect();
        paths.sort();
        paths
    }
}

impl ReadSource for MappedSource {
    type Open = <LocalSource as ReadSource>::Open;
    type Iter = Entries;

    fn size(&self) -> RusticResult<Option<u64>> {
        self.entries()
            .try_fold(0, |size, entry| {
                let entry = entry?;
                Ok(size
                    + if entry.node.is_file() {
                        entry.node.meta.size
                    } else {
                        0
                    })
            })
            .map(Some)
    }

    fn entries(&self) -> Self::Iter {
        let mut streams: Vec<Entries> = self
            .roots
            .iter()
            .map(|root| {
                let source = root.source.clone();
                let destination = root.destination.clone();
                let excluded = root.excluded.clone();
                let directory_entry = root.directory_node.clone().map(|node| {
                    Ok(ReadSourceEntry {
                        path: source.clone(),
                        node,
                        open: None,
                    })
                });
                Box::new(
                    directory_entry
                        .into_iter()
                        .chain(root.reader.entries())
                        .filter(move |entry| {
                            !entry.as_ref().is_ok_and(|entry| {
                                excluded.iter().any(|path| entry.path.starts_with(path))
                            })
                        })
                        .map(move |entry| {
                            entry.map(|mut entry| {
                                let relative = entry
                                    .path
                                    .strip_prefix(&source)
                                    .expect("entry remains under source root");
                                entry.path = if relative.as_os_str().is_empty() {
                                    destination.clone()
                                } else {
                                    destination.join(relative)
                                };
                                if entry.path == destination {
                                    if let Some(name) = destination.file_name() {
                                        // Use Rustic's filename encoding, including renamed individual files.
                                        entry.node.name = Node::new_node(
                                            name,
                                            entry.node.node_type.clone(),
                                            entry.node.meta.clone(),
                                        )
                                        .name;
                                    }
                                }
                                entry
                            })
                        }),
                ) as Entries
            })
            .collect();
        let mut pending: Vec<Option<Entry>> =
            streams.iter_mut().map(|stream| stream.next()).collect();
        let mut heap = BinaryHeap::new();
        for (index, entry) in pending.iter().enumerate() {
            if let Some(entry) = entry {
                heap.push(Reverse((
                    entry
                        .as_ref()
                        .map(|entry| entry.path.clone())
                        .unwrap_or_default(),
                    index,
                )));
            }
        }
        // Merge sorted source walks in snapshot order with memory proportional to source count.
        Box::new(std::iter::from_fn(move || {
            let Reverse((_, index)) = heap.pop()?;
            let entry = pending[index].take();
            pending[index] = streams[index].next();
            if let Some(next) = &pending[index] {
                heap.push(Reverse((
                    next.as_ref()
                        .map(|entry| entry.path.clone())
                        .unwrap_or_default(),
                    index,
                )));
            }
            entry
        }))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use rustic_core::ReadSourceOpen;
    use std::collections::BTreeMap;
    use std::io::Read;
    use std::time::{SystemTime, UNIX_EPOCH};

    struct Workspace(PathBuf);
    impl Workspace {
        fn new() -> Self {
            let path = std::env::temp_dir().join(format!(
                "mapped-source-{}",
                SystemTime::now()
                    .duration_since(UNIX_EPOCH)
                    .unwrap()
                    .as_nanos()
            ));
            std::fs::create_dir_all(&path).unwrap();
            Self(path)
        }
        fn write(&self, path: &str, content: &str) -> PathBuf {
            let file = self.0.join(path);
            std::fs::create_dir_all(file.parent().unwrap()).unwrap();
            std::fs::write(&file, content).unwrap();
            file
        }
        fn mapping(&self, source: &str, target: &str) -> SourceMapping {
            SourceMapping {
                source_path: self.0.join(source).to_string_lossy().into_owned(),
                snapshot_path: target.into(),
            }
        }
    }
    impl Drop for Workspace {
        fn drop(&mut self) {
            let _ = std::fs::remove_dir_all(&self.0);
        }
    }

    fn contents(source: &MappedSource) -> BTreeMap<String, String> {
        let entries = source.entries().collect::<RusticResult<Vec<_>>>().unwrap();
        assert!(entries.windows(2).all(|pair| pair[0].path <= pair[1].path));
        entries
            .into_iter()
            .filter(|entry| entry.node.is_file())
            .map(|entry| {
                assert_eq!(
                    entry.node.name().into_owned(),
                    entry.path.file_name().unwrap().to_os_string()
                );
                let mut content = String::new();
                entry
                    .open
                    .unwrap()
                    .open()
                    .unwrap()
                    .read_to_string(&mut content)
                    .unwrap();
                (entry.path.to_string_lossy().into_owned(), content)
            })
            .collect()
    }

    #[test]
    fn maps_multiple_directories_and_renamed_files_in_snapshot_order() {
        let workspace = Workspace::new();
        workspace.write("first/nested/a", "a");
        workspace.write("second/b", "b");
        workspace.write("single", "single");
        let source = MappedSource::new(&[
            workspace.mapping("first", "z/data"),
            workspace.mapping("second", "/a/config"),
            workspace.mapping("single", "middle/renamed\"文件.txt"),
        ])
        .unwrap();
        assert_eq!(
            contents(&source),
            BTreeMap::from([
                ("/a/config/b".into(), "b".into()),
                ("/middle/renamed\"文件.txt".into(), "single".into()),
                ("/z/data/nested/a".into(), "a".into()),
            ])
        );
    }

    #[test]
    fn child_mapping_overrides_parent_and_identical_mappings_do_not_duplicate_files() {
        let workspace = Workspace::new();
        workspace.write("parent/ordinary/file", "ordinary");
        workspace.write("parent/private/nested/config", "private");
        let source = MappedSource::new(&[
            workspace.mapping("parent", "data"),
            workspace.mapping("parent/ordinary", "data/ordinary"),
            workspace.mapping("parent/private", "settings"),
            workspace.mapping("parent/private/nested", "settings/nested"),
        ])
        .unwrap();
        let files = contents(&source);
        assert_eq!(files.len(), 2);
        assert_eq!(files["/data/ordinary/file"], "ordinary");
        assert_eq!(files["/settings/nested/config"], "private");
        assert_eq!(
            source
                .entries()
                .filter(|entry| entry.as_ref().is_ok_and(|entry| entry.node.is_file()))
                .count(),
            2
        );
    }

    #[test]
    fn rejects_destinations_below_files_and_symlinks() {
        let workspace = Workspace::new();
        workspace.write("parent/file", "old");
        workspace.write("other/payload", "new");
        #[cfg(unix)]
        {
            use std::os::unix::fs::symlink;
            symlink(workspace.0.join("other"), workspace.0.join("parent/link")).unwrap();
            symlink(
                workspace.0.join("missing"),
                workspace.0.join("parent/dangling"),
            )
            .unwrap();
        }
        let mut ancestors = vec!["file"];
        #[cfg(unix)]
        ancestors.extend(["link", "dangling"]);
        for ancestor in ancestors {
            assert!(
                MappedSource::new(&[
                    workspace.mapping("parent", "data"),
                    workspace.mapping("other", &format!("data/{ancestor}/missing/nested")),
                ])
                .is_err(),
                "accepted a destination below {ancestor}"
            );
        }
        // A file moved out of the parent no longer blocks a nested destination.
        let source = MappedSource::new(&[
            workspace.mapping("parent", "data"),
            workspace.mapping("parent/file", "moved-file"),
            workspace.mapping("other", "data/file/nested"),
        ])
        .unwrap();
        let files = contents(&source);
        assert_eq!(files["/moved-file"], "old");
        assert_eq!(files["/data/file/nested/payload"], "new");
    }

    #[test]
    fn retains_mapped_empty_directories() {
        let workspace = Workspace::new();
        std::fs::create_dir_all(workspace.0.join("parent/empty")).unwrap();
        let source = MappedSource::new(&[
            workspace.mapping("parent", "data"),
            workspace.mapping("parent/empty", "renamed\"目录"),
        ])
        .unwrap();
        let entries = source.entries().collect::<RusticResult<Vec<_>>>().unwrap();
        assert_eq!(entries.len(), 2);
        assert_eq!(entries[0].path, Path::new("/data"));
        assert_eq!(entries[1].path, Path::new("/renamed\"目录"));
        assert!(entries.iter().all(|entry| entry.node.is_dir()));
        assert_eq!(
            entries[1].node.name(),
            Path::new("renamed\"目录").as_os_str()
        );
    }

    #[test]
    fn mapping_to_snapshot_root_does_not_emit_an_unnamed_node() {
        let workspace = Workspace::new();
        workspace.write("parent/file", "payload");
        let source = MappedSource::new(&[workspace.mapping("parent", "/")]).unwrap();
        assert_eq!(
            contents(&source),
            BTreeMap::from([("/file".into(), "payload".into())])
        );
        assert_eq!(source.entries().count(), 1);
    }

    #[test]
    fn rejects_ambiguous_destinations_and_invalid_paths() {
        let workspace = Workspace::new();
        workspace.write("parent/existing/file", "old");
        workspace.write("other/file", "new");
        for target in ["data", "data/existing"] {
            assert!(
                MappedSource::new(&[
                    workspace.mapping("parent", "data"),
                    workspace.mapping("other", target),
                ])
                .is_err()
            );
        }
        for target in ["", "../escape", "data/../escape"] {
            assert!(MappedSource::new(&[workspace.mapping("parent", target)]).is_err());
        }
        assert!(MappedSource::new(&[workspace.mapping("other/file", "/")]).is_err());
        assert!(
            MappedSource::new(&[
                workspace.mapping("parent", "one"),
                workspace.mapping("parent/.", "two"),
            ])
            .is_err()
        );
        assert!(MappedSource::new(&[]).is_err());
        // Distinct sources can share a destination ancestor when no physical entries collide.
        let source = MappedSource::new(&[
            workspace.mapping("parent", "data"),
            workspace.mapping("other", "data/new"),
        ])
        .unwrap();
        assert_eq!(contents(&source).len(), 2);
    }
}

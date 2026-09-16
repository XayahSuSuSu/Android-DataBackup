use rustic_backend::BackendOptions;
use rustic_core::repofile::{Node, SnapshotFile};
use rustic_core::{
    BackupOptions, CheckOptions, ConfigOptions, Credentials, KeyOptions, LocalDestination,
    LsOptions, OpenStatus, Repository, RepositoryBackends, RepositoryOptions, RestoreOptions,
    SnapshotOptions,
};
use std::path::{Component, Path};

use crate::Result;
use crate::mapped_source::{MappedSource, SourceMapping};

use crate::progress::{AndroidProgressBars, RusticProgressCallback};

/// Initializes a repository with the supplied password and default configuration.
pub fn init_repository(repository_path: &str, password: &str) -> Result<()> {
    let credentials = Credentials::password(password);

    Repository::new(&RepositoryOptions::default(), &backends(repository_path)?)?.init(
        &credentials,
        &KeyOptions::default(),
        &ConfigOptions::default(),
    )?;

    Ok(())
}

/// Returns whether a repository configuration exists at the given path.
///
/// This does not validate credentials or repository integrity.
pub fn repository_exists(repository_path: &str) -> Result<bool> {
    let repo = Repository::new(&RepositoryOptions::default(), &backends(repository_path)?)?;

    Ok(repo.config_id()?.is_some())
}

/// Validates that the repository can be opened with the supplied password.
///
/// This does not perform a repository integrity check.
pub fn validate_repository(repository_path: &str, password: &str) -> Result<()> {
    open_repository(repository_path, password)?;

    Ok(())
}

/// Backs up the source paths with the supplied tags and returns the snapshot ID.
pub fn create_snapshot(
    repository_path: &str,
    password: &str,
    source_paths: &[SourceMapping],
    tags: &[String],
) -> Result<String> {
    create_snapshot_from_repository(
        open_repository(repository_path, password)?,
        source_paths,
        tags,
    )
}

/// Backs up the source paths with the supplied tags and reports byte progress.
///
/// Returns the snapshot ID after the backup completes.
pub fn create_snapshot_with_progress<C: RusticProgressCallback>(
    repository_path: &str,
    password: &str,
    source_paths: &[SourceMapping],
    tags: &[String],
    callback: C,
) -> Result<String> {
    create_snapshot_from_repository(
        open_repository_with_progress(repository_path, password, callback)?,
        source_paths,
        tags,
    )
}

fn create_snapshot_from_repository(
    repo: Repository<OpenStatus>,
    source_paths: &[SourceMapping],
    tags: &[String],
) -> Result<String> {
    let repo = repo.to_indexed_ids()?;
    let source = MappedSource::new(source_paths)?;
    let snapshot_options = tags
        .iter()
        .try_fold(SnapshotOptions::default(), |options, tag| {
            options.add_tags(tag)
        })?;
    let snapshot = repo.archive(
        &BackupOptions::default(),
        &source,
        snapshot_options.to_snapshot()?,
        &source.snapshot_paths(),
    )?;

    Ok(snapshot.id.to_hex().to_string())
}

/// Restores the selected snapshot to the destination path.
/// `snapshot_id` also accepts `snapshot_id:path` to select a single file or directory.
/// Directory contents are placed directly in the destination. For a file, use a filename
/// or an existing destination directory (which retains the snapshot basename).
pub fn restore_snapshot(
    repository_path: &str,
    password: &str,
    snapshot_id: &str,
    destination_path: &str,
) -> Result<()> {
    restore_snapshot_with_options(
        repository_path,
        password,
        snapshot_id,
        destination_path,
        &RestoreOptions::default(),
    )
}

pub fn restore_snapshot_with_options(
    repository_path: &str,
    password: &str,
    snapshot_id: &str,
    destination_path: &str,
    options: &RestoreOptions,
) -> Result<()> {
    let repo = open_repository(repository_path, password)?.to_indexed()?;
    let node = repo.node_from_snapshot_path(snapshot_id, |_| true)?;
    let ls_options = LsOptions::default();
    let nodes = repo.ls(&node, &ls_options)?;
    let destination = LocalDestination::new(destination_path, true, !node.is_dir())?;
    let restore_plan = repo.prepare_restore(options, nodes.clone(), &destination, false)?;

    repo.restore(restore_plan, options, nodes, &destination)?;

    Ok(())
}

fn validate_external_node(path: &Path, node: &Node) -> Result<()> {
    if path
        .components()
        .any(|part| !matches!(part, Component::Normal(_) | Component::CurDir))
    {
        return Err(format!("Invalid external snapshot path: {}", path.display()).into());
    }
    if !node.is_dir() && !node.is_file() {
        return Err(format!("Unsupported external snapshot entry: {}", path.display()).into());
    }
    Ok(())
}

/// Validates that the selected external-data snapshot is a directory containing
/// only files and directories with relative paths and no parent traversal.
/// Call before clearing destination contents.
pub(crate) fn validate_external_snapshot(
    repository_path: &str,
    password: &str,
    snapshot_id: &str,
) -> Result<()> {
    let repo = open_repository(repository_path, password)?.to_indexed()?;
    let node = repo.node_from_snapshot_path(snapshot_id, |_| true)?;
    if !node.is_dir() {
        return Err("External snapshot source is not a directory".into());
    }
    for entry in repo.ls(&node, &LsOptions::default())? {
        let (path, node) = entry?;
        validate_external_node(&path, &node)?;
    }
    Ok(())
}

/// Restores external-data directory contents without importing source ownership,
/// modes, extended attributes or hardlink relationships.
/// The caller must validate the snapshot and prepare the destination before calling,
/// then repair destination metadata even if this function returns an error.
pub(crate) fn restore_external_snapshot(
    repository_path: &str,
    password: &str,
    snapshot_id: &str,
    destination_path: &str,
) -> Result<()> {
    let repo = open_repository(repository_path, password)?.to_indexed()?;
    let node = repo.node_from_snapshot_path(snapshot_id, |_| true)?;
    if !node.is_dir() {
        return Err("External snapshot source is not a directory".into());
    }
    let nodes = repo.ls(&node, &LsOptions::default())?.map(|entry| {
        entry.map(|(path, mut node)| {
            // Storage permissions and security attributes belong to the destination device.
            // Emulated storage cannot represent hardlinks; materialize each file independently.
            node.meta.mode = None;
            node.meta.extended_attributes.clear();
            node.meta.links = 1;
            (path, node)
        })
    });
    let options = RestoreOptions::default()
        .no_ownership(true)
        .verify_existing(true);
    let destination = LocalDestination::new(destination_path, true, false)?;
    let plan = repo.prepare_restore(&options, nodes.clone(), &destination, false)?;
    repo.restore(plan, &options, nodes, &destination)?;
    Ok(())
}

/// Reads the original numeric UID of a snapshot directory.
pub fn read_snapshot_directory_uid(
    repository_path: &str,
    password: &str,
    snapshot_id: &str,
) -> Result<i32> {
    let repo = open_repository(repository_path, password)?.to_indexed()?;
    let node = repo.node_from_snapshot_path(snapshot_id, |_| true)?;
    if !node.is_dir() {
        return Err("Snapshot source is not a directory".into());
    }
    Ok(node
        .meta
        .uid
        .ok_or("Snapshot directory has no UID")?
        .try_into()?)
}

/// Removes exactly one snapshot and returns the remaining metadata.
/// Shared data is retained until repository pruning.
pub fn delete_snapshot(repository_path: &str, password: &str, snapshot_id: &str) -> Result<String> {
    let repo = open_repository(repository_path, password)?;
    let snapshot = repo.get_snapshot_from_str(snapshot_id, |_| true)?;
    let mut snapshots = repo.get_all_snapshots()?;
    snapshots.retain(|entry| entry.id != snapshot.id);
    // Prepare the response before deleting: metadata errors must not report a completed deletion as failed.
    let serialized = serialize_snapshots(snapshots)?;
    repo.delete_snapshots(&[snapshot.id])?;
    Ok(serialized)
}

/// Returns snapshot metadata as a JSON array, ordered from newest to oldest.
///
/// Each object includes `created_at` as milliseconds since the Unix epoch.
pub fn list_snapshots(repository_path: &str, password: &str) -> Result<String> {
    // Snapshot metadata lives in dedicated snapshot files. Loading the repository's
    // complete data index here makes a metadata-only list operation unnecessarily slow.
    let repo = open_repository(repository_path, password)?;
    serialize_snapshots(repo.get_all_snapshots()?)
}

fn serialize_snapshots(mut snapshots: Vec<SnapshotFile>) -> Result<String> {
    snapshots.sort_by(|left, right| right.time.cmp(&left.time));

    let snapshots = snapshots
        .into_iter()
        .map(|snapshot| -> Result<serde_json::Value> {
            let created_at = snapshot.time.timestamp().as_millisecond();
            let mut value = serde_json::to_value(snapshot)?;
            if let serde_json::Value::Object(ref mut object) = value {
                object.insert("created_at".to_string(), created_at.into());
            }
            Ok(value)
        })
        .collect::<Result<Vec<_>>>()?;

    Ok(serde_json::to_string(&snapshots)?)
}

/// Checks repository integrity using Rustic's default checks and trusting cached data.
pub fn check_repository(repository_path: &str, password: &str) -> Result<()> {
    let repo = open_repository(repository_path, password)?;

    repo.check(CheckOptions::default().trust_cache(true))?;

    Ok(())
}

fn open_repository(repository_path: &str, password: &str) -> Result<Repository<OpenStatus>> {
    Ok(
        Repository::new(&RepositoryOptions::default(), &backends(repository_path)?)?
            .open(&Credentials::password(password))?,
    )
}

fn open_repository_with_progress<C: RusticProgressCallback>(
    repository_path: &str,
    password: &str,
    callback: C,
) -> Result<Repository<OpenStatus>> {
    Ok(Repository::new_with_progress(
        &RepositoryOptions::default(),
        &backends(repository_path)?,
        AndroidProgressBars::new(callback),
    )?
    .open(&Credentials::password(password))?)
}

fn backends(repository_path: &str) -> Result<RepositoryBackends> {
    Ok(BackendOptions::default()
        .repository(repository_path)
        .to_backends()?)
}

/// Reads UTF-8 text from the specified files in a snapshot without restoring them to the filesystem.
///
/// Returns a JSON object mapping each requested path to its text content.
///
/// # Errors
///
/// Returns an error if the repository or requested files cannot be read, the snapshot
/// ID cannot be resolved or has an invalid format, or any file contains invalid UTF-8.
pub fn read_snapshot_text_files(
    repository_path: &str,
    password: &str,
    snapshot_id: &str,
    paths: &[String],
) -> Result<String> {
    let repo = open_repository(repository_path, password)?.to_indexed()?;
    let mut result = serde_json::Map::new();
    for path in paths {
        let node = repo.node_from_snapshot_path(&format!("{snapshot_id}:{path}"), |_| true)?;
        let mut content = Vec::new();
        repo.dump(&node, &mut content)?;
        result.insert(path.clone(), String::from_utf8(content)?.into());
    }
    Ok(serde_json::to_string(&result)?)
}

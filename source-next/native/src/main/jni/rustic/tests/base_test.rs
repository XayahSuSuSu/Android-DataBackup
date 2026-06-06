use std::error::Error;
use std::fs;
use std::path::Path;
use std::time::{SystemTime, UNIX_EPOCH};

fn temp_path(name: &str) -> Result<std::path::PathBuf, Box<dyn Error>> {
    Ok(std::env::temp_dir().join(format!(
        "rustic-{name}-{}",
        SystemTime::now().duration_since(UNIX_EPOCH)?.as_nanos()
    )))
}

#[test]
fn create_restore_and_check_snapshot_lifecycle() -> Result<(), Box<dyn Error>> {
    let root = temp_path("snapshot-lifecycle")?;
    let repository = root.join("repo");
    let source = root.join("source");
    let restore = root.join("restore");
    let password = "password";
    let content = "Hello from rustic";

    fs::create_dir_all(source.join("nested"))?;
    fs::write(source.join("nested").join("note.txt"), content)?;

    rustic::init_repository(repository.to_str().unwrap(), password)?;
    let snapshot_id = rustic::create_snapshot(
        repository.to_str().unwrap(),
        password,
        &[source.to_string_lossy().into_owned()],
        &["instrumented".to_string()],
    )?;

    assert!(!snapshot_id.is_empty());

    rustic::restore_snapshot(
        repository.to_str().unwrap(),
        password,
        &snapshot_id,
        restore.to_str().unwrap(),
    )?;
    rustic::check_repository(repository.to_str().unwrap(), password)?;

    let restored = find_file(&restore, "note.txt")?;
    assert_eq!(fs::read_to_string(restored)?, content);

    fs::remove_dir_all(root)?;
    Ok(())
}

fn find_file(root: &Path, name: &str) -> Result<std::path::PathBuf, Box<dyn Error>> {
    for entry in fs::read_dir(root)? {
        let path = entry?.path();
        if path.is_dir() {
            if let Ok(found) = find_file(&path, name) {
                return Ok(found);
            }
        } else if path.file_name().is_some_and(|file_name| file_name == name) {
            return Ok(path);
        }
    }

    Err(format!("missing restored file {name}").into())
}

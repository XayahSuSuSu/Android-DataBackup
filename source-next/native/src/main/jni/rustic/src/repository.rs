use rustic_backend::BackendOptions;
use rustic_core::{
    BackupOptions, CheckOptions, ConfigOptions, Credentials, KeyOptions, LocalDestination,
    LsOptions, OpenStatus, PathList, Repository, RepositoryBackends, RepositoryOptions,
    RestoreOptions, SnapshotOptions,
};

use crate::Result;

pub fn init_repository(repository_path: &str, password: &str) -> Result<()> {
    let credentials = Credentials::password(password);

    Repository::new(&RepositoryOptions::default(), &backends(repository_path)?)?.init(
        &credentials,
        &KeyOptions::default(),
        &ConfigOptions::default(),
    )?;

    Ok(())
}

pub fn create_snapshot(
    repository_path: &str,
    password: &str,
    source_paths: &[String],
    tags: &[String],
) -> Result<String> {
    let repo = open_repository(repository_path, password)?.to_indexed_ids()?;
    let source = source_paths
        .iter()
        .map(std::path::PathBuf::from)
        .collect::<PathList>()
        .sanitize()?;
    let snapshot_options = tags
        .iter()
        .try_fold(SnapshotOptions::default(), |options, tag| {
            options.add_tags(tag)
        })?;
    let snapshot = repo.backup(
        &BackupOptions::default(),
        &source,
        snapshot_options.to_snapshot()?,
    )?;

    Ok(snapshot.id.to_string())
}

pub fn restore_snapshot(
    repository_path: &str,
    password: &str,
    snapshot_id: &str,
    destination_path: &str,
) -> Result<()> {
    let repo = open_repository(repository_path, password)?.to_indexed()?;
    let node = repo.node_from_snapshot_path(snapshot_id, |_| true)?;
    let ls_options = LsOptions::default();
    let nodes = repo.ls(&node, &ls_options)?;
    let destination = LocalDestination::new(destination_path, true, !node.is_dir())?;
    let restore_options = RestoreOptions::default();
    let restore_plan =
        repo.prepare_restore(&restore_options, nodes.clone(), &destination, false)?;

    repo.restore(restore_plan, &restore_options, nodes, &destination)?;

    Ok(())
}

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

fn backends(repository_path: &str) -> Result<RepositoryBackends> {
    Ok(BackendOptions::default()
        .repository(repository_path)
        .to_backends()?)
}

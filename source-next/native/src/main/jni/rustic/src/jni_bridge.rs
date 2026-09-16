use jni::errors::ThrowRuntimeExAndDefault;
use jni::objects::{JObject, JObjectArray, JString};
use jni::sys::jboolean;
use jni::{EnvUnowned, jni_sig, jni_str};

use crate::SourceMapping;
use crate::error::NativeError;
use crate::jni_progress::JniProgressCallback;
use crate::repository::{
    check_repository, create_snapshot, create_snapshot_with_progress, delete_snapshot,
    init_repository, list_snapshots, read_snapshot_text_files, repository_exists,
    validate_repository,
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeInitLogger<'local>(
    _unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
) {
    android_logger::init_once(
        android_logger::Config::default().with_max_level(log::LevelFilter::Info),
    );
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeInitRepository<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
) {
    unowned_env
        .with_env(|_env| -> Result<(), NativeError> {
            init_repository(&repository_path.to_string(), &password.to_string())
                .map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeRepositoryExists<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
) -> jboolean {
    unowned_env
        .with_env(|_env| -> Result<jboolean, NativeError> {
            repository_exists(&repository_path.to_string())
                .map(jboolean::from)
                .map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeValidateRepository<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
) {
    unowned_env
        .with_env(|_env| -> Result<(), NativeError> {
            validate_repository(&repository_path.to_string(), &password.to_string())
                .map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeCreateSnapshot<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
    source_paths: JObjectArray<'local, JString<'local>>,
    snapshot_paths: JObjectArray<'local, JString<'local>>,
    tags: JObjectArray<'local, JString<'local>>,
    callback: JObject<'local>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> Result<JString<'local>, NativeError> {
            let source_paths = string_array_to_vec(env, &source_paths)?;
            let snapshot_paths = string_array_to_vec(env, &snapshot_paths)?;
            let tags = string_array_to_vec(env, &tags)?;
            let repository_path = repository_path.to_string();
            let password = password.to_string();
            if source_paths.len() != snapshot_paths.len() {
                return Err(NativeError::from(Box::<dyn std::error::Error>::from(
                    "Source and snapshot path counts differ",
                )));
            }
            let source_paths: Vec<_> = source_paths
                .into_iter()
                .zip(snapshot_paths)
                .map(|(source_path, snapshot_path)| SourceMapping {
                    source_path,
                    snapshot_path,
                })
                .collect();
            let snapshot_id = if callback.as_raw().is_null() {
                create_snapshot(&repository_path, &password, &source_paths, &tags)
                    .map_err(NativeError::from)?
            } else {
                let vm = env.get_java_vm()?;
                // Keep the callback accessible from Rustic worker threads.
                let callback = env.new_global_ref(&callback)?;
                let callback = JniProgressCallback::new(env, vm, callback)?;
                create_snapshot_with_progress(
                    &repository_path,
                    &password,
                    &source_paths,
                    &tags,
                    callback,
                )
                .map_err(NativeError::from)?
            };

            env.new_string(snapshot_id).map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeRestoreSnapshot<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
    snapshot_id: JString<'local>,
    destination_path: JString<'local>,
    options: JObject<'local>,
) {
    unowned_env
        .with_env(|env| -> Result<(), NativeError> {
            let mut read_bool = |name| env.get_field(&options, name, jni_sig!("Z"))?.z();
            let restore_options = rustic_core::RestoreOptions::default()
                .delete(read_bool(jni_str!("delete"))?)
                .numeric_id(read_bool(jni_str!("numericId"))?)
                .no_ownership(read_bool(jni_str!("noOwnership"))?)
                .verify_existing(read_bool(jni_str!("verifyExisting"))?);
            crate::repository::restore_snapshot_with_options(
                &repository_path.to_string(),
                &password.to_string(),
                &snapshot_id.to_string(),
                &destination_path.to_string(),
                &restore_options,
            )
            .map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeReadSnapshotDirectoryUid<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
    snapshot_id: JString<'local>,
) -> jni::sys::jint {
    unowned_env
        .with_env(|_env| -> Result<i32, NativeError> {
            crate::repository::read_snapshot_directory_uid(
                &repository_path.to_string(),
                &password.to_string(),
                &snapshot_id.to_string(),
            )
            .map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeListSnapshots<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> Result<JString<'local>, NativeError> {
            let snapshots = list_snapshots(&repository_path.to_string(), &password.to_string())
                .map_err(NativeError::from)?;
            env.new_string(snapshots).map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeCheckRepository<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
) {
    unowned_env
        .with_env(|_env| -> Result<(), NativeError> {
            check_repository(&repository_path.to_string(), &password.to_string())
                .map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

fn string_array_to_vec<'local>(
    env: &mut jni::Env<'local>,
    array: &JObjectArray<'local, JString<'local>>,
) -> Result<Vec<String>, NativeError> {
    (0..array.len(env)?)
        .map(|index| {
            let value: JString<'local> = array.get_element(env, index)?;
            Ok(value.to_string())
        })
        .collect()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeReadSnapshotTextFiles<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
    snapshot_id: JString<'local>,
    paths: JObjectArray<'local, JString<'local>>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> Result<JString<'local>, NativeError> {
            let paths = string_array_to_vec(env, &paths)?;
            let files = read_snapshot_text_files(
                &repository_path.to_string(),
                &password.to_string(),
                &snapshot_id.to_string(),
                &paths,
            )
            .map_err(NativeError::from)?;
            env.new_string(files).map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeDeleteSnapshot<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
    snapshot_id: JString<'local>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> Result<JString<'local>, NativeError> {
            let snapshots = delete_snapshot(
                &repository_path.to_string(),
                &password.to_string(),
                &snapshot_id.to_string(),
            )
            .map_err(NativeError::from)?;
            env.new_string(snapshots).map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

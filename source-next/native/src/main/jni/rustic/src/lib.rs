#![deny(improper_ctypes_definitions)]

use std::error::Error;

mod error;
mod jni_bridge;
mod repository;

pub type Result<T> = std::result::Result<T, Box<dyn Error>>;

pub use repository::{check_repository, create_snapshot, init_repository, restore_snapshot};

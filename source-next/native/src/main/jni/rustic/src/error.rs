use std::error::Error;

use thiserror::Error;

#[derive(Debug, Error)]
#[error("{0}")]
pub(crate) struct NativeError(String);

impl From<jni::errors::Error> for NativeError {
    fn from(value: jni::errors::Error) -> Self {
        Self(value.to_string())
    }
}

impl From<Box<dyn Error>> for NativeError {
    fn from(value: Box<dyn Error>) -> Self {
        Self(value.to_string())
    }
}

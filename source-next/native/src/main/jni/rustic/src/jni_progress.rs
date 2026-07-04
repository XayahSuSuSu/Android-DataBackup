use jni::ids::JMethodID;
use jni::objects::{JClass, JObject};
use jni::refs::Global;
use jni::signature::{Primitive, ReturnType};
use jni::{Env, JValue, JavaVM, jni_sig, jni_str};
use log::warn;

use crate::progress::RusticProgressCallback;

#[derive(Debug)]
pub(crate) struct JniProgressCallback {
    vm: JavaVM,
    callback: Global<JObject<'static>>,
    // Keeps the callback class loaded for the cached method ID.
    _callback_class: Global<JClass<'static>>,
    on_progress_method: JMethodID,
}

impl JniProgressCallback {
    pub(crate) fn new(
        env: &mut Env<'_>,
        vm: JavaVM,
        callback: Global<JObject<'static>>,
    ) -> jni::errors::Result<Self> {
        let callback_class = env.get_object_class(&callback)?;
        // Resolve once; progress callbacks should not repeat method lookups.
        let on_progress_method =
            env.get_method_id(&callback_class, jni_str!("onProgress"), jni_sig!("(JJF)V"))?;
        let callback_class = env.new_global_ref(&callback_class)?;

        Ok(Self {
            vm,
            callback,
            _callback_class: callback_class,
            on_progress_method,
        })
    }
}

impl RusticProgressCallback for JniProgressCallback {
    fn on_progress(&self, bytes_done: u64, speed: u64, progress: f32) {
        let result: Result<(), jni::errors::Error> = self.vm.attach_current_thread(|env| {
            let args = [
                JValue::Long(bytes_done as i64).as_jni(),
                JValue::Long(speed as i64).as_jni(),
                JValue::Float(progress).as_jni(),
            ];

            // SAFETY: on_progress_method is resolved once from callback's class
            // with the exact `(JJF)V` signature, and the class is held globally.
            unsafe {
                env.call_method_unchecked(
                    &self.callback,
                    self.on_progress_method,
                    ReturnType::Primitive(Primitive::Void),
                    &args,
                )?;
            }
            Ok(())
        });

        if let Err(err) = result {
            warn!("Failed to invoke rustic progress callback: {err}");
        }
    }
}

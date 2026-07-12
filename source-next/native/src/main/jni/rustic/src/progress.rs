use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};

use rustic_core::{Progress, ProgressBars, ProgressType, RusticProgress};

pub(crate) const PROGRESS_CALLBACK_INTERVAL: Duration = Duration::from_secs(1);

// Repository code depends on this trait, not on any JNI-specific callback type.
pub trait RusticProgressCallback: Send + Sync + 'static + std::fmt::Debug {
    fn on_progress(&self, bytes_done: u64, speed: u64, progress: f32);
}

#[derive(Debug)]
pub(crate) struct AndroidProgressBars {
    callback: Arc<dyn RusticProgressCallback>,
}

impl AndroidProgressBars {
    pub(crate) fn new<C: RusticProgressCallback>(callback: C) -> Self {
        Self {
            callback: Arc::new(callback),
        }
    }
}

impl ProgressBars for AndroidProgressBars {
    fn progress(&self, progress_type: ProgressType, _prefix: &str) -> Progress {
        match progress_type {
            // Rustic reports byte progress for backup/restore data flow; other UI
            // progress types are not meaningful for the Android callback.
            ProgressType::Bytes => Progress::new(AndroidProgress::new(self.callback.clone())),
            ProgressType::Spinner | ProgressType::Counter => Progress::hidden(),
        }
    }
}

#[derive(Debug)]
struct AndroidProgress {
    callback: Arc<dyn RusticProgressCallback>,
    state: Mutex<ThrottledProgressState>,
}

impl AndroidProgress {
    fn new(callback: Arc<dyn RusticProgressCallback>) -> Self {
        Self {
            callback,
            state: Mutex::new(ThrottledProgressState::new(Instant::now())),
        }
    }

    fn emit(&self, bytes_done: u64, speed: u64, progress: f32) {
        self.callback.on_progress(bytes_done, speed, progress);
    }
}

impl RusticProgress for AndroidProgress {
    fn is_hidden(&self) -> bool {
        false
    }

    fn set_length(&self, len: u64) {
        self.state.lock().unwrap().set_length(len);
    }

    fn set_title(&self, _title: &str) {}

    fn inc(&self, inc: u64) {
        let now = Instant::now();
        let event = self.state.lock().unwrap().advance(inc, now, false);
        if let Some(event) = event {
            self.emit(event.bytes_done, event.speed, event.progress);
        }
    }

    fn finish(&self) {
        let now = Instant::now();
        // Always finish with the average speed across the complete transfer.
        let event = self.state.lock().unwrap().advance(0, now, true);
        if let Some(event) = event {
            self.emit(event.bytes_done, event.speed, event.progress);
        }
    }
}

#[derive(Debug)]
struct ProgressEvent {
    bytes_done: u64,
    speed: u64,
    progress: f32,
}

#[derive(Debug)]
struct ThrottledProgressState {
    // Totals are kept here so each rustic Progress instance can calculate speed
    // without coupling the callback implementation to rustic internals.
    bytes_done: u64,
    length: Option<u64>,
    started_at: Instant,
    last_emit_bytes: u64,
    last_emit_at: Option<Instant>,
    finished: bool,
}

impl ThrottledProgressState {
    fn new(started_at: Instant) -> Self {
        Self {
            bytes_done: 0,
            length: None,
            started_at,
            last_emit_bytes: 0,
            last_emit_at: None,
            finished: false,
        }
    }

    fn set_length(&mut self, len: u64) {
        self.length = (len > 0).then_some(len);
    }

    fn advance(&mut self, bytes: u64, now: Instant, finish: bool) -> Option<ProgressEvent> {
        if self.finished {
            return None;
        }

        self.bytes_done = self.bytes_done.saturating_add(bytes);
        if finish {
            self.finished = true;
        }

        // Rustic may call inc frequently; keep Java callbacks coarse-grained.
        let should_emit = finish
            || self.last_emit_at.is_none_or(|last_emit_at| {
                now.duration_since(last_emit_at) >= PROGRESS_CALLBACK_INTERVAL
            });

        if !should_emit
            || self.bytes_done == 0
            || (!finish && self.bytes_done == self.last_emit_bytes)
        {
            return None;
        }

        let speed = if finish {
            bytes_per_second(self.bytes_done, now.duration_since(self.started_at))
        } else {
            let (bytes_since_last_event, speed_since) = self
                .last_emit_at
                .map_or((self.bytes_done, self.started_at), |last_emit_at| {
                    (self.bytes_done - self.last_emit_bytes, last_emit_at)
                });
            bytes_per_second(bytes_since_last_event, now.duration_since(speed_since))
        };

        self.last_emit_at = Some(now);
        self.last_emit_bytes = self.bytes_done;

        Some(ProgressEvent {
            bytes_done: self.bytes_done,
            speed,
            progress: self.progress(),
        })
    }

    fn progress(&self) -> f32 {
        self.length
            .map(|length| (self.bytes_done as f32 / length as f32).clamp(0.0, 1.0))
            .unwrap_or(0.0)
    }
}

fn bytes_per_second(bytes: u64, elapsed: Duration) -> u64 {
    let seconds = elapsed.as_secs_f64();
    if seconds <= 0.0 {
        0
    } else {
        (bytes as f64 / seconds).round() as u64
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn progress_is_throttled_to_one_second() {
        let start = Instant::now();
        let mut state = ThrottledProgressState::new(start);

        assert_eq!(state.advance(1024, start, false).unwrap().bytes_done, 1024);
        assert!(
            state
                .advance(1024, start + Duration::from_millis(999), false)
                .is_none()
        );

        let event = state
            .advance(1024, start + Duration::from_secs(1), false)
            .unwrap();

        assert_eq!(event.bytes_done, 3072);
        assert_eq!(event.speed, 2048);
    }

    #[test]
    fn finish_reports_average_speed_for_all_bytes() {
        let start = Instant::now();
        let mut state = ThrottledProgressState::new(start);

        assert_eq!(state.advance(1024, start, false).unwrap().bytes_done, 1024);
        let event = state
            .advance(1024, start + Duration::from_millis(250), true)
            .unwrap();

        assert_eq!(event.bytes_done, 2048);
        assert_eq!(event.speed, 8192);
    }

    #[test]
    fn finish_reports_average_when_no_bytes_were_added_since_last_event() {
        let start = Instant::now();
        let mut state = ThrottledProgressState::new(start);

        assert!(state.advance(1024, start, false).is_some());
        let event = state
            .advance(0, start + Duration::from_millis(250), true)
            .unwrap();

        assert_eq!(event.bytes_done, 1024);
        assert_eq!(event.speed, 4096);
    }

    #[test]
    fn first_progress_event_uses_start_time_for_speed() {
        let start = Instant::now();
        let mut state = ThrottledProgressState::new(start);

        let event = state
            .advance(1024, start + Duration::from_millis(250), false)
            .unwrap();

        assert_eq!(event.bytes_done, 1024);
        assert_eq!(event.speed, 4096);
    }

    #[test]
    fn progress_event_reports_fraction_when_length_is_known() {
        let start = Instant::now();
        let mut state = ThrottledProgressState::new(start);

        state.set_length(4096);

        let first = state.advance(1024, start, false).unwrap();
        let second = state
            .advance(2048, start + Duration::from_secs(1), false)
            .unwrap();

        assert_eq!(first.progress, 0.25);
        assert_eq!(second.progress, 0.75);
    }
}

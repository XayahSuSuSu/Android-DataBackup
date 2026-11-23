package com.xayah.databackup.entity.backup

/**
 * Zstandard presets for Archive app parts. Restore speed is nearly identical for every preset.
 * Long-distance matching windows stay within Zstandard's default 128 MiB decoder limit, so older
 * builds can still extract archives created with any preset. Keep enum names stable: they are stored in settings.
 *
 * Multithreaded long-distance matching keeps the whole window in memory for each stream, roughly 2–4 times the
 * window size per concurrently archived app, so [Small] archives one app at a time.
 *
 * @property level Zstandard compression level.
 * @property longWindowLog Window size (log2) for long-distance matching, or 0 to disable it.
 * @property canRunInParallel Whether several apps may be archived concurrently with this preset.
 */
enum class ArchiveCompression(val level: Int, val longWindowLog: Int, val canRunInParallel: Boolean) {
    Fast(level = 1, longWindowLog = 0, canRunInParallel = true),
    Balanced(level = 3, longWindowLog = 25, canRunInParallel = true),
    Small(level = 9, longWindowLog = 26, canRunInParallel = false),
}

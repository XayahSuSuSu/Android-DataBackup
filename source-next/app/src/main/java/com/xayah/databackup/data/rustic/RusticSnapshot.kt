package com.xayah.databackup.data.rustic

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

private val FULL_SNAPSHOT_ID_PATTERN = Regex("[0-9a-fA-F]{64}")

/**
 * Requires a full 64-character hexadecimal snapshot ID.
 *
 * @throws IllegalArgumentException if [snapshotId] is not a full snapshot ID.
 */
internal fun requireFullSnapshotId(snapshotId: String) {
    require(snapshotId.matches(FULL_SNAPSHOT_ID_PATTERN)) { "A full snapshot ID is required" }
}

@JsonClass(generateAdapter = true)
data class RusticSnapshot(
    val id: String = "",
    @Json(name = "created_at") val createdAt: Long = 0,
    val time: String = "",
    @Json(name = "program_version") val programVersion: String = "",
    val parent: String? = null,
    val parents: List<String> = emptyList(),
    val tree: String = "",
    val label: String = "",
    val paths: List<String> = emptyList(),
    val hostname: String = "",
    val username: String = "",
    val uid: Long = 0,
    val gid: Long = 0,
    val tags: List<String> = emptyList(),
    val original: String? = null,
    val delete: Any? = null,
    val summary: RusticSnapshotSummary? = null,
    val description: String? = null,
) {
    companion object {
        private const val BACKUP_TAG = "databackup"
        private const val CONFIG_TAG_PREFIX = "$BACKUP_TAG:config:"

        fun createBackupTags(configUuid: String): List<String> = listOf(BACKUP_TAG, "$CONFIG_TAG_PREFIX$configUuid")
    }
}

@JsonClass(generateAdapter = true)
data class RusticSnapshotSummary(
    @Json(name = "files_new") val filesNew: Long = 0,
    @Json(name = "files_changed") val filesChanged: Long = 0,
    @Json(name = "files_unmodified") val filesUnmodified: Long = 0,
    @Json(name = "total_files_processed") val totalFilesProcessed: Long = 0,
    @Json(name = "total_bytes_processed") val totalBytesProcessed: Long = 0,
    @Json(name = "dirs_new") val dirsNew: Long = 0,
    @Json(name = "dirs_changed") val dirsChanged: Long = 0,
    @Json(name = "dirs_unmodified") val dirsUnmodified: Long = 0,
    @Json(name = "total_dirs_processed") val totalDirsProcessed: Long = 0,
    @Json(name = "total_dirsize_processed") val totalDirsizeProcessed: Long = 0,
    @Json(name = "data_blobs") val dataBlobs: Long = 0,
    @Json(name = "tree_blobs") val treeBlobs: Long = 0,
    @Json(name = "data_added") val dataAdded: Long = 0,
    @Json(name = "data_added_packed") val dataAddedPacked: Long = 0,
    @Json(name = "data_added_files") val dataAddedFiles: Long = 0,
    @Json(name = "data_added_files_packed") val dataAddedFilesPacked: Long = 0,
    @Json(name = "data_added_trees") val dataAddedTrees: Long = 0,
    @Json(name = "data_added_trees_packed") val dataAddedTreesPacked: Long = 0,
    val command: String = "",
    @Json(name = "backup_start") val backupStart: String = "",
    @Json(name = "backup_end") val backupEnd: String = "",
    @Json(name = "backup_duration") val backupDuration: Double = 0.0,
    @Json(name = "total_duration") val totalDuration: Double = 0.0,
)

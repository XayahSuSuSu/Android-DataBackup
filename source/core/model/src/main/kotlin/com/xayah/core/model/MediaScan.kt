package com.xayah.core.model

enum class MediaKind {
    Images,
    Videos,
    Audio,
}

data class ScannedMediaFile(
    val path: String,
    val name: String,
    val kind: MediaKind,
)

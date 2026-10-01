package com.xayah.databackup.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubAsset(
    @SerialName("id")
    val id: Long = 0L,
    @SerialName("name")
    val name: String = "",
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("size")
    val size: Long = 0L,
    @SerialName("download_count")
    val downloadCount: Long = 0L,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = "",
)

@Serializable
data class GitHubRelease(
    @SerialName("id")
    val id: Long = 0L,
    @SerialName("tag_name")
    val tagName: String = "",
    @SerialName("name")
    val name: String? = null,
    @SerialName("body")
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String = "",
    @SerialName("published_at")
    val publishedAt: String? = null,
    @SerialName("draft")
    val draft: Boolean = false,
    @SerialName("prerelease")
    val prerelease: Boolean = false,
    @SerialName("assets")
    val assets: List<GitHubAsset> = emptyList(),
)

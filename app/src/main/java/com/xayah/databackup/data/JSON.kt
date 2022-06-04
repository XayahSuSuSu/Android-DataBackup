package com.xayah.databackup.data

data class Release(
    val html_url: String,
    val name: String,
    val assets: List<Asset>,
    val body: String
)

data class Asset(
    val browser_download_url: String
)

data class Issue(
    val html_url: String,
    val title: String,
    val body: String
)
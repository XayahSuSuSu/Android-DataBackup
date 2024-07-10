package com.xayah.core.network.retrofit

import com.xayah.core.network.model.Release
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton

private const val BASE_URL = "https://api.github.com/repos/XayahSuSuSu/Android-DataBackup/"

private interface Api {
    @GET(value = "releases")
    suspend fun getReleases(): List<Release>

    @GET(value = "releases/latest")
    suspend fun getLatestRelease(): Release
}

@Singleton
class GitHubRepository @Inject constructor() {
    private val service = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(Api::class.java)

    suspend fun getReleases(): List<Release> = service.getReleases()
    suspend fun getLatestRelease(): Release = service.getLatestRelease()
}
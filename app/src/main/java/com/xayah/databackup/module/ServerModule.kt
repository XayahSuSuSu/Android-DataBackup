package com.xayah.databackup.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import okhttp3.OkHttpClient

@Module
@InstallIn(ViewModelComponent::class)
object ServerModule {
    @Provides
    @GitHub
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()
}

package com.xayah.core.service.packages.backup.impl

import android.content.Context
import android.content.Intent
import com.xayah.core.service.packages.backup.ProcessingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LocalProcessingImpl @Inject constructor() : ProcessingService() {
    @Inject
    @ApplicationContext
    override lateinit var context: Context

    override val intent by lazy { Intent(context, LocalBackupImpl::class.java) }
}

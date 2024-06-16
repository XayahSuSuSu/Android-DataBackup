package com.xayah.core.service.medium.restore

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ProcessingServiceLocalImpl @Inject constructor() : ProcessingService() {
    @Inject
    @ApplicationContext
    override lateinit var context: Context

    override val intent by lazy { Intent(context, RestoreServiceLocalImpl::class.java) }
}

package com.xayah.databackup

import android.app.Application
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.feature.backup.BackupSetupViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

class App : Application() {
    companion object {
        lateinit var application: Application
    }

    private val appModule = module {
        singleOf(::BackupConfigRepository) bind BackupConfigRepository::class
        viewModelOf(::BackupSetupViewModel)
    }

    override fun onCreate() {
        super.onCreate()
        application = this

        startKoin {
            androidLogger()
            androidContext(application)
            modules(appModule)
        }
    }
}

package com.xayah.databackup

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.xayah.databackup.data.AppRepository
import com.xayah.databackup.data.ArchiveBackupProcessRepository
import com.xayah.databackup.data.ArchiveRepository
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.BackupSelectionRepository
import com.xayah.databackup.data.CallLogRepository
import com.xayah.databackup.data.ContactRepository
import com.xayah.databackup.data.FileRepository
import com.xayah.databackup.data.GitHubReleaseRepository
import com.xayah.databackup.data.MessageRepository
import com.xayah.databackup.data.NetworkRepository
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.data.RusticBackupProcessRepository
import com.xayah.databackup.data.RusticRepository
import com.xayah.databackup.data.TranslatorRepository
import com.xayah.databackup.feature.about.TranslatorsViewModel
import com.xayah.databackup.feature.backup.BackupConfigViewModel
import com.xayah.databackup.feature.backup.BackupLibraryViewModel
import com.xayah.databackup.feature.backup.BackupSetupViewModel
import com.xayah.databackup.feature.backup.NewBackupViewModel
import com.xayah.databackup.feature.backup.apps.AppsViewModel
import com.xayah.databackup.feature.backup.archive.BackupProcessViewModel
import com.xayah.databackup.feature.backup.call_logs.CallLogsViewModel
import com.xayah.databackup.feature.backup.contacts.ContactsViewModel
import com.xayah.databackup.feature.backup.messages.MessagesViewModel
import com.xayah.databackup.feature.backup.networks.NetworksViewModel
import com.xayah.databackup.feature.backup.rustic.RusticBackupProcessViewModel
import com.xayah.databackup.feature.dashboard.DashboardViewModel
import com.xayah.databackup.feature.restore.RestoreProcessViewModel
import com.xayah.databackup.feature.restore.RestoreSetupViewModel
import com.xayah.databackup.feature.restore.RestoreViewModel
import com.xayah.databackup.feature.update.UpdatesViewModel
import com.xayah.databackup.service.backup.BackupAppSourceHelper
import com.xayah.databackup.service.backup.BackupSerializationHelper
import com.xayah.databackup.service.backup.archive.BackupAppsHelper
import com.xayah.databackup.service.backup.archive.BackupCallLogsHelper
import com.xayah.databackup.service.backup.archive.BackupContactsHelper
import com.xayah.databackup.service.backup.archive.BackupMessagesHelper
import com.xayah.databackup.service.backup.archive.BackupNetworksHelper
import com.xayah.databackup.service.backup.rustic.RusticBackupSourceHelper
import com.xayah.databackup.service.restore.archive.ArchiveRestoreHelper
import com.xayah.databackup.service.restore.rustic.RusticRestoreHelper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.xayah.databackup.feature.restore.apps.AppsViewModel as RestoreAppsViewModel
import com.xayah.databackup.feature.restore.call_logs.CallLogsViewModel as RestoreCallLogsViewModel
import com.xayah.databackup.feature.restore.contacts.ContactsViewModel as RestoreContactsViewModel
import com.xayah.databackup.feature.restore.messages.MessagesViewModel as RestoreMessagesViewModel
import com.xayah.databackup.feature.restore.networks.NetworksViewModel as RestoreNetworksViewModel

class App : Application(), SingletonImageLoader.Factory {
    companion object {
        lateinit var application: Application
    }

    private val mAppModule = module {
        factory { RestoreRepository(get(), get(), get()) }

        singleOf(::BackupConfigRepository)
        singleOf(::AppRepository)
        singleOf(::FileRepository)
        singleOf(::NetworkRepository)
        singleOf(::ContactRepository)
        singleOf(::CallLogRepository)
        singleOf(::MessageRepository)
        singleOf(::ArchiveBackupProcessRepository)
        singleOf(::GitHubReleaseRepository)
        singleOf(::TranslatorRepository)
        singleOf(::BackupAppsHelper)
        singleOf(::BackupNetworksHelper)
        singleOf(::BackupContactsHelper)
        singleOf(::BackupCallLogsHelper)
        singleOf(::BackupMessagesHelper)
        singleOf(::BackupAppSourceHelper)
        singleOf(::BackupSerializationHelper)
        singleOf(::RusticRepository)
        singleOf(::BackupSelectionRepository)
        singleOf(::RusticBackupSourceHelper)
        singleOf(::RusticBackupProcessRepository)

        single { RestoreProcessRepository(get<RusticRestoreHelper>(), get<ArchiveRestoreHelper>()) }

        singleOf(::RusticRestoreHelper)
        singleOf(::ArchiveRestoreHelper)
        singleOf(::ArchiveRepository)

        viewModelOf(::DashboardViewModel)
        viewModelOf(::BackupSetupViewModel)
        viewModelOf(::BackupLibraryViewModel)
        viewModelOf(::NewBackupViewModel)
        viewModelOf(::BackupProcessViewModel)
        viewModelOf(::RusticBackupProcessViewModel)
        viewModelOf(::BackupConfigViewModel)
        viewModelOf(::RestoreViewModel)
        viewModelOf(::RestoreAppsViewModel)
        viewModelOf(::RestoreNetworksViewModel)
        viewModelOf(::RestoreContactsViewModel)
        viewModelOf(::RestoreCallLogsViewModel)
        viewModelOf(::RestoreMessagesViewModel)
        viewModelOf(::RestoreSetupViewModel)
        viewModelOf(::RestoreProcessViewModel)
        viewModelOf(::AppsViewModel)
        viewModelOf(::NetworksViewModel)
        viewModelOf(::ContactsViewModel)
        viewModelOf(::CallLogsViewModel)
        viewModelOf(::MessagesViewModel)
        viewModelOf(::UpdatesViewModel)
        viewModelOf(::TranslatorsViewModel)
    }

    override fun onCreate() {
        super.onCreate()
        application = this

        startKoin {
            androidLogger()
            androidContext(application)
            modules(mAppModule)
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(HttpClient(CIO)))
            }
            .build()
    }
}

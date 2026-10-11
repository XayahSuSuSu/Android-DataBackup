package com.xayah.databackup

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.xayah.databackup.data.AppRepository
import com.xayah.databackup.data.ArchiveRepository
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.data.BackupSelectionRepository
import com.xayah.databackup.data.CallLogRepository
import com.xayah.databackup.data.ContactRepository
import com.xayah.databackup.data.FileRepository
import com.xayah.databackup.data.GitHubReleaseRepository
import com.xayah.databackup.data.MessageRepository
import com.xayah.databackup.data.NetworkRepository
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.data.RusticRepository
import com.xayah.databackup.data.TaskRepository
import com.xayah.databackup.data.TranslatorRepository
import com.xayah.databackup.feature.about.TranslatorsViewModel
import com.xayah.databackup.feature.backup.BackupConfigViewModel
import com.xayah.databackup.feature.backup.BackupLibraryViewModel
import com.xayah.databackup.feature.backup.BackupProcessViewModel
import com.xayah.databackup.feature.backup.BackupSetupViewModel
import com.xayah.databackup.feature.backup.NewBackupViewModel
import com.xayah.databackup.feature.backup.apps.AppsViewModel
import com.xayah.databackup.feature.backup.call_logs.CallLogsViewModel
import com.xayah.databackup.feature.backup.contacts.ContactsViewModel
import com.xayah.databackup.feature.backup.messages.MessagesViewModel
import com.xayah.databackup.feature.backup.networks.NetworksViewModel
import com.xayah.databackup.feature.dashboard.DashboardViewModel
import com.xayah.databackup.feature.restore.RestoreProcessViewModel
import com.xayah.databackup.feature.restore.RestoreSetupViewModel
import com.xayah.databackup.feature.restore.RestoreViewModel
import com.xayah.databackup.feature.update.UpdatesViewModel
import com.xayah.databackup.service.backup.BackupAppSourceHelper
import com.xayah.databackup.service.backup.BackupMetadataHelper
import com.xayah.databackup.service.backup.BackupMmsSourceHelper
import com.xayah.databackup.service.backup.BackupSerializationHelper
import com.xayah.databackup.service.backup.archive.ArchiveBackupAppsHelper
import com.xayah.databackup.service.backup.archive.ArchiveBackupHelper
import com.xayah.databackup.service.backup.rustic.RusticBackupHelper
import com.xayah.databackup.service.backup.rustic.RusticBackupSourceHelper
import com.xayah.databackup.service.restore.archive.ArchiveRestoreHelper
import com.xayah.databackup.service.restore.rustic.RusticRestoreHelper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
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
        // Shared data repositories
        singleOf(::AppRepository)
        singleOf(::FileRepository)
        singleOf(::NetworkRepository)
        singleOf(::ContactRepository)
        singleOf(::CallLogRepository)
        singleOf(::MessageRepository)
        singleOf(::ArchiveRepository)
        singleOf(::RusticRepository)
        singleOf(::GitHubReleaseRepository)
        singleOf(::TranslatorRepository)

        // Backup configuration and selection
        singleOf(::BackupConfigRepository)
        singleOf(::BackupSelectionRepository)

        // Shared backup operations
        singleOf(::BackupAppSourceHelper)
        singleOf(::BackupMmsSourceHelper)
        singleOf(::BackupSerializationHelper)
        singleOf(::BackupMetadataHelper)

        singleOf(::TaskRepository)

        // Backup backends and execution
        singleOf(::ArchiveBackupAppsHelper)
        singleOf(::ArchiveBackupHelper)
        singleOf(::RusticBackupSourceHelper)
        singleOf(::RusticBackupHelper)
        single { BackupProcessRepository(get<ArchiveBackupHelper>(), get<RusticBackupHelper>(), get(), get()) }

        // Restore backends and execution
        singleOf(::ArchiveRestoreHelper)
        singleOf(::RusticRestoreHelper)
        single { RestoreProcessRepository(get<RusticRestoreHelper>(), get<ArchiveRestoreHelper>(), get()) }
        // Each restore navigation entry owns its selection and pending request.
        factory { RestoreRepository(get(), get(), get()) }

        // Backup screens
        viewModelOf(::BackupLibraryViewModel)
        viewModelOf(::NewBackupViewModel)
        viewModelOf(::BackupConfigViewModel)
        viewModelOf(::BackupSetupViewModel)
        viewModelOf(::AppsViewModel)
        viewModelOf(::NetworksViewModel)
        viewModelOf(::ContactsViewModel)
        viewModelOf(::CallLogsViewModel)
        viewModelOf(::MessagesViewModel)
        viewModel { parameters -> BackupProcessViewModel(get(), parameters.get()) }

        // Restore screens
        viewModelOf(::RestoreViewModel)
        viewModelOf(::RestoreAppsViewModel)
        viewModelOf(::RestoreNetworksViewModel)
        viewModelOf(::RestoreContactsViewModel)
        viewModelOf(::RestoreCallLogsViewModel)
        viewModelOf(::RestoreMessagesViewModel)
        viewModelOf(::RestoreSetupViewModel)
        viewModel { parameters -> RestoreProcessViewModel(get(), parameters.get()) }

        // Dashboard, updates and about screens
        viewModelOf(::DashboardViewModel)
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

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(HttpClient(CIO)))
            }
            .build()
    }
}

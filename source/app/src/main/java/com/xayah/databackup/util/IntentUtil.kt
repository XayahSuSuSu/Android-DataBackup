package com.xayah.databackup.util

import android.content.Context
import android.content.Intent
import com.xayah.databackup.ui.activity.directory.DirectoryActivity
import com.xayah.databackup.ui.activity.directory.router.DirectoryRoutes
import com.xayah.databackup.ui.activity.operation.OperationActivity
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes

object IntentUtil {
    const val ExtraRoute = "ExtraRoute"

    fun toOperationActivity(context: Context, route: OperationRoutes) {
        context.startActivity(Intent(context, OperationActivity::class.java).also { intent ->
            intent.putExtra(ExtraRoute, route.route)
        })
    }

    fun toDirectoryActivity(context: Context, route: DirectoryRoutes) {
        context.startActivity(Intent(context, DirectoryActivity::class.java).also { intent ->
            intent.putExtra(ExtraRoute, route.route)
        })
    }
}
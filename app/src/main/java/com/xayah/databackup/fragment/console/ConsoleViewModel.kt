package com.xayah.databackup.fragment.console

import android.app.Activity
import android.os.Environment
import androidx.lifecycle.ViewModel
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.topjohnwu.superuser.internal.Utils.context


class ConsoleViewModel : ViewModel() {
    var isInitialized = false

    lateinit var mTermuxTerminalSessionClient: TermuxTerminalSessionClient

    lateinit var mTermuxTerminalViewClient: TermuxTerminalViewClient

    lateinit var session: TerminalSession

    fun initialize(terminalView: TerminalView, activity: Activity) {
        mTermuxTerminalSessionClient = TermuxTerminalSessionClient(terminalView)
        mTermuxTerminalViewClient =
            TermuxTerminalViewClient(activity, mTermuxTerminalSessionClient)
        if (!isInitialized) {
            val shellEnvironmentClient = TermuxShellEnvironmentClient()
            val environment: Array<String> = shellEnvironmentClient.buildEnvironment(
                context, true, Environment.getExternalStorageDirectory().path
            )

            session = TerminalSession(
                "/system/bin/sh", Environment.getExternalStorageDirectory().path,
                arrayOf("/system/bin/sh"),
                environment, 10,
                mTermuxTerminalSessionClient
            )
            session.initializeEmulator(10, 10)
            isInitialized = true
        } else {
            session.updateTerminalSessionClient(mTermuxTerminalSessionClient)
        }
        mTermuxTerminalViewClient.setSoftKeyboardState()
    }
}
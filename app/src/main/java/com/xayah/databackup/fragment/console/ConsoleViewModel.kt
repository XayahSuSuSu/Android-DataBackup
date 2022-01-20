package com.xayah.databackup.fragment.console

import android.app.Activity
import android.os.Environment
import androidx.lifecycle.ViewModel
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.topjohnwu.superuser.Shell

class ConsoleViewModel : ViewModel() {
    var isInitialized = false

    var pid = -1

    lateinit var mTermuxTerminalSessionClient: TermuxTerminalSessionClient

    lateinit var mTermuxTerminalViewClient: TermuxTerminalViewClient

    lateinit var session: TerminalSession

    var onProcessCompletedListener: () -> Unit = {}

    fun initialize(terminalView: TerminalView?, activity: Activity) {
        mTermuxTerminalSessionClient = TermuxTerminalSessionClient(terminalView)
        mTermuxTerminalViewClient =
            TermuxTerminalViewClient(activity, mTermuxTerminalSessionClient)
        if (!isInitialized) {
            val shellEnvironmentClient = TermuxShellEnvironmentClient()
            val environment: Array<String> = shellEnvironmentClient.buildEnvironment(
                activity.applicationContext, true, Environment.getExternalStorageDirectory().path
            )
            val environmentList = environment.toMutableList()
            environmentList.add("APP_ENV=1")

            session = TerminalSession(
                "/system/bin/su", Environment.getExternalStorageDirectory().path,
                arrayOf("/system/bin/su"),
                environmentList.toTypedArray(), 24,
                mTermuxTerminalSessionClient
            )
            session.initializeEmulator(80, 24)
            isInitialized = true
            Shell.su("kill $pid").exec()
            pid = session.pid
            session.setOnProcessCompletedListener { onProcessCompletedListener() }
        } else {
            session.updateTerminalSessionClient(mTermuxTerminalSessionClient)
        }
        if (terminalView != null)
            mTermuxTerminalViewClient.setSoftKeyboardState()
    }

    fun write(data: String) {
        session.write(data + "\r")
    }
}
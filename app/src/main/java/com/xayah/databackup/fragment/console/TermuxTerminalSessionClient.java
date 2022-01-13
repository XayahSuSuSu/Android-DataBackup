package com.xayah.databackup.fragment.console;

import com.termux.shared.terminal.TermuxTerminalSessionClientBase;
import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalView;

public class TermuxTerminalSessionClient extends TermuxTerminalSessionClientBase {

    final TerminalView mTerminalView;

    public TermuxTerminalSessionClient(TerminalView terminalView) {
        this.mTerminalView = terminalView;
    }

    @Override
    public void onTextChanged(TerminalSession changedSession) {
        if (mTerminalView != null)
            if (mTerminalView.getCurrentSession() == changedSession)
                mTerminalView.onScreenUpdated();
    }

}

package com.xayah.databackup.fragment.console;

import android.app.Activity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.termux.shared.logger.Logger;
import com.termux.shared.terminal.TermuxTerminalViewClientBase;
import com.termux.shared.view.KeyboardUtils;
import com.termux.terminal.KeyHandler;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

public class TermuxTerminalViewClient extends TermuxTerminalViewClientBase {

    private static final String LOG_TAG = "TermuxTerminalViewClient";
    final Activity mActivity;
    final TermuxTerminalSessionClient mTermuxTerminalSessionClient;
    boolean mVirtualFnKeyDown;
    private Runnable mShowSoftKeyboardRunnable;

    public TermuxTerminalViewClient(Activity activity, TermuxTerminalSessionClient termuxTerminalSessionClient) {
        this.mActivity = activity;
        this.mTermuxTerminalSessionClient = termuxTerminalSessionClient;
    }

    @Override
    public boolean onCodePoint(final int codePoint, boolean ctrlDown, TerminalSession session) {
        if (mVirtualFnKeyDown) {
            int resultingKeyCode = -1;
            int resultingCodePoint = -1;
            boolean altDown = false;
            int lowerCase = Character.toLowerCase(codePoint);
            switch (lowerCase) {
                // Arrow keys.
                case 'w':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_UP;
                    break;
                case 'a':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_LEFT;
                    break;
                case 's':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_DOWN;
                    break;
                case 'd':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
                    break;

                // Page up and down.
                case 'p':
                    resultingKeyCode = KeyEvent.KEYCODE_PAGE_UP;
                    break;
                case 'n':
                    resultingKeyCode = KeyEvent.KEYCODE_PAGE_DOWN;
                    break;

                // Some special keys:
                case 't':
                    resultingKeyCode = KeyEvent.KEYCODE_TAB;
                    break;
                case 'i':
                    resultingKeyCode = KeyEvent.KEYCODE_INSERT;
                    break;
                case 'h':
                    resultingCodePoint = '~';
                    break;

                // Special characters to input.
                case 'u':
                    resultingCodePoint = '_';
                    break;
                case 'l':
                    resultingCodePoint = '|';
                    break;

                // Function keys.
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    resultingKeyCode = (codePoint - '1') + KeyEvent.KEYCODE_F1;
                    break;
                case '0':
                    resultingKeyCode = KeyEvent.KEYCODE_F10;
                    break;

                // Other special keys.
                case 'e':
                    resultingCodePoint = /*Escape*/ 27;
                    break;
                case '.':
                    resultingCodePoint = /*^.*/ 28;
                    break;

                case 'b': // alt+b, jumping backward in readline.
                case 'f': // alf+f, jumping forward in readline.
                case 'x': // alt+x, common in emacs.
                    resultingCodePoint = lowerCase;
                    altDown = true;
                    break;

                // Volume control.
                case 'v':
                    resultingCodePoint = -1;
                    break;

                // Writing mode:
                case 'q':
                case 'k':
                    mVirtualFnKeyDown = false; // force disable fn key down to restore keyboard input into terminal view, fixes termux/termux-app#1420
                    break;
            }

            if (resultingKeyCode != -1) {
                TerminalEmulator term = session.getEmulator();
                session.write(KeyHandler.getCode(resultingKeyCode, 0, term.isCursorKeysApplicationMode(), term.isKeypadApplicationMode()));
            } else if (resultingCodePoint != -1) {
                session.writeCodePoint(altDown, resultingCodePoint);
            }
            return true;
        } else if (ctrlDown) {
            return codePoint == 106 /* Ctrl+j or \n */ && !session.isRunning();
        }

        return false;
    }

    public void setSoftKeyboardState() {
        // Set flag to automatically push up TerminalView when keyboard is opened instead of showing over it
        KeyboardUtils.setSoftInputModeAdjustResize(mActivity);
        // Clear any previous flags to disable soft keyboard in case setting updated
        KeyboardUtils.clearDisableSoftKeyboardFlags(mActivity);

        // Request focus for TerminalView
        // Also show the keyboard, since onFocusChange will not be called if TerminalView already
        // had focus on startup to show the keyboard, like when opening url with context menu
        // "Select URL" long press and returning to Termux app with back button. This
        // will also show keyboard even if it was closed before opening url. #2111
        Logger.logVerbose(LOG_TAG, "Requesting TerminalView focus and showing soft keyboard");
        mTermuxTerminalSessionClient.mTerminalView.requestFocus();
        mTermuxTerminalSessionClient.mTerminalView.postDelayed(getShowSoftKeyboardRunnable(), 300);
    }

    private Runnable getShowSoftKeyboardRunnable() {
        if (mShowSoftKeyboardRunnable == null) {
            mShowSoftKeyboardRunnable = () -> KeyboardUtils.showSoftKeyboard(mActivity, mTermuxTerminalSessionClient.mTerminalView);
        }
        return mShowSoftKeyboardRunnable;
    }

    @Override
    public void onSingleTapUp(MotionEvent e) {
        TerminalEmulator term = mTermuxTerminalSessionClient.mTerminalView.mEmulator;

        if (!term.isMouseTrackingActive() && !e.isFromSource(InputDevice.SOURCE_MOUSE)) {
            if (!KeyboardUtils.areDisableSoftKeyboardFlagsSet(mActivity))
                KeyboardUtils.showSoftKeyboard(mActivity, mTermuxTerminalSessionClient.mTerminalView);
            else
                Logger.logVerbose(LOG_TAG, "Not showing soft keyboard onSingleTapUp since its disabled");
        }
    }
}

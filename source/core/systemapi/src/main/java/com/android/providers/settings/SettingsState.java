package com.android.providers.settings;

public interface SettingsState {
    static final String SYSTEM_PACKAGE_NAME = "android";
    static final int MAX_BYTES_PER_APP_PACKAGE_UNLIMITED = -1;

    static final int SETTINGS_TYPE_GLOBAL = 0;
    static final int SETTINGS_TYPE_SYSTEM = 1;
    static final int SETTINGS_TYPE_SECURE = 2;
    static final int SETTINGS_TYPE_SSAID = 3;
    static final int SETTINGS_TYPE_CONFIG = 4;

    static final int SETTINGS_TYPE_MASK = 0xF0000000;
    static final int SETTINGS_TYPE_SHIFT = 28;

    static int makeKey(int type, int userId) {
        return (type << SETTINGS_TYPE_SHIFT) | userId;
    }

    Setting getSettingLocked(String name);

    boolean insertSettingLocked(String name, String value, String tag, boolean makeDefault, String packageName);

    interface Setting {
        String getValue();
    }
}

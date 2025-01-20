<div align="center">
	<span style="font-weight: bold"> <a> English </a> </span>
</div>

# Usage
```
unset LD_LIBRARY_PATH
export CLASSPATH="/path_to_dex/classes.dex"
app_process /system/bin com.xayah.dex.CCUtil help
app_process /system/bin com.xayah.dex.HiddenApiUtil help
app_process /system/bin com.xayah.dex.HttpUtil help
app_process /system/bin com.xayah.dex.SsaidUtil help
app_process /system/bin com.xayah.dex.NotificationUtil help
```

# Commands
```
CCUtil commands:
  help

  s2t TEXT

  t2s TEXT
```

```
HiddenApiUtil commands:
  help

  getPackageUid USER_ID PACKAGE PACKAGE PACKAGE ...

  getPackageLabel USER_ID PACKAGE PACKAGE PACKAGE ...

  getPackageArchiveInfo APK_FILE

  getInstalledPackagesAsUser USER_ID FILTER_FLAG(user|system|xposed) FORMAT(label|pkgName|flag)

  getRuntimePermissions USER_ID PACKAGE

  grantRuntimePermission USER_ID PACKAGE PERM_NAME PERM_NAME PERM_NAME ...

  revokeRuntimePermission USER_ID PACKAGE PERM_NAME PERM_NAME PERM_NAME ...

  setOpsMode USER_ID PACKAGE OP MODE OP MODE OP MODE ...

  setDisplayPowerMode MODE(POWER_MODE_OFF: 0, POWER_MODE_NORMAL: 2)
```

```
HttpUtil commands:
  help

  get URL
```

```
Ssaid commands:
  help
    Print this help text.

  get USER_ID PACKAGE
    Get ssaid.

  set USER_ID PACKAGE SSAID
    Set ssaid.
```

```
NotificationUtil commands:
  help

  notify [flags] <tag> <text>


flags:
  -t|--title <text>
  -p|--progress <max> <progress> <indeterminate>

examples:
  notify -t 'This is title' 'This is tag' 'This is content.'
  notify -p 100 50 false -t 'This is title' 'This is tag' 'This is content.'
  notify -p 0 0 true -t 'This is title' 'This is tag' 'This is content.'
```

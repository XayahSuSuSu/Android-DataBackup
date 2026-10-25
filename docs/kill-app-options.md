# Killing app options

`Settings -> Backup settings -> Killing app options` decides what happens to an app right before
its data is read (backup) or written (restore). The very same list is shown under
`Settings -> Restore settings`, and both pages read and write the same preference, so a single
choice applies to backups and restores alike.

* Preference key: `kill_app_option` in DataStore (`KeyKillAppOption`), holding the name of
  `com.xayah.core.model.KillAppOption`.
* Read once per task in `AbstractBackupService.onProcessing()` / `AbstractRestoreService.onProcessing()`,
  then applied per package with `AbstractPackagesService.killApp()`.
* When option III is active, the suspension is lifted again by `releaseApp()` once the package has
  been processed.

| Option | Enum | Mechanism | Runs as |
| --- | --- | --- | --- |
| Disabled | `DISABLED` | Nothing | – |
| Option One | `OPTION_I` | Root shell retry loop (`killall` / `am force-stop` / `am kill`) | root shell |
| Option II (default) | `OPTION_II` | One `IActivityManager.forceStopPackageAsUser()` call | root service (Binder) |
| Advanced pause | `OPTION_III` | `am force-stop` followed by `pm suspend` | root shell |

## Disabled

Only a log line is written (`Won't kill <package>.`). The app keeps running while its data is
copied, which is the fastest option and the only one that leaves the app untouched.

## Option One

Implemented in `BaseUtil.killPackage()`. A new root shell (libsu) is opened, the global mount
namespace is entered (`nsenter --mount=/proc/1/ns/mnt sh`), and this loop is executed:

```sh
until [[ $(dumpsys activity processes | grep "packageList" | cut -d '{' -f2 | cut -d '}' -f1 \
        | egrep -w "$packageName" | sed -n '1p') = "" ]]; do
    killall -9 "$packageName" &>/dev/null
    am force-stop --user "$userId" "$packageName" &>/dev/null
    am kill "$packageName" &>/dev/null
done
```

* `dumpsys activity processes` lists the running processes of the package; the loop keeps going
  until the package no longer appears in it.
* `killall -9` sends `SIGKILL` to every process whose name matches the package name.
* `am force-stop` asks ActivityManager to stop the package, `am kill` kills its background processes.
* The whole job is given 3 seconds (`timeout = 3`); when it expires, the command is closed and the
  backup continues anyway.
* Because it is a shell loop, the package can be started again by a broadcast or a job in the middle
  of the loop, which is why it has to be retried. This is the option that most often leaves the app
  running while its data is copied.

## Option II (default)

Implemented in `AbstractPackagesService.killApp()` together with
`RemoteRootService.forceStopPackageAsUser()`:

```kotlin
mRootService.forceStopPackageAsUser(pkg.packageEntity.packageName, pkg.packageEntity.userId)
```

* The call crosses Binder into the root process (`:root`, running as uid 0) and ends up in the
  hidden `ActivityManager.forceStopPackageAsUser(packageName, userId)`.
* It is a single call: ActivityManager force-stops every process of the package, marks the package
  as stopped and cancels its alarms/jobs, so nothing restarts it until the user launches it again.
* No shell, no retry loop, no 3 second budget – this is why it is the default.

## Option III — Advanced pause

Both mechanisms above only *stop* the app. An app with a foreground service, an `AlarmManager`
alarm, a `JobScheduler` job or a push channel can come back while its files are being archived.
Option III additionally **suspends** the package, which the platform honours for every start
request, and the launcher shows the icon greyed out.

Implemented in `SuspendUtil.suspendPackage()` + `BaseUtil.suspendPackage()` (and the matching
`resumePackage()` / `resumeAll()`):

```sh
am force-stop --user "$userId" "$packageName"
pm suspend  --user "$userId" "$packageName"
```

```sh
pm unsuspend --user "$userId" "$packageName"
```

* `pm suspend` requires `android.permission.SUSPEND_APPS`, which is only granted to root/shell, so
  the command is executed through the root shell (`libsu`).
* While suspended, the package cannot be started by the launcher, by broadcasts, by services, by
  jobs or by other apps; its icon is greyed out and its notifications are suppressed. Reading its
  files keeps working, which is exactly what a backup needs.
* The suspension is lifted by `releaseApp()` in a `finally` block right after that app has been
  processed, so it lasts for the duration of **one app**, not the whole task.
* Every suspended package is written to `files/suspended_packages.txt` before the command is issued.
  At the beginning of a task `releaseApps()` calls `pm unsuspend` for every entry of that file, so a
  task that was killed, crashed or interrupted by a reboot cannot leave an app suspended forever.
* **System apps are never suspended.** For a package with `PackageEntity.isSystemApp == true` the
  option falls back to a plain force-stop (`forceStopPackageAsUser`), because suspending
  `com.android.systemui`, the launcher or another core package can take the device down.
  The app never suspends itself either.

### Verifying the state by hand

```sh
adb shell dumpsys package <package> | grep -i suspended   # suspended=true means paused
adb shell pm unsuspend --user 0 <package>                 # lift it manually
```

`pm suspend` exists since Android 7.0 (API 24), which matches the app's `minSdk`.

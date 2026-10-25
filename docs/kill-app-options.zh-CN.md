# 杀死应用选项（实现说明）

`设置 -> 备份设置 -> 杀死应用选项` 决定每个应用在读取数据（备份）或写入数据（恢复）之前被如何处理。
同样的选项也会出现在 `设置 -> 恢复设置` 中，两个页面读写的是同一个偏好项，因此选择一次即对备份与恢复同时生效。

* 偏好项：DataStore 中的 `kill_app_option`（`KeyKillAppOption`），值为
  `com.xayah.core.model.KillAppOption` 的枚举名。
* 每个任务开始时读取一次：`AbstractBackupService.onProcessing()` /
  `AbstractRestoreService.onProcessing()`，随后按应用逐个调用 `AbstractPackagesService.killApp()`。
* 选中「高级暂停」时，应用处理结束后由 `releaseApp()` 解除挂起。

| 选项 | 枚举 | 实现方式 | 执行身份 |
| --- | --- | --- | --- |
| 禁用 | `DISABLED` | 不做任何处理 | – |
| 方案I | `OPTION_I` | root shell 循环重试（`killall` / `am force-stop` / `am kill`） | root shell |
| 方案II（默认） | `OPTION_II` | 调用一次 `IActivityManager.forceStopPackageAsUser()` | root 服务（Binder）|
| 高级暂停 | `OPTION_III` | 先 `am force-stop`，再 `pm suspend` | root shell |

## 禁用

只写一行日志（`Won't kill <包名>.`），应用在数据复制期间保持运行。速度最快，也是唯一完全不干扰应用的选项。

## 方案I

实现在 `BaseUtil.killPackage()`。先通过 libsu 新开一个 root shell，切入全局挂载命名空间
（`nsenter --mount=/proc/1/ns/mnt sh`），再执行下面的循环：

```sh
until [[ $(dumpsys activity processes | grep "packageList" | cut -d '{' -f2 | cut -d '}' -f1 \
        | egrep -w "$packageName" | sed -n '1p') = "" ]]; do
    killall -9 "$packageName" &>/dev/null
    am force-stop --user "$userId" "$packageName" &>/dev/null
    am kill "$packageName" &>/dev/null
done
```

* 用 `dumpsys activity processes` 判断该包是否还在进程列表中，只要还在就一直循环。
* `killall -9` 向所有同名的进程发送 `SIGKILL`。
* `am force-stop` 请求 ActivityManager 停止该包，`am kill` 杀掉其后台进程。
* 整体给 3 秒（`timeout = 3`），超时后关闭命令、照常继续备份。
* 因为只是一个 shell 循环，循环过程中应用仍可能被广播或任务重新拉起，所以才需要反复重试。
  这也是最容易出现「备份时应用仍在运行」的选项。

## 方案II（默认）

实现在 `AbstractPackagesService.killApp()` 与 `RemoteRootService.forceStopPackageAsUser()`：

```kotlin
mRootService.forceStopPackageAsUser(pkg.packageEntity.packageName, pkg.packageEntity.userId)
```

* 经由 Binder 进入 root 进程（`:root`，uid 0），最终调用隐藏 API
  `ActivityManager.forceStopPackageAsUser(packageName, userId)`。
* 只有一次调用：ActivityManager 停止该包的全部进程、把包标记为 stopped，并取消其闹钟与任务，
  在用户主动点开之前不会被重新拉起。
* 不需要 shell、没有重试循环、也没有 3 秒限制，因此是默认值。

## 高级暂停（方案III）

前两个方案只是「停掉」应用。带有前台服务、`AlarmManager` 闹钟、`JobScheduler` 任务或推送通道的应用，
完全可以在文件被打包的过程中重新起来。方案III 在此基础上把包**挂起（suspend）**，
系统会拦截所有启动请求，同时桌面图标变灰。

实现在 `SuspendUtil.suspendPackage()` + `BaseUtil.suspendPackage()`（以及配套的
`resumePackage()` / `resumeAll()`）：

```sh
am force-stop --user "$userId" "$packageName"
pm suspend  --user "$userId" "$packageName"
```

```sh
pm unsuspend --user "$userId" "$packageName"
```

* `pm suspend` 需要 `android.permission.SUSPEND_APPS`，该权限只授予 root/shell，因此命令通过 root shell 执行。
* 挂起期间：桌面、广播、服务、任务栏、其他应用都无法拉起它；图标变灰、通知被抑制；
  但读取其文件不受影响，这正是备份需要的效果。
* 解除挂起放在 `finally` 里的 `releaseApp()`，即该应用处理完立即解除——作用范围是**单个应用**，
  而不是整个任务。
* 挂起前会先把 `uid:包名` 写入 `files/suspended_packages.txt`；任务开始时 `releaseApps()` 会对文件中
  的每一项执行 `pm unsuspend`。因此即使任务被强杀、崩溃或中途重启，也不会让某个应用永远处于挂起状态。
* **系统应用绝不挂起。** `PackageEntity.isSystemApp == true` 的包会退化为直接强制停止
  （`forceStopPackageAsUser`）——挂起 `com.android.systemui`、桌面或其它核心包可能直接让设备不可用。
  应用本身也不会挂起自己。

### 手动验证状态

```sh
adb shell dumpsys package <包名> | grep -i suspended   # suspended=true 表示处于挂起
adb shell pm unsuspend --user 0 <包名>                 # 手动解除
```

`pm suspend` 自 Android 7.0（API 24）起提供，与本应用的 `minSdk` 一致。

# 杀死应用选项（实现说明）

`设置 -> 备份设置 -> 杀死应用选项` 决定每个应用在读取数据（备份）或写入数据（恢复）之前被如何处理。
同样的选项也会出现在 `设置 -> 恢复设置` 中，两个页面读写的是同一个偏好项，因此选择一次即对备份与恢复同时生效。

* 偏好项：DataStore 中的 `kill_app_option`（`KeyKillAppOption`），值为
  `com.xayah.core.model.KillAppOption` 的枚举名。
* 每个任务开始时读取一次：`AbstractBackupService.onProcessing()` /
  `AbstractRestoreService.onProcessing()`，随后按应用逐个调用 `AbstractPackagesService.killApp()`。
* 选中「高级暂停」时，应用处理结束后由 `releaseApp()` 解除挂起。
* 停止/挂起应用都只是任务里的**尽力而为的辅助步骤**：任何一步失败（包括用户在此期间尝试打开应用）
  只写日志，不会中止任务，也不会把该应用判定为备份失败。详见
  [备份过程中尝试打开被备份的应用](#备份过程中尝试打开被备份的应用)。

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

## 备份过程中尝试打开被备份的应用

目标语义是两条**同时**成立：备份期间被备份的应用打不开；而用户「尝试打开」这个动作本身
**不影响备份结果**。实现上分三层保障。

### 1. 打不开由挂起保证

`pm suspend` 之后，任何启动请求都会被系统拒绝——`am start` 返回
`Activity not started`，桌面点击无反应，图标变灰。这一点在模拟器（android-33 x86_64）上做过对照验证：

| 场景 | 一次 `am start` 的结果 | 应用是否写入启动记录 |
| --- | --- | --- |
| 未挂起 | `pid: 20094`，进程拉起 | 写入 |
| 已挂起 | `pid: (none)`，进程未创建 | 不写入 |

### 2. 「尝试打开」不导致失败：`tar` 退出码 1 视为可接受

挂起之前，或者挂起被系统自动解除的瞬间，应用可能已经在运行。这时读它的数据会出现
「读的时候文件变了」，`tar` 会以退出码 **1** 结束（`file changed as we read it`）。
归档本身仍然完整可读，但在旧逻辑里 `ShellResult.isSuccess` 只认 0，于是整包被判失败。

`PackagesBackupUtil.isAcceptableTarResult()` 把判定改为：

| 退出码 | 含义 | 处理 |
| --- | --- | --- |
| 0 | 正常结束 | 通过 |
| 1 | 有文件在读取过程中变化/消失 | **通过**，以 `testArchive()` 的解压校验为准 |
| 2 | 致命错误 | 失败 |
| 其它 / 缺少结果 / 超时 | 命令异常 | 失败 |

也就是说，真正决定成败的是 `testArchive()` 能否正常解压并列出归档内容，而不是「有没有人动过文件」。

### 3. 处理中途被拉起：`reinforcePause()` 重新挂起

`killApp()` 只在每个应用开始时执行一次，而读数据分成 APK、用户数据（DE）、应用数据等多趟。
为了不留下「APK 读完了、数据开始读的时候应用已经起来了」这个窗口，读完前两趟之后会再挂起一次：

```
backup(APK) -> reinforcePause() -> backup(USER) -> backup(USER_DE) -> reinforcePause() -> backup(DATA) -> ...
```

恢复同理：安装完 APK 之后系统可能把应用拉起来，因此在写 `PACKAGE_DATA` 之前也调一次
`reinforcePause()`。只有在「高级暂停」且非系统应用时才会真正执行，重复执行是安全的
（`pm suspend` 对已挂起的包再次执行不会报错）。

### 4. 任何一步出错都不拖垮任务

`killApp()` / `releaseApp()` / `releaseApps()` / `SuspendUtil` / `BaseUtil.suspendPackage()`
全部用 `runCatching` 包住，失败只写一条日志：

```
Failed to pause <包名>: ...
Failed to re-apply the pause of <包名>: ...
```

`SuspendUtil` 内部跑在 `Dispatchers.IO + NonCancellable` 上，所以即使任务被取消，
该解除的挂起仍然会被解除，不会把应用留在灰掉的状态。


# ZhApiServer

RK3576/RK3576S **Android 14 系统 API** 常驻服务，对应《RK3576 Android14 系统API介绍 v1.0.1》，Binder 接口统一 **zh** 前缀。

与 **ZhBroadcastServer**（广播）互补：应用 `bindService` 后同步调用，适合产测/设置类程序。

| 项 | 值 |
|----|-----|
| 包名 | `com.zhkj.zhapiserver` |
| 进程 UID | `android.uid.system`（平台签名 priv-app） |
| AIDL | `aidl/com/zhkj/zhapi/IZhApiService.aidl` |
| 实现入口 | `ZhApiServiceImpl.java` |

---

## 快速用法

```java
Intent intent = new Intent("com.zhkj.zhapi.ZHAPI_SERVER");
intent.setPackage("com.zhkj.zhapiserver");
bindService(intent, new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mApi = IZhApiService.Stub.asInterface(service);
        // mApi.zhGetAndroidDeviceModel();
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
        mApi = null;
    }
}, Context.BIND_AUTO_CREATE);
```

**客户端集成**

1. Manifest 声明：`<uses-permission android:name="com.zhkj.zhapi.permission.BIND_ZHAPI" />`
2. 拷贝 `aidl/com/zhkj/zhapi/IZhApiService.aidl` 到应用工程，或依赖 Soong 模块 `zh.zhapi-java`
3. Gradle：`buildFeatures { aidl true }`
4. **须与设备端 transaction 编号一致**：stub 从 `FIRST_CALL_TRANSACTION + 0` 起编（与 Android 标准 aidl 相同）。请统一使用 `zh.zhapi-java` 或同一份 `.aidl` + 重新生成 stub 后的 `IZhApiService.java`，否则会出现返回值错位（如型号/版本为 null）。

**服务注册**

| 项 | 值 |
|----|-----|
| Action | `com.zhkj.zhapi.ZHAPI_SERVER` |
| 绑定权限 | `com.zhkj.zhapi.permission.BIND_ZHAPI`（**normal**） |

普通 App 在 Manifest 声明权限即可绑定（安装时自动授予）：

```xml
<uses-permission android:name="com.zhkj.zhapi.permission.BIND_ZHAPI" />
```

> 注意：normal 权限下任意已安装 App 均可调用关机、静默安装、GPIO 等板级接口，仅适合封闭设备/内网场景。

开机 **BOOT_COMPLETED** → `BootReceiver` → `startService`；应用也可 `bindService` 拉起。

**日志**

```bash
adb logcat -s ZhApiServiceImpl ZhApiService ZhSilentInstall ZhAutoBoot ZhNodeIO ZhGpioCtrl ZhPwmCtrl
```

---

## AIDL 接口一览

### 系统信息

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `zhGetAndroidDeviceModel()` | String | `Build.MODEL` |
| `zhGetAndroidVersion()` | String | `Build.VERSION.RELEASE`（如 `14`） |
| `zhGetKernelVersion()` | String | `ro.kernel.version`，空则读 `/proc/version` |
| `getSerialNumber()` | String | `Build.getSerial()`，失败回退 `ro.serialno` |
| `zhGetFirmwareVersion()` | String | `ro.build.display.id` |
| `zhGetBuildDate()` | String | 编译时间字符串 |
| `zhGetRAMSize()` | String | 总内存，如 `4.0G` / `512M` |
| `zhGetInternalStorageMemory()` | String | 内部存储总容量 |
| `zhGetAvailableInternalMemorySize()` | String | 内部存储可用容量 |
| `zhGetOsVersion()` | String | `ro.build.version.incremental` |

### 电源 / 显示

| 方法 | 参数 | 说明 |
|------|------|------|
| `zhShutDown()` | 无 | 关机 |
| `zhReboot()` | 无 | 重启 |
| `zhSetLCDOn()` | 无 | 开背光，写 `bl_power=0` |
| `zhSetLCDOff()` | 无 | 关背光，写 `bl_power=1` |
| `zhGetBacklightPwmPeriod()` | — | 读背光 PWM 周期（ns） |
| `zhGetBacklightPwmPolarity()` | — | 读背光 PWM 极性 `0`/`1` |
| `zhSetBacklightPwmPeriod(periodNs)` | 周期 ns | 写 `pwm_period_ns`，并持久化到 lcdparam |
| `zhSetBacklightPwmPolarity(polarity)` | `0`/`1` | 写 `pwm_polarity`，并持久化 |
| `zhSetBacklightPwmConfig(periodNs, polarity)` | 周期 + 极性 | 一次写齐 |
| `zhTakeScreenshot(path, name)` | 保存目录、文件名 | 调用 `/system/bin/screencap -p` |
| `zhGetScreenWidth()` | — | 屏幕宽度 px |
| `zhGetScreenHeight()` | — | 屏幕高度 px |
| `zhSetNavigationBarVisibility(enable)` | true=显示 | 发 SystemUI 广播 |
| `zhSetNavigationBarCanSwap(enable)` | true=上滑显示导航栏 | 广播 + `persist.sys.swipeupnavigationbar.enable` |
| `zhSetStatusBarVisibility(enable)` | true=显示 | 发 SystemUI 广播 |

背光节点：

| 路径 | 说明 |
|------|------|
| `/sys/class/backlight/backlight/bl_power` | `0`=亮，`1`=灭 |
| `/sys/class/backlight/backlight/pwm_period_ns` | 背光 PWM 周期（ns） |
| `/sys/class/backlight/backlight/pwm_polarity` | 背光 PWM 极性 |

> 背光 PWM ≠ 板级通用 PWM（`pwm0`）。亮度仍用系统亮度 / `zhSetLCDOn/Off`；`zhSetBacklightPwm*` 只调背光控制器的 period/极性。

### 应用 / 存储

| 方法 | 参数 | 说明 |
|------|------|------|
| `zhSilentInstallApk(apkPath, openApp)` | APK 路径；是否装完启动 | `pm install -r -g`；`openApp=true` 时监听安装广播后 launch |
| `zhGetInternalSDPath()` | — | `Environment.getExternalStorageDirectory()` |
| `zhGetSDPath()` | — | TF 卡路径，`StorageManager` 可移动卷，多盘 `:` 分隔 |
| `zhGetUSBPath()` | — | U 盘路径，识别 description/path 含 `usb`，多盘 `:` 分隔 |

### 以太网 / WiFi

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zhGetEthMacAddress()` | — | String | `eth0` MAC |
| `zhGetEth1MacAddress()` | — | String | `eth1` MAC |
| `zhGetWifiMacAddress()` | — | String | `wlan0` MAC |
| `zhGetIpAddress()` | — | String | **eth0 IPv4**；EthernetManager → 静态配置 → dhcp 属性 → NetworkInterface |
| `zhSetEthIPAddress(ip, mask, gateway, dns, dns2)` | 静态 IP 五元组 | boolean | `EthernetManager.setConfiguration("eth0", STATIC)` |
| `zhSetEthOnOff(enable)` | true=开 | boolean | `EthernetManager.setEthernetEnabled` |
| `zhSetEthDhcp()` | 无 | void | eth0 切 DHCP |
| `zhGetCurrentNetType()` | — | String | 当前活跃网络：`WIFI` / `ETHERNET` / `4G` / `3G` 等 |
| `zhGetWifiIpAddressInfo()` | — | String | 当前 WiFi 连接 IPv4 |
| `zhSetNetworkEnable(networkType, enable)` | `ConnectivityManager.TYPE_*` | boolean | 开关 WiFi 或以太网 |
| `zhSetWifiDhcpConnect(ssid, security, password)` | 见下表 | boolean | 添加网络并 DHCP 连接 |
| `zhSetWifiStaticConnect(...)` | SSID + security + 密码 + 静态 IP 六元组 | boolean | 静态 IP 写入 `WifiConfiguration` |

**WiFi `security` 参数**

| 值 | 含义 |
|----|------|
| 0 | 开放 |
| 1 | WEP |
| 2 | WPA / WPA2-PSK（默认） |
| 3 | WPA-EAP（802.1x，需额外企业配置） |
| 4 | WPA3-SAE |

### 串口 / 人体感应 / ADB / NTP / OTA

| 方法 | 参数 | 说明 |
|------|------|------|
| `zhGetUartPath(uartNum)` | `uart0`~`uart3` 或设备名 | 映射 `/dev/ttyS0`~`ttyS3` |
| `zhSetHumanSensor(timeSec)` | 超时秒数，0=关 | 写 `Settings.Global`：`zh_human_sensor_timeout`（需上层读此值执行关屏） |
| `zhEnableAdb(enable)` | true=开 | 写 `Settings.Global.ADB_ENABLED` |
| `zhSetNtpTimeServer(server)` | NTP 地址 | 写 `Settings.Global.NTP_SERVER` |
| `zhGetNtpTimeServer()` | — | 读 NTP 服务器 |
| `zhUpdateSystemOs(osPath)` | OTA 包路径 | `RecoverySystem.installPackage` |

### USB0 Type-A

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zhSetUsb0Power(enable)` | true=开 5V | boolean | 发 `com.android.zh_set_usb0_power` 广播 |
| `zhGetUsb0Power()` | — | boolean | 读 `usb0pwren` |
| `zhSetUsb0Mode(mode)` | `host` / `otg` | boolean | 发 `com.android.zh_set_usb0_mode` 广播 |
| `zhGetUsb0Mode()` | — | String | 读 u2phy `otg_mode` |

角色 sysfs：`/sys/devices/platform/2602e000.syscon/2602e000.syscon:usb2-phy@0/otg_mode`（不再经 zhctl 转发）。

Host 插 U 盘：`zhSetUsb0Mode("host")`（广播内会开 5V）。切 OTG：`zhSetUsb0Mode("otg")`（广播内会关 5V）。

### 板级 GPIO（zhctl）

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zhSetGpioCtrl(nodeName, value)` | 节点名、0/1 | boolean | 写 `/sys/devices/platform/zhctl/GPIOctrl/<name>` |
| `zhGetGpioCtrl(nodeName)` | 节点名 | int | 读 GPIO 值 |
| `zhGetGpioCtrlStatus(nodeName)` | 节点名 | String | 读 status 节点 |

**rk3576s_u 已接节点**（以 `rk3576s-tablet-zhctl.dtsi` 为准）

| nodeName | 引脚 | 说明 |
|----------|------|------|
| `gpio1`~`gpio4` | gpio3 PD2 / PD3 / PD6 / PD7 | 扩展 GPIO |
| `usb0pwren` | gpio3 PA5 | USB0 Type-A 5V |
| `usb2pwren` | gpio0 PC3 | USB 电源 |
| `usb3pwren` | gpio0 PD3 | USB 电源 |
| `fanpower` | gpio3 PC2 | 风扇电源 |

`usb5pwren` / `hubreset` / `usbsw` 等在 dtsi 中注释掉的节点，sysfs 不会出现。

### 板级通用 PWM（zhctl PWMctrl）

硬件：`pwm1_6ch_0` / GPIO0_B4（`pwm1m0_ch0`），sysfs 名默认 **`pwm0`**。

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zhSetPwmEnable(name, enable)` | 名（空=pwm0）、true/false | boolean | 使能 |
| `zhGetPwmEnable(name)` | 名 | boolean | 读使能 |
| `zhSetPwmDuty(name, dutyNs)` | 占空比 ns（0..period） | boolean | 写占空比 |
| `zhGetPwmDuty(name)` | 名 | int | 读占空比 ns，失败 `-1` |
| `zhSetPwmPeriod(name, periodNs)` | 周期 ns（>0） | boolean | 写周期；duty 过大时内核侧会压到 period |
| `zhGetPwmPeriod(name)` | 名 | int | 读周期 ns，失败 `-1` |
| `zhSetPwm(name, enable, dutyNs, periodNs)` | 一次写齐 | boolean | `dutyNs`/`periodNs` 为负表示该项不改 |
| `zhGetPwmStatus(name)` | 名 | String | 如 `enable=1 duty=25000 period=50000` |

```text
/sys/devices/platform/zhctl/PWMctrl/pwm0
/sys/devices/platform/zhctl/PWMctrl/pwm0_duty
/sys/devices/platform/zhctl/PWMctrl/pwm0_period
/sys/devices/platform/zhctl/PWMctrl/pwm0_status
```

DTS 默认周期 `50000` ns（20 kHz），占空比 `0`、关闭。与背光 PWM **不是同一路**。

```java
// 20 kHz、50% 占空比并打开
mApi.zhSetPwm("pwm0", true, 25000, 50000);
// 或分步
mApi.zhSetPwmPeriod(null, 50000);
mApi.zhSetPwmDuty(null, 25000);
mApi.zhSetPwmEnable(null, true);
```

---

## 目录结构

| 路径 | 说明 |
|------|------|
| `aidl/.../IZhApiService.aidl` | 对外 API 定义 |
| `stub/java/.../IZhApiService.java` | 预生成 AIDL stub（`gen_izhapi_stub.py`） |
| `service/.../ZhApiService.java` | Service 入口 |
| `service/.../ZhApiServiceImpl.java` | AIDL 实现 |
| `service/.../util/ZhNodeIO.java` | sysfs 读写 |
| `service/.../util/ZhGpioCtrl.java` | zhctl GPIO |
| `service/.../util/ZhPwmCtrl.java` | zhctl PWMctrl |
| `service/.../util/ZhBacklightPwm.java` | 背光 period/polarity |
| `service/.../util/ZhSilentInstall.java` | 静默安装 |
| `service/.../util/ZhAutoBoot.java` | 装完/开机拉起应用 |
| `zhapi.mk` | 编入 `rk3576s_u.mk` 等产品 |

---

## 编译与部署

```bash
# 修改 .aidl 后须重新生成 stub
python3 vendor/zhkj/zhapi/tools/gen_izhapi_stub.py

m zh.zhapi-java ZhApiServer
```

出厂路径：`/system/priv-app/ZhApiServer/ZhApiServer.apk`

### APK 热更新

须 **platform 签名**、**包名不变**、**`versionCode` 递增**。不要用 `android:persistent="true"`。

```bash
adb install -r -d --user 0 ZhApiServer.apk
adb shell dumpsys package com.zhkj.zhapiserver | grep versionCode
```

---

## 与 ZhBroadcast 对照

| 能力 | 广播（zhbroadcast） | AIDL（zhapi） |
|------|---------------------|---------------|
| 背光开关 | `com.android.lcd_bl_on/off` | `zhSetLCDOn()` / `zhSetLCDOff()` |
| 背光 PWM 参数 | `com.android.zh_set_backlight_pwm` | `zhSetBacklightPwm*` |
| 通用 PWM | `com.android.zh_set_pwm` | `zhSetPwm*` / `zhGetPwm*` |
| 以太网 IP 查询 | `replyEthInfo` 广播回复 | `zhGetIpAddress()` |
| 静默安装 | `com.android.zh_slient_install` | `zhSilentInstallApk()` |
| GPIO | 各 action | `zhSetGpioCtrl()` / `zhGetGpioCtrl()` |
| 关机/重启 | `zh_reboot` / `zh_shutdown` | `zhReboot()` / `zhShutDown()` |
广播适合脚本/adb 一次性触发；AIDL 适合应用内频繁同步调用。

---

## 板级说明

### 文档有、本板未接（DTS/zhctl 无节点）

| 文档节点 | 说明 |
|----------|------|
| `di1` / `di2` / `do1` / `do2` | 数字量 IO，本板未上 |
| `lcdblctr` / `lcd0blctr` | 不走 zhctl，用背光 API |
| `lcd1blctr` | 无副屏 |
| `usb6pwren` / `usb7pwren` | 本板未接 |

公版文档路径为 `yhctl`，本方案为 **`zhctl`**：

```text
/sys/devices/platform/zhctl/GPIOctrl/<name>
/sys/devices/platform/zhctl/PWMctrl/pwm0{,_duty,_period,_status}
```

### 文档习惯 vs 当前实现

| 文档/公版习惯 | 当前 zhapi |
|---------------|------------|
| `lcdblctr` 写 GPIO | `zhSetLCDOn()` / `zhSetLCDOff()` → `bl_power` |
| 各 USB 单独 Java 方法 | 统一 `zhSetGpioCtrl("usb3pwren", 1)` |
| `setNodeString` 任意路径 | 未单独暴露；用 `zhSetGpioCtrl` 或应用自读 sysfs |

---

## 已知限制

| 项 | 说明 |
|----|------|
| `zhSetHumanSensor` | 仅写 Settings，无硬件 PIR 驱动联动 |
| `zhTakeScreenshot` | AIDL 无返回值；依赖 screencap 权限与路径可写 |
| WiFi EAP (security=3) | 仅设安全类型，企业证书/账号需额外配置 |
| 导航栏/状态栏 | 依赖 SystemUI 接收广播，Android 14 平板需实机验证 |
| `zhGetIpAddress` | 仅 eth0 IPv4；WiFi 用 `zhGetWifiIpAddressInfo()` |
| 未接 GPIO 节点 | `zhSetGpioCtrl` 写 sysfs 失败 |

---

## 仓库内相关定制

| 项 | 路径 |
|----|------|
| zhctl 内核 | `kernel-6.1/drivers/misc/zhctl/` |
| zhctl DTS | `kernel-6.1/arch/.../rk3576s-tablet-zhctl.dtsi` |
| 系统属性 | `device/rockchip/rk3576/rk3576s_u/zhproperty.mk` |
| PCIe 以太网 | `vendor/rockchip/common/ethernet/`（YT6801，eth0） |
| 广播接口 | `vendor/zhkj/zhbroadcast/` |

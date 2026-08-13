# RK3576 ZhAPI 使用说明

> 适用平台：RK3576 / RK3576S 公版 Android 14  
> 接口前缀：`zh`  
> AIDL 文件：`com/zhkj/zhapi/IZhApiService.aidl`

---

## 1. 概述

ZhAPI 是 RK3576 板级系统能力封装，通过 AIDL 跨进程调用。系统预装服务 **`com.zhkj.zhapiserver`**，第三方 App 绑定后即可调用设备信息、网络、GPIO、PWM、OTA 等接口。

| 项目 | 值 |
|------|-----|
| 服务包名 | `com.zhkj.zhapiserver` |
| 服务类名 | `com.zhkj.zhapi.server.ZhApiService` |
| 绑定 Action | `com.zhkj.zhapi.ZHAPI_SERVER` |
| 绑定权限 | `com.zhkj.zhapi.permission.BIND_ZHAPI`（normal，安装时自动授予） |
| AIDL 包名 | `com.zhkj.zhapi` |
| 接口名 | `IZhApiService` |

---

## 2. 接入步骤

### 2.1 拷贝 AIDL

将 `IZhApiService.aidl` 放到工程：

```
app/src/main/aidl/com/zhkj/zhapi/IZhApiService.aidl
```

`app/build.gradle` 开启 AIDL：

```gradle
android {
    buildFeatures {
        aidl true
    }
}
```

### 2.2 声明权限与可见性

`AndroidManifest.xml`：

```xml
<uses-permission android:name="com.zhkj.zhapi.permission.BIND_ZHAPI" />

<queries>
    <package android:name="com.zhkj.zhapiserver" />
    <intent>
        <action android:name="com.zhkj.zhapi.ZHAPI_SERVER" />
    </intent>
</queries>
```

### 2.3 绑定服务

```java
private static final String ACTION = "com.zhkj.zhapi.ZHAPI_SERVER";
private static final String PKG = "com.zhkj.zhapiserver";
private static final String CLS = "com.zhkj.zhapi.server.ZhApiService";

private IZhApiService zhApiService;

private final ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        zhApiService = IZhApiService.Stub.asInterface(binder);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        zhApiService = null;
    }
};

public void bind(Context context) {
    Intent intent = new Intent(ACTION);
    intent.setComponent(new ComponentName(PKG, CLS));
    context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
}

public void unbind(Context context) {
    context.unbindService(connection);
}
```

### 2.4 权限说明

`BIND_ZHAPI` 为 **normal** 级别（系统侧已放宽限制）。只要在 `AndroidManifest.xml` 中声明：

```xml
<uses-permission android:name="com.zhkj.zhapi.permission.BIND_ZHAPI" />
```

应用通过 `adb install` 或应用商店正常安装后，安装时即自动授予该权限，**无需** platform 签名，**无需**安装为系统 priv-app。

---

## 3. 通用约定

### 3.1 布尔参数

接口中 `boolean enable` 类参数：

| 值 | 含义 |
|----|------|
| `true` / `1` | 开启、启用、高电平 |
| `false` / `0` | 关闭、禁用、低电平 |

GPIO 写接口 `zhSetGpioCtrl` 的 `value` 使用整型：`0` = 低电平，`1` = 高电平。

### 3.2 返回值

- `String`：失败或不可用时可能返回 `null` 或空字符串，调用方需判空。
- `boolean`：设置类接口，`true` 表示成功，`false` 表示失败。
- `int`：屏幕尺寸等，单位为像素。
- `void`：无返回值，异常通过 AIDL `RemoteException` 抛出。

### 3.3 线程

AIDL 调用可能阻塞，**不要在主线程**直接调用，建议在子线程或协程中执行。

---

## 4. 接口明细

### 4.1 系统信息

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `zhGetAndroidDeviceModel()` | String | 设备型号 |
| `zhGetAndroidVersion()` | String | Android 版本号 |
| `zhGetKernelVersion()` | String | 内核版本 |
| `getSerialNumber()` | String | 设备序列号 SN |
| `zhGetFirmwareVersion()` | String | 固件版本 |
| `zhGetBuildDate()` | String | 固件编译日期 |
| `zhGetRAMSize()` | String | RAM 总容量 |
| `zhGetInternalStorageMemory()` | String | 内部存储总容量 |
| `zhGetAvailableInternalMemorySize()` | String | 内部存储可用容量 |
| `zhGetOsVersion()` | String | 系统 OS 版本（厂商自定义） |

**示例：**

```java
String model = zhApiService.zhGetAndroidDeviceModel();
String sn = zhApiService.getSerialNumber();
```

---

### 4.2 电源 / 显示

| 方法 | 参数 | 说明 |
|------|------|------|
| `zhShutDown()` | — | 关机（危险操作） |
| `zhReboot()` | — | 重启（危险操作） |
| `zhSetLCDOn()` | — | 开背光，写 `bl_power=0` |
| `zhSetLCDOff()` | — | 关背光，写 `bl_power=1` |
| `zhTakeScreenshot(path, name)` | 保存目录、文件名 | 调用 `/system/bin/screencap -p` |
| `zhGetScreenWidth()` | — | 屏幕宽度（px） |
| `zhGetScreenHeight()` | — | 屏幕高度（px） |
| `zhSetNavigationBarVisibility(enable)` | true=显示，false=隐藏 | 发 SystemUI 广播 |
| `zhSetNavigationBarCanSwap(enable)` | true=上滑显示导航栏 | 广播 + `persist.sys.swipeupnavigationbar.enable` |
| `zhSetStatusBarVisibility(enable)` | true=显示，false=隐藏 | 发 SystemUI 广播 |

**示例：**

```java
zhApiService.zhSetNavigationBarVisibility(false);  // 隐藏导航栏
zhApiService.zhTakeScreenshot("/sdcard/Pictures", "cap.png");
int w = zhApiService.zhGetScreenWidth();
```

---

### 4.3 应用 / 存储

| 方法 | 参数 | 说明 |
|------|------|------|
| `zhSilentInstallApk(apkPath, openApp)` | APK 绝对路径；openApp：安装后是否启动 | 静默安装（需系统权限） |
| `zhGetInternalSDPath()` | — | 内部存储路径 |
| `zhGetSDPath()` | — | 外置 SD 卡路径 |
| `zhGetUSBPath()` | — | USB 存储路径 |

**示例：**

```java
zhApiService.zhSilentInstallApk("/sdcard/app.apk", false);
String internal = zhApiService.zhGetInternalSDPath();
```

---

### 4.4 以太网 / WiFi

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zhGetEthMacAddress()` | — | String | eth0 MAC 地址 |
| `zhGetEth1MacAddress()` | — | String | eth1 MAC 地址 |
| `zhGetWifiMacAddress()` | — | String | wlan0 MAC 地址 |
| `zhGetIpAddress()` | — | String | **eth0 IPv4**（WiFi 请用 `zhGetWifiIpAddressInfo`） |
| `zhSetEthIPAddress(ip, mask, gateway, dns, dns2)` | IP、掩码、网关、DNS1、DNS2 | boolean | eth0 静态 IP |
| `zhSetEthOnOff(enable)` | true=开，false=关 | boolean | 以太网开关 |
| `zhSetEthDhcp()` | — | void | eth0 切换为 DHCP |
| `zhGetCurrentNetType()` | — | String | 当前活跃网络：`WIFI` / `ETHERNET` / `4G` 等 |
| `zhGetWifiIpAddressInfo()` | — | String | 当前 WiFi 连接 IPv4 |
| `zhSetNetworkEnable(networkType, enable)` | `ConnectivityManager.TYPE_*`；true=启用 | boolean | 按类型开关 WiFi / 以太网 |
| `zhSetWifiDhcpConnect(ssid, security, password)` | SSID、加密类型、密码 | boolean | WiFi DHCP 连接 |
| `zhSetWifiStaticConnect(...)` | SSID、加密类型、密码、IP、网关、掩码、DNS1、DNS2 | boolean | WiFi 静态 IP 连接 |

**networkType 常用值（`ConnectivityManager`）：**

| 值 | 含义 |
|----|------|
| `1` | WiFi（`TYPE_WIFI`） |
| `9` | 以太网（`TYPE_ETHERNET`） |

**security（WiFi 加密类型）：**

| 值 | 含义 |
|----|------|
| `0` | 开放（无加密） |
| `1` | WEP |
| `2` | WPA / WPA2-PSK（默认） |
| `3` | WPA-EAP（802.1x，需额外企业配置） |
| `4` | WPA3-SAE |

**示例：**

```java
String mac = zhApiService.zhGetEthMacAddress();
zhApiService.zhSetEthDhcp();
zhApiService.zhSetEthOnOff(true);

zhApiService.zhSetNetworkEnable(9, true);   // 启用以太网 TYPE_ETHERNET
zhApiService.zhSetNetworkEnable(1, false);  // 关闭 WiFi

zhApiService.zhSetWifiDhcpConnect("MyWiFi", 2, "12345678");
```

---

### 4.5 串口 / 人体感应 / ADB / NTP / OTA

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zhGetUartPath(uartNum)` | `uart0`~`uart3` 或设备名 | String | 映射 `/dev/ttyS0`~`ttyS3` |
| `zhSetHumanSensor(timeSec)` | 超时秒数，0=关 | void | 写 `Settings.Global`：`zh_human_sensor_timeout`（需上层读此值执行关屏） |
| `zhEnableAdb(enable)` | true=开启，false=关闭 | void | 写 `Settings.Global.ADB_ENABLED` |
| `zhSetNtpTimeServer(server)` | NTP 服务器地址 | boolean | 写 `Settings.Global.NTP_SERVER` |
| `zhGetNtpTimeServer()` | — | String | 获取当前 NTP 服务器 |
| `zhUpdateSystemOs(osPath)` | 升级包绝对路径 | void | `RecoverySystem.installPackage`（危险操作） |

**示例：**

```java
String uart = zhApiService.zhGetUartPath("uart0");
zhApiService.zhEnableAdb(true);
zhApiService.zhSetNtpTimeServer("ntp.aliyun.com");
zhApiService.zhUpdateSystemOs("/sdcard/update.zip");
```

---

### 4.6 USB0 Type-A

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zhSetUsb0Power(enable)` | true=开 5V | boolean | 发 `com.android.zh_set_usb0_power` 广播 |
| `zhGetUsb0Power()` | — | boolean | 读 `usb0pwren` |
| `zhSetUsb0Mode(mode)` | `host` / `otg` | boolean | 发 `com.android.zh_set_usb0_mode` 广播 |
| `zhGetUsb0Mode()` | — | String | 读 u2phy `otg_mode` |

角色 sysfs：`/sys/devices/platform/2602e000.syscon/2602e000.syscon:usb2-phy@0/otg_mode`。

Host 插 U 盘：`zhSetUsb0Mode("host")`（广播内会开 5V）。切 OTG：`zhSetUsb0Mode("otg")`（广播内会关 5V）。

**示例：**

```java
zhApiService.zhSetUsb0Mode("host");
boolean on = zhApiService.zhGetUsb0Power();
String mode = zhApiService.zhGetUsb0Mode();
```

---

### 4.7 板级 GPIO（zhctl sysfs）

通过 sysfs 节点控制扩展 IO、USB 电源等。API 中 `nodeName` 传**节点短名**（不含完整路径）。

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zhSetGpioCtrl(nodeName, value)` | 节点名；0 或 1 | boolean | 写 `/sys/devices/platform/zhctl/GPIOctrl/<name>` |
| `zhGetGpioCtrl(nodeName)` | 节点名 | int | 读取当前电平值 |
| `zhGetGpioCtrlStatus(nodeName)` | 节点名 | String | 读取 status 节点 |

**rk3576s_u 已接节点**（以 `rk3576s-tablet-zhctl.dtsi` 为准）：

| nodeName | 引脚 | 说明 |
|----------|------|------|
| `gpio1`~`gpio4` | gpio3 PD2 / PD3 / PD6 / PD7 | 扩展 GPIO |
| `usb0pwren` | gpio3 PA5 | USB0 Type-A 5V |
| `usb2pwren` | gpio0 PC3 | USB 电源 |
| `usb3pwren` | gpio0 PD3 | USB 电源 |
| `fanpower` | gpio3 PC2 | 风扇电源 |

`usb5pwren` / `hubreset` / `usbsw` 等在 dtsi 中注释掉的节点，sysfs 不会出现。

**value 含义：**

| value | 含义 |
|-------|------|
| `0` | 低电平 / 关闭电源 |
| `1` | 高电平 / 打开电源 |

**示例：**

```java
// gpio1 输出高电平
zhApiService.zhSetGpioCtrl("gpio1", 1);

// 关闭 usb3 电源
zhApiService.zhSetGpioCtrl("usb3pwren", 0);

// 读取 gpio1 电平
int level = zhApiService.zhGetGpioCtrl("gpio1");

// 读取 gpio1 状态字符串
String status = zhApiService.zhGetGpioCtrlStatus("gpio1");
```

**等效 adb 命令：**

```bash
# 写
adb shell echo 1 > /sys/devices/platform/zhctl/GPIOctrl/gpio1

# 读
adb shell cat /sys/devices/platform/zhctl/GPIOctrl/gpio1
adb shell cat /sys/devices/platform/zhctl/GPIOctrl/gpio1_status
```

---

### 4.8 板级通用 PWM（zhctl PWMctrl）

硬件：`pwm1_6ch_0` / GPIO0_B4（`pwm1m0_ch0`），sysfs 名默认 **`pwm0`**。

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `zhSetPwmEnable(name, enable)` | 名（空/`null`=pwm0）、true/false | boolean | 使能 |
| `zhGetPwmEnable(name)` | 名 | boolean | 读使能 |
| `zhSetPwmDuty(name, dutyNs)` | 占空比 ns（0..period） | boolean | 写占空比 |
| `zhGetPwmDuty(name)` | 名 | int | 读占空比 ns，失败 `-1` |
| `zhSetPwmPeriod(name, periodNs)` | 周期 ns（>0） | boolean | 写周期；duty 过大时内核侧会压到 period |
| `zhGetPwmPeriod(name)` | 名 | int | 读周期 ns，失败 `-1` |
| `zhSetPwm(name, enable, dutyNs, periodNs)` | 一次写齐 | boolean | `dutyNs`/`periodNs` 为负表示该项不改 |
| `zhGetPwmStatus(name)` | 名 | String | 如 `enable=1 duty=25000 period=50000` |

sysfs 路径：

```text
/sys/devices/platform/zhctl/PWMctrl/pwm0
/sys/devices/platform/zhctl/PWMctrl/pwm0_duty
/sys/devices/platform/zhctl/PWMctrl/pwm0_period
/sys/devices/platform/zhctl/PWMctrl/pwm0_status
```

DTS 默认周期 `50000` ns（20 kHz），占空比 `0`、关闭。

**示例：**

```java
// 20 kHz、50% 占空比并打开
zhApiService.zhSetPwm("pwm0", true, 25000, 50000);

// 或分步（name 传 null / 空字符串等价于 pwm0）
zhApiService.zhSetPwmPeriod(null, 50000);
zhApiService.zhSetPwmDuty(null, 25000);
zhApiService.zhSetPwmEnable(null, true);

String status = zhApiService.zhGetPwmStatus("pwm0");
```

**等效 adb 命令：**

```bash
adb shell cat /sys/devices/platform/zhctl/PWMctrl/pwm0_status
adb shell echo 50000 > /sys/devices/platform/zhctl/PWMctrl/pwm0_period
adb shell echo 25000 > /sys/devices/platform/zhctl/PWMctrl/pwm0_duty
adb shell echo 1 > /sys/devices/platform/zhctl/PWMctrl/pwm0
```

---

## 5. 完整调用示例

```java
public class ZhApiClient {
    private IZhApiService api;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            api = IZhApiService.Stub.asInterface(binder);
            new Thread(() -> {
                try {
                    Log.i("Demo", "Model: " + api.zhGetAndroidDeviceModel());
                    Log.i("Demo", "SN: " + api.getSerialNumber());
                    Log.i("Demo", "Eth MAC: " + api.zhGetEthMacAddress());
                    api.zhSetNavigationBarVisibility(false);
                } catch (RemoteException e) {
                    Log.e("Demo", "call failed", e);
                }
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            api = null;
        }
    };

    public void connect(Context ctx) {
        Intent i = new Intent("com.zhkj.zhapi.ZHAPI_SERVER");
        i.setComponent(new ComponentName(
                "com.zhkj.zhapiserver",
                "com.zhkj.zhapi.server.ZhApiService"));
        ctx.bindService(i, conn, Context.BIND_AUTO_CREATE);
    }
}
```

---

## 6. 常见问题

### Q1：绑定报 `SecurityException: Not allowed to bind to service`

**原因：** Manifest 未声明 `BIND_ZHAPI`，或安装的是旧版未带该权限的 APK。  
**解决：** 确认 Manifest 已声明权限，卸载旧版后重新安装。可执行 `adb shell dumpsys package 你的包名` 检查 `BIND_ZHAPI: granted=true`。

### Q2：`bindService` 返回 false

**原因：** 设备未预装 `com.zhkj.zhapiserver`，或服务未启动。  
**解决：** 确认固件含 ZhApiServer，执行 `adb shell pm path com.zhkj.zhapiserver` 检查。

### Q3：接口返回 null

**原因：** 硬件不存在（如无 SD 卡、无 eth1）、节点不可访问等。  
**解决：** 调用前判空，结合日志排查。

---

## 7. 危险操作提醒

以下接口会直接影响设备运行，调用前务必二次确认：

- `zhShutDown()` — 关机
- `zhReboot()` — 重启
- `zhUpdateSystemOs(osPath)` — 系统 OTA，错误包可能导致变砖
- `zhSilentInstallApk(...)` — 静默安装，请确保 APK 来源可信
- GPIO / USB 电源写操作 — 可能影响外设供电
- 板级 PWM 写操作 — 可能驱动风扇等外设，注意 period/duty 范围

---

## 8. 测试 App

本项目 `zhapitest` 已集成全部接口的可视化测试，编译安装方式：

```powershell
.\gradlew.bat assembleDebug
.\scripts\install_debug.ps1
```

或手动安装：

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

安装完成后在设备桌面打开 **「ZhAPI 测试」** 即可逐项验证。

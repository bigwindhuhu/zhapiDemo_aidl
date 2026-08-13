# ZhAPI Demo (AIDL)

[中文](#中文) | [English](#english)

RK3576 / RK3576S Android 14 板级系统 API（ZhAPI）的 **AIDL 对接示例与测试 App**。  
Demo / test app for the board-level **ZhAPI** system service on RK3576 / RK3576S (Android 14) via AIDL.

仓库地址 / Repository: https://github.com/bigwindhuhu/zhapiDemo_aidl

---

<a id="中文"></a>

## 中文

### 1. 项目简介

本工程是面向客户的 **ZhAPI 接入 Demo**，演示如何通过 AIDL 绑定系统预装服务 `com.zhkj.zhapiserver`，并调用设备信息、电源/显示、网络、GPIO、PWM、USB、OTA 等板级能力。

| 项目 | 说明 |
|------|------|
| 适用平台 | RK3576 / RK3576S 公版 Android 14 |
| Demo 包名 | `com.example.zhapitest` |
| 系统服务包名 | `com.zhkj.zhapiserver` |
| 服务类名 | `com.zhkj.zhapi.server.ZhApiService` |
| 绑定 Action | `com.zhkj.zhapi.ZHAPI_SERVER` |
| 绑定权限 | `com.zhkj.zhapi.permission.BIND_ZHAPI`（normal，安装时自动授予） |
| AIDL 接口 | `com.zhkj.zhapi.IZhApiService` |
| 接口前缀 | `zh` |

> **前提**：设备固件中需已预装 ZhAPI 系统服务。普通应用无需 platform 签名、无需 priv-app。

更完整的接口说明见：[`docs/ZhAPI使用说明.md`](docs/ZhAPI使用说明.md)

---

### 2. 功能一览

Demo App 按分类提供可点击测试项，覆盖：

| 分类 | 能力示例 |
|------|----------|
| 系统信息 | 型号、Android/内核版本、SN、固件版本、RAM/存储、OS 版本 |
| 电源 / 显示 | 开关机、LCD 背光、截屏、分辨率、导航栏/状态栏 |
| 应用 / 存储 | 静默安装 APK、内部存储 / SD / USB 路径 |
| 以太网 / WiFi | MAC、IP、DHCP/静态 IP、网络开关、WiFi 连接 |
| 串口 / 外设 | UART 路径、人体感应、ADB 开关、NTP、系统 OTA |
| USB0 | Type-A 供电与 Host/OTG 模式 |
| GPIO | 扩展 IO、USB 电源、风扇等 sysfs 节点读写 |
| PWM | 使能、占空比、周期配置 |

---

### 3. 工程结构

```
zhapiDemo_aidl/
├── app/
│   ├── src/main/aidl/com/zhkj/zhapi/
│   │   └── IZhApiService.aidl      # ZhAPI AIDL（与设备端保持一致）
│   ├── src/main/java/.../
│   │   ├── MainActivity.java       # 分类测试 UI
│   │   └── ZhApiHelper.java        # 绑定 / 调用封装（可参考复用）
│   └── src/main/AndroidManifest.xml
├── docs/
│   ├── ZhAPI使用说明.md            # 完整接口文档（客户集成必读）
│   └── README.md                   # 服务端侧补充说明
├── scripts/
│   └── install_debug.ps1           # 编译并 adb 安装 Debug APK
└── README.md                       # 本文件
```

---

### 4. 环境要求

- Android Studio (推荐最新稳定版) 或命令行 Gradle
- JDK 17 或更高（推荐使用本机已安装 JDK，配置 `JAVA_HOME`）
- Android SDK（compileSdk / targetSdk 以工程 `app/build.gradle` 为准）
- 已刷入 ZhAPI 服务的 RK3576 / RK3576S 设备，并通过 USB 连接 `adb`

---

### 5. 快速编译与安装

#### Android Studio

1. 用 Android Studio 打开本工程根目录  
2. 同步 Gradle  
3. 连接设备，运行 `app`（Debug）

#### 命令行

```bash
# Windows
gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

生成的 APK：

```
app/build/outputs/apk/debug/app-debug.apk
```

安装：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.zhapitest/.MainActivity
```

Windows 也可使用脚本：

```powershell
.\scripts\install_debug.ps1
```

---

### 6. 客户自有 App 接入步骤

#### 6.1 拷贝 AIDL

将本仓库中的 AIDL 放到您的工程：

```
app/src/main/aidl/com/zhkj/zhapi/IZhApiService.aidl
```

在 `app/build.gradle` 中开启 AIDL：

```gradle
android {
    buildFeatures {
        aidl true
    }
}
```

> **重要**：AIDL 必须与设备端服务使用同一份定义（transaction 编号一致）。请直接使用本仓库提供的 `IZhApiService.aidl`，不要混用不同版本的 stub。

#### 6.2 Manifest 权限与可见性

```xml
<uses-permission android:name="com.zhkj.zhapi.permission.BIND_ZHAPI" />

<queries>
    <package android:name="com.zhkj.zhapiserver" />
    <intent>
        <action android:name="com.zhkj.zhapi.ZHAPI_SERVER" />
    </intent>
</queries>
```

#### 6.3 绑定服务（示例）

可直接参考 `ZhApiHelper.java`，核心代码如下：

```java
Intent intent = new Intent("com.zhkj.zhapi.ZHAPI_SERVER");
intent.setComponent(new ComponentName(
        "com.zhkj.zhapiserver",
        "com.zhkj.zhapi.server.ZhApiService"));
bindService(intent, connection, Context.BIND_AUTO_CREATE);

// onServiceConnected:
IZhApiService api = IZhApiService.Stub.asInterface(binder);
String model = api.zhGetAndroidDeviceModel();
```

#### 6.4 调用注意

- AIDL 调用可能阻塞，**请勿在主线程**直接调用耗时接口  
- `String` 返回值可能为 `null` 或空，请判空  
- 关机、重启、OTA、静默安装等为高风险操作，产测/量产脚本中请加确认逻辑  

---

### 7. 日志排查

```bash
adb logcat -s ZhApiHelper ZhApiServiceImpl ZhApiService ZhSilentInstall ZhAutoBoot ZhNodeIO ZhGpioCtrl ZhPwmCtrl
```

常见问题：

| 现象 | 可能原因 |
|------|----------|
| 绑定失败 | 设备未预装 `com.zhkj.zhapiserver`，或 Manifest 未声明权限/`queries` |
| 返回值错位 / 为 null | AIDL 与设备端版本不一致 |
| 权限相关异常 | 未声明 `BIND_ZHAPI`（normal 权限，声明后安装即授予） |

---

### 8. 文档与支持

| 文档 | 内容 |
|------|------|
| [`docs/ZhAPI使用说明.md`](docs/ZhAPI使用说明.md) | 全量接口参数、返回值、示例与 GPIO/PWM 节点说明 |
| 本 README | Demo 编译、安装与最小接入指引 |

如需定制接口或板级适配，请联系硬件/系统交付方并提供设备型号与固件版本号。

---

<a id="english"></a>

## English

### 1. Overview

This repository is a **customer-facing ZhAPI AIDL demo** for RK3576 / RK3576S (Android 14). It shows how to bind the preinstalled system service `com.zhkj.zhapiserver` and call board-level APIs: device info, power/display, network, GPIO, PWM, USB, OTA, and more.

| Item | Value |
|------|--------|
| Platform | RK3576 / RK3576S reference Android 14 |
| Demo package | `com.example.zhapitest` |
| System service package | `com.zhkj.zhapiserver` |
| Service class | `com.zhkj.zhapi.server.ZhApiService` |
| Bind action | `com.zhkj.zhapi.ZHAPI_SERVER` |
| Bind permission | `com.zhkj.zhapi.permission.BIND_ZHAPI` (normal; granted at install) |
| AIDL interface | `com.zhkj.zhapi.IZhApiService` |
| API prefix | `zh` |

> **Prerequisite**: The device firmware must include the ZhAPI system service. Third-party apps do **not** need platform signing or priv-app installation.

Full API reference (Chinese): [`docs/ZhAPI使用说明.md`](docs/ZhAPI使用说明.md)

---

### 2. Features

The demo app exposes categorized actionable tests:

| Category | Examples |
|----------|----------|
| System info | Model, Android/kernel version, SN, firmware, RAM/storage, OS version |
| Power / display | Shutdown/reboot, LCD backlight, screenshot, resolution, nav/status bar |
| Apps / storage | Silent APK install, internal / SD / USB paths |
| Ethernet / Wi-Fi | MAC, IP, DHCP/static IP, network toggle, Wi-Fi connect |
| UART / misc | UART path, human sensor, ADB, NTP, system OTA |
| USB0 | Type-A power and Host/OTG mode |
| GPIO | Extended IO / USB power / fan via sysfs nodes |
| PWM | Enable, duty cycle, period |

---

### 3. Project Layout

```
zhapiDemo_aidl/
├── app/
│   ├── src/main/aidl/com/zhkj/zhapi/
│   │   └── IZhApiService.aidl      # Keep in sync with device-side service
│   ├── src/main/java/.../
│   │   ├── MainActivity.java       # Categorized test UI
│   │   └── ZhApiHelper.java        # Bind/call helper (reuse as reference)
│   └── src/main/AndroidManifest.xml
├── docs/
│   ├── ZhAPI使用说明.md            # Full API documentation
│   └── README.md                   # Server-side notes
├── scripts/
│   └── install_debug.ps1           # Build + adb install (Windows)
└── README.md                       # This file
```

---

### 4. Requirements

- Android Studio (latest stable recommended) or command-line Gradle
- JDK 17+ (prefer your local JDK via `JAVA_HOME`)
- Android SDK matching `app/build.gradle`
- RK3576 / RK3576S device with ZhAPI service installed, connected via `adb`

---

### 5. Build & Install

#### Android Studio

1. Open the repository root in Android Studio  
2. Sync Gradle  
3. Run the `app` configuration on a connected device  

#### Command line

```bash
# Windows
gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install & launch:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.zhapitest/.MainActivity
```

On Windows you can also run:

```powershell
.\scripts\install_debug.ps1
```

---

### 6. Integrate ZhAPI into Your App

#### 6.1 Copy the AIDL

```
app/src/main/aidl/com/zhkj/zhapi/IZhApiService.aidl
```

Enable AIDL in `app/build.gradle`:

```gradle
android {
    buildFeatures {
        aidl true
    }
}
```

> **Important**: The AIDL must match the device-side definition (same transaction order). Always use the `IZhApiService.aidl` from this repo; do not mix stubs from different versions.

#### 6.2 Manifest permission & package visibility

```xml
<uses-permission android:name="com.zhkj.zhapi.permission.BIND_ZHAPI" />

<queries>
    <package android:name="com.zhkj.zhapiserver" />
    <intent>
        <action android:name="com.zhkj.zhapi.ZHAPI_SERVER" />
    </intent>
</queries>
```

#### 6.3 Bind the service (sample)

See `ZhApiHelper.java` for a complete helper. Minimal snippet:

```java
Intent intent = new Intent("com.zhkj.zhapi.ZHAPI_SERVER");
intent.setComponent(new ComponentName(
        "com.zhkj.zhapiserver",
        "com.zhkj.zhapi.server.ZhApiService"));
bindService(intent, connection, Context.BIND_AUTO_CREATE);

// in onServiceConnected:
IZhApiService api = IZhApiService.Stub.asInterface(binder);
String model = api.zhGetAndroidDeviceModel();
```

#### 6.4 Call guidelines

- AIDL calls may block — **do not** invoke long-running APIs on the main thread  
- `String` results may be `null` or empty — always null-check  
- Shutdown, reboot, OTA, and silent install are high-risk; add confirmation in factory/production flows  

---

### 7. Troubleshooting

```bash
adb logcat -s ZhApiHelper ZhApiServiceImpl ZhApiService ZhSilentInstall ZhAutoBoot ZhNodeIO ZhGpioCtrl ZhPwmCtrl
```

| Symptom | Likely cause |
|---------|--------------|
| Bind fails | ZhAPI service not preinstalled, or missing permission/`queries` in Manifest |
| Wrong / null return values | AIDL mismatch with device firmware |
| Permission errors | Missing `BIND_ZHAPI` declaration (normal permission; granted at install) |

---

### 8. Documentation & Support

| Doc | Content |
|-----|---------|
| [`docs/ZhAPI使用说明.md`](docs/ZhAPI使用说明.md) | Full API parameters, return values, samples, GPIO/PWM nodes |
| This README | Demo build/install and minimal integration guide |

For custom APIs or board bring-up, contact your hardware/system vendor with the device model and firmware version.

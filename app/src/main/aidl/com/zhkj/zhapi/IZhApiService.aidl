// SPDX-License-Identifier: Apache-2.0
// RK3576 系统 API（zh 前缀）。绑定 com.zhkj.zhapi.ZHAPI_SERVER / com.zhkj.zhapiserver
package com.zhkj.zhapi;

interface IZhApiService {

    // 系统信息
    String zhGetAndroidDeviceModel();
    String zhGetAndroidVersion();
    String zhGetKernelVersion();
    String getSerialNumber();
    String zhGetFirmwareVersion();
    String zhGetBuildDate();
    String zhGetRAMSize();
    String zhGetInternalStorageMemory();
    String zhGetAvailableInternalMemorySize();
    String zhGetOsVersion();

    // 电源 / 显示
    void zhShutDown();
    void zhReboot();
    void zhSetLCDOn();
    void zhSetLCDOff();
    int zhGetBacklightPwmPeriod();
    int zhGetBacklightPwmPolarity();
    boolean zhSetBacklightPwmPeriod(int periodNs);
    boolean zhSetBacklightPwmPolarity(int polarity);
    boolean zhSetBacklightPwmConfig(int periodNs, int polarity);
    void zhTakeScreenshot(String path, String name);
    int zhGetScreenWidth();
    int zhGetScreenHeight();
    void zhSetNavigationBarVisibility(boolean enable);
    void zhSetNavigationBarCanSwap(boolean enable);
    void zhSetStatusBarVisibility(boolean enable);

    // 应用 / 存储
    void zhSilentInstallApk(String apkPath, boolean openApp);
    String zhGetInternalSDPath();
    String zhGetSDPath();
    String zhGetUSBPath();

    // 以太网 / WiFi
    String zhGetEthMacAddress();
    String zhGetEth1MacAddress();
    String zhGetWifiMacAddress();
    String zhGetIpAddress();
    boolean zhSetEthIPAddress(String ip, String mask, String gateway, String dns, String dns2);
    boolean zhSetEthOnOff(boolean enable);
    void zhSetEthDhcp();
    String zhGetCurrentNetType();
    String zhGetWifiIpAddressInfo();
    boolean zhSetNetworkEnable(int networkType, boolean enable);
    boolean zhSetWifiDhcpConnect(String ssid, int security, String password);
    boolean zhSetWifiStaticConnect(String ssid, int security, String password, String ipAddr, String gateway, String mask, String dns1, String dns2);

    // 串口 / 人体感应 / ADB / NTP / OTA
    String zhGetUartPath(String uartNum);
    void zhSetHumanSensor(int timeSec);
    void zhEnableAdb(boolean enable);
    boolean zhSetNtpTimeServer(String server);
    String zhGetNtpTimeServer();
    void zhUpdateSystemOs(String osPath);

    // USB0 Type-A（zhctl usb0pwren + u2phy otg_mode）
    boolean zhSetUsb0Power(boolean enable);
    boolean zhGetUsb0Power();
    boolean zhSetUsb0Mode(String mode);
    String zhGetUsb0Mode();

    // 板级 GPIO（zhctl sysfs）
    boolean zhSetGpioCtrl(String nodeName, int value);
    int zhGetGpioCtrl(String nodeName);
    String zhGetGpioCtrlStatus(String nodeName);

    // 板级通用 PWM（zhctl PWMctrl，默认 pwm0 = pwm1_6ch_0）
    boolean zhSetPwmEnable(String name, boolean enable);
    boolean zhGetPwmEnable(String name);
    boolean zhSetPwmDuty(String name, int dutyNs);
    int zhGetPwmDuty(String name);
    boolean zhSetPwmPeriod(String name, int periodNs);
    int zhGetPwmPeriod(String name);
    boolean zhSetPwm(String name, boolean enable, int dutyNs, int periodNs);
    String zhGetPwmStatus(String name);
}

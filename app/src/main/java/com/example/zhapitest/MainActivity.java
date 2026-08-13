package com.example.zhapitest;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.zhkj.zhapi.IZhApiService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ZhApiHelper.ConnectionListener {

    private TextView tvConnectionStatus;
    private TextView tvLog;
    private MaterialButton btnConnect;
    private LinearLayout apiContainer;
    private ZhApiHelper zhApiHelper;
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvLog = findViewById(R.id.tvLog);
        btnConnect = findViewById(R.id.btnConnect);
        apiContainer = findViewById(R.id.apiContainer);

        zhApiHelper = new ZhApiHelper(this);
        zhApiHelper.setConnectionListener(this);

        btnConnect.setOnClickListener(v -> toggleConnection());
        findViewById(R.id.btnClearLog).setOnClickListener(v -> tvLog.setText(""));

        setupApiButtons();
        zhApiHelper.bind();
    }

    @Override
    protected void onDestroy() {
        zhApiHelper.unbind();
        super.onDestroy();
    }

    private void toggleConnection() {
        if (zhApiHelper.isBound()) {
            zhApiHelper.unbind();
            updateConnectionUi(false, getString(R.string.status_disconnected));
        } else {
            zhApiHelper.bind();
        }
    }

    private void updateConnectionUi(boolean connected, String statusText) {
        tvConnectionStatus.setText(statusText);
        btnConnect.setText(connected ? R.string.action_disconnect : R.string.action_connect);
    }

    private void appendLog(String message) {
        String line = timeFormat.format(new Date()) + "  " + message + "\n";
        tvLog.append(line);
    }

    private void runOnApi(Runnable action) {
        if (!zhApiHelper.isBound()) {
            Toast.makeText(this, R.string.toast_not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            action.run();
        }).start();
    }

    private void handleResult(ZhApiHelper.ApiResult<?> result) {
        runOnUiThread(() -> appendLog(result.message));
    }

    private void confirmDangerousAction(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (d, w) -> runOnApi(onConfirm))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupApiButtons() {
        addCategory(getString(R.string.category_system_info), new ApiAction[]{
                api("设备型号", () -> handleResult(zhApiHelper.call("zhGetAndroidDeviceModel",
                        IZhApiService::zhGetAndroidDeviceModel))),
                api("Android 版本", () -> handleResult(zhApiHelper.call("zhGetAndroidVersion",
                        IZhApiService::zhGetAndroidVersion))),
                api("内核版本", () -> handleResult(zhApiHelper.call("zhGetKernelVersion",
                        IZhApiService::zhGetKernelVersion))),
                api("序列号 SN", () -> handleResult(zhApiHelper.call("getSerialNumber",
                        IZhApiService::getSerialNumber))),
                api("固件版本", () -> handleResult(zhApiHelper.call("zhGetFirmwareVersion",
                        IZhApiService::zhGetFirmwareVersion))),
                api("编译日期", () -> handleResult(zhApiHelper.call("zhGetBuildDate",
                        IZhApiService::zhGetBuildDate))),
                api("RAM 大小", () -> handleResult(zhApiHelper.call("zhGetRAMSize",
                        IZhApiService::zhGetRAMSize))),
                api("内部存储总量", () -> handleResult(zhApiHelper.call("zhGetInternalStorageMemory",
                        IZhApiService::zhGetInternalStorageMemory))),
                api("内部存储可用", () -> handleResult(zhApiHelper.call("zhGetAvailableInternalMemorySize",
                        IZhApiService::zhGetAvailableInternalMemorySize))),
                api("OS 版本", () -> handleResult(zhApiHelper.call("zhGetOsVersion",
                        IZhApiService::zhGetOsVersion))),
        });

        addCategory(getString(R.string.category_power_display), new ApiAction[]{
                api("屏幕宽度", () -> handleResult(zhApiHelper.call("zhGetScreenWidth",
                        IZhApiService::zhGetScreenWidth))),
                api("屏幕高度", () -> handleResult(zhApiHelper.call("zhGetScreenHeight",
                        IZhApiService::zhGetScreenHeight))),
                toggleApi("LCD", enable -> runOnApi(() -> {
                    if (enable) {
                        handleResult(zhApiHelper.callVoid("zhSetLCDOn", IZhApiService::zhSetLCDOn));
                    } else {
                        handleResult(zhApiHelper.callVoid("zhSetLCDOff", IZhApiService::zhSetLCDOff));
                    }
                })),
                inputApi("截屏", values -> runOnApi(() -> handleResult(zhApiHelper.callVoid("zhTakeScreenshot",
                                s -> s.zhTakeScreenshot(values[0], values[1])))),
                        Field.text("保存路径", "/sdcard/Pictures"),
                        Field.text("文件名", "screenshot.png")),
                toggleApi("导航栏", enable -> runOnApi(() -> handleResult(zhApiHelper.callVoid(
                        "zhSetNavigationBarVisibility(" + enable + ")",
                        s -> s.zhSetNavigationBarVisibility(enable))))),
                toggleApi("导航栏上滑", enable -> runOnApi(() -> handleResult(zhApiHelper.callVoid(
                        "zhSetNavigationBarCanSwap(" + enable + ")",
                        s -> s.zhSetNavigationBarCanSwap(enable))))),
                toggleApi("状态栏", enable -> runOnApi(() -> handleResult(zhApiHelper.callVoid(
                        "zhSetStatusBarVisibility(" + enable + ")",
                        s -> s.zhSetStatusBarVisibility(enable))))),
                api("重启", () -> confirmDangerousAction(
                        getString(R.string.title_reboot),
                        getString(R.string.msg_reboot),
                        () -> handleResult(zhApiHelper.callVoid("zhReboot", IZhApiService::zhReboot)))),
                api("关机", () -> confirmDangerousAction(
                        getString(R.string.title_shutdown),
                        getString(R.string.msg_shutdown),
                        () -> handleResult(zhApiHelper.callVoid("zhShutDown", IZhApiService::zhShutDown)))),
        });

        addCategory(getString(R.string.category_storage), new ApiAction[]{
                api("内部 SD 路径", () -> handleResult(zhApiHelper.call("zhGetInternalSDPath",
                        IZhApiService::zhGetInternalSDPath))),
                api("SD 卡路径", () -> handleResult(zhApiHelper.call("zhGetSDPath",
                        IZhApiService::zhGetSDPath))),
                api("USB 路径", () -> handleResult(zhApiHelper.call("zhGetUSBPath",
                        IZhApiService::zhGetUSBPath))),
                inputApi("静默安装 APK", values -> {
                            boolean openApp = parseToggle(values[1]);
                            runOnApi(() -> handleResult(zhApiHelper.callVoid("zhSilentInstallApk",
                                    s -> s.zhSilentInstallApk(values[0], openApp))));
                        },
                        Field.text(getString(R.string.field_apk_path), "/sdcard/test.apk"),
                        Field.toggle(getString(R.string.field_open_after_install), false)),
        });

        addCategory(getString(R.string.category_network), new ApiAction[]{
                api("Eth0 MAC", () -> handleResult(zhApiHelper.call("zhGetEthMacAddress",
                        IZhApiService::zhGetEthMacAddress))),
                api("Eth1 MAC", () -> handleResult(zhApiHelper.call("zhGetEth1MacAddress",
                        IZhApiService::zhGetEth1MacAddress))),
                api("WiFi MAC", () -> handleResult(zhApiHelper.call("zhGetWifiMacAddress",
                        IZhApiService::zhGetWifiMacAddress))),
                api("IP 地址", () -> handleResult(zhApiHelper.call("zhGetIpAddress",
                        IZhApiService::zhGetIpAddress))),
                api("当前网络类型", () -> handleResult(zhApiHelper.call("zhGetCurrentNetType",
                        IZhApiService::zhGetCurrentNetType))),
                api("WiFi IP 信息", () -> handleResult(zhApiHelper.call("zhGetWifiIpAddressInfo",
                        IZhApiService::zhGetWifiIpAddressInfo))),
                api("以太网 DHCP", () -> handleResult(zhApiHelper.callVoid("zhSetEthDhcp",
                        IZhApiService::zhSetEthDhcp))),
                toggleApi("以太网开关", enable -> runOnApi(() -> handleResult(zhApiHelper.call(
                        "zhSetEthOnOff(" + enable + ")",
                        s -> s.zhSetEthOnOff(enable))))),
                inputApi("以太网静态 IP", values -> runOnApi(() -> handleResult(zhApiHelper.call("zhSetEthIPAddress",
                                s -> s.zhSetEthIPAddress(values[0], values[1], values[2], values[3], values[4])))),
                        Field.text("IP", "192.168.1.100"),
                        Field.text("掩码", "255.255.255.0"),
                        Field.text("网关", "192.168.1.1"),
                        Field.text("DNS1", "8.8.8.8"),
                        Field.text("DNS2", "")),
                inputApi("网络开关", values -> {
                            int type = parseInt(values[0], 0);
                            boolean enable = parseToggle(values[1]);
                            runOnApi(() -> handleResult(zhApiHelper.call("zhSetNetworkEnable",
                                    s -> s.zhSetNetworkEnable(type, enable))));
                        },
                        Field.number(getString(R.string.network_type_hint), "9"),
                        Field.toggle(getString(R.string.field_enable), true)),
                inputApi("WiFi DHCP", values -> runOnApi(() -> handleResult(zhApiHelper.call("zhSetWifiDhcpConnect",
                                s -> s.zhSetWifiDhcpConnect(values[0], parseInt(values[1], 2), values[2])))),
                        Field.text("SSID", "ZHKJ_AP"),
                        Field.number("加密类型 (0开放/1WEP/2WPA/3EAP/4WPA3)", "2"),
                        Field.text("密码", "zh118118")),
                inputApi("WiFi 静态 IP", values -> runOnApi(() -> handleResult(zhApiHelper.call("zhSetWifiStaticConnect",
                                s -> s.zhSetWifiStaticConnect(values[0], parseInt(values[1], 2), values[2],
                                        values[3], values[4], values[5], values[6], values[7])))),
                        Field.text("SSID", "ZHKJ_AP"),
                        Field.number("加密类型 (0开放/1WEP/2WPA/3EAP/4WPA3)", "2"),
                        Field.text("密码", "zh118118"),
                        Field.text("IP", "192.168.1.210"),
                        Field.text("网关", "192.168.1.1"),
                        Field.text("掩码", "255.255.255.0"),
                        Field.text("DNS1", "8.8.8.8"),
                        Field.text("DNS2", "")),
        });

        addCategory(getString(R.string.category_misc), new ApiAction[]{
                inputApi("串口路径", values -> runOnApi(() -> handleResult(zhApiHelper.call("zhGetUartPath",
                                s -> s.zhGetUartPath(values[0])))),
                        Field.text("串口 (uart0~uart3)", "uart0")),
                inputApi("人体感应", values -> runOnApi(() -> handleResult(zhApiHelper.callVoid("zhSetHumanSensor",
                                s -> s.zhSetHumanSensor(parseInt(values[0], 30))))),
                        Field.number("超时秒数 (0=关)", "30")),
                toggleApi("ADB", enable -> runOnApi(() -> handleResult(zhApiHelper.callVoid(
                        "zhEnableAdb(" + enable + ")",
                        s -> s.zhEnableAdb(enable))))),
                inputApi("设置 NTP", values -> runOnApi(() -> handleResult(zhApiHelper.call("zhSetNtpTimeServer",
                                s -> s.zhSetNtpTimeServer(values[0])))),
                        Field.text("NTP 服务器", "ntp.aliyun.com")),
                api("获取 NTP", () -> handleResult(zhApiHelper.call("zhGetNtpTimeServer",
                        IZhApiService::zhGetNtpTimeServer))),
                inputApi("系统 OTA", values -> confirmDangerousAction(
                                getString(R.string.title_ota),
                                getString(R.string.msg_ota, values[0]),
                                () -> handleResult(zhApiHelper.callVoid("zhUpdateSystemOs",
                                        s -> s.zhUpdateSystemOs(values[0])))),
                        Field.text("固件路径", "/sdcard/update.zip")),
        });

        addCategory(getString(R.string.category_usb0), new ApiAction[]{
                toggleApi("USB0 电源", enable -> runOnApi(() -> handleResult(zhApiHelper.call(
                        "zhSetUsb0Power(" + enable + ")",
                        s -> s.zhSetUsb0Power(enable))))),
                api("读 USB0 电源", () -> handleResult(zhApiHelper.call("zhGetUsb0Power",
                        IZhApiService::zhGetUsb0Power))),
                inputApi("USB0 模式", values -> runOnApi(() -> handleResult(zhApiHelper.call("zhSetUsb0Mode",
                                s -> s.zhSetUsb0Mode(values[0])))),
                        Field.text(getString(R.string.field_usb0_mode), "host")),
                api("读 USB0 模式", () -> handleResult(zhApiHelper.call("zhGetUsb0Mode",
                        IZhApiService::zhGetUsb0Mode))),
        });

        addCategory(getString(R.string.category_gpio), new ApiAction[]{
                inputApi("GPIO 写", values -> runOnApi(() -> handleResult(zhApiHelper.call("zhSetGpioCtrl",
                                s -> s.zhSetGpioCtrl(values[0], parseToggle(values[1]) ? 1 : 0)))),
                        Field.text(getString(R.string.field_gpio_node), "gpio1"),
                        Field.toggle(getString(R.string.field_gpio_level), true)),
                inputApi("GPIO 读", values -> runOnApi(() -> handleResult(zhApiHelper.call("zhGetGpioCtrl",
                                s -> s.zhGetGpioCtrl(values[0])))),
                        Field.text("节点名", "gpio1")),
                inputApi("GPIO 状态", values -> runOnApi(() -> handleResult(zhApiHelper.call("zhGetGpioCtrlStatus",
                                s -> s.zhGetGpioCtrlStatus(values[0])))),
                        Field.text("节点名", "gpio1")),
        });

        addCategory(getString(R.string.category_pwm), new ApiAction[]{
                inputApi("PWM 使能", values -> {
                            String name = emptyToNull(values[0]);
                            boolean enable = parseToggle(values[1]);
                            runOnApi(() -> handleResult(zhApiHelper.call(
                                    "zhSetPwmEnable(" + name + "," + enable + ")",
                                    s -> s.zhSetPwmEnable(name, enable))));
                        },
                        Field.text(getString(R.string.field_pwm_name), "pwm0"),
                        Field.toggle(getString(R.string.field_enable), true)),
                inputApi("读 PWM 使能", values -> runOnApi(() -> handleResult(zhApiHelper.call(
                                "zhGetPwmEnable",
                                s -> s.zhGetPwmEnable(emptyToNull(values[0]))))),
                        Field.text(getString(R.string.field_pwm_name), "pwm0")),
                inputApi("设 PWM 占空比", values -> runOnApi(() -> handleResult(zhApiHelper.call(
                                "zhSetPwmDuty",
                                s -> s.zhSetPwmDuty(emptyToNull(values[0]), parseInt(values[1], 25000))))),
                        Field.text(getString(R.string.field_pwm_name), "pwm0"),
                        Field.number(getString(R.string.field_pwm_duty), "25000")),
                inputApi("读 PWM 占空比", values -> runOnApi(() -> handleResult(zhApiHelper.call(
                                "zhGetPwmDuty",
                                s -> s.zhGetPwmDuty(emptyToNull(values[0]))))),
                        Field.text(getString(R.string.field_pwm_name), "pwm0")),
                inputApi("设 PWM 周期", values -> runOnApi(() -> handleResult(zhApiHelper.call(
                                "zhSetPwmPeriod",
                                s -> s.zhSetPwmPeriod(emptyToNull(values[0]), parseInt(values[1], 50000))))),
                        Field.text(getString(R.string.field_pwm_name), "pwm0"),
                        Field.number(getString(R.string.field_pwm_period), "50000")),
                inputApi("读 PWM 周期", values -> runOnApi(() -> handleResult(zhApiHelper.call(
                                "zhGetPwmPeriod",
                                s -> s.zhGetPwmPeriod(emptyToNull(values[0]))))),
                        Field.text(getString(R.string.field_pwm_name), "pwm0")),
                inputApi("PWM 一次写齐", values -> {
                            String name = emptyToNull(values[0]);
                            boolean enable = parseToggle(values[1]);
                            int dutyNs = parseInt(values[2], 25000);
                            int periodNs = parseInt(values[3], 50000);
                            runOnApi(() -> handleResult(zhApiHelper.call(
                                    "zhSetPwm(" + name + "," + enable + "," + dutyNs + "," + periodNs + ")",
                                    s -> s.zhSetPwm(name, enable, dutyNs, periodNs))));
                        },
                        Field.text(getString(R.string.field_pwm_name), "pwm0"),
                        Field.toggle(getString(R.string.field_enable), true),
                        Field.number(getString(R.string.field_pwm_duty), "25000"),
                        Field.number(getString(R.string.field_pwm_period), "50000")),
                inputApi("读 PWM 状态", values -> runOnApi(() -> handleResult(zhApiHelper.call(
                                "zhGetPwmStatus",
                                s -> s.zhGetPwmStatus(emptyToNull(values[0]))))),
                        Field.text(getString(R.string.field_pwm_name), "pwm0")),
        });
    }

    private static String emptyToNull(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        return value.trim();
    }

    private void addCategory(String title, ApiAction[] actions) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_api_category, apiContainer, false);
        ((TextView) card.findViewById(R.id.tvCategoryTitle)).setText(title);
        ChipGroup chipGroup = card.findViewById(R.id.chipGroup);
        for (ApiAction action : actions) {
            Chip chip = new Chip(this);
            chip.setText(action.label);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                if (!zhApiHelper.isBound()) {
                    Toast.makeText(this, R.string.toast_not_connected, Toast.LENGTH_SHORT).show();
                    return;
                }
                action.run.run();
            });
            chipGroup.addView(chip);
        }
        apiContainer.addView(card);
    }

    private static ApiAction api(String label, Runnable run) {
        return new ApiAction(label, run);
    }

    private ApiAction inputApi(String label, InputCallback callback, Field... fields) {
        return new ApiAction(label, () -> showInputDialog(label, fields, callback));
    }

    private ApiAction toggleApi(String label, ToggleCallback callback) {
        return new ApiAction(label, () -> showToggleDialog(label, callback));
    }

    private void showToggleDialog(String title, ToggleCallback callback) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_toggle, null);
        TextView tvLabel = view.findViewById(R.id.tvToggleLabel);
        MaterialSwitch switchToggle = view.findViewById(R.id.switchToggle);
        tvLabel.setText(title);
        switchToggle.setText(getString(R.string.toggle_off));

        switchToggle.setOnCheckedChangeListener((button, checked) ->
                button.setText(getString(checked ? R.string.toggle_on : R.string.toggle_off)));

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.dialog_confirm, (d, w) -> callback.onToggle(switchToggle.isChecked()))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showInputDialog(String title, Field[] fields, InputCallback callback) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, 0);

        List<View> inputs = new ArrayList<>();
        for (Field field : fields) {
            if (field.type == FieldType.TOGGLE) {
                View row = LayoutInflater.from(this).inflate(R.layout.dialog_field_toggle, container, false);
                TextView label = row.findViewById(R.id.tvFieldLabel);
                MaterialSwitch toggle = row.findViewById(R.id.switchField);
                label.setText(field.hint);
                toggle.setChecked(field.defaultOn);
                toggle.setText(getString(field.defaultOn ? R.string.toggle_on : R.string.toggle_off));
                toggle.setOnCheckedChangeListener((button, checked) ->
                        button.setText(getString(checked ? R.string.toggle_on : R.string.toggle_off)));
                container.addView(row);
                inputs.add(toggle);
            } else {
                EditText editText = new EditText(this);
                editText.setHint(field.hint);
                editText.setText(field.defaultValue);
                editText.setSingleLine(true);
                if (field.type == FieldType.NUMBER) {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                }
                container.addView(editText);
                inputs.add(editText);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(container)
                .setPositiveButton(R.string.dialog_confirm, (d, w) -> {
                    String[] values = new String[inputs.size()];
                    for (int i = 0; i < inputs.size(); i++) {
                        View input = inputs.get(i);
                        if (input instanceof MaterialSwitch) {
                            values[i] = ((MaterialSwitch) input).isChecked() ? "1" : "0";
                        } else {
                            values[i] = ((EditText) input).getText().toString().trim();
                        }
                    }
                    callback.onInput(values);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private static boolean parseToggle(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        String v = value.trim();
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    private static int parseInt(String text, int defaultValue) {
        if (TextUtils.isEmpty(text)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            updateConnectionUi(true, getString(R.string.status_connected));
            appendLog(getString(R.string.log_connected));
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            updateConnectionUi(false, getString(R.string.status_disconnected));
            appendLog(getString(R.string.log_disconnected));
        });
    }

    @Override
    public void onConnectionFailed(String error) {
        runOnUiThread(() -> {
            updateConnectionUi(false, error);
            appendLog(error);
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
    }

    private static final class ApiAction {
        final String label;
        final Runnable run;

        ApiAction(String label, Runnable run) {
            this.label = label;
            this.run = run;
        }
    }

    private enum FieldType {
        TEXT, NUMBER, TOGGLE
    }

    private static final class Field {
        final String hint;
        final String defaultValue;
        final FieldType type;
        final boolean defaultOn;

        private Field(String hint, String defaultValue, FieldType type, boolean defaultOn) {
            this.hint = hint;
            this.defaultValue = defaultValue;
            this.type = type;
            this.defaultOn = defaultOn;
        }

        static Field text(String hint, String defaultValue) {
            return new Field(hint, defaultValue, FieldType.TEXT, false);
        }

        static Field number(String hint, String defaultValue) {
            return new Field(hint, defaultValue, FieldType.NUMBER, false);
        }

        static Field toggle(String hint, boolean defaultOn) {
            return new Field(hint, defaultOn ? "1" : "0", FieldType.TOGGLE, defaultOn);
        }
    }

    private interface InputCallback {
        void onInput(String[] values);
    }

    private interface ToggleCallback {
        void onToggle(boolean enable);
    }
}

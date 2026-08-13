package com.example.zhapitest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.zhkj.zhapi.IZhApiService;

public class ZhApiHelper {

    private static final String TAG = "ZhApiHelper";
    public static final String ACTION_ZHAPI_SERVER = "com.zhkj.zhapi.ZHAPI_SERVER";
    public static final String PACKAGE_ZHAPI_SERVER = "com.zhkj.zhapiserver";
    public static final String CLASS_ZHAPI_SERVICE = "com.zhkj.zhapi.server.ZhApiService";
    public static final String PERMISSION_BIND_ZHAPI = "com.zhkj.zhapi.permission.BIND_ZHAPI";

    private final Context context;
    private IZhApiService service;
    private boolean bound;
    private ConnectionListener listener;

    public interface ConnectionListener {
        void onConnected();

        void onDisconnected();

        void onConnectionFailed(String error);
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = IZhApiService.Stub.asInterface(binder);
            bound = true;
            Log.i(TAG, "Connected to " + name);
            if (listener != null) {
                listener.onConnected();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            bound = false;
            Log.w(TAG, "Disconnected from " + name);
            if (listener != null) {
                listener.onDisconnected();
            }
        }
    };

    public ZhApiHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.listener = listener;
    }

    public boolean isBound() {
        return bound && service != null;
    }

    public IZhApiService getService() {
        return service;
    }

    public boolean bind() {
        if (bound) {
            return true;
        }
        Intent intent = new Intent(ACTION_ZHAPI_SERVER);
        intent.setComponent(new ComponentName(PACKAGE_ZHAPI_SERVER, CLASS_ZHAPI_SERVICE));
        try {
            boolean ok = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            if (!ok && listener != null) {
                listener.onConnectionFailed("bindService 返回 false，请确认设备已安装 " + PACKAGE_ZHAPI_SERVER);
            }
            return ok;
        } catch (SecurityException e) {
            Log.e(TAG, "bind failed", e);
            if (listener != null) {
                listener.onConnectionFailed(
                        "缺少 " + PERMISSION_BIND_ZHAPI + " 权限。"
                                + "请在 AndroidManifest.xml 声明该权限后重新安装应用。");
            }
            return false;
        }
    }

    public void unbind() {
        if (!bound) {
            return;
        }
        try {
            context.unbindService(connection);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "unbind ignored", e);
        }
        service = null;
        bound = false;
    }

    public interface RemoteCall<T> {
        T run(IZhApiService api) throws RemoteException;
    }

    public <T> ApiResult<T> call(String apiName, RemoteCall<T> call) {
        if (!isBound()) {
            return ApiResult.error("服务未连接，请先绑定 " + PACKAGE_ZHAPI_SERVER);
        }
        try {
            T value = call.run(service);
            return ApiResult.success(apiName, value);
        } catch (RemoteException e) {
            Log.e(TAG, apiName + " failed", e);
            return ApiResult.error(apiName + " 调用失败: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, apiName + " failed", e);
            return ApiResult.error(apiName + " 异常: " + e.getMessage());
        }
    }

    public ApiResult<Void> callVoid(String apiName, VoidRemoteCall call) {
        if (!isBound()) {
            return ApiResult.error("服务未连接，请先绑定 " + PACKAGE_ZHAPI_SERVER);
        }
        try {
            call.run(service);
            return ApiResult.successVoid(apiName);
        } catch (RemoteException e) {
            Log.e(TAG, apiName + " failed", e);
            return ApiResult.error(apiName + " 调用失败: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, apiName + " failed", e);
            return ApiResult.error(apiName + " 异常: " + e.getMessage());
        }
    }

    public interface VoidRemoteCall {
        void run(IZhApiService api) throws RemoteException;
    }

    public static final class ApiResult<T> {
        public final boolean ok;
        public final String apiName;
        public final T value;
        public final String message;

        private ApiResult(boolean ok, String apiName, T value, String message) {
            this.ok = ok;
            this.apiName = apiName;
            this.value = value;
            this.message = message;
        }

        static <T> ApiResult<T> success(String apiName, T value) {
            return new ApiResult<>(true, apiName, value, formatSuccess(apiName, value));
        }

        static ApiResult<Void> successVoid(String apiName) {
            return new ApiResult<>(true, apiName, null, apiName + " => 执行成功");
        }

        static <T> ApiResult<T> error(String message) {
            return new ApiResult<>(false, null, null, message);
        }

        private static String formatSuccess(String apiName, Object value) {
            if (value == null) {
                return apiName + " => null";
            }
            if (value instanceof Boolean) {
                return apiName + " => " + value;
            }
            return apiName + " => " + value;
        }
    }
}

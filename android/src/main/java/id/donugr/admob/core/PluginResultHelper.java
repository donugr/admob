package id.donugr.admob.core;

import com.getcapacitor.JSObject;

public final class PluginResultHelper {
    private PluginResultHelper() {
    }

    public static JSObject success(String status) {
        JSObject result = new JSObject();
        result.put("ok", true);
        if (status != null && !status.isEmpty()) {
            result.put("status", status);
        }
        return result;
    }

    public static JSObject success(String status, JSObject data) {
        JSObject result = success(status);
        result.put("data", data);
        return result;
    }

    public static JSObject failure(String code, String message, String status) {
        JSObject result = new JSObject();
        result.put("ok", false);
        result.put("code", code);
        result.put("message", message);
        result.put("status", status);
        return result;
    }

    public static JSObject readyPayload(boolean ready) {
        JSObject data = new JSObject();
        data.put("ready", ready);
        return data;
    }
}

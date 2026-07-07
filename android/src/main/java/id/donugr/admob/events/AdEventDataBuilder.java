package id.donugr.admob.events;

import android.app.Activity;
import android.content.res.Configuration;
import com.getcapacitor.JSObject;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public final class AdEventDataBuilder {
    private AdEventDataBuilder() {}

    public static JSObject creativeSize(AdView adView, Activity activity) {
        JSObject data = new JSObject();
        JSObject size = new JSObject();
        if (adView != null) {
            AdSize adSize = adView.getAdSize();
            if (adSize != null) {
                size.put("widthDp", adSize.getWidth());
                size.put("heightDp", adSize.getHeight());
                size.put("widthPx", activity == null ? 0 : adSize.getWidthInPixels(activity));
                size.put("heightPx", activity == null ? 0 : adSize.getHeightInPixels(activity));
            }
        }
        data.put("creativeSize", size);
        return data;
    }

    public static JSObject hostSize(Integer x, Integer y, Integer width, Integer height, String anchor) {
        JSObject data = new JSObject();
        JSObject hostSize = new JSObject();
        if (x != null) hostSize.put("xPx", x);
        if (y != null) hostSize.put("yPx", y);
        hostSize.put("widthPx", width == null ? 0 : width);
        hostSize.put("heightPx", height == null ? 0 : height);
        if (anchor != null && !anchor.isEmpty()) hostSize.put("anchor", anchor);
        data.put("hostSize", hostSize);
        return data;
    }

    public static JSObject fullscreen(Activity activity, boolean active) {
        JSObject data = new JSObject();
        JSObject fullscreen = new JSObject();
        fullscreen.put("active", active);
        if (activity != null) {
            int orientation = activity.getResources().getConfiguration().orientation;
            fullscreen.put("orientation", orientation == Configuration.ORIENTATION_LANDSCAPE ? "landscape" : "portrait");
        }
        long now = System.currentTimeMillis();
        fullscreen.put(active ? "shownAt" : "dismissedAt", now);
        data.put("fullscreen", fullscreen);
        return data;
    }

    public static JSObject reward(int amount, String type) {
        JSObject data = new JSObject();
        JSObject reward = new JSObject();
        reward.put("amount", amount);
        reward.put("type", type == null ? "" : type);
        data.put("reward", reward);
        return data;
    }
}

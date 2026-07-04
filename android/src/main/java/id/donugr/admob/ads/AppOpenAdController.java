package id.donugr.admob.ads;

import android.app.Activity;
import android.text.TextUtils;
import com.getcapacitor.PluginCall;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback;
import id.donugr.admob.core.PluginResultHelper;
import id.donugr.admob.core.RuntimeConfig;
import id.donugr.admob.events.AdEventDispatcher;
import id.donugr.admob.util.SystemUiHelper;
import id.donugr.admob.util.TestAdPresetResolver;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AppOpenAdController {
    private static final long APP_OPEN_MAX_AGE_MS = 4L * 60L * 60L * 1000L;

    private final AppOpenHost host;
    private final RuntimeConfig runtimeConfig;
    private final AdEventDispatcher events;
    private final Map<String, AppOpenAd> appOpenAds = new ConcurrentHashMap<>();
    private final Map<String, Long> loadTimes = new ConcurrentHashMap<>();
    private final Set<String> loadingPlacements = ConcurrentHashMap.newKeySet();

    public AppOpenAdController(AppOpenHost host, RuntimeConfig runtimeConfig, AdEventDispatcher events) {
        this.host = host;
        this.runtimeConfig = runtimeConfig;
        this.events = events;
    }

    public void preload(PluginCall call) {
        if (!runtimeConfig.isEnabled()) {
            call.resolve(PluginResultHelper.failure("ADS_DISABLED", "Ads are disabled.", "disabled"));
            return;
        }

        final String placementId = host.requireTrimmed(call, "placementId");
        final String adUnitId = resolveAdUnitId(
            call,
            placementId,
            host.requireTrimmed(call, "adUnitId"),
            host.requireTrimmed(call, "testAdPreset")
        );
        if (adUnitId == null) {
            return;
        }
        if (TextUtils.isEmpty(placementId) || TextUtils.isEmpty(adUnitId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "Missing placementId or app open ad unit id.", "error"));
            return;
        }
        if (loadingPlacements.contains(placementId)) {
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        loadingPlacements.add(placementId);
        AppOpenAd.load(
            host.getPluginContext(),
            adUnitId,
            new AdRequest.Builder().build(),
            new AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(AppOpenAd appOpenAd) {
                    loadingPlacements.remove(placementId);
                    appOpenAds.put(placementId, appOpenAd);
                    loadTimes.put(placementId, new Date().getTime());
                    wireCallbacks(placementId, appOpenAd);
                    events.emit("app_open", placementId, "loaded", null, "App open ad loaded.");
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    loadingPlacements.remove(placementId);
                    appOpenAds.remove(placementId);
                    loadTimes.remove(placementId);
                    events.emit("app_open", placementId, "failed", String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                }
            }
        );
        call.resolve(PluginResultHelper.success("loading"));
    }

    public void isReady(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        boolean ready = appOpenAds.get(placementId) != null && isFresh(placementId);
        call.resolve(PluginResultHelper.success(ready ? "ready" : "not_ready", PluginResultHelper.readyPayload(ready)));
    }

    public void show(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        Activity activity = host.getPluginActivity();
        AppOpenAd appOpenAd = appOpenAds.get(placementId);
        if (activity == null) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for app open show.", "not_ready"));
            return;
        }
        if (appOpenAd == null || !isFresh(placementId)) {
            appOpenAds.remove(placementId);
            loadTimes.remove(placementId);
            call.resolve(PluginResultHelper.failure("NOT_READY", "App open ad is not ready or has expired.", "not_ready"));
            return;
        }
        releaseSystemUiIfNeeded(activity);
        appOpenAd.show(activity);
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void clearAll() {
        appOpenAds.clear();
        loadTimes.clear();
        loadingPlacements.clear();
    }

    private boolean isFresh(String placementId) {
        Long loadedAt = loadTimes.get(placementId);
        return loadedAt != null && new Date().getTime() - loadedAt < APP_OPEN_MAX_AGE_MS;
    }

    private void wireCallbacks(final String placementId, final AppOpenAd appOpenAd) {
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                releaseSystemUiIfNeeded(host.getPluginActivity());
                events.emit("app_open", placementId, "shown", null, "App open ad shown.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                appOpenAds.remove(placementId);
                loadTimes.remove(placementId);
                events.emit("app_open", placementId, "dismissed", null, "App open ad dismissed.");
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                appOpenAds.remove(placementId);
                loadTimes.remove(placementId);
                events.emit("app_open", placementId, "failed", String.valueOf(adError.getCode()), adError.getMessage());
            }

            @Override
            public void onAdClicked() {
                releaseSystemUiIfNeeded(host.getPluginActivity());
                events.emit("app_open", placementId, "clicked", null, "App open ad clicked.");
            }

            @Override
            public void onAdImpression() {
                events.emit("app_open", placementId, "impression", null, "App open ad impression recorded.");
            }
        });
    }

    private String resolveAdUnitId(PluginCall call, String placementId, String explicitAdUnitId, String testAdPreset) {
        return TestAdPresetResolver.resolve(
            call,
            runtimeConfig.isTestMode(),
            explicitAdUnitId,
            testAdPreset,
            runtimeConfig.resolvePlacement(placementId),
            "app_open"
        );
    }

    private void releaseSystemUiIfNeeded(Activity activity) {
        if (!runtimeConfig.isReleaseSystemUiOnAdInteraction()) {
            return;
        }
        SystemUiHelper.releaseForAdInteraction(activity);
    }

    public interface AppOpenHost {
        android.content.Context getPluginContext();
        Activity getPluginActivity();
        String requireTrimmed(PluginCall call, String key);
    }
}

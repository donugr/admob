package id.donugr.admob.ads;

import android.app.Activity;
import android.text.TextUtils;
import com.getcapacitor.PluginCall;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import id.donugr.admob.core.PluginResultHelper;
import id.donugr.admob.core.RuntimeConfig;
import id.donugr.admob.events.AdEventDispatcher;
import id.donugr.admob.util.TestAdPresetResolver;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InterstitialAdController {
    private final InterstitialHost host;
    private final RuntimeConfig runtimeConfig;
    private final AdEventDispatcher events;
    private final Map<String, InterstitialAd> interstitialAds = new ConcurrentHashMap<>();
    private final Set<String> loadingPlacements = ConcurrentHashMap.newKeySet();

    public InterstitialAdController(InterstitialHost host, RuntimeConfig runtimeConfig, AdEventDispatcher events) {
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
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "Missing placementId or interstitial ad unit id.", "error"));
            return;
        }
        if (loadingPlacements.contains(placementId)) {
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        loadingPlacements.add(placementId);
        InterstitialAd.load(
            host.getPluginContext(),
            adUnitId,
            new AdRequest.Builder().build(),
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(InterstitialAd interstitialAd) {
                    loadingPlacements.remove(placementId);
                    interstitialAds.put(placementId, interstitialAd);
                    wireCallbacks(placementId, interstitialAd);
                    events.emit("interstitial", placementId, "loaded", null, "Interstitial loaded.");
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    loadingPlacements.remove(placementId);
                    interstitialAds.remove(placementId);
                    events.emit("interstitial", placementId, "failed", String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                }
            }
        );
        call.resolve(PluginResultHelper.success("loading"));
    }

    public void isReady(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        boolean ready = interstitialAds.get(placementId) != null;
        call.resolve(PluginResultHelper.success(ready ? "ready" : "not_ready", PluginResultHelper.readyPayload(ready)));
    }

    public void show(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        InterstitialAd interstitialAd = interstitialAds.get(placementId);
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for interstitial show.", "not_ready"));
            return;
        }
        if (interstitialAd == null) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Interstitial is not ready.", "not_ready"));
            return;
        }
        interstitialAd.show(activity);
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void clearAll() {
        interstitialAds.clear();
        loadingPlacements.clear();
    }

    private void wireCallbacks(final String placementId, final InterstitialAd interstitialAd) {
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                events.emit("interstitial", placementId, "shown", null, "Interstitial shown.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAds.remove(placementId);
                events.emit("interstitial", placementId, "dismissed", null, "Interstitial dismissed.");
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                interstitialAds.remove(placementId);
                events.emit("interstitial", placementId, "failed", String.valueOf(adError.getCode()), adError.getMessage());
            }

            @Override
            public void onAdClicked() {
                events.emit("interstitial", placementId, "clicked", null, "Interstitial clicked.");
            }

            @Override
            public void onAdImpression() {
                events.emit("interstitial", placementId, "impression", null, "Interstitial impression recorded.");
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
            "interstitial"
        );
    }

    public interface InterstitialHost {
        android.content.Context getPluginContext();
        Activity getPluginActivity();
        String requireTrimmed(PluginCall call, String key);
    }
}

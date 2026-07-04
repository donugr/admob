package id.donugr.admob.ads;

import android.app.Activity;
import android.text.TextUtils;
import com.getcapacitor.PluginCall;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import id.donugr.admob.core.PluginResultHelper;
import id.donugr.admob.core.RuntimeConfig;
import id.donugr.admob.events.AdEventDispatcher;
import id.donugr.admob.util.TestAdPresetResolver;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RewardedInterstitialAdController {
    private final RewardedInterstitialHost host;
    private final RuntimeConfig runtimeConfig;
    private final AdEventDispatcher events;
    private final Map<String, RewardedInterstitialAd> rewardedInterstitialAds = new ConcurrentHashMap<>();
    private final Set<String> loadingPlacements = ConcurrentHashMap.newKeySet();

    public RewardedInterstitialAdController(RewardedInterstitialHost host, RuntimeConfig runtimeConfig, AdEventDispatcher events) {
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
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "Missing placementId or rewarded interstitial ad unit id.", "error"));
            return;
        }
        if (loadingPlacements.contains(placementId)) {
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        loadingPlacements.add(placementId);
        RewardedInterstitialAd.load(
            host.getPluginContext(),
            adUnitId,
            new AdRequest.Builder().build(),
            new RewardedInterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedInterstitialAd rewardedInterstitialAd) {
                    loadingPlacements.remove(placementId);
                    rewardedInterstitialAds.put(placementId, rewardedInterstitialAd);
                    wireCallbacks(placementId, rewardedInterstitialAd);
                    events.emit("rewarded_interstitial", placementId, "loaded", null, "Rewarded interstitial ad loaded.");
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    loadingPlacements.remove(placementId);
                    rewardedInterstitialAds.remove(placementId);
                    events.emit("rewarded_interstitial", placementId, "failed", String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                }
            }
        );
        call.resolve(PluginResultHelper.success("loading"));
    }

    public void isReady(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        boolean ready = rewardedInterstitialAds.get(placementId) != null;
        call.resolve(PluginResultHelper.success(ready ? "ready" : "not_ready", PluginResultHelper.readyPayload(ready)));
    }

    public void show(PluginCall call) {
        final String placementId = host.requireTrimmed(call, "placementId");
        RewardedInterstitialAd rewardedInterstitialAd = rewardedInterstitialAds.get(placementId);
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for rewarded interstitial show.", "not_ready"));
            return;
        }
        if (rewardedInterstitialAd == null) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Rewarded interstitial ad is not ready.", "not_ready"));
            return;
        }

        rewardedInterstitialAd.show(activity, rewardItem -> {
            String message = "Reward earned.";
            if (rewardItem != null) {
                message = "Reward earned: " + rewardItem.getAmount() + " " + rewardItem.getType();
            }
            events.emit("rewarded_interstitial", placementId, "reward_earned", null, message);
        });
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void clearAll() {
        rewardedInterstitialAds.clear();
        loadingPlacements.clear();
    }

    private void wireCallbacks(final String placementId, final RewardedInterstitialAd rewardedInterstitialAd) {
        rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                events.emit("rewarded_interstitial", placementId, "shown", null, "Rewarded interstitial ad shown.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedInterstitialAds.remove(placementId);
                events.emit("rewarded_interstitial", placementId, "dismissed", null, "Rewarded interstitial ad dismissed.");
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                rewardedInterstitialAds.remove(placementId);
                events.emit("rewarded_interstitial", placementId, "failed", String.valueOf(adError.getCode()), adError.getMessage());
            }

            @Override
            public void onAdClicked() {
                events.emit("rewarded_interstitial", placementId, "clicked", null, "Rewarded interstitial ad clicked.");
            }

            @Override
            public void onAdImpression() {
                events.emit("rewarded_interstitial", placementId, "impression", null, "Rewarded interstitial ad impression recorded.");
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
            "rewarded_interstitial"
        );
    }

    public interface RewardedInterstitialHost {
        android.content.Context getPluginContext();
        Activity getPluginActivity();
        String requireTrimmed(PluginCall call, String key);
    }
}

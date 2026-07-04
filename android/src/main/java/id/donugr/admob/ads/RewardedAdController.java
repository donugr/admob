package id.donugr.admob.ads;

import android.app.Activity;
import android.text.TextUtils;
import com.getcapacitor.PluginCall;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import id.donugr.admob.core.PluginResultHelper;
import id.donugr.admob.core.RuntimeConfig;
import id.donugr.admob.events.AdEventDispatcher;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RewardedAdController {
    private static final String TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    private final RewardedHost host;
    private final RuntimeConfig runtimeConfig;
    private final AdEventDispatcher events;
    private final Map<String, RewardedAd> rewardedAds = new ConcurrentHashMap<>();
    private final Set<String> loadingPlacements = ConcurrentHashMap.newKeySet();

    public RewardedAdController(RewardedHost host, RuntimeConfig runtimeConfig, AdEventDispatcher events) {
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
        final String adUnitId = resolveAdUnitId(placementId, host.requireTrimmed(call, "adUnitId"));
        if (TextUtils.isEmpty(placementId) || TextUtils.isEmpty(adUnitId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "Missing placementId or rewarded ad unit id.", "error"));
            return;
        }
        if (loadingPlacements.contains(placementId)) {
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        loadingPlacements.add(placementId);
        RewardedAd.load(
            host.getPluginContext(),
            adUnitId,
            new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedAd rewardedAd) {
                    loadingPlacements.remove(placementId);
                    rewardedAds.put(placementId, rewardedAd);
                    wireCallbacks(placementId, rewardedAd);
                    events.emit("rewarded", placementId, "loaded", null, "Rewarded ad loaded.");
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    loadingPlacements.remove(placementId);
                    rewardedAds.remove(placementId);
                    events.emit("rewarded", placementId, "failed", String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                }
            }
        );
        call.resolve(PluginResultHelper.success("loading"));
    }

    public void isReady(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        boolean ready = rewardedAds.get(placementId) != null;
        call.resolve(PluginResultHelper.success(ready ? "ready" : "not_ready", PluginResultHelper.readyPayload(ready)));
    }

    public void show(PluginCall call) {
        final String placementId = host.requireTrimmed(call, "placementId");
        RewardedAd rewardedAd = rewardedAds.get(placementId);
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for rewarded show.", "not_ready"));
            return;
        }
        if (rewardedAd == null) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Rewarded ad is not ready.", "not_ready"));
            return;
        }

        rewardedAd.show(activity, rewardItem -> {
            String message = "Reward earned.";
            if (rewardItem != null) {
                message = "Reward earned: " + rewardItem.getAmount() + " " + rewardItem.getType();
            }
            events.emit("rewarded", placementId, "reward_earned", null, message);
        });
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void clearAll() {
        rewardedAds.clear();
        loadingPlacements.clear();
    }

    private void wireCallbacks(final String placementId, final RewardedAd rewardedAd) {
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                events.emit("rewarded", placementId, "shown", null, "Rewarded ad shown.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAds.remove(placementId);
                events.emit("rewarded", placementId, "dismissed", null, "Rewarded ad dismissed.");
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                rewardedAds.remove(placementId);
                events.emit("rewarded", placementId, "failed", String.valueOf(adError.getCode()), adError.getMessage());
            }

            @Override
            public void onAdClicked() {
                events.emit("rewarded", placementId, "clicked", null, "Rewarded ad clicked.");
            }

            @Override
            public void onAdImpression() {
                events.emit("rewarded", placementId, "impression", null, "Rewarded ad impression recorded.");
            }
        });
    }

    private String resolveAdUnitId(String placementId, String explicitAdUnitId) {
        if (runtimeConfig.isTestMode()) {
            return TEST_REWARDED_AD_UNIT_ID;
        }
        if (explicitAdUnitId != null && !explicitAdUnitId.isEmpty()) {
            return explicitAdUnitId;
        }
        return runtimeConfig.resolvePlacement(placementId);
    }

    public interface RewardedHost {
        android.content.Context getPluginContext();
        Activity getPluginActivity();
        String requireTrimmed(PluginCall call, String key);
    }
}

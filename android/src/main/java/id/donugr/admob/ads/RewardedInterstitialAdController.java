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
import id.donugr.admob.util.SystemUiHelper;
import id.donugr.admob.util.TestAdPresetResolver;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RewardedInterstitialAdController {
    private final RewardedInterstitialHost host;
    private final RuntimeConfig runtimeConfig;
    private final AdEventDispatcher events;
    private final Map<String, RewardedInterstitialAd> rewardedInterstitialAds = new ConcurrentHashMap<>();
    private final Map<String, FullscreenPlacementState> placementStates = new ConcurrentHashMap<>();
    private final Set<String> loadingPlacements = ConcurrentHashMap.newKeySet();
    private final Set<String> disposedPlacements = ConcurrentHashMap.newKeySet();

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
            logState("debug", placementId, "preload_skip_loading", "Rewarded interstitial preload skipped because this placement is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        disposedPlacements.remove(placementId);
        FullscreenPlacementState state = getState(placementId);
        long requestToken = state.markLoading();
        logState("debug", placementId, "state_transition", "Rewarded interstitial state: " + state.status + ".");
        events.log("info", "rewarded_interstitial", "preload_start", "Rewarded interstitial preload started.", placementId, null, null, null, null);
        loadingPlacements.add(placementId);
        RewardedInterstitialAd.load(
            host.getPluginContext(),
            adUnitId,
            new AdRequest.Builder().build(),
            new RewardedInterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedInterstitialAd rewardedInterstitialAd) {
                    loadingPlacements.remove(placementId);
                    FullscreenPlacementState currentState = getState(placementId);
                    if (disposedPlacements.contains(placementId) || !currentState.matchesActiveRequest(requestToken)) {
                        events.log("warn", "rewarded_interstitial", "callback_skip_disposed", "Rewarded interstitial loaded callback ignored because this placement is already disposed or stale.", placementId, null, null, null, null);
                        return;
                    }
                    rewardedInterstitialAds.put(placementId, rewardedInterstitialAd);
                    currentState.markLoaded();
                    logState("debug", placementId, "state_transition", "Rewarded interstitial state: " + currentState.status + ".");
                    wireCallbacks(placementId, rewardedInterstitialAd);
                    events.emit("rewarded_interstitial", placementId, "loaded", null, "Rewarded interstitial ad loaded.");
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    loadingPlacements.remove(placementId);
                    FullscreenPlacementState currentState = getState(placementId);
                    if (disposedPlacements.contains(placementId) || !currentState.matchesActiveRequest(requestToken)) {
                        events.log("warn", "rewarded_interstitial", "callback_skip_disposed", "Rewarded interstitial failed callback ignored because this placement is already disposed or stale.", placementId, null, null, null, null);
                        return;
                    }
                    rewardedInterstitialAds.remove(placementId);
                    currentState.markFailed(String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                    logState("warn", placementId, "state_transition", "Rewarded interstitial state: " + currentState.status + ".");
                    events.emit("rewarded_interstitial", placementId, "failed", String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                }
            }
        );
        call.resolve(PluginResultHelper.success("loading"));
    }

    public void isReady(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        boolean ready = rewardedInterstitialAds.get(placementId) != null;
        if (!ready) {
            FullscreenPlacementState state = placementStates.get(placementId);
            if (state != null) {
                logState("debug", placementId, "ready_check_not_ready", "Rewarded interstitial ready check returned false. state=" + state.status + ", disposed=" + state.disposed + ", showing=" + state.showing + ", lastErrorCode=" + state.lastErrorCode + ".");
            }
        }
        call.resolve(PluginResultHelper.success(ready ? "ready" : "not_ready", PluginResultHelper.readyPayload(ready)));
    }

    public void show(PluginCall call) {
        final String placementId = host.requireTrimmed(call, "placementId");
        RewardedInterstitialAd rewardedInterstitialAd = rewardedInterstitialAds.get(placementId);
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            logState("warn", placementId, "show_activity_unavailable", "Rewarded interstitial show failed because activity is unavailable.");
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for rewarded interstitial show.", "not_ready"));
            return;
        }
        if (rewardedInterstitialAd == null) {
            logState("warn", placementId, "show_not_ready", "Rewarded interstitial show failed because this placement is not ready.");
            call.resolve(PluginResultHelper.failure("NOT_READY", "Rewarded interstitial ad is not ready.", "not_ready"));
            return;
        }
        if (disposedPlacements.contains(placementId)) {
            events.log("warn", "rewarded_interstitial", "show_skip_disposed", "Rewarded interstitial show skipped because this placement is already disposed.", placementId, null, null, null, null);
            call.resolve(PluginResultHelper.failure("NOT_READY", "Rewarded interstitial ad is already disposed.", "not_ready"));
            return;
        }
        FullscreenPlacementState state = getState(placementId);
        if (state.showing) {
            logState("warn", placementId, "show_skip_already_showing", "Rewarded interstitial show skipped because this placement is already showing.");
            call.resolve(PluginResultHelper.failure("NOT_READY", "Rewarded interstitial ad is already showing.", "not_ready"));
            return;
        }

        state.markShowing();
        logState("debug", placementId, "state_transition", "Rewarded interstitial state: " + state.status + ".");
        events.log("info", "rewarded_interstitial", "show_start", "Rewarded interstitial show started.", placementId, null, null, null, null);
        releaseSystemUiIfNeeded(activity);
        rewardedInterstitialAd.show(activity, rewardItem -> {
            if (disposedPlacements.contains(placementId)) {
                return;
            }
            getState(placementId).recordPhase("reward_earned");
            logState("debug", placementId, "callback_order", "Rewarded interstitial callback: reward_earned.");
            String message = "Reward earned.";
            if (rewardItem != null) {
                message = "Reward earned: " + rewardItem.getAmount() + " " + rewardItem.getType();
            }
            events.emit("rewarded_interstitial", placementId, "reward_earned", null, message);
        });
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void clearAll() {
        disposedPlacements.addAll(rewardedInterstitialAds.keySet());
        for (String placementId : rewardedInterstitialAds.keySet()) {
            FullscreenPlacementState state = getState(placementId);
            state.markDisposed();
            logState("debug", placementId, "state_transition", "Rewarded interstitial state: " + state.status + ".");
        }
        rewardedInterstitialAds.clear();
        loadingPlacements.clear();
    }

    private void wireCallbacks(final String placementId, final RewardedInterstitialAd rewardedInterstitialAd) {
        rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                FullscreenPlacementState state = getState(placementId);
                state.recordPhase("shown");
                logState("debug", placementId, "callback_order", "Rewarded interstitial callback: shown.");
                releaseSystemUiIfNeeded(host.getPluginActivity());
                events.emit("rewarded_interstitial", placementId, "shown", null, "Rewarded interstitial ad shown.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                disposedPlacements.add(placementId);
                rewardedInterstitialAds.remove(placementId);
                FullscreenPlacementState state = getState(placementId);
                state.markDismissed();
                state.recordPhase("dismissed");
                logState("debug", placementId, "callback_order", "Rewarded interstitial callback: dismissed.");
                logState("debug", placementId, "state_transition", "Rewarded interstitial state: " + state.status + ".");
                events.emit("rewarded_interstitial", placementId, "dismissed", null, "Rewarded interstitial ad dismissed.");
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                disposedPlacements.add(placementId);
                rewardedInterstitialAds.remove(placementId);
                FullscreenPlacementState state = getState(placementId);
                state.markFailed(String.valueOf(adError.getCode()), adError.getMessage());
                state.recordPhase("failed");
                logState("warn", placementId, "callback_order", "Rewarded interstitial callback: failed_to_show.");
                logState("warn", placementId, "state_transition", "Rewarded interstitial state: " + state.status + ".");
                events.emit("rewarded_interstitial", placementId, "failed", String.valueOf(adError.getCode()), adError.getMessage());
            }

            @Override
            public void onAdClicked() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                getState(placementId).recordPhase("clicked");
                logState("debug", placementId, "callback_order", "Rewarded interstitial callback: clicked.");
                releaseSystemUiIfNeeded(host.getPluginActivity());
                events.emit("rewarded_interstitial", placementId, "clicked", null, "Rewarded interstitial ad clicked.");
            }

            @Override
            public void onAdImpression() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                getState(placementId).recordPhase("impression");
                logState("debug", placementId, "callback_order", "Rewarded interstitial callback: impression.");
                events.emit("rewarded_interstitial", placementId, "impression", null, "Rewarded interstitial ad impression recorded.");
            }
        });
    }

    private FullscreenPlacementState getState(String placementId) {
        return placementStates.computeIfAbsent(placementId, ignored -> new FullscreenPlacementState());
    }

    private void logState(String level, String placementId, String code, String message) {
        events.log(level, "rewarded_interstitial", code, message, placementId, null, null, null, null);
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

    private void releaseSystemUiIfNeeded(Activity activity) {
        if (!runtimeConfig.isReleaseSystemUiOnAdInteraction()) {
            return;
        }
        SystemUiHelper.releaseForAdInteraction(activity);
    }

    public interface RewardedInterstitialHost {
        android.content.Context getPluginContext();
        Activity getPluginActivity();
        String requireTrimmed(PluginCall call, String key);
    }
}

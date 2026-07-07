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
import id.donugr.admob.events.AdEventDataBuilder;
import id.donugr.admob.util.SystemUiHelper;
import id.donugr.admob.util.TestAdPresetResolver;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RewardedAdController {
    private final RewardedHost host;
    private final RuntimeConfig runtimeConfig;
    private final AdEventDispatcher events;
    private final FullscreenAdCoordinator fullscreenCoordinator;
    private final Map<String, RewardedAd> rewardedAds = new ConcurrentHashMap<>();
    private final Map<String, FullscreenPlacementState> placementStates = new ConcurrentHashMap<>();
    private final Set<String> loadingPlacements = ConcurrentHashMap.newKeySet();
    private final Set<String> disposedPlacements = ConcurrentHashMap.newKeySet();

    public RewardedAdController(RewardedHost host, RuntimeConfig runtimeConfig, AdEventDispatcher events, FullscreenAdCoordinator fullscreenCoordinator) {
        this.host = host;
        this.runtimeConfig = runtimeConfig;
        this.events = events;
        this.fullscreenCoordinator = fullscreenCoordinator;
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
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "Missing placementId or rewarded ad unit id.", "error"));
            return;
        }
        if (loadingPlacements.contains(placementId)) {
            logState("debug", placementId, "preload_skip_loading", "Rewarded preload skipped because this placement is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        disposedPlacements.remove(placementId);
        FullscreenPlacementState state = getState(placementId);
        long requestToken = state.markLoading();
        logState("debug", placementId, "state_transition", "Rewarded state: " + state.status + ".");
        events.log("info", "rewarded", "preload_start", "Rewarded preload started.", placementId, null, null, null, null);
        loadingPlacements.add(placementId);
        RewardedAd.load(
            host.getPluginContext(),
            adUnitId,
            new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedAd rewardedAd) {
                    loadingPlacements.remove(placementId);
                    FullscreenPlacementState currentState = getState(placementId);
                    if (disposedPlacements.contains(placementId) || !currentState.matchesActiveRequest(requestToken)) {
                        events.log("warn", "rewarded", "callback_skip_disposed", "Rewarded loaded callback ignored because this placement is already disposed or stale.", placementId, null, null, null, null);
                        return;
                    }
                    rewardedAds.put(placementId, rewardedAd);
                    currentState.markLoaded();
                    logState("debug", placementId, "state_transition", "Rewarded state: " + currentState.status + ".");
                    wireCallbacks(placementId, rewardedAd);
                    events.emit("rewarded", placementId, "loaded", null, "Rewarded ad loaded.");
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    loadingPlacements.remove(placementId);
                    FullscreenPlacementState currentState = getState(placementId);
                    if (disposedPlacements.contains(placementId) || !currentState.matchesActiveRequest(requestToken)) {
                        events.log("warn", "rewarded", "callback_skip_disposed", "Rewarded failed callback ignored because this placement is already disposed or stale.", placementId, null, null, null, null);
                        return;
                    }
                    rewardedAds.remove(placementId);
                    currentState.markFailed(String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                    logState("warn", placementId, "state_transition", "Rewarded state: " + currentState.status + ".");
                    events.emit("rewarded", placementId, "failed", String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                }
            }
        );
        call.resolve(PluginResultHelper.success("loading"));
    }

    public void isReady(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        boolean ready = rewardedAds.get(placementId) != null;
        if (!ready) {
            FullscreenPlacementState state = placementStates.get(placementId);
            if (state != null) {
                logState("debug", placementId, "ready_check_not_ready", "Rewarded ready check returned false. state=" + state.status + ", disposed=" + state.disposed + ", showing=" + state.showing + ", lastErrorCode=" + state.lastErrorCode + ".");
            }
        }
        call.resolve(PluginResultHelper.success(ready ? "ready" : "not_ready", PluginResultHelper.readyPayload(ready)));
    }

    public void show(PluginCall call) {
        final String placementId = host.requireTrimmed(call, "placementId");
        RewardedAd rewardedAd = rewardedAds.get(placementId);
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            logState("warn", placementId, "show_activity_unavailable", "Rewarded show failed because activity is unavailable.");
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for rewarded show.", "not_ready"));
            return;
        }
        if (rewardedAd == null) {
            logState("warn", placementId, "show_not_ready", "Rewarded show failed because this placement is not ready.");
            call.resolve(PluginResultHelper.failure("NOT_READY", "Rewarded ad is not ready.", "not_ready"));
            return;
        }
        if (disposedPlacements.contains(placementId)) {
            events.log("warn", "rewarded", "show_skip_disposed", "Rewarded show skipped because this placement is already disposed.", placementId, null, null, null, null);
            call.resolve(PluginResultHelper.failure("NOT_READY", "Rewarded ad is already disposed.", "not_ready"));
            return;
        }
        FullscreenPlacementState state = getState(placementId);
        if (state.showing) {
            logState("warn", placementId, "show_skip_already_showing", "Rewarded show skipped because this placement is already showing.");
            call.resolve(PluginResultHelper.failure("NOT_READY", "Rewarded ad is already showing.", "not_ready"));
            return;
        }

        if (!fullscreenCoordinator.tryAcquire("rewarded", placementId)) {
            call.resolve(PluginResultHelper.failure("FULLSCREEN_ALREADY_SHOWING", "Another fullscreen ad is already showing.", "not_ready"));
            return;
        }
        state.markShowing();
        logState("debug", placementId, "state_transition", "Rewarded state: " + state.status + ".");
        events.log("info", "rewarded", "show_start", "Rewarded show started.", placementId, null, null, null, null);
        releaseSystemUiIfNeeded(activity);
        rewardedAd.show(activity, rewardItem -> {
            if (disposedPlacements.contains(placementId)) {
                return;
            }
            getState(placementId).recordPhase("reward_earned");
            logState("debug", placementId, "callback_order", "Rewarded callback: reward_earned.");
            String message = "Reward earned.";
            if (rewardItem != null) {
                message = "Reward earned: " + rewardItem.getAmount() + " " + rewardItem.getType();
            }
            events.emit("rewarded", placementId, "reward_earned", null, message, null, AdEventDataBuilder.reward(rewardItem == null ? 0 : rewardItem.getAmount(), rewardItem == null ? "" : rewardItem.getType()));
        });
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void clearAll() {
        disposedPlacements.addAll(rewardedAds.keySet());
        for (String placementId : rewardedAds.keySet()) {
            FullscreenPlacementState state = getState(placementId);
            state.markDisposed();
            logState("debug", placementId, "state_transition", "Rewarded state: " + state.status + ".");
        }
        rewardedAds.clear();
        loadingPlacements.clear();
    }

    private void wireCallbacks(final String placementId, final RewardedAd rewardedAd) {
        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                FullscreenPlacementState state = getState(placementId);
                state.recordPhase("shown");
                logState("debug", placementId, "callback_order", "Rewarded callback: shown.");
                releaseSystemUiIfNeeded(host.getPluginActivity());
                events.emit("rewarded", placementId, "shown", null, "Rewarded ad shown.", null, AdEventDataBuilder.fullscreen(host.getPluginActivity(), true));
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                disposedPlacements.add(placementId);
                rewardedAds.remove(placementId);
                FullscreenPlacementState state = getState(placementId);
                state.markDismissed();
                state.recordPhase("dismissed");
                logState("debug", placementId, "callback_order", "Rewarded callback: dismissed.");
                logState("debug", placementId, "state_transition", "Rewarded state: " + state.status + ".");
                fullscreenCoordinator.release("rewarded", placementId);
                events.emit("rewarded", placementId, "dismissed", null, "Rewarded ad dismissed.", null, AdEventDataBuilder.fullscreen(host.getPluginActivity(), false));
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                disposedPlacements.add(placementId);
                rewardedAds.remove(placementId);
                FullscreenPlacementState state = getState(placementId);
                state.markFailed(String.valueOf(adError.getCode()), adError.getMessage());
                state.recordPhase("failed");
                logState("warn", placementId, "callback_order", "Rewarded callback: failed_to_show.");
                logState("warn", placementId, "state_transition", "Rewarded state: " + state.status + ".");
                fullscreenCoordinator.release("rewarded", placementId);
                events.emit("rewarded", placementId, "failed", String.valueOf(adError.getCode()), adError.getMessage(), null, AdEventDataBuilder.fullscreen(host.getPluginActivity(), false));
            }

            @Override
            public void onAdClicked() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                getState(placementId).recordPhase("clicked");
                logState("debug", placementId, "callback_order", "Rewarded callback: clicked.");
                releaseSystemUiIfNeeded(host.getPluginActivity());
                events.emit("rewarded", placementId, "clicked", null, "Rewarded ad clicked.");
            }

            @Override
            public void onAdImpression() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                getState(placementId).recordPhase("impression");
                logState("debug", placementId, "callback_order", "Rewarded callback: impression.");
                events.emit("rewarded", placementId, "impression", null, "Rewarded ad impression recorded.");
            }
        });
    }

    private FullscreenPlacementState getState(String placementId) {
        return placementStates.computeIfAbsent(placementId, ignored -> new FullscreenPlacementState());
    }

    private void logState(String level, String placementId, String code, String message) {
        events.log(level, "rewarded", code, message, placementId, null, null, null, null);
    }

    private String resolveAdUnitId(PluginCall call, String placementId, String explicitAdUnitId, String testAdPreset) {
        return TestAdPresetResolver.resolve(
            call,
            runtimeConfig.isTestMode(),
            explicitAdUnitId,
            testAdPreset,
            runtimeConfig.resolvePlacement(placementId),
            "rewarded"
        );
    }

    private void releaseSystemUiIfNeeded(Activity activity) {
        if (!runtimeConfig.isReleaseSystemUiOnAdInteraction()) {
            return;
        }
        SystemUiHelper.releaseForAdInteraction(activity);
    }

    public interface RewardedHost {
        android.content.Context getPluginContext();
        Activity getPluginActivity();
        String requireTrimmed(PluginCall call, String key);
    }
}

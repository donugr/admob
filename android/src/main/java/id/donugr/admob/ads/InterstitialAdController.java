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
import id.donugr.admob.events.AdEventDataBuilder;
import id.donugr.admob.util.SystemUiHelper;
import id.donugr.admob.util.TestAdPresetResolver;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InterstitialAdController {
    private final InterstitialHost host;
    private final RuntimeConfig runtimeConfig;
    private final AdEventDispatcher events;
    private final FullscreenAdCoordinator fullscreenCoordinator;
    private final Map<String, InterstitialAd> interstitialAds = new ConcurrentHashMap<>();
    private final Map<String, FullscreenPlacementState> placementStates = new ConcurrentHashMap<>();
    private final Set<String> loadingPlacements = ConcurrentHashMap.newKeySet();
    private final Set<String> disposedPlacements = ConcurrentHashMap.newKeySet();

    public InterstitialAdController(InterstitialHost host, RuntimeConfig runtimeConfig, AdEventDispatcher events, FullscreenAdCoordinator fullscreenCoordinator) {
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
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "Missing placementId or interstitial ad unit id.", "error"));
            return;
        }
        if (loadingPlacements.contains(placementId)) {
            logState("debug", placementId, "preload_skip_loading", "Interstitial preload skipped because this placement is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        disposedPlacements.remove(placementId);
        FullscreenPlacementState state = getState(placementId);
        long requestToken = state.markLoading();
        logState("debug", placementId, "state_transition", "Interstitial state: " + state.status + ".");
        events.log("info", "interstitial", "preload_start", "Interstitial preload started.", placementId, null, null, null, null);
        loadingPlacements.add(placementId);
        InterstitialAd.load(
            host.getPluginContext(),
            adUnitId,
            new AdRequest.Builder().build(),
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(InterstitialAd interstitialAd) {
                    loadingPlacements.remove(placementId);
                    FullscreenPlacementState currentState = getState(placementId);
                    if (disposedPlacements.contains(placementId) || !currentState.matchesActiveRequest(requestToken)) {
                        events.log("warn", "interstitial", "callback_skip_disposed", "Interstitial loaded callback ignored because this placement is already disposed or stale.", placementId, null, null, null, null);
                        return;
                    }
                    interstitialAds.put(placementId, interstitialAd);
                    currentState.markLoaded();
                    logState("debug", placementId, "state_transition", "Interstitial state: " + currentState.status + ".");
                    wireCallbacks(placementId, interstitialAd);
                    events.emit("interstitial", placementId, "loaded", null, "Interstitial loaded.");
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    loadingPlacements.remove(placementId);
                    FullscreenPlacementState currentState = getState(placementId);
                    if (disposedPlacements.contains(placementId) || !currentState.matchesActiveRequest(requestToken)) {
                        events.log("warn", "interstitial", "callback_skip_disposed", "Interstitial failed callback ignored because this placement is already disposed or stale.", placementId, null, null, null, null);
                        return;
                    }
                    interstitialAds.remove(placementId);
                    currentState.markFailed(String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                    logState("warn", placementId, "state_transition", "Interstitial state: " + currentState.status + ".");
                    events.emit("interstitial", placementId, "failed", String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                }
            }
        );
        call.resolve(PluginResultHelper.success("loading"));
    }

    public void isReady(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        boolean ready = interstitialAds.get(placementId) != null;
        if (!ready) {
            FullscreenPlacementState state = placementStates.get(placementId);
            if (state != null) {
                logState("debug", placementId, "ready_check_not_ready", "Interstitial ready check returned false. state=" + state.status + ", disposed=" + state.disposed + ", showing=" + state.showing + ", lastErrorCode=" + state.lastErrorCode + ".");
            }
        }
        call.resolve(PluginResultHelper.success(ready ? "ready" : "not_ready", PluginResultHelper.readyPayload(ready)));
    }

    public void show(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        InterstitialAd interstitialAd = interstitialAds.get(placementId);
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            logState("warn", placementId, "show_activity_unavailable", "Interstitial show failed because activity is unavailable.");
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for interstitial show.", "not_ready"));
            return;
        }
        if (interstitialAd == null) {
            logState("warn", placementId, "show_not_ready", "Interstitial show failed because this placement is not ready.");
            call.resolve(PluginResultHelper.failure("NOT_READY", "Interstitial is not ready.", "not_ready"));
            return;
        }
        if (disposedPlacements.contains(placementId)) {
            events.log("warn", "interstitial", "show_skip_disposed", "Interstitial show skipped because this placement is already disposed.", placementId, null, null, null, null);
            call.resolve(PluginResultHelper.failure("NOT_READY", "Interstitial is already disposed.", "not_ready"));
            return;
        }
        FullscreenPlacementState state = getState(placementId);
        if (state.showing) {
            logState("warn", placementId, "show_skip_already_showing", "Interstitial show skipped because this placement is already showing.");
            call.resolve(PluginResultHelper.failure("NOT_READY", "Interstitial is already showing.", "not_ready"));
            return;
        }
        if (!fullscreenCoordinator.tryAcquire("interstitial", placementId)) {
            call.resolve(PluginResultHelper.failure("FULLSCREEN_ALREADY_SHOWING", "Another fullscreen ad is already showing.", "not_ready"));
            return;
        }
        state.markShowing();
        logState("debug", placementId, "state_transition", "Interstitial state: " + state.status + ".");
        events.log("info", "interstitial", "show_start", "Interstitial show started.", placementId, null, null, null, null);
        releaseSystemUiIfNeeded(activity);
        interstitialAd.show(activity);
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void clearAll() {
        disposedPlacements.addAll(interstitialAds.keySet());
        for (String placementId : interstitialAds.keySet()) {
            FullscreenPlacementState state = getState(placementId);
            state.markDisposed();
            logState("debug", placementId, "state_transition", "Interstitial state: " + state.status + ".");
        }
        interstitialAds.clear();
        loadingPlacements.clear();
    }

    private void wireCallbacks(final String placementId, final InterstitialAd interstitialAd) {
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                if (disposedPlacements.contains(placementId)) {
                    events.log("warn", "interstitial", "callback_skip_disposed", "Interstitial shown callback ignored because this placement is already disposed.", placementId, null, null, null, null);
                    return;
                }
                FullscreenPlacementState state = getState(placementId);
                state.recordPhase("shown");
                logState("debug", placementId, "callback_order", "Interstitial callback: shown.");
                releaseSystemUiIfNeeded(host.getPluginActivity());
                events.emit("interstitial", placementId, "shown", null, "Interstitial shown.", null, AdEventDataBuilder.fullscreen(host.getPluginActivity(), true));
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                disposedPlacements.add(placementId);
                interstitialAds.remove(placementId);
                FullscreenPlacementState state = getState(placementId);
                state.markDismissed();
                state.recordPhase("dismissed");
                logState("debug", placementId, "callback_order", "Interstitial callback: dismissed.");
                logState("debug", placementId, "state_transition", "Interstitial state: " + state.status + ".");
                fullscreenCoordinator.release("interstitial", placementId);
                events.emit("interstitial", placementId, "dismissed", null, "Interstitial dismissed.", null, AdEventDataBuilder.fullscreen(host.getPluginActivity(), false));
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                disposedPlacements.add(placementId);
                interstitialAds.remove(placementId);
                FullscreenPlacementState state = getState(placementId);
                state.markFailed(String.valueOf(adError.getCode()), adError.getMessage());
                state.recordPhase("failed");
                logState("warn", placementId, "callback_order", "Interstitial callback: failed_to_show.");
                logState("warn", placementId, "state_transition", "Interstitial state: " + state.status + ".");
                fullscreenCoordinator.release("interstitial", placementId);
                events.emit("interstitial", placementId, "failed", String.valueOf(adError.getCode()), adError.getMessage(), null, AdEventDataBuilder.fullscreen(host.getPluginActivity(), false));
            }

            @Override
            public void onAdClicked() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                getState(placementId).recordPhase("clicked");
                logState("debug", placementId, "callback_order", "Interstitial callback: clicked.");
                releaseSystemUiIfNeeded(host.getPluginActivity());
                events.emit("interstitial", placementId, "clicked", null, "Interstitial clicked.");
            }

            @Override
            public void onAdImpression() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                getState(placementId).recordPhase("impression");
                logState("debug", placementId, "callback_order", "Interstitial callback: impression.");
                events.emit("interstitial", placementId, "impression", null, "Interstitial impression recorded.");
            }
        });
    }

    private FullscreenPlacementState getState(String placementId) {
        return placementStates.computeIfAbsent(placementId, ignored -> new FullscreenPlacementState());
    }

    private void logState(String level, String placementId, String code, String message) {
        events.log(level, "interstitial", code, message, placementId, null, null, null, null);
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

    private void releaseSystemUiIfNeeded(Activity activity) {
        if (!runtimeConfig.isReleaseSystemUiOnAdInteraction()) {
            return;
        }
        SystemUiHelper.releaseForAdInteraction(activity);
    }

    public interface InterstitialHost {
        android.content.Context getPluginContext();
        Activity getPluginActivity();
        String requireTrimmed(PluginCall call, String key);
    }
}

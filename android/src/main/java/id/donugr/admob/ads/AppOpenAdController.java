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
import id.donugr.admob.events.AdEventDataBuilder;
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
    private final FullscreenAdCoordinator fullscreenCoordinator;
    private final Map<String, AppOpenAd> appOpenAds = new ConcurrentHashMap<>();
    private final Map<String, Long> loadTimes = new ConcurrentHashMap<>();
    private final Map<String, FullscreenPlacementState> placementStates = new ConcurrentHashMap<>();
    private final Set<String> loadingPlacements = ConcurrentHashMap.newKeySet();
    private final Set<String> disposedPlacements = ConcurrentHashMap.newKeySet();

    public AppOpenAdController(AppOpenHost host, RuntimeConfig runtimeConfig, AdEventDispatcher events, FullscreenAdCoordinator fullscreenCoordinator) {
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
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "Missing placementId or app open ad unit id.", "error"));
            return;
        }
        if (loadingPlacements.contains(placementId)) {
            logState("debug", placementId, "preload_skip_loading", "App open preload skipped because this placement is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        disposedPlacements.remove(placementId);
        FullscreenPlacementState state = getState(placementId);
        long requestToken = state.markLoading();
        logState("debug", placementId, "state_transition", "App open state: " + state.status + ".");
        events.log("info", "app_open", "preload_start", "App open preload started.", placementId, null, null, null, null);
        loadingPlacements.add(placementId);
        AppOpenAd.load(
            host.getPluginContext(),
            adUnitId,
            new AdRequest.Builder().build(),
            new AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(AppOpenAd appOpenAd) {
                    loadingPlacements.remove(placementId);
                    FullscreenPlacementState currentState = getState(placementId);
                    if (disposedPlacements.contains(placementId) || !currentState.matchesActiveRequest(requestToken)) {
                        events.log("warn", "app_open", "callback_skip_disposed", "App open loaded callback ignored because this placement is already disposed or stale.", placementId, null, null, null, null);
                        return;
                    }
                    appOpenAds.put(placementId, appOpenAd);
                    loadTimes.put(placementId, new Date().getTime());
                    currentState.markLoaded();
                    logState("debug", placementId, "state_transition", "App open state: " + currentState.status + ".");
                    wireCallbacks(placementId, appOpenAd);
                    events.emit("app_open", placementId, "loaded", null, "App open ad loaded.");
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    loadingPlacements.remove(placementId);
                    FullscreenPlacementState currentState = getState(placementId);
                    if (disposedPlacements.contains(placementId) || !currentState.matchesActiveRequest(requestToken)) {
                        events.log("warn", "app_open", "callback_skip_disposed", "App open failed callback ignored because this placement is already disposed or stale.", placementId, null, null, null, null);
                        return;
                    }
                    appOpenAds.remove(placementId);
                    loadTimes.remove(placementId);
                    currentState.markFailed(String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                    logState("warn", placementId, "state_transition", "App open state: " + currentState.status + ".");
                    events.emit("app_open", placementId, "failed", String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                }
            }
        );
        call.resolve(PluginResultHelper.success("loading"));
    }

    public void isReady(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        boolean ready = appOpenAds.get(placementId) != null && isFresh(placementId);
        if (!ready) {
            FullscreenPlacementState state = placementStates.get(placementId);
            if (state != null) {
                logState("debug", placementId, "ready_check_not_ready", "App open ready check returned false. state=" + state.status + ", disposed=" + state.disposed + ", showing=" + state.showing + ", fresh=" + isFresh(placementId) + ", lastErrorCode=" + state.lastErrorCode + ".");
            }
        }
        call.resolve(PluginResultHelper.success(ready ? "ready" : "not_ready", PluginResultHelper.readyPayload(ready)));
    }

    public void show(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        Activity activity = host.getPluginActivity();
        AppOpenAd appOpenAd = appOpenAds.get(placementId);
        if (activity == null) {
            logState("warn", placementId, "show_activity_unavailable", "App open show failed because activity is unavailable.");
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for app open show.", "not_ready"));
            return;
        }
        if (appOpenAd == null || !isFresh(placementId)) {
            logState("warn", placementId, "show_expired_or_not_ready", "App open show failed because this placement is not ready or has expired.");
            appOpenAds.remove(placementId);
            loadTimes.remove(placementId);
            call.resolve(PluginResultHelper.failure("NOT_READY", "App open ad is not ready or has expired.", "not_ready"));
            return;
        }
        if (disposedPlacements.contains(placementId)) {
            events.log("warn", "app_open", "show_skip_disposed", "App open show skipped because this placement is already disposed.", placementId, null, null, null, null);
            call.resolve(PluginResultHelper.failure("NOT_READY", "App open ad is already disposed.", "not_ready"));
            return;
        }
        FullscreenPlacementState state = getState(placementId);
        if (state.showing) {
            logState("warn", placementId, "show_skip_already_showing", "App open show skipped because this placement is already showing.");
            call.resolve(PluginResultHelper.failure("NOT_READY", "App open ad is already showing.", "not_ready"));
            return;
        }
        if (!fullscreenCoordinator.tryAcquire("app_open", placementId)) {
            call.resolve(PluginResultHelper.failure("FULLSCREEN_ALREADY_SHOWING", "Another fullscreen ad is already showing.", "not_ready"));
            return;
        }
        state.markShowing();
        logState("debug", placementId, "state_transition", "App open state: " + state.status + ".");
        events.log("info", "app_open", "show_start", "App open show started.", placementId, null, null, null, null);
        releaseSystemUiIfNeeded(activity);
        appOpenAd.show(activity);
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void clearAll() {
        disposedPlacements.addAll(appOpenAds.keySet());
        for (String placementId : appOpenAds.keySet()) {
            FullscreenPlacementState state = getState(placementId);
            state.markDisposed();
            logState("debug", placementId, "state_transition", "App open state: " + state.status + ".");
        }
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
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                FullscreenPlacementState state = getState(placementId);
                state.recordPhase("shown");
                logState("debug", placementId, "callback_order", "App open callback: shown.");
                releaseSystemUiIfNeeded(host.getPluginActivity());
                events.emit("app_open", placementId, "shown", null, "App open ad shown.", null, AdEventDataBuilder.fullscreen(host.getPluginActivity(), true));
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                disposedPlacements.add(placementId);
                appOpenAds.remove(placementId);
                loadTimes.remove(placementId);
                FullscreenPlacementState state = getState(placementId);
                state.markDismissed();
                state.recordPhase("dismissed");
                logState("debug", placementId, "callback_order", "App open callback: dismissed.");
                logState("debug", placementId, "state_transition", "App open state: " + state.status + ".");
                fullscreenCoordinator.release("app_open", placementId);
                events.emit("app_open", placementId, "dismissed", null, "App open ad dismissed.", null, AdEventDataBuilder.fullscreen(host.getPluginActivity(), false));
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                disposedPlacements.add(placementId);
                appOpenAds.remove(placementId);
                loadTimes.remove(placementId);
                FullscreenPlacementState state = getState(placementId);
                state.markFailed(String.valueOf(adError.getCode()), adError.getMessage());
                state.recordPhase("failed");
                logState("warn", placementId, "callback_order", "App open callback: failed_to_show.");
                logState("warn", placementId, "state_transition", "App open state: " + state.status + ".");
                fullscreenCoordinator.release("app_open", placementId);
                events.emit("app_open", placementId, "failed", String.valueOf(adError.getCode()), adError.getMessage(), null, AdEventDataBuilder.fullscreen(host.getPluginActivity(), false));
            }

            @Override
            public void onAdClicked() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                getState(placementId).recordPhase("clicked");
                logState("debug", placementId, "callback_order", "App open callback: clicked.");
                releaseSystemUiIfNeeded(host.getPluginActivity());
                events.emit("app_open", placementId, "clicked", null, "App open ad clicked.");
            }

            @Override
            public void onAdImpression() {
                if (disposedPlacements.contains(placementId)) {
                    return;
                }
                getState(placementId).recordPhase("impression");
                logState("debug", placementId, "callback_order", "App open callback: impression.");
                events.emit("app_open", placementId, "impression", null, "App open ad impression recorded.");
            }
        });
    }

    private FullscreenPlacementState getState(String placementId) {
        return placementStates.computeIfAbsent(placementId, ignored -> new FullscreenPlacementState());
    }

    private void logState(String level, String placementId, String code, String message) {
        events.log(level, "app_open", code, message, placementId, null, null, null, null);
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

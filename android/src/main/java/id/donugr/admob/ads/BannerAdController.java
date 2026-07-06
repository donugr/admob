package id.donugr.admob.ads;

import android.app.Activity;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import com.getcapacitor.PluginCall;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import id.donugr.admob.core.PluginResultHelper;
import id.donugr.admob.core.RuntimeConfig;
import id.donugr.admob.events.AdEventDispatcher;
import id.donugr.admob.util.SystemUiHelper;
import id.donugr.admob.util.TestAdPresetResolver;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BannerAdController {
    private static final String BANNER_HOST_PREFIX = "donugr-admob:banner:";
    private static final long DUPLICATE_LOADED_WINDOW_MS = 1500L;
    private static final long DUPLICATE_IMPRESSION_WINDOW_MS = 2500L;

    private final BannerHost host;
    private final RuntimeConfig runtimeConfig;
    private final AdEventDispatcher events;
    private final Map<String, AdView> bannerViews = new ConcurrentHashMap<>();
    private final Map<String, FrameLayout> bannerContainers = new ConcurrentHashMap<>();
    private final Map<String, BannerEmissionState> emissionStates = new ConcurrentHashMap<>();

    public BannerAdController(BannerHost host, RuntimeConfig runtimeConfig, AdEventDispatcher events) {
        this.host = host;
        this.runtimeConfig = runtimeConfig;
        this.events = events;
    }

    public void showBanner(PluginCall call) {
        if (!runtimeConfig.isEnabled()) {
            call.resolve(PluginResultHelper.failure("ADS_DISABLED", "Ads are disabled.", "disabled"));
            return;
        }

        final Activity activity = host.getPluginActivity();
        if (activity == null) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for banner display.", "not_ready"));
            return;
        }

        final String placementId = host.requireTrimmed(call, "placementId");
        final String position = "top".equals(host.requireTrimmed(call, "position")) ? "top" : "bottom";
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
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "Missing placementId or banner ad unit id.", "error"));
            return;
        }

        activity.runOnUiThread(() -> {
            destroyBannerInternal(placementId, false);
            resetEmissionState(placementId);

            FrameLayout container = ensureBannerContainer(activity, placementId, position);
            if (container == null) {
                call.resolve(PluginResultHelper.failure("NOT_READY", "Unable to create a banner host container.", "not_ready"));
                return;
            }

            AdView adView = new AdView(activity);
            adView.setAdUnitId(adUnitId);
            adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, resolveAdaptiveBannerWidth(activity)));
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    if (shouldSuppressLoadedEvent(placementId)) {
                        events.emit("banner", placementId, "banner_loaded_duplicate_ignored", null, "Duplicate banner loaded event ignored for the active placement.");
                        return;
                    }
                    recordLoadedEmission(placementId);
                    events.emit("banner", placementId, "loaded", null, "Banner loaded.");
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    if (shouldSuppressFailedAfterReady(placementId)) {
                        events.emit(
                            "banner",
                            placementId,
                            "banner_failed_after_ready_ignored",
                            null,
                            "Banner failed callback ignored because this placement is already stable for the active load. code=" +
                                loadAdError.getCode() +
                                ", message=" + loadAdError.getMessage()
                        );
                        return;
                    }
                    recordPhase(placementId, "failed");
                    events.emit("banner", placementId, "failed", String.valueOf(loadAdError.getCode()), loadAdError.getMessage());
                }

                @Override
                public void onAdOpened() {
                    recordPhase(placementId, "shown");
                    events.emit("banner", placementId, "shown", null, "Banner opened.");
                }

                @Override
                public void onAdClosed() {
                    recordPhase(placementId, "dismissed");
                    events.emit("banner", placementId, "dismissed", null, "Banner closed.");
                }

                @Override
                public void onAdClicked() {
                    releaseSystemUiIfNeeded(host.getPluginActivity());
                    recordPhase(placementId, "clicked");
                    events.emit("banner", placementId, "clicked", null, "Banner clicked.");
                }

                @Override
                public void onAdImpression() {
                    if (shouldSuppressImpressionEvent(placementId)) {
                        events.emit("banner", placementId, "banner_impression_duplicate_ignored", null, "Duplicate banner impression event ignored for the active placement.");
                        return;
                    }
                    recordImpressionEmission(placementId);
                    events.emit("banner", placementId, "impression", null, "Banner impression recorded.");
                }
            });

            container.removeAllViews();
            container.setVisibility(View.VISIBLE);
            container.addView(adView);
            bannerViews.put(placementId, adView);
            adView.loadAd(new AdRequest.Builder().build());
            call.resolve(PluginResultHelper.success("loading"));
        });
    }

    public void hideBanner(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        if (TextUtils.isEmpty(placementId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "placementId is required.", "error"));
            return;
        }
        FrameLayout container = bannerContainers.get(placementId);
        if (container != null) {
            container.setVisibility(View.GONE);
        }
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void destroyBanner(PluginCall call) {
        String placementId = host.requireTrimmed(call, "placementId");
        if (TextUtils.isEmpty(placementId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "placementId is required.", "error"));
            return;
        }
        destroyBannerInternal(placementId, true);
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void clearAll() {
        for (String placementId : bannerViews.keySet()) {
            destroyBannerInternal(placementId, false);
        }
        emissionStates.clear();
    }

    private String resolveAdUnitId(PluginCall call, String placementId, String explicitAdUnitId, String testAdPreset) {
        return TestAdPresetResolver.resolve(
            call,
            runtimeConfig.isTestMode(),
            explicitAdUnitId,
            testAdPreset,
            runtimeConfig.resolvePlacement(placementId),
            "banner"
        );
    }

    private int resolveAdaptiveBannerWidth(Activity activity) {
        View rootView = activity.findViewById(android.R.id.content);
        int widthPixels = rootView != null && rootView.getWidth() > 0 ? rootView.getWidth() : 0;
        if (widthPixels <= 0) {
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            widthPixels = metrics.widthPixels;
        }
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.max(1, Math.round(widthPixels / density));
    }

    private FrameLayout ensureBannerContainer(Activity activity, String placementId, String position) {
        FrameLayout existing = bannerContainers.get(placementId);
        ViewGroup contentRoot = activity.findViewById(android.R.id.content);
        if (contentRoot == null) {
            return null;
        }

        if (existing != null && existing.getParent() instanceof ViewGroup) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) existing.getLayoutParams();
            params.gravity = "top".equals(position) ? Gravity.TOP : Gravity.BOTTOM;
            existing.setLayoutParams(params);
            return existing;
        }

        FrameLayout container = new FrameLayout(activity);
        container.setTag(BANNER_HOST_PREFIX + placementId);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = "top".equals(position) ? Gravity.TOP : Gravity.BOTTOM;
        container.setLayoutParams(params);
        contentRoot.addView(container);
        bannerContainers.put(placementId, container);
        return container;
    }

    private void removeBannerContainer(String placementId) {
        FrameLayout container = bannerContainers.remove(placementId);
        if (container == null) {
            return;
        }
        ViewParent parent = container.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(container);
        }
    }

    private void destroyBannerInternal(String placementId, boolean emitDestroyed) {
        Activity activity = host.getPluginActivity();
        runOnUiThreadBlocking(activity, () -> {
            AdView adView = bannerViews.remove(placementId);
            if (adView != null) {
                adView.destroy();
            }
            removeBannerContainer(placementId);
        });
        emissionStates.remove(placementId);
        if (emitDestroyed) {
            events.emit("banner", placementId, "destroyed", null, "Banner destroyed.");
        }
    }

    private void runOnUiThreadBlocking(Activity activity, Runnable action) {
        if (action == null) {
            return;
        }

        if (activity == null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                action.run();
            }
            return;
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        activity.runOnUiThread(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private BannerEmissionState getEmissionState(String placementId) {
        return emissionStates.computeIfAbsent(placementId, ignored -> new BannerEmissionState());
    }

    private void resetEmissionState(String placementId) {
        emissionStates.put(placementId, new BannerEmissionState());
    }

    private void recordPhase(String placementId, String phase) {
        getEmissionState(placementId).lastEmittedPhase = phase == null ? "" : phase;
    }

    private void recordLoadedEmission(String placementId) {
        BannerEmissionState state = getEmissionState(placementId);
        state.lastLoadedAtEpochMs = System.currentTimeMillis();
        state.lastEmittedPhase = "loaded";
    }

    private void recordImpressionEmission(String placementId) {
        BannerEmissionState state = getEmissionState(placementId);
        state.lastImpressionAtEpochMs = System.currentTimeMillis();
        state.lastEmittedPhase = "impression";
    }

    private boolean shouldSuppressLoadedEvent(String placementId) {
        BannerEmissionState state = getEmissionState(placementId);
        if (state.lastLoadedAtEpochMs <= 0L) {
            return false;
        }
        return System.currentTimeMillis() - state.lastLoadedAtEpochMs <= DUPLICATE_LOADED_WINDOW_MS;
    }

    private boolean shouldSuppressImpressionEvent(String placementId) {
        BannerEmissionState state = getEmissionState(placementId);
        if (state.lastImpressionAtEpochMs <= 0L) {
            return false;
        }
        return System.currentTimeMillis() - state.lastImpressionAtEpochMs <= DUPLICATE_IMPRESSION_WINDOW_MS;
    }

    private boolean shouldSuppressFailedAfterReady(String placementId) {
        BannerEmissionState state = getEmissionState(placementId);
        return state.lastLoadedAtEpochMs > 0L;
    }

    private void releaseSystemUiIfNeeded(Activity activity) {
        if (!runtimeConfig.isReleaseSystemUiOnAdInteraction()) {
            return;
        }
        SystemUiHelper.releaseForAdInteraction(activity);
    }

    public interface BannerHost {
        Activity getPluginActivity();
        String requireTrimmed(PluginCall call, String key);
    }

    private static final class BannerEmissionState {
        long lastLoadedAtEpochMs;
        long lastImpressionAtEpochMs;
        String lastEmittedPhase = "";
    }
}

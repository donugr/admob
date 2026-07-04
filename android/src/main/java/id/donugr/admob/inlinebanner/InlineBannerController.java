package id.donugr.admob.inlinebanner;

import android.app.Activity;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import id.donugr.admob.core.PluginResultHelper;
import id.donugr.admob.core.RuntimeConfig;
import id.donugr.admob.events.AdEventDispatcher;
import id.donugr.admob.util.HostOverlayHelper;
import id.donugr.admob.util.HostOverlayHelper.InlineBannerLayoutContext;
import id.donugr.admob.util.LayoutFingerprintHelper;
import id.donugr.admob.util.RuntimeIdValidator;
import id.donugr.admob.util.TestAdPresetResolver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class InlineBannerController {
    private static final String HOST_CONTAINER_TAG_PREFIX = "donugr-admob:inline-banner:";
    private static final int DEFAULT_INLINE_MARGIN_DP = 16;
    private static final int LAYOUT_JITTER_THRESHOLD_PX = 2;

    private final InlineBannerHost host;
    private final RuntimeConfig runtimeConfig;
    private final AdEventDispatcher events;
    private final InlineBannerSlotStore slotStore = new InlineBannerSlotStore();

    public InlineBannerController(InlineBannerHost host, RuntimeConfig runtimeConfig, AdEventDispatcher events) {
        this.host = host;
        this.runtimeConfig = runtimeConfig;
        this.events = events;
    }

    public void preload(PluginCall call) {
        if (!runtimeConfig.isEnabled()) {
            call.resolve(PluginResultHelper.failure("ADS_DISABLED", "Ads are disabled.", "disabled"));
            return;
        }

        InlineBannerCallOptions options = parseOptions(call, true);
        if (options == null) {
            return;
        }

        InlineBannerSlotState slot = getOrCreateSlot(options.slotId, options.placementId, options.hostId, options.adUnitId);
        if (slot.isLoading()) {
            notifyInlineBannerDebug(options.placementId, options.slotId, "preload_skip_loading", "Inline banner preload skipped because this slot is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        if (isSlotReusable(slot)) {
            notifyInlineBannerDebug(options.placementId, options.slotId, "preload_reused", "Inline banner preload reused the current ready slot.");
            call.resolve(PluginResultHelper.success("ready"));
            return;
        }

        if (hasLoadingSlotForPlacement(options.placementId, options.slotId)) {
            notifyInlineBannerDebug(options.placementId, options.slotId, "preload_skip_loading", "Inline banner preload skipped because another slot for this placement is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        Activity activity = host.getPluginActivity();
        if (activity == null) {
            call.resolve(PluginResultHelper.failure("ACTIVITY_UNAVAILABLE", "Activity is unavailable for inline banner preload.", "not_ready"));
            return;
        }

        AtomicReference<JSObject> resultRef = new AtomicReference<>();
        runOnUiThreadBlocking(activity, () -> {
            AdView adView = createAdView(activity, options);
            if (adView == null) {
                String code = "ADVIEW_UNAVAILABLE";
                String message = "Unable to create inline banner view. " + buildGeometrySummary(options, null);
                slot.markFailed(code, message);
                notifyInlineBannerFailed(slot, code, slot.lastErrorMessage);
                resultRef.set(PluginResultHelper.failure(code, slot.lastErrorMessage, "not_ready"));
                return;
            }

            long requestToken = slot.markLoading(adView);
            notifyInlineBannerDebug(options.placementId, options.slotId, "preload_start", "Inline banner preload started. " + buildGeometrySummary(options, null));
            startLoad(slot, requestToken);
            resultRef.set(PluginResultHelper.success("loading"));
        });

        call.resolve(resultRef.get() != null ? resultRef.get() : PluginResultHelper.failure("NOT_READY", "Inline banner preload did not complete on the UI thread.", "not_ready"));
    }

    public void isReady(PluginCall call) {
        String slotId = host.requireTrimmed(call, "slotId");
        if (TextUtils.isEmpty(slotId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "slotId is required.", "error"));
            return;
        }

        InlineBannerSlotState slot = slotStore.get(slotId);
        boolean ready = slot != null && slot.isReady();
        call.resolve(PluginResultHelper.success(ready ? "ready" : "not_ready", PluginResultHelper.readyPayload(ready)));
    }

    public void attach(PluginCall call) {
        if (!runtimeConfig.isEnabled()) {
            call.resolve(PluginResultHelper.failure("ADS_DISABLED", "Ads are disabled.", "disabled"));
            return;
        }

        InlineBannerCallOptions options = parseOptions(call, true);
        if (options == null) {
            return;
        }

        InlineBannerSlotState slot = slotStore.get(options.slotId);
        if (slot == null) {
            notifyInlineBannerDebug(options.placementId, options.slotId, "preload_skip_loading", "Inline banner attach failed because the slot does not exist. " + buildGeometrySummary(options, null));
            call.resolve(PluginResultHelper.failure("SLOT_NOT_FOUND", "Inline banner slot does not exist yet.", "not_ready"));
            return;
        }
        if (!slot.isReady()) {
            String message = "Inline banner slot is not ready yet. state=" + slot.status +
                ", loading=" + slot.loading +
                ", lastErrorCode=" + slot.lastErrorCode +
                ", lastErrorMessage=" + slot.lastErrorMessage;
            notifyInlineBannerDebug(options.placementId, options.slotId, "preload_skip_loading", message + " " + buildGeometrySummary(options, null));
            call.resolve(PluginResultHelper.failure("SLOT_NOT_READY", message, "not_ready"));
            return;
        }

        Activity activity = host.getPluginActivity();
        if (activity == null) {
            slot.markFailed("ACTIVITY_UNAVAILABLE", "Activity is unavailable for inline banner attach.");
            notifyInlineBannerFailed(slot, "ACTIVITY_UNAVAILABLE", slot.lastErrorMessage);
            call.resolve(PluginResultHelper.failure("ACTIVITY_UNAVAILABLE", slot.lastErrorMessage, "not_ready"));
            return;
        }

        ViewGroup contentRoot = activity.findViewById(android.R.id.content);
        if (contentRoot == null) {
            String message = "Unable to resolve inline banner content root. " + buildGeometrySummary(options, null);
            slot.markFailed("HOST_CONTAINER_UNAVAILABLE", message);
            notifyInlineBannerFailed(slot, "HOST_CONTAINER_UNAVAILABLE", slot.lastErrorMessage);
            call.resolve(PluginResultHelper.failure("HOST_CONTAINER_UNAVAILABLE", slot.lastErrorMessage, "not_ready"));
            return;
        }

        View bridgeWebView = host.getBridgeWebView();
        if (bridgeWebView == null) {
            String message = "Capacitor bridge WebView is unavailable for inline banner attach. " + buildGeometrySummary(options, null);
            slot.markFailed("WEBVIEW_UNAVAILABLE", message);
            notifyInlineBannerFailed(slot, "WEBVIEW_UNAVAILABLE", slot.lastErrorMessage);
            call.resolve(PluginResultHelper.failure("WEBVIEW_UNAVAILABLE", slot.lastErrorMessage, "not_ready"));
            return;
        }

        InlineBannerLayoutContext layoutContext = HostOverlayHelper.createInlineBannerLayoutContext(
            contentRoot,
            bridgeWebView,
            options.hostX,
            options.hostY,
            options.hostWidth,
            options.hostHeight,
            options.hostAnchor
        );
        String geometrySummary = buildGeometrySummary(options, layoutContext);

        if (layoutContext.partialRect) {
            String message = "hostRect must include x, y, and width when any explicit inline hostRect values are provided. " + geometrySummary;
            slot.markFailed("HOST_RECT_INVALID", message);
            notifyInlineBannerFailed(slot, "HOST_RECT_INVALID", slot.lastErrorMessage);
            call.resolve(PluginResultHelper.failure("HOST_RECT_INVALID", slot.lastErrorMessage, "error"));
            return;
        }

        if (layoutContext.hasExplicitRect() && !layoutContext.measurable) {
            String message = "Unable to measure WebView/content root for inline banner hostRect normalization. " + geometrySummary;
            slot.markFailed("WEBVIEW_UNAVAILABLE", message);
            notifyInlineBannerFailed(slot, "WEBVIEW_UNAVAILABLE", slot.lastErrorMessage);
            call.resolve(PluginResultHelper.failure("WEBVIEW_UNAVAILABLE", slot.lastErrorMessage, "not_ready"));
            return;
        }

        if (layoutContext.hasExplicitRect() && layoutContext.fullyOutOfBounds) {
            String message = "Normalized inline banner hostRect is fully outside the visible native root. " + geometrySummary;
            slot.markFailed("HOST_RECT_OUT_OF_BOUNDS", message);
            notifyInlineBannerFailed(slot, "HOST_RECT_OUT_OF_BOUNDS", slot.lastErrorMessage);
            call.resolve(PluginResultHelper.failure("HOST_RECT_OUT_OF_BOUNDS", slot.lastErrorMessage, "not_ready"));
            return;
        }

        String hostFingerprint = buildHostRectFingerprint(options, layoutContext);
        if (
            slot.isAttachedToHost(options.hostId, hostFingerprint) &&
            slot.adView != null &&
            slot.adView.getParent() instanceof ViewGroup
        ) {
            notifyInlineBannerDebug(options.placementId, options.slotId, "layout_skipped_same_rect", "Inline banner host layout unchanged for this slot. " + geometrySummary);
            notifyInlineBannerDebug(options.placementId, options.slotId, "attach_skipped_same_host", "Inline banner attach skipped because the slot is already attached to the same host. " + geometrySummary);
            call.resolve(PluginResultHelper.success("ready"));
            return;
        }

        AtomicReference<JSObject> resultRef = new AtomicReference<>();
        runOnUiThreadBlocking(activity, () -> {
            FrameLayout hostContainer = resolveHostContainer(contentRoot, options, layoutContext);
            if (hostContainer == null) {
                slot.markFailed("HOST_CONTAINER_UNAVAILABLE", "Unable to resolve inline banner host container. " + geometrySummary);
                notifyInlineBannerFailed(slot, "HOST_CONTAINER_UNAVAILABLE", slot.lastErrorMessage);
                resultRef.set(PluginResultHelper.failure("HOST_CONTAINER_UNAVAILABLE", slot.lastErrorMessage, "not_ready"));
                return;
            }

            cleanupSlotView(slot);
            if (slot.adView == null) {
                slot.markFailed("ADVIEW_UNAVAILABLE", "Inline banner view is unavailable. " + geometrySummary);
                notifyInlineBannerFailed(slot, "ADVIEW_UNAVAILABLE", slot.lastErrorMessage);
                resultRef.set(PluginResultHelper.failure("ADVIEW_UNAVAILABLE", slot.lastErrorMessage, "not_ready"));
                return;
            }

            boolean changed = applyHostContainerLayout(hostContainer, options, layoutContext);
            if (!changed) {
                notifyInlineBannerDebug(options.placementId, options.slotId, "layout_skipped_same_rect", "Inline banner host layout unchanged for this slot after normalization. " + geometrySummary);
            }

            hostContainer.removeAllViews();
            hostContainer.setVisibility(View.VISIBLE);
            hostContainer.addView(slot.adView);
            slot.updateIdentity(options.placementId, options.hostId, options.adUnitId);
            slot.markAttached(options.hostId, hostFingerprint);
            notifyInlineBannerAttached(slot, "Inline banner attached. " + geometrySummary);
            resultRef.set(PluginResultHelper.success("ready"));
        });

        call.resolve(resultRef.get() != null ? resultRef.get() : PluginResultHelper.failure("NOT_READY", "Inline banner attach did not complete on the UI thread.", "not_ready"));
    }

    public void detach(PluginCall call) {
        String slotId = host.requireTrimmed(call, "slotId");
        if (TextUtils.isEmpty(slotId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "slotId is required.", "error"));
            return;
        }

        InlineBannerSlotState slot = slotStore.get(slotId);
        if (slot == null) {
            call.resolve(PluginResultHelper.success("not_ready"));
            return;
        }

        cleanupSlotView(slot);
        slot.markDetached();
        notifyInlineBannerDetached(slot, "Inline banner detached.");
        call.resolve(PluginResultHelper.success(slot.isReady() ? "ready" : "not_ready"));
    }

    public void destroy(PluginCall call) {
        String slotId = host.requireTrimmed(call, "slotId");
        if (TextUtils.isEmpty(slotId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "slotId is required.", "error"));
            return;
        }

        InlineBannerSlotState slot = slotStore.get(slotId);
        if (slot != null) {
            notifyInlineBannerEvent(slot.placementId, slot.slotId, "destroyed", null, "Inline banner destroyed.");
        }
        cleanupAndRemoveSlot(slotId);
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void refresh(PluginCall call) {
        if (!runtimeConfig.isEnabled()) {
            call.resolve(PluginResultHelper.failure("ADS_DISABLED", "Ads are disabled.", "disabled"));
            return;
        }

        InlineBannerCallOptions options = parseOptions(call, true);
        if (options == null) {
            return;
        }

        InlineBannerSlotState currentSlot = slotStore.get(options.slotId);
        if (currentSlot != null && currentSlot.isLoading()) {
            notifyInlineBannerDebug(options.placementId, options.slotId, "preload_skip_loading", "Inline banner refresh skipped because this slot is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        if (hasLoadingSlotForPlacement(options.placementId, options.slotId)) {
            notifyInlineBannerDebug(options.placementId, options.slotId, "preload_skip_loading", "Inline banner refresh skipped because another slot for this placement is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        cleanupAndRemoveSlot(options.slotId);
        preload(call);
    }

    public void clearAll() {
        for (InlineBannerSlotState slot : slotStore.snapshot().values()) {
            cleanupSlot(slot);
        }
        clearAllHostContainers();
        slotStore.clear();
    }

    public String getModuleStatus() {
        return "active";
    }

    public int getActiveSlotsCount() {
        return slotStore.snapshot().size();
    }

    public int getLoadingSlotsCount() {
        int count = 0;
        for (InlineBannerSlotState slot : slotStore.snapshot().values()) {
            if (slot != null && slot.isLoading()) {
                count += 1;
            }
        }
        return count;
    }

    public int getReadySlotsCount() {
        int count = 0;
        for (InlineBannerSlotState slot : slotStore.snapshot().values()) {
            if (slot != null && slot.isReady()) {
                count += 1;
            }
        }
        return count;
    }

    public int getAttachedSlotsCount() {
        int count = 0;
        for (InlineBannerSlotState slot : slotStore.snapshot().values()) {
            if (
                slot != null &&
                InlineBannerSlotState.STATUS_ATTACHED.equals(slot.status) &&
                slot.adView != null &&
                slot.adView.getParent() instanceof ViewGroup
            ) {
                count += 1;
            }
        }
        return count;
    }

    public int getFailedSlotsCount() {
        int count = 0;
        for (InlineBannerSlotState slot : slotStore.snapshot().values()) {
            if (slot != null && InlineBannerSlotState.STATUS_FAILED.equals(slot.status)) {
                count += 1;
            }
        }
        return count;
    }

    private InlineBannerCallOptions parseOptions(PluginCall call, boolean requirePlacement) {
        String placementId = host.requireTrimmed(call, "placementId");
        String slotId = host.requireTrimmed(call, "slotId");
        String hostId = host.requireTrimmed(call, "hostId");
        String explicitAdUnitId = host.requireTrimmed(call, "adUnitId");
        String testAdPreset = host.requireTrimmed(call, "testAdPreset");
        JSObject hostRect = call.getObject("hostRect");
        Integer hostX = parseOptionalInt(hostRect, "x");
        Integer hostY = parseOptionalInt(hostRect, "y");
        Integer hostWidth = parseOptionalInt(hostRect, "width");
        Integer hostHeight = parseOptionalInt(hostRect, "height");
        String hostAnchor = parseHostAnchor(hostRect);

        if (requirePlacement && TextUtils.isEmpty(placementId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "placementId is required.", "error"));
            return null;
        }

        if (!RuntimeIdValidator.isSafe(placementId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_INVALID", "placementId must use only letters, numbers, dot, underscore, colon, or dash.", "error"));
            return null;
        }

        if (TextUtils.isEmpty(slotId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "slotId is required.", "error"));
            return null;
        }

        if (!RuntimeIdValidator.isSafe(slotId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_INVALID", "slotId must use only letters, numbers, dot, underscore, colon, or dash.", "error"));
            return null;
        }

        if (TextUtils.isEmpty(hostId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "hostId is required.", "error"));
            return null;
        }

        if (!RuntimeIdValidator.isSafe(hostId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_INVALID", "hostId must use only letters, numbers, dot, underscore, colon, or dash.", "error"));
            return null;
        }

        String adUnitId = resolveAdUnitId(call, placementId, explicitAdUnitId, testAdPreset);
        if (adUnitId == null) {
            return null;
        }
        if (TextUtils.isEmpty(adUnitId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "Missing ad unit id for inline banner placement \"" + placementId + "\".", "error"));
            return null;
        }

        return new InlineBannerCallOptions(placementId, slotId, hostId, adUnitId, hostX, hostY, hostWidth, hostHeight, hostAnchor);
    }

    private InlineBannerSlotState getOrCreateSlot(String slotId, String placementId, String hostId, String adUnitId) {
        InlineBannerSlotState slot = slotStore.getOrCreate(slotId);
        slot.updateIdentity(placementId, hostId, adUnitId);
        return slot;
    }

    private boolean hasLoadingSlotForPlacement(String placementId, String excludeSlotId) {
        for (InlineBannerSlotState slot : slotStore.snapshot().values()) {
            if (
                slot != null &&
                slot.isLoading() &&
                placementId.equals(slot.placementId) &&
                !slot.slotId.equals(excludeSlotId)
            ) {
                return true;
            }
        }
        return false;
    }

    private boolean isSlotReusable(InlineBannerSlotState slot) {
        return slot != null && slot.isReady();
    }

    private String resolveAdUnitId(PluginCall call, String placementId, String explicitAdUnitId, String testAdPreset) {
        return TestAdPresetResolver.resolve(
            call,
            runtimeConfig.isTestMode(),
            explicitAdUnitId,
            testAdPreset,
            runtimeConfig.resolvePlacement(placementId),
            "inline_banner"
        );
    }

    private void startLoad(InlineBannerSlotState slot, long requestToken) {
        if (slot == null || slot.adView == null) {
            return;
        }

        AdView adView = slot.adView;
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                InlineBannerSlotState current = slotStore.get(slot.slotId);
                if (current == null || !current.matchesActiveRequest(requestToken)) {
                    return;
                }

                current.markReady();
                notifyInlineBannerLoaded(current, "Inline banner loaded.");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                InlineBannerSlotState current = slotStore.get(slot.slotId);
                if (current == null || !current.matchesActiveRequest(requestToken)) {
                    return;
                }

                String code = String.valueOf(loadAdError.getCode());
                String message = loadAdError.getMessage();
                current.markFailed(code, message);
                notifyInlineBannerFailed(current, code, message);
            }

            @Override
            public void onAdOpened() {
                InlineBannerSlotState current = slotStore.get(slot.slotId);
                if (current != null) {
                    notifyInlineBannerEvent(current.placementId, current.slotId, "shown", null, "Inline banner opened.");
                }
            }

            @Override
            public void onAdClosed() {
                InlineBannerSlotState current = slotStore.get(slot.slotId);
                if (current != null) {
                    notifyInlineBannerEvent(current.placementId, current.slotId, "dismissed", null, "Inline banner closed.");
                }
            }

            @Override
            public void onAdClicked() {
                InlineBannerSlotState current = slotStore.get(slot.slotId);
                if (current != null) {
                    notifyInlineBannerClicked(current);
                }
            }

            @Override
            public void onAdImpression() {
                InlineBannerSlotState current = slotStore.get(slot.slotId);
                if (current != null) {
                    notifyInlineBannerImpression(current);
                }
            }
        });
        adView.loadAd(new AdRequest.Builder().build());
    }

    private AdView createAdView(Activity activity, InlineBannerCallOptions options) {
        if (activity == null) {
            return null;
        }

        int widthPx = resolveBannerWidthPx(activity, options);
        int widthDp = pxToDp(widthPx);
        if (widthDp <= 0) {
            return null;
        }

        AdSize adSize = resolveAdSize(activity, options, widthDp);
        AdView adView = new AdView(activity);
        adView.setAdUnitId(options.adUnitId);
        adView.setAdSize(adSize);
        return adView;
    }

    private AdSize resolveAdSize(Activity activity, InlineBannerCallOptions options, int widthDp) {
        int maxHeightDp = pxToDp(options.hostHeight == null ? 0 : options.hostHeight);
        if (maxHeightDp > 0) {
            return AdSize.getInlineAdaptiveBannerAdSize(widthDp, maxHeightDp);
        }
        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, widthDp);
    }

    private int resolveBannerWidthPx(Activity activity, InlineBannerCallOptions options) {
        if (options.hostWidth != null && options.hostWidth > 0) {
            return options.hostWidth;
        }

        View webView = host.getBridgeWebView();
        if (webView != null && webView.getWidth() > 0) {
            return webView.getWidth();
        }

        View rootView = activity.findViewById(android.R.id.content);
        if (rootView != null && rootView.getWidth() > 0) {
            return rootView.getWidth();
        }

        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }

    private int pxToDp(int px) {
        if (px <= 0) {
            return 0;
        }
        float density = host.getPluginContext() != null ? host.getPluginContext().getResources().getDisplayMetrics().density : 1f;
        return Math.max(1, Math.round(px / density));
    }

    private int dpToPx(int dp) {
        float density = host.getPluginContext() != null ? host.getPluginContext().getResources().getDisplayMetrics().density : 1f;
        return Math.round(dp * density);
    }

    private Integer parseOptionalInt(JSObject object, String key) {
        if (object == null || !object.has(key)) {
            return null;
        }

        try {
            return object.getInteger(key);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String parseHostAnchor(JSObject hostRect) {
        if (hostRect == null) {
            return "top";
        }

        String anchor = String.valueOf(hostRect.optString("anchor", "top")).trim().toLowerCase();
        return "bottom".equals(anchor) ? "bottom" : "top";
    }

    private String buildHostRectFingerprint(InlineBannerCallOptions options, InlineBannerLayoutContext layoutContext) {
        if (layoutContext != null && layoutContext.hasExplicitRect()) {
            return LayoutFingerprintHelper.buildHostFingerprint(
                layoutContext.appliedLeft,
                layoutContext.appliedTop,
                layoutContext.appliedWidth,
                options.hostHeight,
                options.hostAnchor
            );
        }
        return LayoutFingerprintHelper.buildHostFingerprint(options.hostX, options.hostY, options.hostWidth, options.hostHeight, options.hostAnchor);
    }

    private boolean applyHostContainerLayout(FrameLayout hostContainer, InlineBannerCallOptions options, InlineBannerLayoutContext layoutContext) {
        FrameLayout.LayoutParams existingParams = hostContainer.getLayoutParams() instanceof FrameLayout.LayoutParams
            ? (FrameLayout.LayoutParams) hostContainer.getLayoutParams()
            : null;
        FrameLayout.LayoutParams params = existingParams == null
            ? new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            : new FrameLayout.LayoutParams(existingParams);

        if (layoutContext != null && layoutContext.hasExplicitRect()) {
            params.width = layoutContext.appliedWidth;
            params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.TOP | Gravity.START;
            params.leftMargin = layoutContext.appliedLeft;
            params.rightMargin = 0;
            params.topMargin = layoutContext.appliedTop;
            params.bottomMargin = 0;
        } else {
            params.width = FrameLayout.LayoutParams.MATCH_PARENT;
            params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.BOTTOM;
            int margin = dpToPx(DEFAULT_INLINE_MARGIN_DP);
            params.leftMargin = margin;
            params.rightMargin = margin;
            params.bottomMargin = margin;
            params.topMargin = 0;
        }

        boolean changed = existingParams == null || !layoutParamsEquivalent(existingParams, params);
        if (changed) {
            hostContainer.setLayoutParams(params);
        }
        return changed;
    }

    private boolean layoutParamsEquivalent(FrameLayout.LayoutParams current, FrameLayout.LayoutParams next) {
        if (current == null || next == null) {
            return false;
        }

        return withinThreshold(current.width, next.width) &&
            current.height == next.height &&
            current.gravity == next.gravity &&
            withinThreshold(current.leftMargin, next.leftMargin) &&
            withinThreshold(current.topMargin, next.topMargin) &&
            withinThreshold(current.rightMargin, next.rightMargin) &&
            withinThreshold(current.bottomMargin, next.bottomMargin);
    }

    private boolean withinThreshold(int current, int next) {
        return Math.abs(current - next) <= LAYOUT_JITTER_THRESHOLD_PX;
    }

    private FrameLayout resolveHostContainer(ViewGroup contentRoot, InlineBannerCallOptions options, InlineBannerLayoutContext layoutContext) {
        String overlayTag = HostOverlayHelper.buildOverlayTag(HOST_CONTAINER_TAG_PREFIX, options.hostId);
        View existing = contentRoot.findViewWithTag(overlayTag);
        if (existing instanceof FrameLayout) {
            return (FrameLayout) existing;
        }

        Activity activity = host.getPluginActivity();
        if (activity == null) {
            return null;
        }

        FrameLayout hostContainer = new FrameLayout(activity);
        hostContainer.setTag(overlayTag);
        applyHostContainerLayout(hostContainer, options, layoutContext);
        hostContainer.setClickable(false);
        hostContainer.setFocusable(false);
        contentRoot.addView(hostContainer);
        return hostContainer;
    }

    private void removeHostContainerIfEmpty(ViewGroup parentView) {
        if (!(parentView instanceof FrameLayout)) {
            return;
        }

        Object tag = parentView.getTag();
        if (!(tag instanceof String) || !String.valueOf(tag).startsWith(HOST_CONTAINER_TAG_PREFIX)) {
            return;
        }

        if (parentView.getChildCount() > 0) {
            return;
        }

        ViewParent containerParent = parentView.getParent();
        if (containerParent instanceof ViewGroup) {
            ((ViewGroup) containerParent).removeView(parentView);
        }
    }

    private void clearAllHostContainers() {
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            return;
        }

        runOnUiThreadBlocking(activity, () -> {
            ViewGroup contentRoot = activity.findViewById(android.R.id.content);
            if (contentRoot == null) {
                return;
            }

            for (int index = contentRoot.getChildCount() - 1; index >= 0; index -= 1) {
                View child = contentRoot.getChildAt(index);
                Object tag = child == null ? null : child.getTag();
                if (tag instanceof String && String.valueOf(tag).startsWith(HOST_CONTAINER_TAG_PREFIX)) {
                    contentRoot.removeViewAt(index);
                }
            }
        });
    }

    private void cleanupSlotView(InlineBannerSlotState slot) {
        if (slot == null || slot.adView == null) {
            return;
        }

        Activity activity = host.getPluginActivity();
        runOnUiThreadBlocking(activity, () -> {
            if (slot.adView != null && slot.adView.getParent() instanceof ViewGroup) {
                ViewGroup parentView = (ViewGroup) slot.adView.getParent();
                parentView.removeView(slot.adView);
                removeHostContainerIfEmpty(parentView);
            }
        });
        slot.clearViewReference();
    }

    private void cleanupSlotAd(InlineBannerSlotState slot) {
        if (slot == null) {
            return;
        }

        slot.clearAdReference();
        if (!InlineBannerSlotState.STATUS_FAILED.equals(slot.status)) {
            slot.status = InlineBannerSlotState.STATUS_IDLE;
        }
        slot.loading = false;
    }

    private void cleanupSlot(InlineBannerSlotState slot) {
        cleanupSlotView(slot);
        cleanupSlotAd(slot);
    }

    private void cleanupAndRemoveSlot(String slotId) {
        InlineBannerSlotState slot = slotStore.remove(slotId);
        cleanupSlot(slot);
    }

    private void runOnUiThreadBlocking(Activity activity, Runnable action) {
        if (activity == null || action == null) {
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

    private void notifyInlineBannerEvent(String placementId, String slotId, String phase, String code, String message) {
        events.emit("inline_banner", placementId, phase, code, message, slotId);
    }

    private void notifyInlineBannerLoaded(InlineBannerSlotState slot, String message) {
        notifyInlineBannerEvent(slot.placementId, slot.slotId, "loaded", null, message);
    }

    private void notifyInlineBannerFailed(InlineBannerSlotState slot, String code, String message) {
        notifyInlineBannerEvent(slot.placementId, slot.slotId, "failed", code, message);
    }

    private void notifyInlineBannerAttached(InlineBannerSlotState slot, String message) {
        notifyInlineBannerEvent(slot.placementId, slot.slotId, "attached", null, message);
    }

    private void notifyInlineBannerDetached(InlineBannerSlotState slot, String message) {
        notifyInlineBannerEvent(slot.placementId, slot.slotId, "detached", null, message);
    }

    private void notifyInlineBannerClicked(InlineBannerSlotState slot) {
        notifyInlineBannerEvent(slot.placementId, slot.slotId, "clicked", null, "Inline banner clicked.");
    }

    private void notifyInlineBannerImpression(InlineBannerSlotState slot) {
        notifyInlineBannerEvent(slot.placementId, slot.slotId, "impression", null, "Inline banner impression recorded.");
    }

    private void notifyInlineBannerDebug(String placementId, String slotId, String phase, String message) {
        notifyInlineBannerEvent(placementId, slotId, phase, null, message);
    }

    private String buildGeometrySummary(InlineBannerCallOptions options, InlineBannerLayoutContext layoutContext) {
        String base = "placementId=" + options.placementId +
            ", slotId=" + options.slotId +
            ", hostId=" + options.hostId;
        if (layoutContext == null) {
            return base + ", rawHostRect={x=" + options.hostX + ", y=" + options.hostY + ", width=" + options.hostWidth + ", height=" + options.hostHeight + ", anchor=" + options.hostAnchor + "}";
        }
        return base + ", " + layoutContext.describeRawRect() + ", " + layoutContext.describeNormalizedRect() + ", " + layoutContext.describeEnvironment();
    }

    private static class InlineBannerCallOptions {
        final String placementId;
        final String slotId;
        final String hostId;
        final String adUnitId;
        final Integer hostX;
        final Integer hostY;
        final Integer hostWidth;
        final Integer hostHeight;
        final String hostAnchor;

        InlineBannerCallOptions(
            String placementId,
            String slotId,
            String hostId,
            String adUnitId,
            Integer hostX,
            Integer hostY,
            Integer hostWidth,
            Integer hostHeight,
            String hostAnchor
        ) {
            this.placementId = placementId;
            this.slotId = slotId;
            this.hostId = hostId;
            this.adUnitId = adUnitId;
            this.hostX = hostX;
            this.hostY = hostY;
            this.hostWidth = hostWidth;
            this.hostHeight = hostHeight;
            this.hostAnchor = hostAnchor;
        }
    }

    public interface InlineBannerHost {
        android.content.Context getPluginContext();
        Activity getPluginActivity();
        View getBridgeWebView();
        String requireTrimmed(PluginCall call, String key);
    }
}

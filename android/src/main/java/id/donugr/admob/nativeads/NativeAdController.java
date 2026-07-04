package id.donugr.admob.nativeads;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import id.donugr.admob.R;
import id.donugr.admob.core.PluginResultHelper;
import id.donugr.admob.core.RuntimeConfig;
import id.donugr.admob.events.AdEventDispatcher;
import id.donugr.admob.util.HostOverlayHelper;
import id.donugr.admob.util.LayoutFingerprintHelper;
import id.donugr.admob.util.RuntimeIdValidator;
import id.donugr.admob.util.TestAdPresetResolver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class NativeAdController {
    private static final int DEFAULT_NATIVE_MARGIN_DP = 16;
    private static final String HOST_CONTAINER_TAG_PREFIX = "donugr-admob:host:";
    private static final long DEFAULT_NATIVE_TTL_MS = 60000L;
    private static final long MIN_NATIVE_TTL_MS = 1000L;
    private static final long MAX_NATIVE_TTL_MS = 3600000L;

    private final NativeHost host;
    private final RuntimeConfig runtimeConfig;
    private final AdEventDispatcher events;
    private final NativeSlotStore slotStore = new NativeSlotStore();

    public NativeAdController(NativeHost host, RuntimeConfig runtimeConfig, AdEventDispatcher events) {
        this.host = host;
        this.runtimeConfig = runtimeConfig;
        this.events = events;
    }

    public void preload(PluginCall call) {
        if (!runtimeConfig.isEnabled()) {
            call.resolve(PluginResultHelper.failure("ADS_DISABLED", "Ads are disabled.", "disabled"));
            return;
        }

        cleanupExpiredSlots();
        NativeCallOptions options = parseNativeOptions(call, true);
        if (options == null) {
            return;
        }

        NativeSlotState slot = getOrCreateSlot(options.slotId, options.placementId, options.hostId, options.adUnitId, options.ttlMs);
        if (slot.isLoading()) {
            notifyNativeDebug(options.placementId, options.slotId, "preload_skip_loading", "Native preload skipped because this slot is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        if (isSlotReusable(slot)) {
            notifyNativeDebug(options.placementId, options.slotId, "preload_reused", "Native preload reused the current ready slot.");
            call.resolve(PluginResultHelper.success("ready"));
            return;
        }

        if (hasLoadingSlotForPlacement(options.placementId, options.slotId)) {
            notifyNativeDebug(options.placementId, options.slotId, "preload_skip_loading", "Native preload skipped because another slot for this placement is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        cleanupSlot(slot);
        long requestToken = slot.markLoading();
        notifyNativeDebug(options.placementId, options.slotId, "preload_start", "Native preload started.");
        startNativeLoad(slot, requestToken);
        call.resolve(PluginResultHelper.success("loading"));
    }

    public void isReady(PluginCall call) {
        cleanupExpiredSlots();
        String slotId = host.requireTrimmed(call, "slotId");
        if (TextUtils.isEmpty(slotId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "slotId is required.", "error"));
            return;
        }

        NativeSlotState slot = slotStore.get(slotId);
        boolean ready = slot != null && slot.isReady(System.currentTimeMillis());
        call.resolve(PluginResultHelper.success(ready ? "ready" : "not_ready", PluginResultHelper.readyPayload(ready)));
    }

    public void attach(PluginCall call) {
        if (!runtimeConfig.isEnabled()) {
            call.resolve(PluginResultHelper.failure("ADS_DISABLED", "Ads are disabled.", "disabled"));
            return;
        }

        cleanupExpiredSlots();
        NativeCallOptions options = parseNativeOptions(call, true);
        if (options == null) {
            return;
        }

        NativeSlotState slot = slotStore.get(options.slotId);
        if (slot == null || !slot.isReady(System.currentTimeMillis())) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Native slot is not ready yet.", "not_ready"));
            return;
        }

        Activity activity = host.getPluginActivity();
        if (activity == null) {
            slot.markFailed("NOT_READY", "Activity is unavailable for native attach.");
            notifyNativeFailed(slot, "NOT_READY", slot.lastErrorMessage);
            call.resolve(PluginResultHelper.failure("NOT_READY", slot.lastErrorMessage, "not_ready"));
            return;
        }

        String hostRectFingerprint = buildHostRectFingerprint(options);
        if (
            slot.isAttachedToHost(options.hostId, hostRectFingerprint, System.currentTimeMillis()) &&
            slot.attachedView != null &&
            slot.attachedView.getParent() instanceof ViewGroup
        ) {
            notifyNativeDebug(options.placementId, options.slotId, "layout_skipped_same_rect", "Native host layout unchanged for this slot.");
            notifyNativeDebug(options.placementId, options.slotId, "attach_skipped_same_host", "Native attach skipped because the slot is already attached to the same host.");
            call.resolve(PluginResultHelper.success("ready"));
            return;
        }

        AtomicReference<JSObject> resultRef = new AtomicReference<>();
        runOnUiThreadBlocking(activity, () -> {
            ViewGroup hostContainer = resolveNativeHostContainer(options);
            if (hostContainer == null) {
                slot.markFailed("NOT_READY", "Unable to resolve native host container.");
                notifyNativeFailed(slot, "NOT_READY", slot.lastErrorMessage);
                resultRef.set(PluginResultHelper.failure("NOT_READY", slot.lastErrorMessage, "not_ready"));
                return;
            }

            cleanupSlotView(slot);
            NativeAdView adView = createAndBindNativeAdView(slot);
            if (adView == null) {
                slot.markFailed("NOT_READY", "Unable to inflate native ad view.");
                notifyNativeFailed(slot, "NOT_READY", slot.lastErrorMessage);
                resultRef.set(PluginResultHelper.failure("NOT_READY", slot.lastErrorMessage, "not_ready"));
                return;
            }

            if (hostContainer instanceof FrameLayout) {
                boolean changed = applyHostContainerLayout((FrameLayout) hostContainer, options);
                if (!changed) {
                    notifyNativeDebug(options.placementId, options.slotId, "layout_skipped_same_rect", "Native host layout unchanged for this slot.");
                }
            }
            hostContainer.removeAllViews();
            hostContainer.addView(adView);
            slot.updateIdentity(options.placementId, options.hostId, options.adUnitId, options.ttlMs);
            slot.markAttached(options.hostId, hostRectFingerprint, adView);
            notifyNativeAttached(slot, "Native ad attached.");
            resultRef.set(PluginResultHelper.success("ready"));
        });

        JSObject result = resultRef.get();
        call.resolve(result != null ? result : PluginResultHelper.failure("NOT_READY", "Native attach did not complete on the UI thread.", "not_ready"));
    }

    public void detach(PluginCall call) {
        cleanupExpiredSlots();
        String slotId = host.requireTrimmed(call, "slotId");
        if (TextUtils.isEmpty(slotId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "slotId is required.", "error"));
            return;
        }

        NativeSlotState slot = slotStore.get(slotId);
        if (slot == null) {
            call.resolve(PluginResultHelper.success("not_ready"));
            return;
        }

        cleanupSlotView(slot);
        slot.markDetached();
        notifyNativeDetached(slot, "Native slot detached.");
        call.resolve(PluginResultHelper.success(slot.isReady(System.currentTimeMillis()) ? "ready" : "not_ready"));
    }

    public void destroy(PluginCall call) {
        cleanupExpiredSlots();
        String slotId = host.requireTrimmed(call, "slotId");
        if (TextUtils.isEmpty(slotId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "slotId is required.", "error"));
            return;
        }

        NativeSlotState slot = slotStore.get(slotId);
        if (slot != null) {
            notifyNativeEvent(slot.placementId, slot.slotId, "destroyed", null, "Native slot destroyed.");
        }
        cleanupAndRemoveSlot(slotId);
        call.resolve(PluginResultHelper.success("ready"));
    }

    public void refresh(PluginCall call) {
        if (!runtimeConfig.isEnabled()) {
            call.resolve(PluginResultHelper.failure("ADS_DISABLED", "Ads are disabled.", "disabled"));
            return;
        }

        cleanupExpiredSlots();
        NativeCallOptions options = parseNativeOptions(call, true);
        if (options == null) {
            return;
        }

        NativeSlotState currentSlot = slotStore.get(options.slotId);
        if (currentSlot != null && currentSlot.isLoading()) {
            notifyNativeDebug(options.placementId, options.slotId, "preload_skip_loading", "Native refresh skipped because this slot is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        if (hasLoadingSlotForPlacement(options.placementId, options.slotId)) {
            notifyNativeDebug(options.placementId, options.slotId, "preload_skip_loading", "Native refresh skipped because another slot for this placement is already loading.");
            call.resolve(PluginResultHelper.success("loading"));
            return;
        }

        cleanupAndRemoveSlot(options.slotId);
        NativeSlotState slot = getOrCreateSlot(options.slotId, options.placementId, options.hostId, options.adUnitId, options.ttlMs);
        long requestToken = slot.markLoading();
        notifyNativeDebug(options.placementId, options.slotId, "preload_start", "Native refresh started a new preload.");
        startNativeLoad(slot, requestToken);
        call.resolve(PluginResultHelper.success("loading"));
    }

    public void clearAll() {
        clearAllSlotsInternal();
    }

    public int getActiveSlotsCount() {
        cleanupExpiredSlots();
        return slotStore.snapshot().size();
    }

    public int getLoadingSlotsCount() {
        cleanupExpiredSlots();
        int count = 0;
        for (NativeSlotState slot : slotStore.snapshot().values()) {
            if (slot != null && slot.isLoading()) {
                count += 1;
            }
        }
        return count;
    }

    public int getAttachedSlotsCount() {
        cleanupExpiredSlots();
        int count = 0;
        for (NativeSlotState slot : slotStore.snapshot().values()) {
            if (slot != null && NativeSlotState.STATUS_ATTACHED.equals(slot.status) && slot.attachedView != null) {
                count += 1;
            }
        }
        return count;
    }

    public int getReadySlotsCount() {
        cleanupExpiredSlots();
        int count = 0;
        long nowMs = System.currentTimeMillis();
        for (NativeSlotState slot : slotStore.snapshot().values()) {
            if (slot != null && slot.isReady(nowMs)) {
                count += 1;
            }
        }
        return count;
    }

    public int getFailedSlotsCount() {
        cleanupExpiredSlots();
        int count = 0;
        for (NativeSlotState slot : slotStore.snapshot().values()) {
            if (slot != null && NativeSlotState.STATUS_FAILED.equals(slot.status)) {
                count += 1;
            }
        }
        return count;
    }

    public int getExpiredSlotsCount() {
        int count = 0;
        long nowMs = System.currentTimeMillis();
        for (NativeSlotState slot : slotStore.snapshot().values()) {
            if (slot != null && slot.isExpired(nowMs)) {
                count += 1;
            }
        }
        return count;
    }

    private NativeCallOptions parseNativeOptions(PluginCall call, boolean requirePlacement) {
        String placementId = host.requireTrimmed(call, "placementId");
        String slotId = host.requireTrimmed(call, "slotId");
        String hostId = host.requireTrimmed(call, "hostId");
        String explicitAdUnitId = host.requireTrimmed(call, "adUnitId");
        String testAdPreset = host.requireTrimmed(call, "testAdPreset");
        long ttlMs = sanitizeTtlMs(call.getLong("ttlMs", DEFAULT_NATIVE_TTL_MS));
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

        if (!isSafeRuntimeId(placementId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_INVALID", "placementId must use only letters, numbers, dot, underscore, colon, or dash.", "error"));
            return null;
        }

        if (TextUtils.isEmpty(slotId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "slotId is required.", "error"));
            return null;
        }

        if (!isSafeRuntimeId(slotId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_INVALID", "slotId must use only letters, numbers, dot, underscore, colon, or dash.", "error"));
            return null;
        }

        if (TextUtils.isEmpty(hostId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "hostId is required.", "error"));
            return null;
        }

        if (!isSafeRuntimeId(hostId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_INVALID", "hostId must use only letters, numbers, dot, underscore, colon, or dash.", "error"));
            return null;
        }

        String adUnitId = resolveNativeAdUnitId(call, placementId, explicitAdUnitId, testAdPreset);
        if (adUnitId == null) {
            return null;
        }
        if (TextUtils.isEmpty(adUnitId)) {
            call.resolve(PluginResultHelper.failure("CONFIG_MISSING", "Missing ad unit id for native placement \"" + placementId + "\".", "error"));
            return null;
        }

        return new NativeCallOptions(placementId, slotId, hostId, adUnitId, ttlMs, hostX, hostY, hostWidth, hostHeight, hostAnchor);
    }

    private NativeSlotState getOrCreateSlot(String slotId, String placementId, String hostId, String adUnitId, long ttlMs) {
        NativeSlotState slot = slotStore.getOrCreate(slotId);
        slot.updateIdentity(placementId, hostId, adUnitId, ttlMs);
        slotStore.put(slot);
        return slot;
    }

    private boolean hasLoadingSlotForPlacement(String placementId, String excludeSlotId) {
        for (NativeSlotState slot : slotStore.snapshot().values()) {
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

    private boolean isSlotReusable(NativeSlotState slot) {
        return slot != null && slot.isReady(System.currentTimeMillis());
    }

    private boolean isSafeRuntimeId(String value) {
        return RuntimeIdValidator.isSafe(value);
    }

    private long sanitizeTtlMs(long ttlMs) {
        if (ttlMs <= 0L) {
            return DEFAULT_NATIVE_TTL_MS;
        }
        return Math.max(MIN_NATIVE_TTL_MS, Math.min(MAX_NATIVE_TTL_MS, ttlMs));
    }

    private void startNativeLoad(NativeSlotState slot, long requestToken) {
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            slot.markFailed("LOAD_FAILED", "Activity is unavailable for native ad loading.");
            notifyNativeFailed(slot, "LOAD_FAILED", slot.lastErrorMessage);
            return;
        }

        AdLoader adLoader = new AdLoader.Builder(activity, slot.adUnitId)
            .forNativeAd(nativeAd -> activity.runOnUiThread(() -> {
                NativeSlotState current = slotStore.get(slot.slotId);
                if (current == null || !current.matchesActiveRequest(requestToken)) {
                    nativeAd.destroy();
                    return;
                }

                current.markReady(nativeAd, System.currentTimeMillis());
                notifyNativeLoaded(current, "Native ad loaded.");
            }))
            .withNativeAdOptions(new NativeAdOptions.Builder().build())
            .withAdListener(new AdListener() {
                @Override
                public void onAdClicked() {
                    NativeSlotState current = slotStore.get(slot.slotId);
                    if (current != null) {
                        notifyNativeClicked(current);
                    }
                }

                @Override
                public void onAdImpression() {
                    NativeSlotState current = slotStore.get(slot.slotId);
                    if (current != null) {
                        notifyNativeImpression(current);
                    }
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    activity.runOnUiThread(() -> {
                        NativeSlotState current = slotStore.get(slot.slotId);
                        if (current == null || !current.matchesActiveRequest(requestToken)) {
                            return;
                        }

                        String code = String.valueOf(loadAdError.getCode());
                        String message = loadAdError.getMessage();
                        current.markFailed(code, message);
                        notifyNativeFailed(current, code, message);
                    });
                }
            })
            .build();

        adLoader.loadAd(new AdRequest.Builder().build());
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

    private String resolveNativeAdUnitId(PluginCall call, String placementId, String explicitAdUnitId, String testAdPreset) {
        return TestAdPresetResolver.resolve(
            call,
            runtimeConfig.isTestMode(),
            explicitAdUnitId,
            testAdPreset,
            runtimeConfig.resolvePlacement(placementId),
            "native"
        );
    }

    private String buildHostRectFingerprint(NativeCallOptions options) {
        return LayoutFingerprintHelper.buildHostFingerprint(
            options.hostX,
            options.hostY,
            options.hostWidth,
            options.hostHeight,
            options.hostAnchor
        );
    }

    private void notifyNativeEvent(String placementId, String slotId, String phase, String code, String message) {
        events.emit("native", placementId, phase, code, message, slotId);
    }

    private void notifyNativeLoaded(NativeSlotState slot, String message) {
        notifyNativeEvent(slot.placementId, slot.slotId, "loaded", null, message);
    }

    private void notifyNativeFailed(NativeSlotState slot, String code, String message) {
        notifyNativeEvent(slot.placementId, slot.slotId, "failed", code, message);
    }

    private void notifyNativeAttached(NativeSlotState slot, String message) {
        notifyNativeEvent(slot.placementId, slot.slotId, "attached", null, message);
    }

    private void notifyNativeDetached(NativeSlotState slot, String message) {
        notifyNativeEvent(slot.placementId, slot.slotId, "detached", null, message);
    }

    private void notifyNativeClicked(NativeSlotState slot) {
        notifyNativeEvent(slot.placementId, slot.slotId, "clicked", null, "Native ad clicked.");
    }

    private void notifyNativeImpression(NativeSlotState slot) {
        notifyNativeEvent(slot.placementId, slot.slotId, "impression", null, "Native ad impression recorded.");
    }

    private void notifyNativeDebug(String placementId, String slotId, String phase, String message) {
        notifyNativeEvent(placementId, slotId, phase, null, message);
    }

    private int dpToPx(int dp) {
        float density = host.getPluginContext() != null ? host.getPluginContext().getResources().getDisplayMetrics().density : 1f;
        return Math.round(dp * density);
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

    private boolean applyHostContainerLayout(FrameLayout hostContainer, NativeCallOptions options) {
        FrameLayout.LayoutParams existingParams = hostContainer.getLayoutParams() instanceof FrameLayout.LayoutParams
            ? (FrameLayout.LayoutParams) hostContainer.getLayoutParams()
            : null;
        FrameLayout.LayoutParams params = existingParams == null
            ? new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            : new FrameLayout.LayoutParams(existingParams);

        if (options.hostX != null && options.hostY != null && options.hostWidth != null && options.hostWidth > 0) {
            params.width = options.hostWidth;
            params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.TOP | Gravity.START;
            params.leftMargin = Math.max(0, options.hostX);
            params.rightMargin = 0;
            int anchoredTop = Math.max(0, options.hostY);
            if ("bottom".equals(options.hostAnchor) && options.hostHeight != null && options.hostHeight > 0) {
                anchoredTop += options.hostHeight;
            }
            params.topMargin = anchoredTop;
            params.bottomMargin = 0;
        } else {
            params.width = FrameLayout.LayoutParams.MATCH_PARENT;
            params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.BOTTOM;
            int margin = dpToPx(DEFAULT_NATIVE_MARGIN_DP);
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

        return current.width == next.width &&
            current.height == next.height &&
            current.gravity == next.gravity &&
            current.leftMargin == next.leftMargin &&
            current.topMargin == next.topMargin &&
            current.rightMargin == next.rightMargin &&
            current.bottomMargin == next.bottomMargin;
    }

    private ViewGroup resolveNativeHostContainer(NativeCallOptions options) {
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            return null;
        }

        ViewGroup contentRoot = activity.findViewById(android.R.id.content);
        if (contentRoot == null) {
            return null;
        }

        String overlayTag = HostOverlayHelper.buildOverlayTag(HOST_CONTAINER_TAG_PREFIX, options.hostId);
        View existing = contentRoot.findViewWithTag(overlayTag);
        if (existing instanceof FrameLayout) {
            return (FrameLayout) existing;
        }

        FrameLayout hostContainer = new FrameLayout(activity);
        hostContainer.setTag(overlayTag);
        applyHostContainerLayout(hostContainer, options);
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

    private void cleanupSlotView(NativeSlotState slot) {
        if (slot == null) {
            return;
        }

        Activity activity = host.getPluginActivity();
        runOnUiThreadBlocking(activity, () -> {
            if (slot.attachedView != null && slot.attachedView.getParent() instanceof ViewGroup) {
                ViewGroup parentView = (ViewGroup) slot.attachedView.getParent();
                parentView.removeView(slot.attachedView);
                removeHostContainerIfEmpty(parentView);
            }
        });
        slot.clearViewReference();
    }

    private void cleanupSlotAd(NativeSlotState slot) {
        if (slot == null) {
            return;
        }

        slot.clearAdReference();
        if (!NativeSlotState.STATUS_FAILED.equals(slot.status)) {
            slot.status = NativeSlotState.STATUS_IDLE;
        }
        slot.loading = false;
    }

    private void cleanupSlot(NativeSlotState slot) {
        cleanupSlotView(slot);
        cleanupSlotAd(slot);
    }

    private void cleanupExpiredSlots() {
        long nowMs = System.currentTimeMillis();
        for (NativeSlotState slot : slotStore.snapshot().values()) {
            if (slot == null || !slot.isExpired(nowMs)) {
                continue;
            }

            cleanupAndRemoveSlot(slot.slotId);
            notifyNativeEvent(slot.placementId, slot.slotId, "destroyed", "EXPIRED", "Native slot expired and was cleaned up.");
        }
    }

    private void clearAllSlotsInternal() {
        for (NativeSlotState slot : slotStore.snapshot().values()) {
            cleanupSlot(slot);
        }
        clearAllHostContainers();
        slotStore.clear();
    }

    private void cleanupAndRemoveSlot(String slotId) {
        NativeSlotState slot = slotStore.remove(slotId);
        cleanupSlot(slot);
    }

    private NativeAdView inflateNativeAdView() {
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            return null;
        }
        return (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.donugr_native_ad, null);
    }

    private void bindOptionalText(TextView view, String value) {
        if (view == null) {
            return;
        }

        if (TextUtils.isEmpty(value)) {
            view.setVisibility(View.GONE);
            view.setText("");
            return;
        }

        view.setVisibility(View.VISIBLE);
        view.setText(value);
    }

    private void bindOptionalDrawable(ImageView view, Drawable drawable) {
        if (view == null) {
            return;
        }

        if (drawable == null) {
            view.setVisibility(View.GONE);
            view.setImageDrawable(null);
            return;
        }

        view.setVisibility(View.VISIBLE);
        view.setImageDrawable(drawable);
    }

    private NativeAdView createAndBindNativeAdView(NativeSlotState slot) {
        NativeAdView adView = inflateNativeAdView();
        if (adView == null || slot.nativeAd == null) {
            return null;
        }

        TextView headlineView = adView.findViewById(R.id.ad_headline);
        TextView bodyView = adView.findViewById(R.id.ad_body);
        Button ctaView = adView.findViewById(R.id.ad_call_to_action);
        ImageView iconView = adView.findViewById(R.id.ad_app_icon);
        TextView badgeView = adView.findViewById(R.id.ad_badge);
        MediaView mediaView = adView.findViewById(R.id.ad_media);

        adView.setHeadlineView(headlineView);
        adView.setBodyView(bodyView);
        adView.setCallToActionView(ctaView);
        adView.setIconView(iconView);
        adView.setMediaView(mediaView);

        bindOptionalText(headlineView, slot.nativeAd.getHeadline());
        bindOptionalText(bodyView, slot.nativeAd.getBody());
        bindOptionalText(ctaView, slot.nativeAd.getCallToAction());
        bindOptionalText(badgeView, "Ad");
        bindOptionalDrawable(iconView, slot.nativeAd.getIcon() != null ? slot.nativeAd.getIcon().getDrawable() : null);

        if (slot.nativeAd.getMediaContent() != null) {
            mediaView.setVisibility(View.VISIBLE);
            mediaView.setMediaContent(slot.nativeAd.getMediaContent());
            VideoController videoController = slot.nativeAd.getMediaContent().getVideoController();
            if (videoController != null) {
                videoController.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                });
            }
        } else {
            mediaView.setVisibility(View.GONE);
        }

        adView.setNativeAd(slot.nativeAd);
        return adView;
    }

    private static class NativeCallOptions {
        final String placementId;
        final String slotId;
        final String hostId;
        final String adUnitId;
        final long ttlMs;
        final Integer hostX;
        final Integer hostY;
        final Integer hostWidth;
        final Integer hostHeight;
        final String hostAnchor;

        NativeCallOptions(
            String placementId,
            String slotId,
            String hostId,
            String adUnitId,
            long ttlMs,
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
            this.ttlMs = ttlMs;
            this.hostX = hostX;
            this.hostY = hostY;
            this.hostWidth = hostWidth;
            this.hostHeight = hostHeight;
            this.hostAnchor = hostAnchor;
        }
    }

    public interface NativeHost {
        android.content.Context getPluginContext();
        Activity getPluginActivity();
        String requireTrimmed(PluginCall call, String key);
    }
}

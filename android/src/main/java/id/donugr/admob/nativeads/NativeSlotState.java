package id.donugr.admob.nativeads;

import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

class NativeSlotState {
    static final String STATUS_IDLE = "idle";
    static final String STATUS_LOADING = "loading";
    static final String STATUS_READY = "ready";
    static final String STATUS_ATTACHED = "attached";
    static final String STATUS_FAILED = "failed";

    final String slotId;
    String placementId;
    String hostId;
    String adUnitId;
    String mediaMode;
    long ttlMs;
    long loadedAtEpochMs;
    String status;
    String lastErrorCode;
    String lastErrorMessage;
    NativeAd nativeAd;
    NativeAdView attachedView;
    String attachedHostId;
    String attachedHostRectFingerprint;
    boolean loading;
    long activeRequestToken;
    long requestCounter;

    NativeSlotState(String slotId) {
        this.slotId = slotId;
        this.placementId = "";
        this.hostId = "";
        this.adUnitId = "";
        this.mediaMode = "auto";
        this.ttlMs = 0L;
        this.loadedAtEpochMs = 0L;
        this.status = STATUS_IDLE;
        this.lastErrorCode = "";
        this.lastErrorMessage = "";
        this.nativeAd = null;
        this.attachedView = null;
        this.attachedHostId = "";
        this.attachedHostRectFingerprint = "";
        this.loading = false;
        this.activeRequestToken = 0L;
        this.requestCounter = 0L;
    }

    void updateIdentity(String placementId, String hostId, String adUnitId, String mediaMode, long ttlMs) {
        this.placementId = placementId;
        this.hostId = hostId;
        this.adUnitId = adUnitId;
        this.mediaMode = mediaMode == null || mediaMode.isEmpty() ? "auto" : mediaMode;
        this.ttlMs = ttlMs;
    }

    boolean isReady(long nowMs) {
        if (nativeAd == null) {
            return false;
        }

        if (!(STATUS_READY.equals(status) || STATUS_ATTACHED.equals(status))) {
            return false;
        }

        return !isExpired(nowMs);
    }

    boolean isLoading() {
        return loading || STATUS_LOADING.equals(status);
    }

    boolean isExpired(long nowMs) {
        return ttlMs > 0 && loadedAtEpochMs > 0 && loadedAtEpochMs + ttlMs <= nowMs;
    }

    long markLoading() {
        clearViewReference();
        clearAdReference();
        status = STATUS_LOADING;
        loading = true;
        loadedAtEpochMs = 0L;
        lastErrorCode = "";
        lastErrorMessage = "";
        requestCounter += 1L;
        activeRequestToken = requestCounter;
        return activeRequestToken;
    }

    void markReady(NativeAd nativeAd, long loadedAtEpochMs) {
        clearViewReference();
        clearAdReference();
        this.nativeAd = nativeAd;
        this.loadedAtEpochMs = loadedAtEpochMs;
        this.status = STATUS_READY;
        this.loading = false;
        this.lastErrorCode = "";
        this.lastErrorMessage = "";
    }

    void markAttached(String hostId, String hostRectFingerprint, NativeAdView attachedView) {
        this.hostId = hostId;
        this.attachedHostId = hostId;
        this.attachedHostRectFingerprint = hostRectFingerprint == null ? "" : hostRectFingerprint;
        this.attachedView = attachedView;
        this.status = STATUS_ATTACHED;
        this.loading = false;
    }

    void markDetached() {
        clearViewReference();
        this.status = nativeAd != null ? STATUS_READY : STATUS_IDLE;
        this.loading = false;
    }

    void markFailed(String code, String message) {
        clearViewReference();
        clearAdReference();
        this.status = STATUS_FAILED;
        this.loading = false;
        this.loadedAtEpochMs = 0L;
        this.lastErrorCode = code == null ? "" : code;
        this.lastErrorMessage = message == null ? "" : message;
    }

    void clearAdReference() {
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        nativeAd = null;
        loadedAtEpochMs = 0L;
    }

    void clearViewReference() {
        attachedView = null;
        attachedHostId = "";
        attachedHostRectFingerprint = "";
    }

    boolean matchesActiveRequest(long requestToken) {
        return activeRequestToken == requestToken;
    }

    boolean isAttachedToHost(String hostId, String hostRectFingerprint, long nowMs) {
        if (!isReady(nowMs) || attachedView == null || !STATUS_ATTACHED.equals(status)) {
            return false;
        }

        String safeHostId = hostId == null ? "" : hostId;
        String safeHostRectFingerprint = hostRectFingerprint == null ? "" : hostRectFingerprint;
        return safeHostId.equals(attachedHostId) && safeHostRectFingerprint.equals(attachedHostRectFingerprint);
    }
}

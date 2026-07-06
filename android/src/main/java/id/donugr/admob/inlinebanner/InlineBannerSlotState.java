package id.donugr.admob.inlinebanner;

import com.google.android.gms.ads.AdView;

class InlineBannerSlotState {
    static final String STATUS_IDLE = "idle";
    static final String STATUS_LOADING = "loading";
    static final String STATUS_READY = "ready";
    static final String STATUS_ATTACHED = "attached";
    static final String STATUS_DISPOSING = "disposing";
    static final String STATUS_DESTROYED = "destroyed";
    static final String STATUS_FAILED = "failed";

    final String slotId;
    String placementId;
    String hostId;
    String adUnitId;
    String status;
    String lastErrorCode;
    String lastErrorMessage;
    AdView adView;
    boolean loading;
    long activeRequestToken;
    long requestCounter;
    String attachedHostId;
    String attachedHostFingerprint;
    long lastLoadedAtEpochMs;
    long lastImpressionAtEpochMs;
    String lastEmittedPhase;
    boolean disposed;

    InlineBannerSlotState(String slotId) {
        this.slotId = slotId;
        this.placementId = "";
        this.hostId = "";
        this.adUnitId = "";
        this.status = STATUS_IDLE;
        this.lastErrorCode = "";
        this.lastErrorMessage = "";
        this.adView = null;
        this.loading = false;
        this.activeRequestToken = 0L;
        this.requestCounter = 0L;
        this.attachedHostId = "";
        this.attachedHostFingerprint = "";
        this.lastLoadedAtEpochMs = 0L;
        this.lastImpressionAtEpochMs = 0L;
        this.lastEmittedPhase = "";
        this.disposed = false;
    }

    void updateIdentity(String placementId, String hostId, String adUnitId) {
        this.placementId = placementId == null ? "" : placementId;
        this.hostId = hostId == null ? "" : hostId;
        this.adUnitId = adUnitId == null ? "" : adUnitId;
    }

    boolean isReady() {
        return !disposed && adView != null && (STATUS_READY.equals(status) || STATUS_ATTACHED.equals(status)) && !loading;
    }

    boolean isLoading() {
        return loading || STATUS_LOADING.equals(status);
    }

    long markLoading(AdView adView) {
        clearViewReference();
        clearAdReference();
        this.adView = adView;
        this.status = STATUS_LOADING;
        this.disposed = false;
        this.loading = true;
        this.lastErrorCode = "";
        this.lastErrorMessage = "";
        resetEmissionGuards();
        this.requestCounter += 1L;
        this.activeRequestToken = this.requestCounter;
        return this.activeRequestToken;
    }

    void markReady() {
        if (disposed) {
            return;
        }
        this.status = STATUS_READY;
        this.loading = false;
        this.lastErrorCode = "";
        this.lastErrorMessage = "";
    }

    void markAttached(String hostId, String hostFingerprint) {
        if (disposed) {
            return;
        }
        this.hostId = hostId == null ? "" : hostId;
        this.attachedHostId = this.hostId;
        this.attachedHostFingerprint = hostFingerprint == null ? "" : hostFingerprint;
        this.status = STATUS_ATTACHED;
        this.loading = false;
    }

    void markDetached() {
        if (disposed) {
            return;
        }
        clearViewReference();
        this.status = adView != null ? STATUS_READY : STATUS_IDLE;
        this.loading = false;
    }

    void markDisposing() {
        this.status = STATUS_DISPOSING;
        this.loading = false;
        this.disposed = true;
    }

    void markDestroyed() {
        clearViewReference();
        this.status = STATUS_DESTROYED;
        this.loading = false;
        this.disposed = true;
    }

    void markFailed(String code, String message) {
        clearViewReference();
        clearAdReference();
        this.status = STATUS_FAILED;
        this.loading = false;
        this.disposed = false;
        this.lastErrorCode = code == null ? "" : code;
        this.lastErrorMessage = message == null ? "" : message;
    }

    void recordEmittedPhase(String phase) {
        this.lastEmittedPhase = phase == null ? "" : phase;
    }

    void recordLoadedEmission(long nowMs) {
        this.lastLoadedAtEpochMs = nowMs;
        recordEmittedPhase("loaded");
    }

    void recordImpressionEmission(long nowMs) {
        this.lastImpressionAtEpochMs = nowMs;
        recordEmittedPhase("impression");
    }

    void resetEmissionGuards() {
        this.lastLoadedAtEpochMs = 0L;
        this.lastImpressionAtEpochMs = 0L;
        this.lastEmittedPhase = "";
    }

    void clearAdReference() {
        if (adView != null) {
            adView.destroy();
        }
        adView = null;
    }

    void clearViewReference() {
        attachedHostId = "";
        attachedHostFingerprint = "";
    }

    boolean matchesActiveRequest(long requestToken) {
        return !disposed && activeRequestToken == requestToken;
    }

    boolean isAttachedToHost(String hostId, String hostFingerprint) {
        if (!isReady() || adView == null || !STATUS_ATTACHED.equals(status)) {
            return false;
        }

        String safeHostId = hostId == null ? "" : hostId;
        String safeHostFingerprint = hostFingerprint == null ? "" : hostFingerprint;
        return safeHostId.equals(attachedHostId) && safeHostFingerprint.equals(attachedHostFingerprint);
    }

    boolean isDisposed() {
        return disposed || STATUS_DISPOSING.equals(status) || STATUS_DESTROYED.equals(status);
    }
}

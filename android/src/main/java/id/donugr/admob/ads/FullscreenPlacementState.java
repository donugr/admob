package id.donugr.admob.ads;

class FullscreenPlacementState {
    static final String STATUS_IDLE = "idle";
    static final String STATUS_LOADING = "loading";
    static final String STATUS_LOADED = "loaded";
    static final String STATUS_SHOWING = "showing";
    static final String STATUS_DISMISSED = "dismissed";
    static final String STATUS_FAILED = "failed";
    static final String STATUS_DISPOSED = "disposed";

    String status = STATUS_IDLE;
    String lastErrorCode = "";
    String lastErrorMessage = "";
    String lastEmittedPhase = "";
    long activeRequestToken = 0L;
    long requestCounter = 0L;
    boolean loading;
    boolean showing;
    boolean disposed;

    long markLoading() {
        requestCounter += 1L;
        activeRequestToken = requestCounter;
        status = STATUS_LOADING;
        lastErrorCode = "";
        lastErrorMessage = "";
        lastEmittedPhase = "";
        loading = true;
        showing = false;
        disposed = false;
        return activeRequestToken;
    }

    void markLoaded() {
        status = STATUS_LOADED;
        loading = false;
        showing = false;
        disposed = false;
        lastErrorCode = "";
        lastErrorMessage = "";
    }

    void markShowing() {
        status = STATUS_SHOWING;
        loading = false;
        showing = true;
        disposed = false;
    }

    void markDismissed() {
        status = STATUS_DISMISSED;
        loading = false;
        showing = false;
        disposed = true;
    }

    void markFailed(String code, String message) {
        status = STATUS_FAILED;
        loading = false;
        showing = false;
        disposed = true;
        lastErrorCode = code == null ? "" : code;
        lastErrorMessage = message == null ? "" : message;
    }

    void markDisposed() {
        status = STATUS_DISPOSED;
        loading = false;
        showing = false;
        disposed = true;
    }

    void recordPhase(String phase) {
        lastEmittedPhase = phase == null ? "" : phase;
    }

    boolean matchesActiveRequest(long requestToken) {
        return !disposed && activeRequestToken == requestToken;
    }
}

package id.donugr.admob.ads;

public class FullscreenAdCoordinator {
    private String activeFormat;
    private String activePlacementId;

    public synchronized boolean tryAcquire(String format, String placementId) {
        if (activeFormat != null) return false;
        activeFormat = format;
        activePlacementId = placementId;
        return true;
    }

    public synchronized void release(String format, String placementId) {
        if (activeFormat == null) return;
        if (activeFormat.equals(format) && activePlacementId.equals(placementId)) {
            activeFormat = null;
            activePlacementId = null;
        }
    }

    public synchronized void clear() {
        activeFormat = null;
        activePlacementId = null;
    }

    public synchronized boolean isShowing() { return activeFormat != null; }
    public synchronized String getActiveFormat() { return activeFormat; }
    public synchronized String getActivePlacementId() { return activePlacementId; }
}

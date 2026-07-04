package id.donugr.admob.util;

public final class HostOverlayHelper {
    private HostOverlayHelper() {
    }

    public static String buildOverlayTag(String prefix, String hostId) {
        return String.valueOf(prefix) + String.valueOf(hostId);
    }
}

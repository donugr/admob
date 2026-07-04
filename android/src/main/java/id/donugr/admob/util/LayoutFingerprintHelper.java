package id.donugr.admob.util;

public final class LayoutFingerprintHelper {
    private LayoutFingerprintHelper() {
    }

    public static String buildHostFingerprint(
        Integer x,
        Integer y,
        Integer width,
        Integer height,
        String anchor
    ) {
        return String.valueOf(x) + "|" +
            String.valueOf(y) + "|" +
            String.valueOf(width) + "|" +
            String.valueOf(height) + "|" +
            String.valueOf(anchor);
    }
}

package id.donugr.admob.util;

import android.view.View;
import android.view.ViewGroup;

public final class HostOverlayHelper {
    private HostOverlayHelper() {
    }

    public static String buildOverlayTag(String prefix, String hostId) {
        return String.valueOf(prefix) + String.valueOf(hostId);
    }

    public static InlineBannerLayoutContext createInlineBannerLayoutContext(
        ViewGroup contentRoot,
        View webView,
        Integer rawX,
        Integer rawY,
        Integer rawWidth,
        Integer rawHeight,
        String hostAnchor
    ) {
        RectSnapshot contentRootRect = RectSnapshot.fromRoot(contentRoot);
        RectSnapshot webViewRect = RectSnapshot.relativeToRoot(webView, contentRoot);
        return new InlineBannerLayoutContext(contentRootRect, webViewRect, rawX, rawY, rawWidth, rawHeight, hostAnchor);
    }

    public static final class InlineBannerLayoutContext {
        public final RectSnapshot contentRootRect;
        public final RectSnapshot webViewRect;
        public final Integer rawX;
        public final Integer rawY;
        public final Integer rawWidth;
        public final Integer rawHeight;
        public final String hostAnchor;
        public final Integer normalizedX;
        public final Integer normalizedY;
        public final Integer normalizedWidth;
        public final Integer normalizedHeight;
        public final int appliedLeft;
        public final int appliedTop;
        public final int appliedWidth;
        public final boolean partialRect;
        public final boolean explicitRectAvailable;
        public final boolean measurable;
        public final boolean fullyOutOfBounds;

        InlineBannerLayoutContext(
            RectSnapshot contentRootRect,
            RectSnapshot webViewRect,
            Integer rawX,
            Integer rawY,
            Integer rawWidth,
            Integer rawHeight,
            String hostAnchor
        ) {
            this.contentRootRect = contentRootRect;
            this.webViewRect = webViewRect;
            this.rawX = rawX;
            this.rawY = rawY;
            this.rawWidth = rawWidth;
            this.rawHeight = rawHeight;
            this.hostAnchor = hostAnchor == null ? "top" : hostAnchor;

            this.partialRect = hasAny(rawX, rawY, rawWidth, rawHeight) && !(rawX != null && rawY != null && rawWidth != null && rawWidth > 0);
            this.explicitRectAvailable = rawX != null && rawY != null && rawWidth != null && rawWidth > 0;
            this.measurable = contentRootRect != null && webViewRect != null && contentRootRect.width > 0 && contentRootRect.height > 0 && webViewRect.width > 0 && webViewRect.height > 0;

            if (explicitRectAvailable && measurable) {
                this.normalizedX = webViewRect.left + rawX;
                this.normalizedY = webViewRect.top + rawY;
                this.normalizedWidth = rawWidth;
                this.normalizedHeight = rawHeight;
            } else {
                this.normalizedX = null;
                this.normalizedY = null;
                this.normalizedWidth = rawWidth;
                this.normalizedHeight = rawHeight;
            }

            if (explicitRectAvailable && measurable && normalizedX != null && normalizedY != null && normalizedWidth != null) {
                int rootWidth = Math.max(1, contentRootRect.width);
                int rootHeight = Math.max(1, contentRootRect.height);
                int anchorTop = normalizedY;
                if ("bottom".equals(this.hostAnchor) && rawHeight != null && rawHeight > 0) {
                    anchorTop += rawHeight;
                }

                this.fullyOutOfBounds =
                    normalizedX >= rootWidth ||
                    anchorTop >= rootHeight ||
                    normalizedX + normalizedWidth <= 0 ||
                    anchorTop + Math.max(rawHeight == null ? 1 : rawHeight, 1) <= 0;

                int clampedLeft = clamp(normalizedX, 0, Math.max(0, rootWidth - 1));
                int clampedTop = clamp(anchorTop, 0, Math.max(0, rootHeight - 1));
                int maxWidth = Math.max(1, rootWidth - clampedLeft);
                this.appliedLeft = clampedLeft;
                this.appliedTop = clampedTop;
                this.appliedWidth = Math.max(1, Math.min(normalizedWidth, maxWidth));
            } else {
                this.fullyOutOfBounds = false;
                this.appliedLeft = 0;
                this.appliedTop = 0;
                this.appliedWidth = rawWidth != null && rawWidth > 0 ? rawWidth : 0;
            }
        }

        public boolean hasExplicitRect() {
            return explicitRectAvailable;
        }

        public String describeRawRect() {
            return "raw{x=" + rawX + ", y=" + rawY + ", width=" + rawWidth + ", height=" + rawHeight + ", anchor=" + hostAnchor + "}";
        }

        public String describeNormalizedRect() {
            return "normalized{x=" + normalizedX + ", y=" + normalizedY + ", width=" + normalizedWidth + ", height=" + normalizedHeight +
                "} applied{left=" + appliedLeft + ", top=" + appliedTop + ", width=" + appliedWidth + "}";
        }

        public String describeEnvironment() {
            return "webView" + (webViewRect == null ? "{unavailable}" : webViewRect.describe()) +
                " contentRoot" + (contentRootRect == null ? "{unavailable}" : contentRootRect.describe());
        }

        private static boolean hasAny(Integer... values) {
            if (values == null) {
                return false;
            }
            for (Integer value : values) {
                if (value != null) {
                    return true;
                }
            }
            return false;
        }

        private static int clamp(int value, int min, int max) {
            if (value < min) {
                return min;
            }
            return Math.min(value, max);
        }
    }

    public static final class RectSnapshot {
        public final int left;
        public final int top;
        public final int width;
        public final int height;

        RectSnapshot(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }

        public static RectSnapshot fromRoot(ViewGroup root) {
            if (root == null) {
                return null;
            }
            return new RectSnapshot(0, 0, resolveViewWidth(root), resolveViewHeight(root));
        }

        public static RectSnapshot relativeToRoot(View view, ViewGroup root) {
            if (view == null || root == null) {
                return null;
            }

            int[] rootLocation = new int[2];
            int[] viewLocation = new int[2];
            root.getLocationOnScreen(rootLocation);
            view.getLocationOnScreen(viewLocation);

            return new RectSnapshot(
                viewLocation[0] - rootLocation[0],
                viewLocation[1] - rootLocation[1],
                resolveViewWidth(view),
                resolveViewHeight(view)
            );
        }

        public String describe() {
            return "{left=" + left + ", top=" + top + ", width=" + width + ", height=" + height + "}";
        }

        private static int resolveViewWidth(View view) {
            if (view == null) {
                return 0;
            }
            return Math.max(0, Math.max(view.getWidth(), view.getMeasuredWidth()));
        }

        private static int resolveViewHeight(View view) {
            if (view == null) {
                return 0;
            }
            return Math.max(0, Math.max(view.getHeight(), view.getMeasuredHeight()));
        }
    }
}

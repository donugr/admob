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
        float density = webView != null && webView.getResources() != null
            ? Math.max(1f, webView.getResources().getDisplayMetrics().density)
            : 1f;
        return new InlineBannerLayoutContext(contentRootRect, webViewRect, rawX, rawY, rawWidth, rawHeight, hostAnchor, density);
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
        public final String normalizationMode;
        public final float normalizationScale;
        public final int appliedLeft;
        public final int appliedTop;
        public final int appliedWidth;
        public final boolean partialRect;
        public final boolean explicitRectAvailable;
        public final boolean measurable;
        public final boolean fullyOutOfBounds;
        private final float density;

        InlineBannerLayoutContext(
            RectSnapshot contentRootRect,
            RectSnapshot webViewRect,
            Integer rawX,
            Integer rawY,
            Integer rawWidth,
            Integer rawHeight,
            String hostAnchor,
            float density
        ) {
            this.contentRootRect = contentRootRect;
            this.webViewRect = webViewRect;
            this.rawX = rawX;
            this.rawY = rawY;
            this.rawWidth = rawWidth;
            this.rawHeight = rawHeight;
            this.hostAnchor = hostAnchor == null ? "top" : hostAnchor;
            this.density = density <= 0f ? 1f : density;

            this.partialRect = hasAny(rawX, rawY, rawWidth, rawHeight) && !(rawX != null && rawY != null && rawWidth != null && rawWidth > 0);
            this.explicitRectAvailable = rawX != null && rawY != null && rawWidth != null && rawWidth > 0;
            this.measurable = contentRootRect != null && webViewRect != null && contentRootRect.width > 0 && contentRootRect.height > 0 && webViewRect.width > 0 && webViewRect.height > 0;

            Candidate candidate = explicitRectAvailable && measurable
                ? selectBestCandidate(contentRootRect, webViewRect, rawX, rawY, rawWidth, rawHeight, this.hostAnchor, this.density)
                : null;

            if (candidate != null) {
                this.normalizedX = candidate.normalizedX;
                this.normalizedY = candidate.normalizedY;
                this.normalizedWidth = candidate.normalizedWidth;
                this.normalizedHeight = candidate.normalizedHeight;
                this.normalizationMode = candidate.mode;
                this.normalizationScale = candidate.scale;
            } else {
                this.normalizedX = null;
                this.normalizedY = null;
                this.normalizedWidth = rawWidth;
                this.normalizedHeight = rawHeight;
                this.normalizationMode = "unavailable";
                this.normalizationScale = 1f;
            }

            if (candidate != null) {
                int rootWidth = Math.max(1, contentRootRect.width);
                int rootHeight = Math.max(1, contentRootRect.height);
                this.fullyOutOfBounds = candidate.fullyOutOfBounds;
                int clampedLeft = clamp(candidate.appliedLeft, 0, Math.max(0, rootWidth - 1));
                int clampedTop = clamp(candidate.appliedTop, 0, Math.max(0, rootHeight - 1));
                int maxWidth = Math.max(1, rootWidth - clampedLeft);
                this.appliedLeft = clampedLeft;
                this.appliedTop = clampedTop;
                this.appliedWidth = Math.max(1, Math.min(candidate.appliedWidth, maxWidth));
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
                ", mode=" + normalizationMode + ", scale=" + normalizationScale + "} applied{left=" + appliedLeft + ", top=" + appliedTop + ", width=" + appliedWidth + "}";
        }

        public String describeFlags() {
            return "flags{partialRect=" + partialRect +
                ", explicitRectAvailable=" + explicitRectAvailable +
                ", measurable=" + measurable +
                ", fullyOutOfBounds=" + fullyOutOfBounds +
                "}";
        }

        public String describeWebViewRect() {
            return "webView" + (webViewRect == null ? "{unavailable}" : webViewRect.describe());
        }

        public String describeContentRootRect() {
            return "contentRoot" + (contentRootRect == null ? "{unavailable}" : contentRootRect.describe());
        }

        public String describeAppliedRect() {
            return "applied{left=" + appliedLeft + ", top=" + appliedTop + ", width=" + appliedWidth + "}";
        }

        public String describeEnvironment() {
            return describeWebViewRect() + " " + describeContentRootRect();
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

        private static Candidate selectBestCandidate(
            RectSnapshot contentRootRect,
            RectSnapshot webViewRect,
            Integer rawX,
            Integer rawY,
            Integer rawWidth,
            Integer rawHeight,
            String hostAnchor
            ,
            float density
        ) {
            Candidate[] candidates = new Candidate[] {
                buildCandidate("webview_css", webViewRect.left, webViewRect.top, 1f, contentRootRect, rawX, rawY, rawWidth, rawHeight, hostAnchor),
                buildCandidate("webview_scaled", webViewRect.left, webViewRect.top, density, contentRootRect, rawX, rawY, rawWidth, rawHeight, hostAnchor),
                buildCandidate("root_css", 0, 0, 1f, contentRootRect, rawX, rawY, rawWidth, rawHeight, hostAnchor),
                buildCandidate("root_scaled", 0, 0, density, contentRootRect, rawX, rawY, rawWidth, rawHeight, hostAnchor),
            };

            float effectiveDensity = density <= 0f ? 1f : density;
            boolean cssLikeWidth = rawWidth != null && webViewRect != null && webViewRect.width > 0 &&
                rawWidth <= Math.round((float) webViewRect.width / effectiveDensity) + 2;
            Candidate best = null;
            int bestScore = Integer.MIN_VALUE;
            for (Candidate candidate : candidates) {
                if (candidate == null) {
                    continue;
                }
                int score = candidate.score;
                if (cssLikeWidth && candidate.scale > 1.05f) {
                    score += 80;
                }
                if (!cssLikeWidth && candidate.scale <= 1.05f) {
                    score += 40;
                }
                if (candidate.mode.startsWith("webview")) {
                    score += 20;
                }
                if (best == null || score > bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
            return best;
        }

        private static Candidate buildCandidate(
            String mode,
            int baseLeft,
            int baseTop,
            float scale,
            RectSnapshot contentRootRect,
            Integer rawX,
            Integer rawY,
            Integer rawWidth,
            Integer rawHeight,
            String hostAnchor
        ) {
            int safeWidth = Math.max(1, Math.round((rawWidth == null ? 0 : rawWidth) * scale));
            int safeHeight = rawHeight == null ? 0 : Math.max(0, Math.round(rawHeight * scale));
            int normalizedX = baseLeft + Math.round((rawX == null ? 0 : rawX) * scale);
            int normalizedY = baseTop + Math.round((rawY == null ? 0 : rawY) * scale);
            int anchorTop = normalizedY;
            if ("bottom".equals(hostAnchor) && safeHeight > 0) {
                anchorTop += safeHeight;
            }

            int rootWidth = Math.max(1, contentRootRect.width);
            int rootHeight = Math.max(1, contentRootRect.height);
            int visibleLeft = Math.max(0, normalizedX);
            int visibleRight = Math.min(rootWidth, normalizedX + safeWidth);
            int visibleWidth = Math.max(0, visibleRight - visibleLeft);
            int visibleTop = Math.max(0, anchorTop);
            int visibleBottom = Math.min(rootHeight, anchorTop + Math.max(safeHeight, 1));
            int visibleHeight = Math.max(0, visibleBottom - visibleTop);
            boolean fullyOutOfBounds = visibleWidth <= 0 || visibleHeight <= 0;

            int score = visibleWidth * 1000 / Math.max(1, safeWidth);
            score -= Math.abs(normalizedX < 0 ? normalizedX : 0) / 4;
            score -= Math.abs(anchorTop < 0 ? anchorTop : 0) / 8;
            if (fullyOutOfBounds) {
                score -= 1000;
            }

            return new Candidate(
                mode,
                scale,
                normalizedX,
                normalizedY,
                safeWidth,
                safeHeight > 0 ? safeHeight : rawHeight,
                visibleLeft,
                visibleTop,
                Math.max(1, visibleWidth),
                fullyOutOfBounds,
                score
            );
        }
    }

    private static final class Candidate {
        final String mode;
        final float scale;
        final int normalizedX;
        final int normalizedY;
        final int normalizedWidth;
        final Integer normalizedHeight;
        final int appliedLeft;
        final int appliedTop;
        final int appliedWidth;
        final boolean fullyOutOfBounds;
        final int score;

        Candidate(
            String mode,
            float scale,
            int normalizedX,
            int normalizedY,
            int normalizedWidth,
            Integer normalizedHeight,
            int appliedLeft,
            int appliedTop,
            int appliedWidth,
            boolean fullyOutOfBounds,
            int score
        ) {
            this.mode = mode;
            this.scale = scale;
            this.normalizedX = normalizedX;
            this.normalizedY = normalizedY;
            this.normalizedWidth = normalizedWidth;
            this.normalizedHeight = normalizedHeight;
            this.appliedLeft = appliedLeft;
            this.appliedTop = appliedTop;
            this.appliedWidth = appliedWidth;
            this.fullyOutOfBounds = fullyOutOfBounds;
            this.score = score;
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

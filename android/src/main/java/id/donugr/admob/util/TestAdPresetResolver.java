package id.donugr.admob.util;

import android.text.TextUtils;
import com.getcapacitor.PluginCall;
import id.donugr.admob.core.PluginResultHelper;

public final class TestAdPresetResolver {
    public static final String PRESET_APP_OPEN = "app_open";
    public static final String PRESET_BANNER_FIXED = "banner_fixed";
    public static final String PRESET_BANNER_ADAPTIVE = "banner_adaptive";
    public static final String PRESET_BANNER_INLINE_ADAPTIVE = "banner_inline_adaptive";
    public static final String PRESET_INTERSTITIAL = "interstitial";
    public static final String PRESET_REWARDED = "rewarded";
    public static final String PRESET_REWARDED_INTERSTITIAL = "rewarded_interstitial";
    public static final String PRESET_NATIVE = "native";
    public static final String PRESET_NATIVE_VIDEO = "native_video";

    private static final String TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921";
    private static final String TEST_BANNER_FIXED_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";
    private static final String TEST_BANNER_ADAPTIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741";
    private static final String TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
    private static final String TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
    private static final String TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379";
    private static final String TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";
    private static final String TEST_NATIVE_VIDEO_AD_UNIT_ID = "ca-app-pub-3940256099942544/1044960115";

    private TestAdPresetResolver() {
    }

    public static String resolve(
        PluginCall call,
        boolean testMode,
        String explicitAdUnitId,
        String testAdPreset,
        String placementAdUnitId,
        String format
    ) {
        if (!TextUtils.isEmpty(explicitAdUnitId)) {
            return explicitAdUnitId.trim();
        }

        if (!TextUtils.isEmpty(testAdPreset)) {
            if (!testMode) {
                call.resolve(PluginResultHelper.failure(
                    "CONFIG_INVALID",
                    "testAdPreset is only allowed when testMode is enabled.",
                    "error"
                ));
                return null;
            }

            String testAdUnitId = resolvePresetForFormat(format, testAdPreset);
            if (TextUtils.isEmpty(testAdUnitId)) {
                call.resolve(PluginResultHelper.failure(
                    "CONFIG_INVALID",
                    "testAdPreset \"" + testAdPreset + "\" is not valid for format \"" + format + "\".",
                    "error"
                ));
                return null;
            }
            return testAdUnitId;
        }

        if (!TextUtils.isEmpty(placementAdUnitId)) {
            return placementAdUnitId;
        }

        if (testMode) {
            return legacyDefaultForFormat(format);
        }

        return null;
    }

    private static String resolvePresetForFormat(String format, String preset) {
        if ("app_open".equals(format)) {
            return PRESET_APP_OPEN.equals(preset) ? TEST_APP_OPEN_AD_UNIT_ID : null;
        }

        if ("interstitial".equals(format)) {
            return PRESET_INTERSTITIAL.equals(preset) ? TEST_INTERSTITIAL_AD_UNIT_ID : null;
        }

        if ("rewarded".equals(format)) {
            return PRESET_REWARDED.equals(preset) ? TEST_REWARDED_AD_UNIT_ID : null;
        }

        if ("rewarded_interstitial".equals(format)) {
            return PRESET_REWARDED_INTERSTITIAL.equals(preset) ? TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID : null;
        }

        if ("native".equals(format)) {
            if (PRESET_NATIVE.equals(preset)) {
                return TEST_NATIVE_AD_UNIT_ID;
            }
            if (PRESET_NATIVE_VIDEO.equals(preset)) {
                return TEST_NATIVE_VIDEO_AD_UNIT_ID;
            }
            return null;
        }

        if ("banner".equals(format)) {
            if (PRESET_BANNER_FIXED.equals(preset)) {
                return TEST_BANNER_FIXED_AD_UNIT_ID;
            }
            if (PRESET_BANNER_ADAPTIVE.equals(preset)) {
                return TEST_BANNER_ADAPTIVE_AD_UNIT_ID;
            }
            return null;
        }

        if ("inline_banner".equals(format)) {
            return PRESET_BANNER_INLINE_ADAPTIVE.equals(preset) ? TEST_BANNER_ADAPTIVE_AD_UNIT_ID : null;
        }

        return null;
    }

    private static String legacyDefaultForFormat(String format) {
        if ("app_open".equals(format)) {
            return TEST_APP_OPEN_AD_UNIT_ID;
        }
        if ("interstitial".equals(format)) {
            return TEST_INTERSTITIAL_AD_UNIT_ID;
        }
        if ("rewarded".equals(format)) {
            return TEST_REWARDED_AD_UNIT_ID;
        }
        if ("rewarded_interstitial".equals(format)) {
            return TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID;
        }
        if ("native".equals(format)) {
            return TEST_NATIVE_AD_UNIT_ID;
        }
        if ("banner".equals(format)) {
            return TEST_BANNER_FIXED_AD_UNIT_ID;
        }
        if ("inline_banner".equals(format)) {
            return TEST_BANNER_FIXED_AD_UNIT_ID;
        }
        return null;
    }
}

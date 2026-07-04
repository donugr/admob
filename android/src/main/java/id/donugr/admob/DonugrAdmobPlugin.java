package id.donugr.admob;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import id.donugr.admob.ads.AppOpenAdController;
import id.donugr.admob.ads.BannerAdController;
import id.donugr.admob.ads.InterstitialAdController;
import id.donugr.admob.ads.RewardedAdController;
import id.donugr.admob.consent.AndroidConsentController;
import id.donugr.admob.core.PluginResultHelper;
import id.donugr.admob.core.RuntimeConfig;
import id.donugr.admob.events.AdEventDispatcher;
import id.donugr.admob.inlinebanner.InlineBannerController;
import id.donugr.admob.nativeads.NativeAdController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@CapacitorPlugin(name = "DonugrAdmob")
public class DonugrAdmobPlugin extends Plugin implements
    AdEventDispatcher.EventSink,
    BannerAdController.BannerHost,
    InterstitialAdController.InterstitialHost,
    RewardedAdController.RewardedHost,
    AppOpenAdController.AppOpenHost,
    NativeAdController.NativeHost,
    InlineBannerController.InlineBannerHost,
    AndroidConsentController.DonugrConsentHost {

    private static final String APPLICATION_ID_METADATA_NAME = "com.google.android.gms.ads.APPLICATION_ID";

    private final RuntimeConfig runtimeConfig = new RuntimeConfig();
    private AdEventDispatcher events;
    private BannerAdController bannerController;
    private InterstitialAdController interstitialController;
    private RewardedAdController rewardedController;
    private AppOpenAdController appOpenController;
    private NativeAdController nativeAdController;
    private InlineBannerController inlineBannerController;
    private AndroidConsentController consentController;

    @Override
    public void load() {
        super.load();
        events = new AdEventDispatcher(this);
        bannerController = new BannerAdController(this, runtimeConfig, events);
        interstitialController = new InterstitialAdController(this, runtimeConfig, events);
        rewardedController = new RewardedAdController(this, runtimeConfig, events);
        appOpenController = new AppOpenAdController(this, runtimeConfig, events);
        nativeAdController = new NativeAdController(this, runtimeConfig, events);
        inlineBannerController = new InlineBannerController(this, runtimeConfig, events);
        consentController = new AndroidConsentController(this, events);
    }

    @Override
    public Context getPluginContext() {
        return getContext();
    }

    @Override
    public Activity getPluginActivity() {
        return getActivity();
    }

    @Override
    public void dispatch(String eventName, JSObject payload) {
        notifyListeners(eventName, payload);
    }

    @Override
    public String requireTrimmed(PluginCall call, String key) {
        return String.valueOf(call.getString(key, "")).trim();
    }

    @PluginMethod
    public void configure(PluginCall call) {
        boolean enabled = call.getBoolean("enabled", false);
        boolean testMode = call.getBoolean("testMode", false);
        runtimeConfig.setEnabled(enabled);
        runtimeConfig.setTestMode(testMode);
        runtimeConfig.setPlacements(extractPlacements(call.getObject("placements", new JSObject())));

        if (!enabled) {
            runtimeConfig.setApplicationId("");
            runtimeConfig.setApplicationIdSource("missing");
            clearAllInternal();
            call.resolve(PluginResultHelper.success("disabled"));
            return;
        }

        runtimeConfig.setApplicationIdSource("missing");
        String resolvedApplicationId = resolveApplicationId(requireTrimmed(call, "applicationId"));
        if (resolvedApplicationId == null) {
            clearAllInternal();
            call.resolve(PluginResultHelper.failure("APPLICATION_ID_INVALID", "Invalid AdMob applicationId. Expected a value like ca-app-pub-xxxxx~yyyyy.", "error"));
            return;
        }
        if (TextUtils.isEmpty(resolvedApplicationId)) {
            clearAllInternal();
            call.resolve(PluginResultHelper.failure("APPLICATION_ID_MISSING", "Missing AdMob applicationId. Provide it in configure() or in AndroidManifest.xml.", "error"));
            return;
        }

        runtimeConfig.setApplicationId(resolvedApplicationId);
        ensureInitialized();
        call.resolve(PluginResultHelper.success("ready"));
    }

    @PluginMethod
    public void configureRequest(PluginCall call) {
        applyRequestConfiguration(call);
        runtimeConfig.setRequestConfigurationConfigured(true);
        call.resolve(PluginResultHelper.success("ready"));
    }

    @PluginMethod
    public void getRuntimeInfo(PluginCall call) {
        JSObject data = new JSObject();
        data.put("platform", "android");
        data.put("enabled", runtimeConfig.isEnabled());
        data.put("testMode", runtimeConfig.isTestMode());
        data.put("applicationIdConfigured", !TextUtils.isEmpty(runtimeConfig.getApplicationId()));
        data.put("applicationIdSource", runtimeConfig.getApplicationIdSource());
        data.put("placementsConfigured", runtimeConfig.getPlacementsCount());
        data.put("requestConfigurationConfigured", runtimeConfig.isRequestConfigurationConfigured());
        data.put("consentStatus", consentController.getConsentStatusName());
        data.put("activeSlots", nativeAdController.getActiveSlotsCount());
        data.put("loadingSlots", nativeAdController.getLoadingSlotsCount());
        data.put("readySlots", nativeAdController.getReadySlotsCount());
        data.put("attachedSlots", nativeAdController.getAttachedSlotsCount());
        data.put("failedSlots", nativeAdController.getFailedSlotsCount());
        data.put("expiredSlots", nativeAdController.getExpiredSlotsCount());
        data.put("inlineBannerActiveSlots", inlineBannerController.getActiveSlotsCount());
        data.put("inlineBannerLoadingSlots", inlineBannerController.getLoadingSlotsCount());
        data.put("inlineBannerReadySlots", inlineBannerController.getReadySlotsCount());
        data.put("inlineBannerAttachedSlots", inlineBannerController.getAttachedSlotsCount());
        data.put("inlineBannerFailedSlots", inlineBannerController.getFailedSlotsCount());
        call.resolve(PluginResultHelper.success(runtimeConfig.isEnabled() ? "ready" : "disabled", data));
    }

    @PluginMethod
    public void requestConsentInfo(PluginCall call) {
        consentController.requestConsentInfo(call);
    }

    @PluginMethod
    public void showConsentFormIfRequired(PluginCall call) {
        consentController.showConsentFormIfRequired(call);
    }

    @PluginMethod
    public void showPrivacyOptions(PluginCall call) {
        consentController.showPrivacyOptions(call);
    }

    @PluginMethod
    public void getConsentStatus(PluginCall call) {
        consentController.getConsentStatus(call);
    }

    @PluginMethod
    public void resetConsentForTesting(PluginCall call) {
        consentController.resetConsentForTesting(call);
    }

    @PluginMethod
    public void getTrackingAuthorizationStatus(PluginCall call) {
        JSObject data = new JSObject();
        data.put("status", "unsupported");
        call.resolve(PluginResultHelper.success("unsupported", data));
    }

    @PluginMethod
    public void requestTrackingAuthorization(PluginCall call) {
        JSObject data = new JSObject();
        data.put("status", "unsupported");
        call.resolve(PluginResultHelper.success("unsupported", data));
    }

    @PluginMethod
    public void loadBanner(PluginCall call) {
        bannerController.showBanner(call);
    }

    @PluginMethod
    public void showBanner(PluginCall call) {
        bannerController.showBanner(call);
    }

    @PluginMethod
    public void hideBanner(PluginCall call) {
        bannerController.hideBanner(call);
    }

    @PluginMethod
    public void destroyBanner(PluginCall call) {
        bannerController.destroyBanner(call);
    }

    @PluginMethod
    public void preloadInterstitial(PluginCall call) {
        interstitialController.preload(call);
    }

    @PluginMethod
    public void isInterstitialReady(PluginCall call) {
        interstitialController.isReady(call);
    }

    @PluginMethod
    public void showInterstitial(PluginCall call) {
        interstitialController.show(call);
    }

    @PluginMethod
    public void preloadRewarded(PluginCall call) {
        rewardedController.preload(call);
    }

    @PluginMethod
    public void isRewardedReady(PluginCall call) {
        rewardedController.isReady(call);
    }

    @PluginMethod
    public void showRewarded(PluginCall call) {
        rewardedController.show(call);
    }

    @PluginMethod
    public void preloadAppOpen(PluginCall call) {
        appOpenController.preload(call);
    }

    @PluginMethod
    public void isAppOpenReady(PluginCall call) {
        appOpenController.isReady(call);
    }

    @PluginMethod
    public void showAppOpen(PluginCall call) {
        appOpenController.show(call);
    }

    @PluginMethod
    public void preloadNative(PluginCall call) {
        nativeAdController.preload(call);
    }

    @PluginMethod
    public void isNativeReady(PluginCall call) {
        nativeAdController.isReady(call);
    }

    @PluginMethod
    public void attachNative(PluginCall call) {
        nativeAdController.attach(call);
    }

    @PluginMethod
    public void detachNative(PluginCall call) {
        nativeAdController.detach(call);
    }

    @PluginMethod
    public void destroyNative(PluginCall call) {
        nativeAdController.destroy(call);
    }

    @PluginMethod
    public void refreshNative(PluginCall call) {
        nativeAdController.refresh(call);
    }

    @PluginMethod
    public void preloadInlineBanner(PluginCall call) {
        inlineBannerController.preload(call);
    }

    @PluginMethod
    public void isInlineBannerReady(PluginCall call) {
        inlineBannerController.isReady(call);
    }

    @PluginMethod
    public void attachInlineBanner(PluginCall call) {
        inlineBannerController.attach(call);
    }

    @PluginMethod
    public void detachInlineBanner(PluginCall call) {
        inlineBannerController.detach(call);
    }

    @PluginMethod
    public void destroyInlineBanner(PluginCall call) {
        inlineBannerController.destroy(call);
    }

    @PluginMethod
    public void refreshInlineBanner(PluginCall call) {
        inlineBannerController.refresh(call);
    }

    @PluginMethod
    public void clearAll(PluginCall call) {
        clearAllInternal();
        call.resolve(PluginResultHelper.success(runtimeConfig.isEnabled() ? "ready" : "disabled"));
    }

    @PluginMethod
    public void removeAllListeners(PluginCall call) {
        super.removeAllListeners();
        call.resolve();
    }

    private void ensureInitialized() {
        if (runtimeConfig.isMobileAdsInitialized()) {
            return;
        }
        MobileAds.initialize(getContext(), initializationStatus -> {
        });
        runtimeConfig.setMobileAdsInitialized(true);
    }

    private Map<String, String> extractPlacements(JSObject input) {
        Map<String, String> extracted = new HashMap<>();
        if (input == null) {
            return extracted;
        }

        Iterator<String> keys = input.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = String.valueOf(input.optString(key, "")).trim();
            if (!key.trim().isEmpty() && !value.isEmpty()) {
                extracted.put(key.trim(), value);
            }
        }
        return extracted;
    }

    private String resolveApplicationId(String explicitApplicationId) {
        String jsValue = String.valueOf(explicitApplicationId == null ? "" : explicitApplicationId).trim();
        if (!TextUtils.isEmpty(jsValue)) {
            if (!isValidApplicationId(jsValue)) {
                return null;
            }
            runtimeConfig.setApplicationIdSource("js");
            return jsValue;
        }

        try {
            PackageManager packageManager = getContext().getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
                getContext().getPackageName(),
                PackageManager.GET_META_DATA
            );
            Bundle metadata = applicationInfo.metaData;
            String manifestValue = metadata == null ? "" : String.valueOf(metadata.getString(APPLICATION_ID_METADATA_NAME, "")).trim();
            if (TextUtils.isEmpty(manifestValue)) {
                return "";
            }
            if (!isValidApplicationId(manifestValue)) {
                return null;
            }
            runtimeConfig.setApplicationIdSource("android_manifest");
            return manifestValue;
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean isValidApplicationId(String value) {
        return !TextUtils.isEmpty(value) && value.startsWith("ca-app-pub-") && value.contains("~");
    }

    private void applyRequestConfiguration(PluginCall call) {
        RequestConfiguration.Builder builder = MobileAds.getRequestConfiguration().toBuilder();

        String maxAdContentRating = requireTrimmed(call, "maxAdContentRating");
        if ("G".equals(maxAdContentRating) || "PG".equals(maxAdContentRating) || "T".equals(maxAdContentRating) || "MA".equals(maxAdContentRating) || "".equals(maxAdContentRating)) {
            builder.setMaxAdContentRating(maxAdContentRating);
        }

        Boolean childDirected = call.getBoolean("tagForChildDirectedTreatment");
        if (childDirected != null) {
            builder.setTagForChildDirectedTreatment(
                childDirected ? RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE : RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
            );
        }

        Boolean underAge = call.getBoolean("tagForUnderAgeOfConsent");
        if (underAge != null) {
            builder.setTagForUnderAgeOfConsent(
                underAge ? RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE : RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
            );
        }

        JSArray testDeviceIds = call.getArray("testDeviceIds");
        if (testDeviceIds != null) {
            List<String> ids = new ArrayList<>();
            for (int index = 0; index < testDeviceIds.length(); index += 1) {
                String value = String.valueOf(testDeviceIds.optString(index, "")).trim();
                if (!value.isEmpty()) {
                    ids.add(value);
                }
            }
            builder.setTestDeviceIds(ids);
        }

        Double appVolume = call.getDouble("appVolume");
        if (appVolume != null) {
            MobileAds.setAppVolume(Math.max(0f, Math.min(1f, appVolume.floatValue())));
        }

        Boolean appMuted = call.getBoolean("appMuted");
        if (appMuted != null) {
            MobileAds.setAppMuted(appMuted);
        }

        MobileAds.setRequestConfiguration(builder.build());
    }

    private void clearAllInternal() {
        bannerController.clearAll();
        interstitialController.clearAll();
        rewardedController.clearAll();
        appOpenController.clearAll();
        nativeAdController.clearAll();
        inlineBannerController.clearAll();
    }

    @Override
    protected void handleOnDestroy() {
        clearAllInternal();
        super.handleOnDestroy();
    }
}

package id.donugr.admob.consent;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import id.donugr.admob.core.PluginResultHelper;
import id.donugr.admob.events.AdEventDispatcher;
import java.util.ArrayList;
import java.util.List;

public class AndroidConsentController {
    private static final String META_DEBUG_GEOGRAPHY = "id.donugr.admob.UMP_DEBUG_GEOGRAPHY";
    private static final String META_TEST_DEVICE_IDS = "id.donugr.admob.UMP_TEST_DEVICE_IDS";

    private final DonugrConsentHost host;
    private final AdEventDispatcher events;
    private final ConsentInformation consentInformation;

    public AndroidConsentController(DonugrConsentHost host, AdEventDispatcher events) {
        this.host = host;
        this.events = events;
        this.consentInformation = UserMessagingPlatform.getConsentInformation(host.getPluginContext());
    }

    public void requestConsentInfo(PluginCall call) {
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for consent update.", "not_ready"));
            return;
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            buildRequestParameters(activity),
            () -> {
                events.emitConsentUpdated();
                call.resolve(PluginResultHelper.success(resolveStatus(), createConsentInfoPayload()));
            },
            requestConsentError -> {
                events.emitConsentUpdated();
                if (consentInformation.canRequestAds()) {
                    call.resolve(PluginResultHelper.success(resolveStatus(), createConsentInfoPayload()));
                    return;
                }
                call.resolve(PluginResultHelper.failure(
                    String.valueOf(requestConsentError.getErrorCode()),
                    requestConsentError.getMessage(),
                    resolveStatus()
                ));
            }
        );
    }

    public void showConsentFormIfRequired(PluginCall call) {
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for consent form.", "not_ready"));
            return;
        }

        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
            activity,
            formError -> {
                events.emitConsentUpdated();
                if (formError != null) {
                    call.resolve(PluginResultHelper.failure(
                        String.valueOf(formError.getErrorCode()),
                        formError.getMessage(),
                        resolveStatus()
                    ));
                    return;
                }
                call.resolve(PluginResultHelper.success(resolveStatus(), createConsentInfoPayload()));
            }
        );
    }

    public void showPrivacyOptions(PluginCall call) {
        Activity activity = host.getPluginActivity();
        if (activity == null) {
            call.resolve(PluginResultHelper.failure("NOT_READY", "Activity is unavailable for privacy options.", "not_ready"));
            return;
        }

        UserMessagingPlatform.showPrivacyOptionsForm(
            activity,
            formError -> {
                events.emitConsentUpdated();
                if (formError != null) {
                    call.resolve(PluginResultHelper.failure(
                        String.valueOf(formError.getErrorCode()),
                        formError.getMessage(),
                        resolveStatus()
                    ));
                    return;
                }
                call.resolve(PluginResultHelper.success(resolveStatus(), createConsentInfoPayload()));
            }
        );
    }

    public void getConsentStatus(PluginCall call) {
        call.resolve(PluginResultHelper.success(resolveStatus(), createConsentInfoPayload()));
    }

    public void resetConsentForTesting(PluginCall call) {
        consentInformation.reset();
        events.emitConsentUpdated();
        call.resolve(PluginResultHelper.success("ready"));
    }

    public String getConsentStatusName() {
        return mapConsentStatus(consentInformation.getConsentStatus());
    }

    private String resolveStatus() {
        return consentInformation.canRequestAds() ? "ready" : "not_ready";
    }

    private JSObject createConsentInfoPayload() {
        JSObject data = new JSObject();
        data.put("status", getConsentStatusName());
        data.put("canRequestAds", consentInformation.canRequestAds());
        data.put(
            "privacyOptionsRequired",
            consentInformation.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        );
        return data;
    }

    private ConsentRequestParameters buildRequestParameters(Activity activity) {
        ConsentRequestParameters.Builder builder = new ConsentRequestParameters.Builder();
        ConsentDebugSettings.Builder debugBuilder = new ConsentDebugSettings.Builder(activity);
        applyDebugMetadata(activity, debugBuilder);
        ConsentDebugSettings debugSettings = debugBuilder.build();
        builder.setConsentDebugSettings(debugSettings);
        return builder.build();
    }

    private void applyDebugMetadata(Activity activity, ConsentDebugSettings.Builder debugBuilder) {
        Bundle metadata = readMetadata();
        if (metadata == null) {
            return;
        }

        String geography = String.valueOf(metadata.getString(META_DEBUG_GEOGRAPHY, "")).trim();
        if ("DISABLED".equalsIgnoreCase(geography)) {
            debugBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED);
        } else if ("EEA".equalsIgnoreCase(geography)) {
            debugBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA);
        } else if ("NOT_EEA".equalsIgnoreCase(geography)) {
            debugBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA);
        } else if ("UK".equalsIgnoreCase(geography)) {
            debugBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_OTHER);
        } else if ("OTHER".equalsIgnoreCase(geography)) {
            debugBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_OTHER);
        } else if ("REGULATED_US_STATE".equalsIgnoreCase(geography)) {
            debugBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_REGULATED_US_STATE);
        } else if ("REGULATED_OTHER".equalsIgnoreCase(geography)) {
            debugBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_OTHER);
        }

        String rawTestDeviceIds = String.valueOf(metadata.getString(META_TEST_DEVICE_IDS, "")).trim();
        for (String deviceId : splitCommaSeparated(rawTestDeviceIds)) {
            debugBuilder.addTestDeviceHashedId(deviceId);
        }
    }

    private Bundle readMetadata() {
        try {
            PackageManager packageManager = host.getPluginContext().getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
                host.getPluginContext().getPackageName(),
                PackageManager.GET_META_DATA
            );
            return applicationInfo.metaData;
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> splitCommaSeparated(String rawValue) {
        List<String> values = new ArrayList<>();
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return values;
        }

        String[] parts = rawValue.split(",");
        for (String part : parts) {
            String normalized = String.valueOf(part).trim();
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private String mapConsentStatus(int status) {
        if (status == ConsentInformation.ConsentStatus.REQUIRED) {
            return "required";
        }
        if (status == ConsentInformation.ConsentStatus.NOT_REQUIRED) {
            return "not_required";
        }
        if (status == ConsentInformation.ConsentStatus.OBTAINED) {
            return "obtained";
        }
        return "unknown";
    }

    public interface DonugrConsentHost {
        android.content.Context getPluginContext();
        Activity getPluginActivity();
    }
}

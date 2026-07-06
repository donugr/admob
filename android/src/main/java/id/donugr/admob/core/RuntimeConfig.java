package id.donugr.admob.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RuntimeConfig {
    private boolean enabled = false;
    private boolean testMode = false;
    private boolean releaseSystemUiOnAdInteraction = true;
    private boolean emitAdEvents = false;
    private boolean requestConfigurationConfigured = false;
    private boolean mobileAdsInitialized = false;
    private String loggingLevel = "off";
    private String applicationId = "";
    private String applicationIdSource = "missing";
    private final Map<String, String> placements = new ConcurrentHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public boolean isReleaseSystemUiOnAdInteraction() {
        return releaseSystemUiOnAdInteraction;
    }

    public void setReleaseSystemUiOnAdInteraction(boolean releaseSystemUiOnAdInteraction) {
        this.releaseSystemUiOnAdInteraction = releaseSystemUiOnAdInteraction;
    }

    public boolean isEmitAdEvents() {
        return emitAdEvents;
    }

    public void setEmitAdEvents(boolean emitAdEvents) {
        this.emitAdEvents = emitAdEvents;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        String normalized = loggingLevel == null ? "off" : loggingLevel.trim().toLowerCase();
        switch (normalized) {
            case "debug":
            case "info":
            case "warn":
            case "error":
                this.loggingLevel = normalized;
                return;
            case "off":
            default:
                this.loggingLevel = "off";
        }
    }

    public boolean isRequestConfigurationConfigured() {
        return requestConfigurationConfigured;
    }

    public void setRequestConfigurationConfigured(boolean requestConfigurationConfigured) {
        this.requestConfigurationConfigured = requestConfigurationConfigured;
    }

    public boolean isMobileAdsInitialized() {
        return mobileAdsInitialized;
    }

    public void setMobileAdsInitialized(boolean mobileAdsInitialized) {
        this.mobileAdsInitialized = mobileAdsInitialized;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId == null ? "" : applicationId;
    }

    public String getApplicationIdSource() {
        return applicationIdSource;
    }

    public void setApplicationIdSource(String applicationIdSource) {
        this.applicationIdSource = applicationIdSource == null ? "missing" : applicationIdSource;
    }

    public void setPlacements(Map<String, String> nextPlacements) {
        placements.clear();
        if (nextPlacements != null) {
            placements.putAll(nextPlacements);
        }
    }

    public String resolvePlacement(String placementId) {
        return placements.getOrDefault(placementId, "");
    }

    public int getPlacementsCount() {
        return placements.size();
    }

    public void reset() {
        enabled = false;
        testMode = false;
        releaseSystemUiOnAdInteraction = true;
        emitAdEvents = false;
        requestConfigurationConfigured = false;
        mobileAdsInitialized = false;
        loggingLevel = "off";
        applicationId = "";
        applicationIdSource = "missing";
        placements.clear();
    }
}

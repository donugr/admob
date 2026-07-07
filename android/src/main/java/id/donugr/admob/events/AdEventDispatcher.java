package id.donugr.admob.events;

import com.getcapacitor.JSObject;

import id.donugr.admob.core.RuntimeConfig;

public class AdEventDispatcher {
    private final EventSink eventSink;
    private final RuntimeConfig runtimeConfig;

    public AdEventDispatcher(
        EventSink eventSink,
        RuntimeConfig runtimeConfig
    ) {
        this.eventSink = eventSink;
        this.runtimeConfig = runtimeConfig;
    }

    public void emit(
        String format,
        String placementId,
        String phase,
        String code,
        String message
    ) {
        emit(
            format,
            placementId,
            phase,
            code,
            message,
            null,
            null
        );
    }

    public void emit(
        String format,
        String placementId,
        String phase,
        String code,
        String message,
        String slotId
    ) {
        emit(
            format,
            placementId,
            phase,
            code,
            message,
            slotId,
            null
        );
    }

    public void emit(
        String format,
        String placementId,
        String phase,
        String code,
        String message,
        String slotId,
        JSObject data
    ) {
        if (!runtimeConfig.isEmitAdEvents()) {
            return;
        }

        JSObject payload = new JSObject();

        payload.put(
            "format",
            format == null || format.isEmpty()
                ? "unknown"
                : format
        );

        payload.put(
            "placementId",
            placementId == null
                ? ""
                : placementId
        );

        if (slotId != null && !slotId.isEmpty()) {
            payload.put("slotId", slotId);
        }

        payload.put(
            "phase",
            phase == null || phase.isEmpty()
                ? "unknown"
                : phase
        );

        if (code != null && !code.isEmpty()) {
            payload.put("code", code);
        }

        if (message != null && !message.isEmpty()) {
            payload.put("message", message);
        }

        if (data != null && data.length() > 0) {
            payload.put("data", data);
        }

        eventSink.dispatch(
            "adEvent",
            payload
        );
    }

    public void debug(
        String scope,
        String code,
        String message
    ) {
        log(
            "debug",
            scope,
            code,
            message,
            null,
            null,
            null,
            null,
            null
        );
    }

    public void info(
        String scope,
        String code,
        String message
    ) {
        log(
            "info",
            scope,
            code,
            message,
            null,
            null,
            null,
            null,
            null
        );
    }

    public void warn(
        String scope,
        String code,
        String message
    ) {
        log(
            "warn",
            scope,
            code,
            message,
            null,
            null,
            null,
            null,
            null
        );
    }

    public void error(
        String scope,
        String code,
        String message
    ) {
        log(
            "error",
            scope,
            code,
            message,
            null,
            null,
            null,
            null,
            null
        );
    }

    public void log(
        String level,
        String scope,
        String code,
        String message,
        String placementId,
        String slotId,
        String hostId,
        String phase,
        JSObject data
    ) {
        if (!shouldEmit(level)) {
            return;
        }

        JSObject payload = new JSObject();

        payload.put(
            "level",
            normalizeLevel(level)
        );

        payload.put(
            "scope",
            scope == null || scope.isEmpty()
                ? "core"
                : scope
        );

        payload.put(
            "code",
            code == null
                ? ""
                : code
        );

        payload.put(
            "message",
            message == null
                ? ""
                : message
        );

        payload.put(
            "timestamp",
            System.currentTimeMillis()
        );

        if (
            placementId != null &&
            !placementId.isEmpty()
        ) {
            payload.put(
                "placementId",
                placementId
            );
        }

        if (
            slotId != null &&
            !slotId.isEmpty()
        ) {
            payload.put(
                "slotId",
                slotId
            );
        }

        if (
            hostId != null &&
            !hostId.isEmpty()
        ) {
            payload.put(
                "hostId",
                hostId
            );
        }

        if (
            phase != null &&
            !phase.isEmpty()
        ) {
            payload.put(
                "phase",
                phase
            );
        }

        if (
            data != null &&
            data.length() > 0
        ) {
            payload.put(
                "data",
                data
            );
        }

        eventSink.dispatch(
            "adLog",
            payload
        );
    }

    public void emitConsentUpdated() {
        emit(
            "banner",
            "consent.global",
            "consent_updated",
            null,
            "Consent status updated."
        );
    }

    private boolean shouldEmit(
        String level
    ) {
        int threshold = levelWeight(
            runtimeConfig.getLoggingLevel()
        );

        int current = levelWeight(level);

        return (
            threshold > 0 &&
            current > 0 &&
            current <= threshold
        );
    }

    private String normalizeLevel(
        String level
    ) {
        String normalized =
            level == null
                ? "debug"
                : level.trim().toLowerCase();

        switch (normalized) {
            case "error":
            case "warn":
            case "info":
                return normalized;

            case "debug":
            default:
                return "debug";
        }
    }

    private int levelWeight(
        String level
    ) {
        if (level == null) {
            return 0;
        }

        switch (
            level.trim().toLowerCase()
        ) {
            case "error":
                return 1;

            case "warn":
                return 2;

            case "info":
                return 3;

            case "debug":
                return 4;

            case "off":
            default:
                return 0;
        }
    }

    public interface EventSink {
        void dispatch(
            String eventName,
            JSObject payload
        );
    }
}
package id.donugr.admob.events;

import com.getcapacitor.JSObject;

public class AdEventDispatcher {
    private final EventSink eventSink;

    public AdEventDispatcher(EventSink eventSink) {
        this.eventSink = eventSink;
    }

    public void emit(String format, String placementId, String phase, String code, String message) {
        emit(format, placementId, phase, code, message, null);
    }

    public void emit(String format, String placementId, String phase, String code, String message, String slotId) {
        JSObject payload = new JSObject();
        payload.put("format", format);
        payload.put("placementId", placementId);
        if (slotId != null && !slotId.isEmpty()) {
            payload.put("slotId", slotId);
        }
        payload.put("phase", phase);
        if (code != null && !code.isEmpty()) {
            payload.put("code", code);
        }
        if (message != null && !message.isEmpty()) {
            payload.put("message", message);
        }
        eventSink.dispatch("adEvent", payload);
    }

    public void emitConsentUpdated() {
        emit("banner", "consent.global", "consent_updated", null, "Consent status updated.");
    }

    public interface EventSink {
        void dispatch(String eventName, JSObject payload);
    }
}

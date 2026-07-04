package id.donugr.admob.inlinebanner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class InlineBannerSlotStore {
    private final Map<String, InlineBannerSlotState> slots = new ConcurrentHashMap<>();

    InlineBannerSlotState getOrCreate(String slotId) {
        return slots.computeIfAbsent(slotId, InlineBannerSlotState::new);
    }

    InlineBannerSlotState get(String slotId) {
        return slots.get(slotId);
    }

    InlineBannerSlotState remove(String slotId) {
        return slots.remove(slotId);
    }

    void clear() {
        slots.clear();
    }

    Map<String, InlineBannerSlotState> snapshot() {
        return new ConcurrentHashMap<>(slots);
    }
}

package id.donugr.admob.nativeads;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class NativeSlotStore {
    private final Map<String, NativeSlotState> slots = new ConcurrentHashMap<>();

    NativeSlotState getOrCreate(String slotId) {
        return slots.computeIfAbsent(slotId, NativeSlotState::new);
    }

    NativeSlotState put(NativeSlotState state) {
        slots.put(state.slotId, state);
        return state;
    }

    NativeSlotState get(String slotId) {
        return slots.get(slotId);
    }

    NativeSlotState remove(String slotId) {
        return slots.remove(slotId);
    }

    void clear() {
        slots.clear();
    }

    Map<String, NativeSlotState> snapshot() {
        return new ConcurrentHashMap<>(slots);
    }
}

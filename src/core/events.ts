import type { AdEvent } from "../definitions"

export type AdEventListener = (event: AdEvent) => void

export class AdEventEmitter {
  private readonly listeners = new Set<AdEventListener>()

  add(listener: AdEventListener) {
    this.listeners.add(listener)
  }

  remove(listener: AdEventListener) {
    this.listeners.delete(listener)
  }

  clear() {
    this.listeners.clear()
  }

  emit(event: AdEvent) {
    for (const listener of this.listeners) {
      listener(event)
    }
  }
}

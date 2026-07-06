import type { AdEvent, AdLogEvent } from "../definitions"

export type AdEventListener = (event: AdEvent) => void
export type AdLogListener = (event: AdLogEvent) => void

export class EventEmitter<T> {
  private readonly listeners = new Set<(event: T) => void>()

  add(listener: (event: T) => void) {
    this.listeners.add(listener)
  }

  remove(listener: (event: T) => void) {
    this.listeners.delete(listener)
  }

  clear() {
    this.listeners.clear()
  }

  emit(event: T) {
    for (const listener of this.listeners) {
      listener(event)
    }
  }
}

export class AdEventEmitter extends EventEmitter<AdEvent> {}
export class AdLogEmitter extends EventEmitter<AdLogEvent> {}

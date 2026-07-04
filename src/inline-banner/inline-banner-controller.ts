import type { BridgeResult } from "../definitions"
import type { InlineBannerOptions, InlineBannerRuntimeSnapshot } from "./inline-banner-types"

function unsupported<T = undefined>(message: string): BridgeResult<T> {
  return {
    ok: false,
    code: "NOT_IMPLEMENTED",
    message,
    status: "unsupported",
  }
}

export class InlineBannerController {
  getModuleStatus(): "placeholder" {
    return "placeholder"
  }

  getRuntimeSnapshot(_slotId: string): InlineBannerRuntimeSnapshot | null {
    return null
  }

  preload(_options: InlineBannerOptions): Promise<BridgeResult> {
    return Promise.resolve(
      unsupported("Inline banner preload will be implemented in the host-based inline banner batch."),
    )
  }

  attach(_options: InlineBannerOptions): Promise<BridgeResult> {
    return Promise.resolve(
      unsupported("Inline banner attach will be implemented in the host-based inline banner batch."),
    )
  }

  detach(_slotId: string): Promise<BridgeResult> {
    return Promise.resolve(
      unsupported("Inline banner detach will be implemented in the host-based inline banner batch."),
    )
  }

  destroy(_slotId: string): Promise<BridgeResult> {
    return Promise.resolve(
      unsupported("Inline banner destroy will be implemented in the host-based inline banner batch."),
    )
  }

  clearAll(): Promise<BridgeResult> {
    return Promise.resolve(
      unsupported("Inline banner clearAll will be implemented in the host-based inline banner batch."),
    )
  }
}

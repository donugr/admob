import type { InlineBannerAdSizeStrategy, NativeHostRect } from "../definitions"

export type InlineBannerModuleStatus = "placeholder"

export type InlineBannerSlotStatus =
  | "idle"
  | "loading"
  | "ready"
  | "attached"
  | "failed"

export type InlineBannerOptions = {
  placementId: string
  slotId: string
  hostId: string
  adUnitId?: string
  hostRect?: NativeHostRect
  adSizeStrategy?: InlineBannerAdSizeStrategy
}

export type InlineBannerRuntimeSnapshot = {
  placementId: string
  slotId: string
  hostId: string
  status: InlineBannerSlotStatus
  attached: boolean
}

export const inlineBannerModuleStatus: InlineBannerModuleStatus = "placeholder"

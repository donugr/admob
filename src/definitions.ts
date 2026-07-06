import type { PluginListenerHandle } from "@capacitor/core"

export type AdFormat = "banner" | "interstitial" | "rewarded" | "rewarded_interstitial" | "native" | "inline_banner" | "app_open"

export type AvailabilityStatus = "ready" | "loading" | "not_ready" | "disabled" | "unsupported" | "error"

export type ConsentStatus = "unknown" | "required" | "not_required" | "obtained" | "denied"

export type TrackingAuthorizationStatus = "not_determined" | "restricted" | "denied" | "authorized" | "unsupported"

export type ApplicationIdSource = "js" | "android_manifest" | "ios_plist" | "missing"

export type MaxAdContentRating = "G" | "PG" | "T" | "MA" | ""

export type LoggingLevel = "off" | "error" | "warn" | "info" | "debug"

export type AdLogLevel = Exclude<LoggingLevel, "off">

export type TestAdPreset =
  | "app_open"
  | "banner_fixed"
  | "banner_adaptive"
  | "banner_inline_adaptive"
  | "interstitial"
  | "rewarded"
  | "rewarded_interstitial"
  | "native"
  | "native_video"

export type BridgeResult<T = undefined> = {
  ok: boolean
  status?: AvailabilityStatus
  code?: string
  message?: string
  data?: T
}

export type ConfigureOptions = {
  enabled: boolean
  testMode: boolean
  releaseSystemUiOnAdInteraction?: boolean
  loggingLevel?: LoggingLevel
  emitAdEvents?: boolean
  applicationId?: string
  placements?: Record<string, string>
}

export type RequestConfigurationOptions = {
  maxAdContentRating?: MaxAdContentRating
  tagForChildDirectedTreatment?: boolean | null
  tagForUnderAgeOfConsent?: boolean | null
  testDeviceIds?: string[]
  appMuted?: boolean
  appVolume?: number
}

export const DEFAULT_REQUEST_CONFIGURATION_OPTIONS: Readonly<Required<Pick<
  RequestConfigurationOptions,
  "maxAdContentRating" | "tagForChildDirectedTreatment" | "tagForUnderAgeOfConsent"
>> & Partial<Pick<RequestConfigurationOptions, "testDeviceIds" | "appMuted" | "appVolume">>> = {
  maxAdContentRating: "",
  tagForChildDirectedTreatment: null,
  tagForUnderAgeOfConsent: null,
}

export type ConsentInfo = {
  status: ConsentStatus
  canRequestAds: boolean
  privacyOptionsRequired: boolean
}

export type RuntimeInfo = {
  platform: "android" | "ios" | "web"
  enabled: boolean
  testMode: boolean
  releaseSystemUiOnAdInteraction?: boolean
  loggingLevel?: LoggingLevel
  emitAdEvents?: boolean
  applicationIdConfigured: boolean
  applicationIdSource: ApplicationIdSource
  placementsConfigured: number
  requestConfigurationConfigured: boolean
  consentStatus: ConsentStatus
  activeSlots?: number
  loadingSlots?: number
  readySlots?: number
  attachedSlots?: number
  failedSlots?: number
  expiredSlots?: number
  inlineBannerActiveSlots?: number
  inlineBannerLoadingSlots?: number
  inlineBannerReadySlots?: number
  inlineBannerAttachedSlots?: number
  inlineBannerFailedSlots?: number
}

export type BannerOptions = {
  placementId: string
  adUnitId?: string
  testAdPreset?: Extract<TestAdPreset, "banner_fixed" | "banner_adaptive">
  position?: "top" | "bottom"
}

export type FullscreenOptions = {
  placementId: string
  adUnitId?: string
  testAdPreset?: Extract<TestAdPreset, "app_open" | "interstitial" | "rewarded">
}

export type RewardedInterstitialOptions = {
  placementId: string
  adUnitId?: string
  testAdPreset?: Extract<TestAdPreset, "rewarded_interstitial">
}

export type NativeMediaMode = "auto" | "video_preferred"

export type NativeHostAnchor = "top" | "bottom"

export type NativeHostRect = {
  x: number
  y: number
  width: number
  height: number
  anchor?: NativeHostAnchor
}

export type NativeOptions = {
  placementId: string
  slotId: string
  hostId: string
  adUnitId?: string
  testAdPreset?: Extract<TestAdPreset, "native" | "native_video">
  ttlMs?: number
  hostRect?: NativeHostRect
  mediaMode?: NativeMediaMode
}

export type InlineBannerAdSizeStrategy = "current_orientation" | "landscape" | "portrait"

export type InlineBannerOptions = {
  placementId: string
  slotId: string
  hostId: string
  adUnitId?: string
  testAdPreset?: Extract<TestAdPreset, "banner_inline_adaptive">
  hostRect?: NativeHostRect
  adSizeStrategy?: InlineBannerAdSizeStrategy
}

export type AdEventPhase =
  | "loaded"
  | "failed"
  | "shown"
  | "dismissed"
  | "clicked"
  | "impression"
  | "reward_earned"
  | "attached"
  | "detached"
  | "destroyed"
  | "consent_updated"
  | "preload_start"
  | "preload_reused"
  | "preload_skip_loading"
  | "attach_skipped_same_host"
  | "layout_skipped_same_rect"

export type AdEvent = {
  format: AdFormat
  placementId: string
  slotId?: string
  phase: AdEventPhase
  code?: string
  message?: string
}

export type AdLogScope = AdFormat | "core" | "consent"

export type AdLogEvent = {
  level: AdLogLevel
  scope: AdLogScope
  code: string
  message: string
  placementId?: string
  slotId?: string
  hostId?: string
  phase?: string
  data?: Record<string, unknown>
  timestamp: number
}

export type DonugrAdmobPlugin = {
  configure(options: ConfigureOptions): Promise<BridgeResult>
  configureRequest(options: RequestConfigurationOptions): Promise<BridgeResult>
  getRuntimeInfo(): Promise<BridgeResult<RuntimeInfo>>
  requestConsentInfo(): Promise<BridgeResult<ConsentInfo>>
  showConsentFormIfRequired(): Promise<BridgeResult<ConsentInfo>>
  showPrivacyOptions(): Promise<BridgeResult<ConsentInfo>>
  getConsentStatus(): Promise<BridgeResult<ConsentInfo>>
  resetConsentForTesting(): Promise<BridgeResult>
  getTrackingAuthorizationStatus(): Promise<BridgeResult<{ status: TrackingAuthorizationStatus }>>
  requestTrackingAuthorization(): Promise<BridgeResult<{ status: TrackingAuthorizationStatus }>>
  loadBanner(options: BannerOptions): Promise<BridgeResult>
  showBanner(options: BannerOptions): Promise<BridgeResult>
  hideBanner(placementId: string): Promise<BridgeResult>
  destroyBanner(placementId: string): Promise<BridgeResult>
  preloadInterstitial(options: FullscreenOptions): Promise<BridgeResult>
  isInterstitialReady(placementId: string): Promise<BridgeResult<{ ready: boolean }>>
  showInterstitial(placementId: string): Promise<BridgeResult>
  preloadRewarded(options: FullscreenOptions): Promise<BridgeResult>
  isRewardedReady(placementId: string): Promise<BridgeResult<{ ready: boolean }>>
  showRewarded(placementId: string): Promise<BridgeResult>
  preloadRewardedInterstitial(options: RewardedInterstitialOptions): Promise<BridgeResult>
  isRewardedInterstitialReady(placementId: string): Promise<BridgeResult<{ ready: boolean }>>
  showRewardedInterstitial(placementId: string): Promise<BridgeResult>
  preloadAppOpen(options: FullscreenOptions): Promise<BridgeResult>
  isAppOpenReady(placementId: string): Promise<BridgeResult<{ ready: boolean }>>
  showAppOpen(placementId: string): Promise<BridgeResult>
  preloadNative(options: NativeOptions): Promise<BridgeResult>
  isNativeReady(slotId: string): Promise<BridgeResult<{ ready: boolean }>>
  attachNative(options: NativeOptions): Promise<BridgeResult>
  detachNative(slotId: string): Promise<BridgeResult>
  destroyNative(slotId: string): Promise<BridgeResult>
  refreshNative(options: NativeOptions): Promise<BridgeResult>
  preloadInlineBanner(options: InlineBannerOptions): Promise<BridgeResult>
  isInlineBannerReady(slotId: string): Promise<BridgeResult<{ ready: boolean }>>
  attachInlineBanner(options: InlineBannerOptions): Promise<BridgeResult>
  detachInlineBanner(slotId: string): Promise<BridgeResult>
  destroyInlineBanner(slotId: string): Promise<BridgeResult>
  refreshInlineBanner(options: InlineBannerOptions): Promise<BridgeResult>
  clearAll(): Promise<BridgeResult>
  addListener(eventName: "adEvent", listenerFunc: (event: AdEvent) => void): Promise<PluginListenerHandle>
  addListener(eventName: "adLog", listenerFunc: (event: AdLogEvent) => void): Promise<PluginListenerHandle>
  removeAllListeners(): Promise<void>
}

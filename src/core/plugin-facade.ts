import type { PluginListenerHandle } from "@capacitor/core"
import type {
  AdEvent,
  AdLogEvent,
  BannerOptions,
  BridgeResult,
  ConfigureOptions,
  ConsentInfo,
  DonugrAdmobPlugin,
  FullscreenOptions,
  InlineBannerOptions,
  NativeOptions,
  RequestConfigurationOptions,
  RewardedInterstitialOptions,
  RuntimeInfo,
  TrackingAuthorizationStatus,
} from "../definitions"
import { ConsentController } from "../consent/consent-controller"
import { TrackingController } from "../consent/tracking-controller"
import { AdEventEmitter, AdLogEmitter } from "./events"
import { PlacementRegistry } from "./placement-registry"
import { RequestConfigurationStore } from "./request-state"
import { fail, ok } from "./result"
import { RuntimeStateStore } from "./runtime-state"

const runtimeState = new RuntimeStateStore()
const requestState = new RequestConfigurationStore()
const placementRegistry = new PlacementRegistry()
const consentController = new ConsentController()
const trackingController = new TrackingController()
const eventEmitter = new AdEventEmitter()
const logEmitter = new AdLogEmitter()

function unsupported<T = undefined>(message: string): BridgeResult<T> {
  return fail("NOT_IMPLEMENTED", message, "unsupported")
}

function readyResult(ready = false) {
  return ok({ ready }, ready ? "ready" : "not_ready")
}

function emitConsentUpdated() {
  const event: AdEvent = {
    format: "banner",
    placementId: "consent.global",
    phase: "consent_updated",
  }
  eventEmitter.emit(event)
}

async function configure(options: ConfigureOptions): Promise<BridgeResult> {
  runtimeState.configure(options)
  placementRegistry.configure(options.placements)
  return ok(undefined, options.enabled ? "ready" : "disabled")
}

async function configureRequest(options: RequestConfigurationOptions): Promise<BridgeResult> {
  requestState.configure(options)
  return ok(undefined, "ready")
}

async function getRuntimeInfo(): Promise<BridgeResult<RuntimeInfo>> {
  const runtime = runtimeState.getSnapshot()
  const consent = consentController.getSnapshot()
  return ok({
    platform: "web",
    enabled: runtime.enabled,
    testMode: runtime.testMode,
    releaseSystemUiOnAdInteraction: runtime.releaseSystemUiOnAdInteraction,
    loggingLevel: runtime.loggingLevel,
    emitAdEvents: runtime.emitAdEvents,
    applicationIdConfigured: Boolean(runtime.applicationId),
    applicationIdSource: runtime.applicationIdSource,
    placementsConfigured: placementRegistry.count(),
    requestConfigurationConfigured: requestState.hasConfiguration(),
    consentStatus: consent.status,
  }, runtime.enabled ? "ready" : "disabled")
}

async function requestConsentInfo(): Promise<BridgeResult<ConsentInfo>> {
  const info = consentController.requestConsentInfo()
  emitConsentUpdated()
  return ok(info, info.canRequestAds ? "ready" : "not_ready")
}

async function showConsentFormIfRequired(): Promise<BridgeResult<ConsentInfo>> {
  const info = consentController.showConsentFormIfRequired()
  emitConsentUpdated()
  return ok(info, info.canRequestAds ? "ready" : "not_ready")
}

async function showPrivacyOptions(): Promise<BridgeResult<ConsentInfo>> {
  return ok(consentController.showPrivacyOptions(), "ready")
}

async function getConsentStatus(): Promise<BridgeResult<ConsentInfo>> {
  const consent = consentController.getSnapshot()
  return ok(consent, consent.canRequestAds ? "ready" : "not_ready")
}

async function resetConsentForTesting(): Promise<BridgeResult> {
  consentController.resetForTesting()
  emitConsentUpdated()
  return ok()
}

async function getTrackingAuthorizationStatus(): Promise<BridgeResult<{ status: TrackingAuthorizationStatus }>> {
  return ok({ status: trackingController.getStatus() }, "ready")
}

async function requestTrackingAuthorization(): Promise<BridgeResult<{ status: TrackingAuthorizationStatus }>> {
  return ok({ status: trackingController.requestAuthorization() }, "ready")
}

async function loadBanner(_options: BannerOptions): Promise<BridgeResult> {
  return unsupported("Banner loading is reserved for the Android implementation batch.")
}

async function showBanner(_options: BannerOptions): Promise<BridgeResult> {
  return unsupported("Banner display is reserved for the Android implementation batch.")
}

async function hideBanner(_placementId: string): Promise<BridgeResult> {
  return unsupported("Banner hiding is reserved for the Android implementation batch.")
}

async function destroyBanner(_placementId: string): Promise<BridgeResult> {
  return unsupported("Banner destroy is reserved for the Android implementation batch.")
}

async function preloadInterstitial(_options: FullscreenOptions): Promise<BridgeResult> {
  return unsupported("Interstitial preload is reserved for the Android implementation batch.")
}

async function isInterstitialReady(_placementId: string): Promise<BridgeResult<{ ready: boolean }>> {
  return readyResult(false)
}

async function showInterstitial(_placementId: string): Promise<BridgeResult> {
  return unsupported("Interstitial show is reserved for the Android implementation batch.")
}

async function preloadRewarded(_options: FullscreenOptions): Promise<BridgeResult> {
  return unsupported("Rewarded preload is reserved for the Android implementation batch.")
}

async function isRewardedReady(_placementId: string): Promise<BridgeResult<{ ready: boolean }>> {
  return readyResult(false)
}

async function showRewarded(_placementId: string): Promise<BridgeResult> {
  return unsupported("Rewarded show is reserved for the Android implementation batch.")
}

async function preloadRewardedInterstitial(_options: RewardedInterstitialOptions): Promise<BridgeResult> {
  return unsupported("Rewarded interstitial preload is reserved for the Android implementation batch.")
}

async function isRewardedInterstitialReady(_placementId: string): Promise<BridgeResult<{ ready: boolean }>> {
  return readyResult(false)
}

async function showRewardedInterstitial(_placementId: string): Promise<BridgeResult> {
  return unsupported("Rewarded interstitial show is reserved for the Android implementation batch.")
}

async function preloadAppOpen(_options: FullscreenOptions): Promise<BridgeResult> {
  return unsupported("App open preload is reserved for the Android implementation batch.")
}

async function isAppOpenReady(_placementId: string): Promise<BridgeResult<{ ready: boolean }>> {
  return readyResult(false)
}

async function showAppOpen(_placementId: string): Promise<BridgeResult> {
  return unsupported("App open show is reserved for the Android implementation batch.")
}

async function preloadNative(_options: NativeOptions): Promise<BridgeResult> {
  return unsupported("Native preload is reserved for the native host engine batch.")
}

async function isNativeReady(_slotId: string): Promise<BridgeResult<{ ready: boolean }>> {
  return readyResult(false)
}

async function attachNative(_options: NativeOptions): Promise<BridgeResult> {
  return unsupported("Native attach is reserved for the native host engine batch.")
}

async function detachNative(_slotId: string): Promise<BridgeResult> {
  return unsupported("Native detach is reserved for the native host engine batch.")
}

async function destroyNative(_slotId: string): Promise<BridgeResult> {
  return unsupported("Native destroy is reserved for the native host engine batch.")
}

async function refreshNative(_options: NativeOptions): Promise<BridgeResult> {
  return unsupported("Native refresh is reserved for the native host engine batch.")
}

async function preloadInlineBanner(_options: InlineBannerOptions): Promise<BridgeResult> {
  return unsupported("Inline banner preload is reserved for the Android inline banner batch.")
}

async function isInlineBannerReady(_slotId: string): Promise<BridgeResult<{ ready: boolean }>> {
  return readyResult(false)
}

async function attachInlineBanner(_options: InlineBannerOptions): Promise<BridgeResult> {
  return unsupported("Inline banner attach is reserved for the Android inline banner batch.")
}

async function detachInlineBanner(_slotId: string): Promise<BridgeResult> {
  return unsupported("Inline banner detach is reserved for the Android inline banner batch.")
}

async function destroyInlineBanner(_slotId: string): Promise<BridgeResult> {
  return unsupported("Inline banner destroy is reserved for the Android inline banner batch.")
}

async function refreshInlineBanner(_options: InlineBannerOptions): Promise<BridgeResult> {
  return unsupported("Inline banner refresh is reserved for the Android inline banner batch.")
}

async function clearAll(): Promise<BridgeResult> {
  placementRegistry.reset()
  requestState.reset()
  consentController.resetForTesting()
  trackingController.reset()
  eventEmitter.clear()
  runtimeState.reset()
  return ok()
}

function addListener(eventName: "adEvent", listenerFunc: (event: AdEvent) => void): Promise<PluginListenerHandle>
function addListener(eventName: "adLog", listenerFunc: (event: AdLogEvent) => void): Promise<PluginListenerHandle>
async function addListener(
  eventName: "adEvent" | "adLog",
  listenerFunc: ((event: AdEvent) => void) | ((event: AdLogEvent) => void),
): Promise<PluginListenerHandle> {
  if (eventName === "adEvent") {
    const typedListener = listenerFunc as (event: AdEvent) => void
    eventEmitter.add(typedListener)
    return {
      remove: async () => {
        eventEmitter.remove(typedListener)
      },
    }
  }

  if (eventName !== "adLog") {
    throw new Error(`Unsupported event name: ${eventName}`)
  }

  const typedListener = listenerFunc as (event: AdLogEvent) => void
  logEmitter.add(typedListener)
  return {
    remove: async () => {
      logEmitter.remove(typedListener)
    },
  }
}

async function removeAllListeners(): Promise<void> {
  eventEmitter.clear()
  logEmitter.clear()
}

export const pluginFacade: DonugrAdmobPlugin = {
  configure,
  configureRequest,
  getRuntimeInfo,
  requestConsentInfo,
  showConsentFormIfRequired,
  showPrivacyOptions,
  getConsentStatus,
  resetConsentForTesting,
  getTrackingAuthorizationStatus,
  requestTrackingAuthorization,
  loadBanner,
  showBanner,
  hideBanner,
  destroyBanner,
  preloadInterstitial,
  isInterstitialReady,
  showInterstitial,
  preloadRewarded,
  isRewardedReady,
  showRewarded,
  preloadRewardedInterstitial,
  isRewardedInterstitialReady,
  showRewardedInterstitial,
  preloadAppOpen,
  isAppOpenReady,
  showAppOpen,
  preloadNative,
  isNativeReady,
  attachNative,
  detachNative,
  destroyNative,
  refreshNative,
  preloadInlineBanner,
  isInlineBannerReady,
  attachInlineBanner,
  detachInlineBanner,
  destroyInlineBanner,
  refreshInlineBanner,
  clearAll,
  addListener,
  removeAllListeners,
}

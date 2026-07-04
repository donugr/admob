import { registerPlugin } from "@capacitor/core"
import type { PluginListenerHandle } from "@capacitor/core"
import { pluginFacade } from "./core/plugin-facade"
import type {
  AdEvent,
  BannerOptions,
  BridgeResult,
  ConfigureOptions,
  ConsentInfo,
  FullscreenOptions,
  InlineBannerOptions,
  NativeOptions,
  RequestConfigurationOptions,
  RuntimeInfo,
  TrackingAuthorizationStatus,
} from "./definitions"

type NativeBridge = {
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
  hideBanner(options: { placementId: string }): Promise<BridgeResult>
  destroyBanner(options: { placementId: string }): Promise<BridgeResult>
  preloadInterstitial(options: FullscreenOptions): Promise<BridgeResult>
  isInterstitialReady(options: { placementId: string }): Promise<BridgeResult<{ ready: boolean }>>
  showInterstitial(options: { placementId: string }): Promise<BridgeResult>
  preloadRewarded(options: FullscreenOptions): Promise<BridgeResult>
  isRewardedReady(options: { placementId: string }): Promise<BridgeResult<{ ready: boolean }>>
  showRewarded(options: { placementId: string }): Promise<BridgeResult>
  preloadAppOpen(options: FullscreenOptions): Promise<BridgeResult>
  isAppOpenReady(options: { placementId: string }): Promise<BridgeResult<{ ready: boolean }>>
  showAppOpen(options: { placementId: string }): Promise<BridgeResult>
  preloadNative(options: NativeOptions): Promise<BridgeResult>
  isNativeReady(options: { slotId: string }): Promise<BridgeResult<{ ready: boolean }>>
  attachNative(options: NativeOptions): Promise<BridgeResult>
  detachNative(options: { slotId: string }): Promise<BridgeResult>
  destroyNative(options: { slotId: string }): Promise<BridgeResult>
  refreshNative(options: NativeOptions): Promise<BridgeResult>
  preloadInlineBanner(options: InlineBannerOptions): Promise<BridgeResult>
  isInlineBannerReady(options: { slotId: string }): Promise<BridgeResult<{ ready: boolean }>>
  attachInlineBanner(options: InlineBannerOptions): Promise<BridgeResult>
  detachInlineBanner(options: { slotId: string }): Promise<BridgeResult>
  destroyInlineBanner(options: { slotId: string }): Promise<BridgeResult>
  refreshInlineBanner(options: InlineBannerOptions): Promise<BridgeResult>
  clearAll(): Promise<BridgeResult>
  addListener(eventName: "adEvent", listenerFunc: (event: AdEvent) => void): Promise<PluginListenerHandle>
  removeAllListeners(): Promise<void>
}

const webBridge: NativeBridge = {
  configure: (options) => pluginFacade.configure(options),
  configureRequest: (options) => pluginFacade.configureRequest(options),
  getRuntimeInfo: () => pluginFacade.getRuntimeInfo(),
  requestConsentInfo: () => pluginFacade.requestConsentInfo(),
  showConsentFormIfRequired: () => pluginFacade.showConsentFormIfRequired(),
  showPrivacyOptions: () => pluginFacade.showPrivacyOptions(),
  getConsentStatus: () => pluginFacade.getConsentStatus(),
  resetConsentForTesting: () => pluginFacade.resetConsentForTesting(),
  getTrackingAuthorizationStatus: () => pluginFacade.getTrackingAuthorizationStatus(),
  requestTrackingAuthorization: () => pluginFacade.requestTrackingAuthorization(),
  loadBanner: (options) => pluginFacade.loadBanner(options),
  showBanner: (options) => pluginFacade.showBanner(options),
  hideBanner: ({ placementId }) => pluginFacade.hideBanner(placementId),
  destroyBanner: ({ placementId }) => pluginFacade.destroyBanner(placementId),
  preloadInterstitial: (options) => pluginFacade.preloadInterstitial(options),
  isInterstitialReady: ({ placementId }) => pluginFacade.isInterstitialReady(placementId),
  showInterstitial: ({ placementId }) => pluginFacade.showInterstitial(placementId),
  preloadRewarded: (options) => pluginFacade.preloadRewarded(options),
  isRewardedReady: ({ placementId }) => pluginFacade.isRewardedReady(placementId),
  showRewarded: ({ placementId }) => pluginFacade.showRewarded(placementId),
  preloadAppOpen: (options) => pluginFacade.preloadAppOpen(options),
  isAppOpenReady: ({ placementId }) => pluginFacade.isAppOpenReady(placementId),
  showAppOpen: ({ placementId }) => pluginFacade.showAppOpen(placementId),
  preloadNative: (options) => pluginFacade.preloadNative(options),
  isNativeReady: ({ slotId }) => pluginFacade.isNativeReady(slotId),
  attachNative: (options) => pluginFacade.attachNative(options),
  detachNative: ({ slotId }) => pluginFacade.detachNative(slotId),
  destroyNative: ({ slotId }) => pluginFacade.destroyNative(slotId),
  refreshNative: (options) => pluginFacade.refreshNative(options),
  preloadInlineBanner: (options) => pluginFacade.preloadInlineBanner(options),
  isInlineBannerReady: ({ slotId }) => pluginFacade.isInlineBannerReady(slotId),
  attachInlineBanner: (options) => pluginFacade.attachInlineBanner(options),
  detachInlineBanner: ({ slotId }) => pluginFacade.detachInlineBanner(slotId),
  destroyInlineBanner: ({ slotId }) => pluginFacade.destroyInlineBanner(slotId),
  refreshInlineBanner: (options) => pluginFacade.refreshInlineBanner(options),
  clearAll: () => pluginFacade.clearAll(),
  addListener: (eventName, listenerFunc) => pluginFacade.addListener(eventName, listenerFunc),
  removeAllListeners: () => pluginFacade.removeAllListeners(),
}

export const nativeBridge = registerPlugin<NativeBridge>("DonugrAdmob", {
  web: async () => webBridge,
})

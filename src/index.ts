import type { AdEvent, AdLogEvent, DonugrAdmobPlugin } from "./definitions"
import type { PluginListenerHandle } from "@capacitor/core"
import { nativeBridge } from "./native-bridge"

function addListener(eventName: "adEvent", listenerFunc: (event: AdEvent) => void): Promise<PluginListenerHandle>
function addListener(eventName: "adLog", listenerFunc: (event: AdLogEvent) => void): Promise<PluginListenerHandle>
function addListener(
  eventName: "adEvent" | "adLog",
  listenerFunc: ((event: AdEvent) => void) | ((event: AdLogEvent) => void),
): Promise<PluginListenerHandle> {
  return nativeBridge.addListener(eventName as "adEvent", listenerFunc as (event: AdEvent) => void)
}

export const DonugrAdmob: DonugrAdmobPlugin = {
  configure: (options) => nativeBridge.configure(options),
  configureRequest: (options) => nativeBridge.configureRequest(options),
  getRuntimeInfo: () => nativeBridge.getRuntimeInfo(),
  requestConsentInfo: () => nativeBridge.requestConsentInfo(),
  showConsentFormIfRequired: () => nativeBridge.showConsentFormIfRequired(),
  showPrivacyOptions: () => nativeBridge.showPrivacyOptions(),
  getConsentStatus: () => nativeBridge.getConsentStatus(),
  resetConsentForTesting: () => nativeBridge.resetConsentForTesting(),
  getTrackingAuthorizationStatus: () => nativeBridge.getTrackingAuthorizationStatus(),
  requestTrackingAuthorization: () => nativeBridge.requestTrackingAuthorization(),
  loadBanner: (options) => nativeBridge.loadBanner(options),
  showBanner: (options) => nativeBridge.showBanner(options),
  hideBanner: (placementId) => nativeBridge.hideBanner({ placementId }),
  destroyBanner: (placementId) => nativeBridge.destroyBanner({ placementId }),
  preloadInterstitial: (options) => nativeBridge.preloadInterstitial(options),
  isInterstitialReady: (placementId) => nativeBridge.isInterstitialReady({ placementId }),
  showInterstitial: (placementId) => nativeBridge.showInterstitial({ placementId }),
  preloadRewarded: (options) => nativeBridge.preloadRewarded(options),
  isRewardedReady: (placementId) => nativeBridge.isRewardedReady({ placementId }),
  showRewarded: (placementId) => nativeBridge.showRewarded({ placementId }),
  preloadRewardedInterstitial: (options) => nativeBridge.preloadRewardedInterstitial(options),
  isRewardedInterstitialReady: (placementId) => nativeBridge.isRewardedInterstitialReady({ placementId }),
  showRewardedInterstitial: (placementId) => nativeBridge.showRewardedInterstitial({ placementId }),
  preloadAppOpen: (options) => nativeBridge.preloadAppOpen(options),
  isAppOpenReady: (placementId) => nativeBridge.isAppOpenReady({ placementId }),
  showAppOpen: (placementId) => nativeBridge.showAppOpen({ placementId }),
  preloadNative: (options) => nativeBridge.preloadNative(options),
  isNativeReady: (slotId) => nativeBridge.isNativeReady({ slotId }),
  attachNative: (options) => nativeBridge.attachNative(options),
  detachNative: (slotId) => nativeBridge.detachNative({ slotId }),
  destroyNative: (slotId) => nativeBridge.destroyNative({ slotId }),
  refreshNative: (options) => nativeBridge.refreshNative(options),
  preloadInlineBanner: (options) => nativeBridge.preloadInlineBanner(options),
  isInlineBannerReady: (slotId) => nativeBridge.isInlineBannerReady({ slotId }),
  attachInlineBanner: (options) => nativeBridge.attachInlineBanner(options),
  detachInlineBanner: (slotId) => nativeBridge.detachInlineBanner({ slotId }),
  destroyInlineBanner: (slotId) => nativeBridge.destroyInlineBanner({ slotId }),
  refreshInlineBanner: (options) => nativeBridge.refreshInlineBanner(options),
  clearAll: () => nativeBridge.clearAll(),
  addListener,
  removeAllListeners: () => nativeBridge.removeAllListeners(),
}

export * from "./definitions"
export { buildNativeHostRect } from "./native/host-rect"

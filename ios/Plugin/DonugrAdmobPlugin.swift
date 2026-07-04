import Foundation
import Capacitor

@objc(DonugrAdmobPlugin)
public class DonugrAdmobPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "DonugrAdmobPlugin"
    public let jsName = "DonugrAdmob"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "configure", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "configureRequest", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getRuntimeInfo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestConsentInfo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showConsentFormIfRequired", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showPrivacyOptions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getConsentStatus", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "resetConsentForTesting", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getTrackingAuthorizationStatus", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestTrackingAuthorization", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "loadBanner", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showBanner", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "hideBanner", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "destroyBanner", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "preloadInterstitial", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isInterstitialReady", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showInterstitial", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "preloadRewarded", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isRewardedReady", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showRewarded", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "preloadRewardedInterstitial", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isRewardedInterstitialReady", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showRewardedInterstitial", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "preloadAppOpen", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isAppOpenReady", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showAppOpen", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "preloadNative", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isNativeReady", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "attachNative", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "detachNative", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "destroyNative", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "refreshNative", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "preloadInlineBanner", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isInlineBannerReady", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "attachInlineBanner", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "detachInlineBanner", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "destroyInlineBanner", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "refreshInlineBanner", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "clearAll", returnType: CAPPluginReturnPromise)
    ]

    @objc public func configure(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "configure")
    }

    @objc public func configureRequest(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "configureRequest")
    }

    @objc public func getRuntimeInfo(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "getRuntimeInfo")
    }

    @objc public func requestConsentInfo(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "requestConsentInfo")
    }

    @objc public func showConsentFormIfRequired(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "showConsentFormIfRequired")
    }

    @objc public func showPrivacyOptions(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "showPrivacyOptions")
    }

    @objc public func getConsentStatus(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "getConsentStatus")
    }

    @objc public func resetConsentForTesting(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "resetConsentForTesting")
    }

    @objc public func getTrackingAuthorizationStatus(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "getTrackingAuthorizationStatus")
    }

    @objc public func requestTrackingAuthorization(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "requestTrackingAuthorization")
    }

    @objc public func loadBanner(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "loadBanner")
    }

    @objc public func showBanner(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "showBanner")
    }

    @objc public func hideBanner(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "hideBanner")
    }

    @objc public func destroyBanner(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "destroyBanner")
    }

    @objc public func preloadInterstitial(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "preloadInterstitial")
    }

    @objc public func isInterstitialReady(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "isInterstitialReady")
    }

    @objc public func showInterstitial(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "showInterstitial")
    }

    @objc public func preloadRewarded(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "preloadRewarded")
    }

    @objc public func isRewardedReady(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "isRewardedReady")
    }

    @objc public func showRewarded(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "showRewarded")
    }

    @objc public func preloadRewardedInterstitial(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "preloadRewardedInterstitial")
    }

    @objc public func isRewardedInterstitialReady(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "isRewardedInterstitialReady")
    }

    @objc public func showRewardedInterstitial(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "showRewardedInterstitial")
    }

    @objc public func preloadAppOpen(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "preloadAppOpen")
    }

    @objc public func isAppOpenReady(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "isAppOpenReady")
    }

    @objc public func showAppOpen(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "showAppOpen")
    }

    @objc public func preloadNative(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "preloadNative")
    }

    @objc public func isNativeReady(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "isNativeReady")
    }

    @objc public func attachNative(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "attachNative")
    }

    @objc public func detachNative(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "detachNative")
    }

    @objc public func destroyNative(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "destroyNative")
    }

    @objc public func refreshNative(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "refreshNative")
    }

    @objc public func preloadInlineBanner(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "preloadInlineBanner")
    }

    @objc public func isInlineBannerReady(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "isInlineBannerReady")
    }

    @objc public func attachInlineBanner(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "attachInlineBanner")
    }

    @objc public func detachInlineBanner(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "detachInlineBanner")
    }

    @objc public func destroyInlineBanner(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "destroyInlineBanner")
    }

    @objc public func refreshInlineBanner(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "refreshInlineBanner")
    }

    @objc public func clearAll(_ call: CAPPluginCall) {
        resolveUnsupported(call, method: "clearAll")
    }

    private func resolveUnsupported(_ call: CAPPluginCall, method: String) {
        call.resolve([
            "ok": false,
            "code": "NOT_IMPLEMENTED",
            "message": "\(method) is not implemented on iOS yet. This package is currently Android-first.",
            "status": "unsupported"
        ])
    }
}

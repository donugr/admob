import { DonugrAdmob, buildNativeHostRect, type AdEvent, type ConfigureOptions } from "@donugr/admob"

type QaPlacements = {
  banner: string
  interstitial: string
  rewarded: string
  appOpen: string
  native: string
  inlineBanner: string
}

type QaIds = {
  log: string
  runtime: string
  nativeHost: string
  inlineBannerHost: string
}

type QaHarnessOptions = {
  applicationId?: string
  testMode?: boolean
  placements?: Partial<QaPlacements>
  ids?: Partial<QaIds>
}

const defaultPlacements: QaPlacements = {
  banner: "banner_home",
  interstitial: "interstitial_break",
  rewarded: "rewarded_bonus",
  appOpen: "app_open_launch",
  native: "native_feed_card",
  inlineBanner: "inline_feed_banner",
}

const defaultIds: QaIds = {
  log: "qa-log",
  runtime: "qa-runtime",
  nativeHost: "qa-native-host",
  inlineBannerHost: "qa-inline-banner-host",
}

const nativeSlotId = "qa.native.slot.1"
const nativeHostId = "qa.native.host.1"
const inlineBannerSlotId = "qa.inline.slot.1"
const inlineBannerHostId = "qa.inline.host.1"

function byId<T extends HTMLElement>(id: string): T {
  const element = document.getElementById(id)
  if (!element) {
    throw new Error(`Missing required element #${id}`)
  }
  return element as T
}

function stringify(value: unknown) {
  return JSON.stringify(value, null, 2)
}

export function mountDonugrAdmobQaPage(options: QaHarnessOptions = {}) {
  const placements = { ...defaultPlacements, ...options.placements }
  const ids = { ...defaultIds, ...options.ids }
  const logElement = byId<HTMLPreElement>(ids.log)
  const runtimeElement = byId<HTMLPreElement>(ids.runtime)
  const nativeHost = byId<HTMLElement>(ids.nativeHost)
  const inlineBannerHost = byId<HTMLElement>(ids.inlineBannerHost)

  let listenerReady = false

  const log = (label: string, value?: unknown) => {
    const line = value === undefined ? label : `${label}\n${stringify(value)}`
    logElement.textContent = `${new Date().toISOString()} ${line}\n\n${logElement.textContent}`
  }

  const refreshRuntime = async () => {
    const runtime = await DonugrAdmob.getRuntimeInfo()
    runtimeElement.textContent = stringify(runtime)
    return runtime
  }

  const ensureListener = async () => {
    if (listenerReady) {
      return
    }

    await DonugrAdmob.addListener("adEvent", (event: AdEvent) => {
      log(`event:${event.format}:${event.phase}`, event)
    })
    listenerReady = true
  }

  const configure = async () => {
    await ensureListener()

    const payload: ConfigureOptions = {
      enabled: true,
      testMode: options.testMode ?? true,
      applicationId: options.applicationId,
      placements: {
        [placements.banner]: "ca-app-pub-3940256099942544/6300978111",
        [placements.interstitial]: "ca-app-pub-3940256099942544/1033173712",
        [placements.rewarded]: "ca-app-pub-3940256099942544/5224354917",
        [placements.appOpen]: "ca-app-pub-3940256099942544/9257395921",
        [placements.native]: "ca-app-pub-3940256099942544/2247696110",
        [placements.inlineBanner]: "ca-app-pub-3940256099942544/6300978111",
      },
    }

    const result = await DonugrAdmob.configure(payload)
    log("configure", result)

    const requestConfig = await DonugrAdmob.configureRequest({
      maxAdContentRating: "T",
      sameAppKey: true,
      tagForChildDirectedTreatment: false,
      tagForUnderAgeOfConsent: false,
      appMuted: false,
      appVolume: 1,
    })
    log("configureRequest", requestConfig)

    await refreshRuntime()
  }

  const requestConsent = async () => {
    log("requestConsentInfo", await DonugrAdmob.requestConsentInfo())
    await refreshRuntime()
  }

  const showConsentForm = async () => {
    log("showConsentFormIfRequired", await DonugrAdmob.showConsentFormIfRequired())
    await refreshRuntime()
  }

  const showPrivacyOptions = async () => {
    log("showPrivacyOptions", await DonugrAdmob.showPrivacyOptions())
    await refreshRuntime()
  }

  const showBannerTop = async () => log("showBannerTop", await DonugrAdmob.showBanner({ placementId: placements.banner, position: "top" }))
  const showBannerBottom = async () => log("showBannerBottom", await DonugrAdmob.showBanner({ placementId: placements.banner, position: "bottom" }))
  const hideBanner = async () => log("hideBanner", await DonugrAdmob.hideBanner(placements.banner))
  const destroyBanner = async () => log("destroyBanner", await DonugrAdmob.destroyBanner(placements.banner))

  const preloadInterstitial = async () => log("preloadInterstitial", await DonugrAdmob.preloadInterstitial({ placementId: placements.interstitial }))
  const interstitialReady = async () => log("isInterstitialReady", await DonugrAdmob.isInterstitialReady(placements.interstitial))
  const showInterstitial = async () => log("showInterstitial", await DonugrAdmob.showInterstitial(placements.interstitial))

  const preloadRewarded = async () => log("preloadRewarded", await DonugrAdmob.preloadRewarded({ placementId: placements.rewarded }))
  const rewardedReady = async () => log("isRewardedReady", await DonugrAdmob.isRewardedReady(placements.rewarded))
  const showRewarded = async () => log("showRewarded", await DonugrAdmob.showRewarded(placements.rewarded))

  const preloadAppOpen = async () => log("preloadAppOpen", await DonugrAdmob.preloadAppOpen({ placementId: placements.appOpen }))
  const appOpenReady = async () => log("isAppOpenReady", await DonugrAdmob.isAppOpenReady(placements.appOpen))
  const showAppOpen = async () => log("showAppOpen", await DonugrAdmob.showAppOpen(placements.appOpen))

  const preloadNative = async () =>
    log(
      "preloadNative",
      await DonugrAdmob.preloadNative({
        placementId: placements.native,
        slotId: nativeSlotId,
        hostId: nativeHostId,
        hostRect: buildNativeHostRect(nativeHost),
        ttlMs: 60000,
      }),
    )

  const nativeReady = async () => log("isNativeReady", await DonugrAdmob.isNativeReady(nativeSlotId))

  const attachNative = async () =>
    log(
      "attachNative",
      await DonugrAdmob.attachNative({
        placementId: placements.native,
        slotId: nativeSlotId,
        hostId: nativeHostId,
        hostRect: buildNativeHostRect(nativeHost),
      }),
    )

  const detachNative = async () => log("detachNative", await DonugrAdmob.detachNative(nativeSlotId))
  const destroyNative = async () => log("destroyNative", await DonugrAdmob.destroyNative(nativeSlotId))

  const refreshNative = async () =>
    log(
      "refreshNative",
      await DonugrAdmob.refreshNative({
        placementId: placements.native,
        slotId: nativeSlotId,
        hostId: nativeHostId,
        hostRect: buildNativeHostRect(nativeHost),
        ttlMs: 60000,
      }),
    )

  const preloadInlineBanner = async () =>
    log(
      "preloadInlineBanner",
      await DonugrAdmob.preloadInlineBanner({
        placementId: placements.inlineBanner,
        slotId: inlineBannerSlotId,
        hostId: inlineBannerHostId,
        hostRect: buildNativeHostRect(inlineBannerHost),
      }),
    )

  const inlineBannerReady = async () => log("isInlineBannerReady", await DonugrAdmob.isInlineBannerReady(inlineBannerSlotId))

  const attachInlineBanner = async () =>
    log(
      "attachInlineBanner",
      await DonugrAdmob.attachInlineBanner({
        placementId: placements.inlineBanner,
        slotId: inlineBannerSlotId,
        hostId: inlineBannerHostId,
        hostRect: buildNativeHostRect(inlineBannerHost),
      }),
    )

  const detachInlineBanner = async () => log("detachInlineBanner", await DonugrAdmob.detachInlineBanner(inlineBannerSlotId))
  const destroyInlineBanner = async () => log("destroyInlineBanner", await DonugrAdmob.destroyInlineBanner(inlineBannerSlotId))

  const refreshInlineBanner = async () =>
    log(
      "refreshInlineBanner",
      await DonugrAdmob.refreshInlineBanner({
        placementId: placements.inlineBanner,
        slotId: inlineBannerSlotId,
        hostId: inlineBannerHostId,
        hostRect: buildNativeHostRect(inlineBannerHost),
      }),
    )

  const clearAll = async () => {
    log("clearAll", await DonugrAdmob.clearAll())
    await refreshRuntime()
  }

  const actions: Record<string, () => Promise<unknown>> = {
    configure,
    refreshRuntime,
    requestConsent,
    showConsentForm,
    showPrivacyOptions,
    showBannerTop,
    showBannerBottom,
    hideBanner,
    destroyBanner,
    preloadInterstitial,
    interstitialReady,
    showInterstitial,
    preloadRewarded,
    rewardedReady,
    showRewarded,
    preloadAppOpen,
    appOpenReady,
    showAppOpen,
    preloadNative,
    nativeReady,
    attachNative,
    detachNative,
    destroyNative,
    refreshNative,
    preloadInlineBanner,
    inlineBannerReady,
    attachInlineBanner,
    detachInlineBanner,
    destroyInlineBanner,
    refreshInlineBanner,
    clearAll,
  }

  document.querySelectorAll<HTMLElement>("[data-qa-action]").forEach((button) => {
    const actionName = button.dataset.qaAction ?? ""
    const action = actions[actionName]
    if (!action) {
      return
    }

    button.addEventListener("click", async () => {
      try {
        await action()
      } catch (error) {
        log(`error:${actionName}`, {
          message: error instanceof Error ? error.message : String(error),
        })
      }
    })
  })

  log("qa-harness-ready", {
    placements,
    ids,
  })
}

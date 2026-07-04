# `@donugr/admob`

Capacitor AdMob plugin with an Android-first public surface for consent, standard ad formats, native host placements, and inline banner host placements.

This package is designed for public npm distribution, conservative policy defaults, and a clean runtime contract that does not depend on app-specific UI assumptions.

## Features

- Android standard ads: banner, interstitial, rewarded, and app open
- Android Google UMP consent flow
- Android host-based native ad engine
- Android host-based inline adaptive banner engine
- Shared TypeScript event and runtime diagnostics surface
- iOS scaffold present for future parity work

## Platform Status

| Platform | Status |
| --- | --- |
| Android | Implemented and build-verified |
| iOS | Scaffold only, not feature-parity complete yet |
| Web | No runtime implementation |

If you need production behavior today, treat this package as Android-first.

## Install

```bash
npm install @donugr/admob
npx cap sync
```

Peer dependency:

- `@capacitor/core@^8`

## Android Setup

Add your AdMob application ID to your app `AndroidManifest.xml`:

```xml
<meta-data
  android:name="com.google.android.gms.ads.APPLICATION_ID"
  android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
```

You can also pass `applicationId` through `configure()`, but manifest-based setup is the safer default for public plugin consumers.

## Quick Start

```ts
import { DonugrAdmob } from "@donugr/admob"

await DonugrAdmob.configure({
  enabled: true,
  testMode: true,
  placements: {
    banner_home: "ca-app-pub-xxxx/banner",
    interstitial_break: "ca-app-pub-xxxx/interstitial",
    rewarded_bonus: "ca-app-pub-xxxx/rewarded",
    app_open_launch: "ca-app-pub-xxxx/appopen",
    native_feed_card: "ca-app-pub-xxxx/native",
    inline_feed_banner: "ca-app-pub-xxxx/inline",
  },
})
```

Keep `testMode: true` and use Google test ads or test devices during development.

## Request Configuration

```ts
await DonugrAdmob.configureRequest({
  maxAdContentRating: "T",
  tagForChildDirectedTreatment: false,
  tagForUnderAgeOfConsent: false,
  testDeviceIds: ["YOUR_TEST_DEVICE_ID"],
  appMuted: false,
  appVolume: 1,
})
```

## Consent

Android consent is powered by Google User Messaging Platform.

```ts
const consentInfo = await DonugrAdmob.requestConsentInfo()

if (consentInfo.data?.canRequestAds === false) {
  await DonugrAdmob.showConsentFormIfRequired()
}
```

Available consent methods:

- `requestConsentInfo()`
- `showConsentFormIfRequired()`
- `showPrivacyOptions()`
- `getConsentStatus()`
- `resetConsentForTesting()`

## Standard Ads

### Banner

```ts
await DonugrAdmob.showBanner({
  placementId: "banner_home",
})
```

Current Android banner behavior is intentionally conservative and oriented to anchored adaptive usage.

### Interstitial

```ts
await DonugrAdmob.preloadInterstitial({
  placementId: "interstitial_break",
})

const ready = await DonugrAdmob.isInterstitialReady("interstitial_break")

if (ready.data?.ready) {
  await DonugrAdmob.showInterstitial("interstitial_break")
}
```

Use interstitials only at natural transitions.

### Rewarded

```ts
await DonugrAdmob.preloadRewarded({
  placementId: "rewarded_bonus",
})

const ready = await DonugrAdmob.isRewardedReady("rewarded_bonus")

if (ready.data?.ready) {
  await DonugrAdmob.showRewarded("rewarded_bonus")
}
```

Only grant rewards from the reward-earned path in your app logic.

### App Open

```ts
await DonugrAdmob.preloadAppOpen({
  placementId: "app_open_launch",
})

const ready = await DonugrAdmob.isAppOpenReady("app_open_launch")

if (ready.data?.ready) {
  await DonugrAdmob.showAppOpen("app_open_launch")
}
```

Use app open ads for launch, foreground return, or explicit loading moments, not arbitrary content interruption.

## Host-Based Formats

### Native Host Engine

Native placements use three identifiers:

- `placementId`: stable inventory identity
- `slotId`: runtime ad-slot lifecycle identity
- `hostId`: logical UI host identity

```ts
await DonugrAdmob.preloadNative({
  placementId: "native_feed_card",
  slotId: "feed.slot.1",
  hostId: "feed.card.1",
  ttlMs: 60000,
})

const ready = await DonugrAdmob.isNativeReady("feed.slot.1")

if (ready.data?.ready) {
  await DonugrAdmob.attachNative({
    placementId: "native_feed_card",
    slotId: "feed.slot.1",
    hostId: "feed.card.1",
    hostRect: {
      x: 16,
      y: 320,
      width: 360,
      height: 120,
      anchor: "top",
    },
  })
}
```

Available native methods:

- `preloadNative()`
- `isNativeReady()`
- `attachNative()`
- `detachNative()`
- `destroyNative()`
- `refreshNative()`

Built-in protections:

- duplicate attach guard
- same-host same-rect layout skip
- TTL cleanup for stale slots
- cleanup on plugin destroy
- identifier validation for `placementId`, `slotId`, and `hostId`

### Inline Banner Host Engine

Inline banner is separate from anchored banner. It is intended for scrolling content placements, not global sticky takeovers.

```ts
await DonugrAdmob.preloadInlineBanner({
  placementId: "inline_feed_banner",
  slotId: "feed.banner.1",
  hostId: "feed.banner.host.1",
  hostRect: {
    x: 16,
    y: 640,
    width: 360,
    height: 120,
    anchor: "top",
  },
})

const ready = await DonugrAdmob.isInlineBannerReady("feed.banner.1")

if (ready.data?.ready) {
  await DonugrAdmob.attachInlineBanner({
    placementId: "inline_feed_banner",
    slotId: "feed.banner.1",
    hostId: "feed.banner.host.1",
    hostRect: {
      x: 16,
      y: 640,
      width: 360,
      height: 120,
      anchor: "top",
    },
  })
}
```

Available inline banner methods:

- `preloadInlineBanner()`
- `isInlineBannerReady()`
- `attachInlineBanner()`
- `detachInlineBanner()`
- `destroyInlineBanner()`
- `refreshInlineBanner()`

Built-in protections:

- host-based `slotId` lifecycle
- duplicate attach guard
- same-host same-rect layout skip
- isolated overlay namespace from native containers
- identifier validation for `placementId`, `slotId`, and `hostId`

## Events

Listen to the `adEvent` channel for shared lifecycle telemetry:

```ts
const handle = await DonugrAdmob.addListener("adEvent", event => {
  console.log(event.format, event.placementId, event.slotId, event.phase)
})
```

Common event phases:

- `loaded`
- `failed`
- `shown`
- `dismissed`
- `clicked`
- `impression`
- `reward_earned`
- `attached`
- `detached`
- `destroyed`
- `consent_updated`

Additional preload and layout phases are emitted for native and inline banner diagnostics.

## Runtime Diagnostics

```ts
const runtime = await DonugrAdmob.getRuntimeInfo()
```

Current Android runtime info includes:

- `enabled`
- `testMode`
- `applicationIdConfigured`
- `applicationIdSource`
- `placementsConfigured`
- `requestConfigurationConfigured`
- `consentStatus`
- `activeSlots`
- `loadingSlots`
- `readySlots`
- `attachedSlots`
- `failedSlots`
- `expiredSlots`
- `inlineBannerActiveSlots`
- `inlineBannerLoadingSlots`
- `inlineBannerReadySlots`
- `inlineBannerAttachedSlots`
- `inlineBannerFailedSlots`

## Policy Notes

This plugin does not attempt to bypass, soften, or reinterpret Google AdMob policy.

- always use test ads during development
- do not inflate clicks or impressions
- do not place interstitials at disruptive moments
- do not grant rewarded outcomes before reward completion
- do not use app open ads as a generic fullscreen interruption tool
- do not use inline banner host mode as a disguised sticky or takeover ad

Plugin consumers remain responsible for compliant ad placement, disclosure, and app behavior.

## Public Package Direction

This package is being prepared for public npm usage, so the public API aims to stay:

- Android-first and explicit about current platform support
- conservative around policy-sensitive behavior
- stable in naming for `placementId`, `slotId`, `hostId`, and event phases
- debuggable through shared runtime and event contracts

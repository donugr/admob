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

## Supported Ad Formats

Current plugin support:

| Format | Android | Notes |
| --- | --- | --- |
| Banner | Supported | Anchored banner display for top or bottom placement |
| Interstitial | Supported | Standard fullscreen interstitial |
| Rewarded | Supported | Rewarded ad with reward-earned event |
| App Open | Supported | App open fullscreen format |
| Native | Supported | Host-based native placement engine |
| Inline Banner | Supported | Host-based inline adaptive banner placement |
| Rewarded Interstitial | Not supported yet | Not exposed in the current public API |
| Native Video preset | Not exposed separately | Native format may render video creatives, but there is no separate public format |

## API Index

### Core Configuration

| Method | Purpose |
| --- | --- |
| `configure(options)` | Enable or disable the plugin runtime and register placement mappings |
| `configureRequest(options)` | Apply global ad-request configuration |
| `getRuntimeInfo()` | Inspect current runtime and slot diagnostics |
| `clearAll()` | Clear in-memory ad state and active host-based state |

### Consent and Privacy

| Method | Purpose |
| --- | --- |
| `requestConsentInfo()` | Refresh consent information |
| `showConsentFormIfRequired()` | Show UMP consent form when required |
| `showPrivacyOptions()` | Show privacy options form |
| `getConsentStatus()` | Read current consent snapshot |
| `resetConsentForTesting()` | Reset consent state for development/testing |
| `getTrackingAuthorizationStatus()` | Read tracking authorization status |
| `requestTrackingAuthorization()` | Request tracking authorization |

### Banner

| Method | Purpose |
| --- | --- |
| `loadBanner(options)` | Convenience alias for banner display |
| `showBanner(options)` | Show anchored banner |
| `hideBanner(placementId)` | Hide banner container without destroying placement identity |
| `destroyBanner(placementId)` | Fully destroy banner instance and container |

### Interstitial

| Method | Purpose |
| --- | --- |
| `preloadInterstitial(options)` | Preload interstitial inventory |
| `isInterstitialReady(placementId)` | Check whether an interstitial is ready |
| `showInterstitial(placementId)` | Show a ready interstitial |

### Rewarded

| Method | Purpose |
| --- | --- |
| `preloadRewarded(options)` | Preload rewarded inventory |
| `isRewardedReady(placementId)` | Check whether a rewarded ad is ready |
| `showRewarded(placementId)` | Show a ready rewarded ad |

### App Open

| Method | Purpose |
| --- | --- |
| `preloadAppOpen(options)` | Preload app open inventory |
| `isAppOpenReady(placementId)` | Check whether an app open ad is ready |
| `showAppOpen(placementId)` | Show a ready app open ad |

### Native Host

| Method | Purpose |
| --- | --- |
| `preloadNative(options)` | Preload a native slot |
| `isNativeReady(slotId)` | Check whether a native slot is ready |
| `attachNative(options)` | Attach a native slot to a UI host |
| `detachNative(slotId)` | Detach native view from host while keeping slot lifecycle |
| `destroyNative(slotId)` | Destroy native slot state |
| `refreshNative(options)` | Force a fresh native preload cycle |

### Inline Banner Host

| Method | Purpose |
| --- | --- |
| `preloadInlineBanner(options)` | Preload an inline banner slot |
| `isInlineBannerReady(slotId)` | Check whether an inline banner slot is ready |
| `attachInlineBanner(options)` | Attach an inline banner slot to a UI host |
| `detachInlineBanner(slotId)` | Detach inline banner from host while keeping slot lifecycle |
| `destroyInlineBanner(slotId)` | Destroy inline banner slot state |
| `refreshInlineBanner(options)` | Force a fresh inline banner preload cycle |

### Events

| Method | Purpose |
| --- | --- |
| `addListener("adEvent", listener)` | Subscribe to cross-format lifecycle events |
| `removeAllListeners()` | Remove all registered listeners |

## Method Return Summary

All plugin methods return a `Promise<BridgeResult<...>>`.

### Result Pattern by Method Family

| Method family | Typical `status` values | `data` payload |
| --- | --- | --- |
| `configure()`, `configureRequest()`, `clearAll()` | `ready`, `disabled` | usually omitted |
| `requestConsentInfo()`, `showConsentFormIfRequired()`, `showPrivacyOptions()`, `getConsentStatus()` | `ready`, `not_ready` | `ConsentInfo` |
| `getTrackingAuthorizationStatus()`, `requestTrackingAuthorization()` | `ready`, `unsupported` | `{ status }` |
| `preloadInterstitial()`, `preloadRewarded()`, `preloadAppOpen()`, `preloadNative()`, `preloadInlineBanner()` | `loading`, `ready`, `disabled`, `error` | usually omitted |
| `showBanner()`, `hideBanner()`, `destroyBanner()` | `loading`, `ready`, `disabled`, `error` | usually omitted |
| `showInterstitial()`, `showRewarded()`, `showAppOpen()` | `ready`, `not_ready`, `error` | usually omitted |
| `attachNative()`, `detachNative()`, `destroyNative()`, `refreshNative()` | `ready`, `loading`, `not_ready`, `error` | usually omitted |
| `attachInlineBanner()`, `detachInlineBanner()`, `destroyInlineBanner()`, `refreshInlineBanner()` | `ready`, `loading`, `not_ready`, `error` | usually omitted |
| `isInterstitialReady()`, `isRewardedReady()`, `isAppOpenReady()`, `isNativeReady()`, `isInlineBannerReady()` | `ready`, `not_ready` | `{ ready: boolean }` |
| `getRuntimeInfo()` | `ready`, `disabled` | `RuntimeInfo` |

### Common Return Examples

Simple success:

```ts
{
  ok: true,
  status: "ready",
}
```

Loading result:

```ts
{
  ok: true,
  status: "loading",
}
```

Readiness result:

```ts
{
  ok: true,
  status: "ready",
  data: {
    ready: true,
  },
}
```

Failure result:

```ts
{
  ok: false,
  code: "CONFIG_MISSING",
  message: "placementId is required.",
  status: "error",
}
```

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

`configureRequest()` lets the app consumer define global ad-request behavior before loading ads.

All fields in `configureRequest()` are optional.

Default behavior:

- `maxAdContentRating`: `""`
- `tagForChildDirectedTreatment`: `null`
- `tagForUnderAgeOfConsent`: `null`
- `testDeviceIds`: omitted
- `appMuted`: omitted
- `appVolume`: omitted

Default interpretation:

- `""` means no explicit content-rating cap from the plugin
- `null` means the plugin does not force that privacy flag in the request configuration
- omitted audio and test-device fields mean the plugin does not override them

### `maxAdContentRating`

Controls the maximum ad content rating that Google should return for requests made by the app.

Allowed values:

- `"G"`: general audiences
- `"PG"`: parental guidance
- `"T"`: teen audiences
- `"MA"`: mature audiences
- `""`: no explicit content-rating cap

Practical guidance:

- use `"G"` for child-oriented or highly conservative apps
- use `"PG"` for general consumer apps that still want a conservative filter
- use `"T"` for broader mainstream apps
- use `"MA"` only when the app experience and audience justify it
- use `""` only if the consumer intentionally wants no explicit cap from this setting

### `tagForChildDirectedTreatment`

Signals whether requests should be treated as child-directed.

Values:

- `true`: request child-directed treatment
- `false`: explicitly indicate the request is not child-directed
- `null` or omit the field: do not override this setting in the request configuration

Important note:

- this is a policy-sensitive flag
- app consumers should only set it when their legal and product requirements actually call for child-directed treatment

### `tagForUnderAgeOfConsent`

Signals whether the user should be treated as under the age of consent for ad-request handling.

Values:

- `true`: mark requests as under age of consent
- `false`: explicitly indicate they are not under age of consent
- `null` or omit the field: do not override this setting in the request configuration

Important note:

- this should be driven by the app consumer's compliance logic, not guessed from UI state alone

### `testDeviceIds`

Defines specific devices that should receive test ads when the consumer uses their own ad unit IDs.

Use this when:

- validating production-like placements in development or staging
- checking real placement mapping without risking invalid live-ad interaction

Android emulators are typically treated as test devices automatically, but physical devices should still be configured explicitly when needed.

### `appMuted`

Controls whether ad audio should start muted at the SDK level.

Values:

- `true`: mute ad audio
- `false`: allow ad audio according to the ad creative and platform behavior

Practical guidance:

- `true` is often safer for apps that want a quieter default experience
- `false` may be acceptable if the app experience already expects audible media

### `appVolume`

Sets the global app volume hint for the Mobile Ads SDK.

Expected range:

- `0` = silent
- `1` = full volume

The plugin clamps the value into the valid `0` to `1` range.

Practical guidance:

- `0` for fully muted behavior
- `0.5` for reduced volume
- `1` for full volume

### Example Interpretation

This configuration:

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

means:

- request ads up to teen-rated content
- do not mark requests as child-directed
- do not mark requests as under age of consent
- serve test ads on the listed device when using consumer-owned ad unit IDs
- allow ad audio
- set app audio volume to full scale for the Mobile Ads SDK

## Test Ad Presets

For development convenience, supported load methods may accept a `testAdPreset`.

Supported presets:

- `app_open`
- `banner_fixed`
- `banner_adaptive`
- `banner_inline_adaptive`
- `interstitial`
- `rewarded`
- `native`

Rules:

- `testAdPreset` is development-only
- `testAdPreset` is only valid when `testMode: true`
- when `testMode: false`, `testAdPreset` is rejected explicitly
- preset names are format-specific and cannot be mixed across methods

Resolution order:

1. `adUnitId`
2. `testAdPreset`
3. `placements[placementId]`

Compatibility note:

- to avoid breaking existing integrations, Android keeps the legacy test-ad fallback when `testMode: true` and none of `adUnitId`, `testAdPreset`, or `placements[placementId]` resolves an ad unit

Example:

```ts
await DonugrAdmob.preloadInlineBanner({
  placementId: "inline_feed_banner",
  slotId: "feed.banner.1",
  hostId: "feed.banner.host.1",
  testAdPreset: "banner_inline_adaptive",
})
```

Additional examples:

```ts
await DonugrAdmob.showBanner({
  placementId: "banner_home",
  testAdPreset: "banner_adaptive",
})

await DonugrAdmob.preloadInterstitial({
  placementId: "interstitial_break",
  testAdPreset: "interstitial",
})

await DonugrAdmob.preloadRewarded({
  placementId: "rewarded_bonus",
  testAdPreset: "rewarded",
})

await DonugrAdmob.preloadAppOpen({
  placementId: "app_open_launch",
  testAdPreset: "app_open",
})

await DonugrAdmob.preloadNative({
  placementId: "native_feed_card",
  slotId: "feed.slot.1",
  hostId: "feed.card.1",
  testAdPreset: "native",
})
```

Preset-to-method guidance:

| Method | Supported `testAdPreset` |
| --- | --- |
| `showBanner()` / `loadBanner()` | `banner_fixed`, `banner_adaptive` |
| `preloadInterstitial()` | `interstitial` |
| `preloadRewarded()` | `rewarded` |
| `preloadAppOpen()` | `app_open` |
| `preloadNative()` | `native` |
| `preloadInlineBanner()` | `banner_inline_adaptive` |

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

Supported options:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `placementId` | `string` | Yes | Placement key used to resolve inventory |
| `adUnitId` | `string` | No | Explicit override ad unit ID |
| `testAdPreset` | `"banner_fixed" \| "banner_adaptive"` | No | Development-only preset when `testMode: true` |
| `position` | `"top" \| "bottom"` | No | Defaults to `"bottom"` in current Android behavior |

```ts
await DonugrAdmob.showBanner({
  placementId: "banner_home",
  position: "bottom",
})
```

Current Android banner behavior is intentionally conservative and oriented to anchored adaptive usage.

### Interstitial

Supported preload options:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `placementId` | `string` | Yes | Placement key used to resolve inventory |
| `adUnitId` | `string` | No | Explicit override ad unit ID |
| `testAdPreset` | `"interstitial"` | No | Development-only preset when `testMode: true` |

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

Supported preload options:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `placementId` | `string` | Yes | Placement key used to resolve inventory |
| `adUnitId` | `string` | No | Explicit override ad unit ID |
| `testAdPreset` | `"rewarded"` | No | Development-only preset when `testMode: true` |

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

Supported preload options:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `placementId` | `string` | Yes | Placement key used to resolve inventory |
| `adUnitId` | `string` | No | Explicit override ad unit ID |
| `testAdPreset` | `"app_open"` | No | Development-only preset when `testMode: true` |

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

Supported options:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `placementId` | `string` | Yes | Placement key used to resolve inventory |
| `slotId` | `string` | Yes | Runtime slot identity |
| `hostId` | `string` | Yes | Logical host identity |
| `adUnitId` | `string` | No | Explicit override ad unit ID |
| `testAdPreset` | `"native"` | No | Development-only preset when `testMode: true` |
| `ttlMs` | `number` | No | Native slot lifetime before stale cleanup |
| `hostRect.x` | `number` | No | Host left coordinate in px |
| `hostRect.y` | `number` | No | Host top coordinate in px |
| `hostRect.width` | `number` | No | Host width in px |
| `hostRect.height` | `number` | No | Host height in px |
| `hostRect.anchor` | `"top" \| "bottom"` | No | Anchor hint for host layout |

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

Supported options:

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `placementId` | `string` | Yes | Placement key used to resolve inventory |
| `slotId` | `string` | Yes | Runtime slot identity |
| `hostId` | `string` | Yes | Logical host identity |
| `adUnitId` | `string` | No | Explicit override ad unit ID |
| `testAdPreset` | `"banner_inline_adaptive"` | No | Development-only preset when `testMode: true` |
| `hostRect.x` | `number` | No | Host left coordinate in px |
| `hostRect.y` | `number` | No | Host top coordinate in px |
| `hostRect.width` | `number` | No | Host width in px |
| `hostRect.height` | `number` | No | Host height in px |
| `hostRect.anchor` | `"top" \| "bottom"` | No | Anchor hint for host layout |

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

### `adEvent` Payload

Event payload shape:

| Field | Type | Always present | Notes |
| --- | --- | --- | --- |
| `format` | `"banner" \| "interstitial" \| "rewarded" \| "native" \| "inline_banner" \| "app_open"` | Yes | Ad format that emitted the event |
| `placementId` | `string` | Yes | Placement identity |
| `slotId` | `string` | No | Present for host-based formats such as native and inline banner |
| `phase` | `string` | Yes | Lifecycle phase |
| `code` | `string` | No | Error or diagnostic code when relevant |
| `message` | `string` | No | Human-readable event message |

Example event payload:

```ts
{
  format: "native",
  placementId: "native_feed_card",
  slotId: "feed.slot.1",
  phase: "loaded",
  message: "Native ad loaded.",
}
```

Common phase usage:

- `loaded`: ad finished loading successfully
- `failed`: ad load or show failed
- `shown`: fullscreen or banner open event
- `dismissed`: fullscreen ad closed
- `clicked`: ad click recorded
- `impression`: impression recorded
- `reward_earned`: rewarded callback path
- `attached`: host-based ad attached to a UI host
- `detached`: host-based ad detached from a UI host
- `destroyed`: slot or banner destroyed
- `consent_updated`: consent state changed
- `preload_start`, `preload_reused`, `preload_skip_loading`, `attach_skipped_same_host`, `layout_skipped_same_rect`: diagnostics for native and inline host flows

## Return Payloads

All plugin methods resolve to a `BridgeResult<T>` shape.

Base result shape:

| Field | Type | Always present | Notes |
| --- | --- | --- | --- |
| `ok` | `boolean` | Yes | `true` for success, `false` for failure |
| `status` | `"ready" \| "loading" \| "not_ready" \| "disabled" \| "unsupported" \| "error"` | No | High-level availability state |
| `code` | `string` | No | Failure code when `ok` is `false` |
| `message` | `string` | No | Failure or diagnostic message |
| `data` | `object` | No | Method-specific payload |

Success example:

```ts
{
  ok: true,
  status: "ready",
  data: {
    ready: true,
  },
}
```

Failure example:

```ts
{
  ok: false,
  code: "NOT_READY",
  message: "Interstitial is not ready.",
  status: "not_ready",
}
```

### Readiness Payloads

Methods:

- `isInterstitialReady()`
- `isRewardedReady()`
- `isAppOpenReady()`
- `isNativeReady()`
- `isInlineBannerReady()`

Payload shape:

| Field | Type | Notes |
| --- | --- | --- |
| `ready` | `boolean` | Indicates whether the ad or slot is currently ready |

Example:

```ts
const result = await DonugrAdmob.isInterstitialReady("interstitial_break")

if (result.ok && result.data?.ready) {
  await DonugrAdmob.showInterstitial("interstitial_break")
}
```

### Consent Payloads

Methods:

- `requestConsentInfo()`
- `showConsentFormIfRequired()`
- `showPrivacyOptions()`
- `getConsentStatus()`

Payload shape:

| Field | Type | Notes |
| --- | --- | --- |
| `status` | `"unknown" \| "required" \| "not_required" \| "obtained" \| "denied"` | Consent status surface exposed by the plugin |
| `canRequestAds` | `boolean` | Whether ads can be requested at this point |
| `privacyOptionsRequired` | `boolean` | Whether privacy options should be shown |

Example:

```ts
const consent = await DonugrAdmob.requestConsentInfo()

if (consent.ok && consent.data?.canRequestAds) {
  // safe to continue loading ads
}
```

### Tracking Authorization Payloads

Methods:

- `getTrackingAuthorizationStatus()`
- `requestTrackingAuthorization()`

Payload shape:

| Field | Type | Notes |
| --- | --- | --- |
| `status` | `"not_determined" \| "restricted" \| "denied" \| "authorized" \| "unsupported"` | Tracking authorization status |

### Runtime Payload

Method:

- `getRuntimeInfo()`

Payload shape:

| Field | Type | Notes |
| --- | --- | --- |
| `platform` | `"android" \| "ios" \| "web"` | Current platform |
| `enabled` | `boolean` | Whether ads are enabled in plugin runtime config |
| `testMode` | `boolean` | Whether runtime is in test mode |
| `applicationIdConfigured` | `boolean` | Whether an app ID is configured |
| `applicationIdSource` | `"js" \| "android_manifest" \| "ios_plist" \| "missing"` | Where the app ID came from |
| `placementsConfigured` | `number` | Number of registered placements |
| `requestConfigurationConfigured` | `boolean` | Whether request configuration has been applied |
| `consentStatus` | `string` | Current consent status |
| `activeSlots` | `number` | Native slot count when available |
| `loadingSlots` | `number` | Native loading slot count when available |
| `readySlots` | `number` | Native ready slot count when available |
| `attachedSlots` | `number` | Native attached slot count when available |
| `failedSlots` | `number` | Native failed slot count when available |
| `expiredSlots` | `number` | Native expired slot count when available |
| `inlineBannerActiveSlots` | `number` | Inline banner slot count when available |
| `inlineBannerLoadingSlots` | `number` | Inline banner loading count when available |
| `inlineBannerReadySlots` | `number` | Inline banner ready count when available |
| `inlineBannerAttachedSlots` | `number` | Inline banner attached count when available |
| `inlineBannerFailedSlots` | `number` | Inline banner failed count when available |

Example:

```ts
const runtime = await DonugrAdmob.getRuntimeInfo()

if (runtime.ok) {
  console.log(runtime.data?.platform, runtime.data?.testMode)
}
```

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

## Environment Guidance

### Development

Use development mode when wiring the plugin, validating layout, and checking event flow.

- set `testMode: true`
- you may use `testAdPreset`
- or use your own ad units plus `testDeviceIds`
- do not click live ads without proper test-device setup

### Staging / Pre-release QA

Use staging to verify real placement mapping before release.

- prefer your own app ad unit IDs
- keep `testDeviceIds` enabled where appropriate
- reduce dependence on `testAdPreset`
- validate consent flow, fullscreen timing, and host-based layout behavior

### Production

Use production inventory owned by the consumer app.

- set `testMode: false`
- do not use `testAdPreset`
- rely on `placements` as the default inventory mapping
- use `adUnitId` only as an explicit override when truly needed
- keep ad unit formats aligned with plugin methods

Production format guidance:

- `showBanner()` uses Banner ad units
- `preloadInlineBanner()` uses Banner ad units
- `preloadNative()` uses Native ad units
- `preloadInterstitial()` uses Interstitial ad units
- `preloadRewarded()` uses Rewarded ad units
- `preloadAppOpen()` uses App Open ad units

## Public Package Direction

This package is intended for public npm usage, so the public API aims to stay:

- Android-first and explicit about current platform support
- conservative around policy-sensitive behavior
- stable in naming for `placementId`, `slotId`, `hostId`, and event phases
- debuggable through shared runtime and event contracts

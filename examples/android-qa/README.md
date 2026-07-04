# Android QA Harness Example

Folder ini adalah template kecil untuk app consumer test supaya QA Android bisa dijalankan dari satu halaman.

## Cara pakai

1. install plugin ini di app consumer test
2. copy atau adapt file [qa-page.ts](/G:/CapacitorPlugins/@donugr-admob/examples/android-qa/qa-page.ts#L1) ke project consumer
3. buat satu halaman dengan tombol `data-qa-action`
4. siapkan dua host element:
   - `#qa-native-host`
   - `#qa-inline-banner-host`
5. panggil `mountDonugrAdmobQaPage()`

## Bootstrap contoh

```ts
import { mountDonugrAdmobQaPage } from "./qa-page"

mountDonugrAdmobQaPage({
  testMode: true,
})
```

## Markup contoh

```html
<div>
  <button data-qa-action="configure">Configure</button>
  <button data-qa-action="refreshRuntime">Runtime</button>
  <button data-qa-action="requestConsent">Consent Info</button>
  <button data-qa-action="showConsentForm">Consent Form</button>
  <button data-qa-action="showPrivacyOptions">Privacy Options</button>
</div>

<div>
  <button data-qa-action="showBannerTop">Banner Top</button>
  <button data-qa-action="showBannerBottom">Banner Bottom</button>
  <button data-qa-action="hideBanner">Hide Banner</button>
  <button data-qa-action="destroyBanner">Destroy Banner</button>
</div>

<div>
  <button data-qa-action="preloadInterstitial">Preload Interstitial</button>
  <button data-qa-action="interstitialReady">Interstitial Ready</button>
  <button data-qa-action="showInterstitial">Show Interstitial</button>
</div>

<div>
  <button data-qa-action="preloadRewarded">Preload Rewarded</button>
  <button data-qa-action="rewardedReady">Rewarded Ready</button>
  <button data-qa-action="showRewarded">Show Rewarded</button>
</div>

<div>
  <button data-qa-action="preloadAppOpen">Preload App Open</button>
  <button data-qa-action="appOpenReady">App Open Ready</button>
  <button data-qa-action="showAppOpen">Show App Open</button>
</div>

<div>
  <button data-qa-action="preloadNative">Preload Native</button>
  <button data-qa-action="nativeReady">Native Ready</button>
  <button data-qa-action="attachNative">Attach Native</button>
  <button data-qa-action="detachNative">Detach Native</button>
  <button data-qa-action="refreshNative">Refresh Native</button>
  <button data-qa-action="destroyNative">Destroy Native</button>
</div>

<div>
  <button data-qa-action="preloadInlineBanner">Preload Inline Banner</button>
  <button data-qa-action="inlineBannerReady">Inline Banner Ready</button>
  <button data-qa-action="attachInlineBanner">Attach Inline Banner</button>
  <button data-qa-action="detachInlineBanner">Detach Inline Banner</button>
  <button data-qa-action="refreshInlineBanner">Refresh Inline Banner</button>
  <button data-qa-action="destroyInlineBanner">Destroy Inline Banner</button>
</div>

<div>
  <button data-qa-action="clearAll">Clear All</button>
</div>

<div id="qa-native-host" style="min-height: 140px; border: 1px dashed #999; margin: 16px 0;">
  Native host target
</div>

<div id="qa-inline-banner-host" style="min-height: 140px; border: 1px dashed #999; margin: 16px 0;">
  Inline banner host target
</div>

<pre id="qa-runtime"></pre>
<pre id="qa-log"></pre>
```

## Action names yang tersedia

- `configure`
- `refreshRuntime`
- `requestConsent`
- `showConsentForm`
- `showPrivacyOptions`
- `showBannerTop`
- `showBannerBottom`
- `hideBanner`
- `destroyBanner`
- `preloadInterstitial`
- `interstitialReady`
- `showInterstitial`
- `preloadRewarded`
- `rewardedReady`
- `showRewarded`
- `preloadAppOpen`
- `appOpenReady`
- `showAppOpen`
- `preloadNative`
- `nativeReady`
- `attachNative`
- `detachNative`
- `refreshNative`
- `destroyNative`
- `preloadInlineBanner`
- `inlineBannerReady`
- `attachInlineBanner`
- `detachInlineBanner`
- `refreshInlineBanner`
- `destroyInlineBanner`
- `clearAll`

## Catatan

- file ini hanya template consumer-side
- tidak ikut publish npm karena `package.json` memakai whitelist `files`
- host element native dan inline banner sengaja dipisah agar QA overlay lebih jelas

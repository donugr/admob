import type { ConsentInfo, ConsentStatus } from "../definitions"

export type ConsentState = ConsentInfo & {
  lastUpdatedAt: number | null
}

export const defaultConsentState = (status: ConsentStatus = "unknown"): ConsentState => ({
  status,
  canRequestAds: false,
  privacyOptionsRequired: false,
  lastUpdatedAt: null,
})

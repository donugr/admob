import type { ConsentInfo } from "../definitions"
import { defaultConsentState, type ConsentState } from "./consent-types"

export class ConsentController {
  private state: ConsentState = defaultConsentState()

  getSnapshot(): ConsentState {
    return { ...this.state }
  }

  requestConsentInfo(): ConsentInfo {
    return this.toConsentInfo()
  }

  showConsentFormIfRequired(): ConsentInfo {
    if (this.state.status === "required") {
      this.state = {
        status: "obtained",
        canRequestAds: true,
        privacyOptionsRequired: true,
        lastUpdatedAt: Date.now(),
      }
    }

    return this.toConsentInfo()
  }

  showPrivacyOptions(): ConsentInfo {
    return this.toConsentInfo()
  }

  resetForTesting() {
    this.state = defaultConsentState()
  }

  seed(next: Partial<ConsentInfo>) {
    this.state = {
      status: next.status ?? this.state.status,
      canRequestAds: next.canRequestAds ?? this.state.canRequestAds,
      privacyOptionsRequired: next.privacyOptionsRequired ?? this.state.privacyOptionsRequired,
      lastUpdatedAt: Date.now(),
    }
  }

  private toConsentInfo(): ConsentInfo {
    return {
      status: this.state.status,
      canRequestAds: this.state.canRequestAds,
      privacyOptionsRequired: this.state.privacyOptionsRequired,
    }
  }
}

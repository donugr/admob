import type { TrackingAuthorizationStatus } from "../definitions"

export class TrackingController {
  private status: TrackingAuthorizationStatus = "unsupported"

  getStatus() {
    return this.status
  }

  seed(status: TrackingAuthorizationStatus) {
    this.status = status
  }

  requestAuthorization() {
    if (this.status === "not_determined") {
      this.status = "authorized"
    }
    return this.status
  }

  reset() {
    this.status = "unsupported"
  }
}

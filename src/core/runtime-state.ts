import type { ApplicationIdSource, ConfigureOptions } from "../definitions"

export type RuntimeState = {
  enabled: boolean
  testMode: boolean
  applicationId: string
  applicationIdSource: ApplicationIdSource
}

const defaultState: RuntimeState = {
  enabled: false,
  testMode: false,
  applicationId: "",
  applicationIdSource: "missing",
}

export class RuntimeStateStore {
  private state: RuntimeState = { ...defaultState }

  getSnapshot(): RuntimeState {
    return { ...this.state }
  }

  configure(options: ConfigureOptions) {
    const applicationId = String(options.applicationId ?? "").trim()
    this.state = {
      enabled: Boolean(options.enabled),
      testMode: Boolean(options.testMode),
      applicationId,
      applicationIdSource: applicationId ? "js" : "missing",
    }
  }

  reset() {
    this.state = { ...defaultState }
  }
}

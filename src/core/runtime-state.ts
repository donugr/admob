import type { ApplicationIdSource, ConfigureOptions, LoggingLevel } from "../definitions"

export type RuntimeState = {
  enabled: boolean
  testMode: boolean
  releaseSystemUiOnAdInteraction: boolean
  loggingLevel: LoggingLevel
  emitAdEvents: boolean
  applicationId: string
  applicationIdSource: ApplicationIdSource
}

const defaultState: RuntimeState = {
  enabled: false,
  testMode: false,
  releaseSystemUiOnAdInteraction: true,
  loggingLevel: "off",
  emitAdEvents: false,
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
      releaseSystemUiOnAdInteraction: options.releaseSystemUiOnAdInteraction ?? true,
      loggingLevel: options.loggingLevel ?? "off",
      emitAdEvents: options.emitAdEvents ?? false,
      applicationId,
      applicationIdSource: applicationId ? "js" : "missing",
    }
  }

  reset() {
    this.state = { ...defaultState }
  }
}

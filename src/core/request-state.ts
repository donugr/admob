import {
  DEFAULT_REQUEST_CONFIGURATION_OPTIONS,
  type RequestConfigurationOptions,
} from "../definitions"

export class RequestConfigurationStore {
  private state: RequestConfigurationOptions = { ...DEFAULT_REQUEST_CONFIGURATION_OPTIONS }

  getSnapshot(): RequestConfigurationOptions {
    return {
      ...DEFAULT_REQUEST_CONFIGURATION_OPTIONS,
      ...this.state,
      testDeviceIds: this.state.testDeviceIds ? [...this.state.testDeviceIds] : undefined,
    }
  }

  configure(options: RequestConfigurationOptions) {
    this.state = {
      ...DEFAULT_REQUEST_CONFIGURATION_OPTIONS,
      ...options,
      testDeviceIds: options.testDeviceIds ? [...options.testDeviceIds] : undefined,
    }
  }

  hasConfiguration() {
    return Object.keys(this.state).length > 0
  }

  reset() {
    this.state = { ...DEFAULT_REQUEST_CONFIGURATION_OPTIONS }
  }
}

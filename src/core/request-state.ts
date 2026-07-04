import type { RequestConfigurationOptions } from "../definitions"

export class RequestConfigurationStore {
  private state: RequestConfigurationOptions = {}

  getSnapshot(): RequestConfigurationOptions {
    return {
      ...this.state,
      testDeviceIds: this.state.testDeviceIds ? [...this.state.testDeviceIds] : undefined,
    }
  }

  configure(options: RequestConfigurationOptions) {
    this.state = {
      ...options,
      testDeviceIds: options.testDeviceIds ? [...options.testDeviceIds] : undefined,
    }
  }

  hasConfiguration() {
    return Object.keys(this.state).length > 0
  }

  reset() {
    this.state = {}
  }
}

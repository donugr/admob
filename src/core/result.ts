import type { AvailabilityStatus, BridgeResult } from "../definitions"

export function ok<T = undefined>(data?: T, status?: AvailabilityStatus): BridgeResult<T> {
  return {
    ok: true,
    status,
    ...(data === undefined ? {} : { data }),
  }
}

export function fail<T = undefined>(code: string, message: string, status: AvailabilityStatus = "error"): BridgeResult<T> {
  return {
    ok: false,
    code,
    message,
    status,
  }
}

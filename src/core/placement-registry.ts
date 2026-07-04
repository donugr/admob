export class PlacementRegistry {
  private placements = new Map<string, string>()

  configure(nextPlacements?: Record<string, string>) {
    this.placements.clear()
    for (const [key, value] of Object.entries(nextPlacements ?? {})) {
      const normalizedKey = String(key).trim()
      const normalizedValue = String(value ?? "").trim()
      if (normalizedKey && normalizedValue) {
        this.placements.set(normalizedKey, normalizedValue)
      }
    }
  }

  resolve(placementId: string) {
    return this.placements.get(String(placementId).trim()) ?? ""
  }

  count() {
    return this.placements.size
  }

  reset() {
    this.placements.clear()
  }
}

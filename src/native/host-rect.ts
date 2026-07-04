import type { NativeHostAnchor, NativeHostRect } from "../definitions"

type RectLike = {
  left: number
  top: number
  width: number
  height: number
}

type RectOptions = {
  anchor?: NativeHostAnchor
}

export function buildNativeHostRect(element: Element, options: RectOptions = {}): NativeHostRect {
  const rect = element.getBoundingClientRect() as RectLike
  return {
    x: Math.round(rect.left),
    y: Math.round(rect.top),
    width: Math.round(rect.width),
    height: Math.round(rect.height),
    anchor: options.anchor ?? "top",
  }
}

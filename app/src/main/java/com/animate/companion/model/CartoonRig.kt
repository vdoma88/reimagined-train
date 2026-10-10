package com.animate.companion.model

/** One coordinate system for skin, garments and shoes; framing never scales layers separately. */
object CartoonRig {
    const val NECK_X = 200f
    const val NECK_Y = 267f
    const val FRAME_WIDTH = 400f
    const val FRAME_TOP = -12f
    const val FULL_HEIGHT = 680f
    const val BREATH_MARGIN = 2f

    fun isBodyLayer(key: String) = key == "body" || key.startsWith("top.") ||
        key.startsWith("bottom.") || key == "boots" || key == "sneakers"

    fun frameHeight(headOnly: Boolean, fullBody: Boolean) = when {
        headOnly -> 344f
        fullBody -> FULL_HEIGHT
        else -> 464f
    }

    fun bounds(key: String, spec: CartoonSpriteSpec, look: CartoonLook, gender: Gender): Bounds {
        val sx = if (isBodyLayer(key)) look.bodyScaleX(gender) else look.headScaleX(gender)
        val sy = if (isBodyLayer(key)) look.bodyScaleY(gender) else look.headScaleY(gender)
        return Bounds(NECK_X + (spec.x - NECK_X) * sx, NECK_Y + (spec.y - NECK_Y) * sy,
            NECK_X + (spec.x + spec.width - NECK_X) * sx,
            NECK_Y + (spec.y + spec.height - NECK_Y) * sy)
    }

    data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float)
}

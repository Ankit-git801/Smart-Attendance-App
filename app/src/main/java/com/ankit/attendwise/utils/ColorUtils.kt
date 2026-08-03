package com.ankit.attendwise.utils

import androidx.compose.ui.graphics.Color
import android.util.Log

import androidx.annotation.Keep

@Keep
object ColorUtils {
    /**
     * Safely parses a hex color string. Returns [fallback] if parsing fails.
     */
    fun safeParseColor(colorHex: String, fallback: Color = Color.Gray): Color {
        return try {
            if (colorHex.isEmpty()) return fallback
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Log.e("ColorUtils", "Failed to parse color: $colorHex. Using fallback.")
            fallback
        }
    }
}

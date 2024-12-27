package com.example.beta.ui.hex

import android.graphics.drawable.Drawable

/**
 * Represents a tile in the hex grid. Holds an icon, label, and an optional click handler.
 */
data class HexTile(
    val icon: Drawable?,
    val label: String = "",
    val onClick: (() -> Unit)? = null
)
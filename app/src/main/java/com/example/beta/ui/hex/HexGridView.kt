package com.example.beta.ui.hex

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A custom view that renders a grid of hexagonal tiles with "infinite" scrolling.
 */
class HexGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Default hex size (radius of each hex)
    private var hexSize: Float = 80f

    // Spacing between hexes:
    private val horizontalSpacing: Float
        get() = 3f * hexSize

    private val verticalSpacing: Float
        get() = sqrt(0.75f) * hexSize

    // Initial offset to ensure the grid starts offscreen
    private val initialOffsetX = -2 * horizontalSpacing
    private val initialOffsetY = -2 * verticalSpacing

    // Holds the list of tiles (icons, labels, callbacks)
    private val hexTileList = mutableListOf<HexTile>()

    // Number of hex tiles in each "logical" row (used to compute tile index)
    private val columns = 5

    // Paint used to draw the hex shapes
    private val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.LTGRAY
    }

    // Text paint for labels (optional)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    // Scroll offsets (in pixels). These values move as the user drags/flicks.
    private var scrollOffsetX = 0f
    private var scrollOffsetY = 0f

    // A GestureDetector to handle dragging (and optionally flinging)
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // Move the grid opposite to finger movement
            scrollOffsetX += distanceX
            scrollOffsetY += distanceY
            invalidate() // redraw
            return true
        }

        // If you want inertial "fling" scrolling, override onFling, apply velocity, etc.
        // override fun onFling(...)
    })

    // Pool of Path objects for reuse
    private val hexPathPool = mutableListOf<Path>()
    private var currentPathIndex = 0

    init {
        // Additional initialization if needed
    }

    /**
     * Sets the size of each hexagon (radius from center to vertex).
     * Triggers the view to re-measure and redraw.
     */
    fun setHexagonSize(size: Float) {
        hexSize = size
        invalidate()
        requestLayout()
    }

    /**
     * Provide a list of HexTiles to render in the grid.
     */
    fun setTiles(tiles: List<HexTile>) {
        hexTileList.clear()
        hexTileList.addAll(tiles)
        invalidate()
        requestLayout()
    }

    /**
     * Measures the view. Adjust as needed for wrap_content / match_parent.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // For demonstration, pick a desired size or just use the parent's suggestions.
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        val measuredWidth = resolveSize(desiredWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    /**
     * We override onTouchEvent so our GestureDetector can process scroll/fling events,
     * and to handle tile clicks.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Let the gestureDetector handle scroll/fling
        val handled = gestureDetector.onTouchEvent(event)

        // If it's a simple "ACTION_DOWN" that wasn't consumed by scrolling,
        // check if we tapped a tile and call its onClick().
        if (!handled && event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            val tileIndex = findHexIndex(x, y)
            if (tileIndex != null && tileIndex in hexTileList.indices) {
                hexTileList[tileIndex].onClick?.invoke()
                return true
            }
        }
        return handled || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val totalApps = hexTileList.size
        if (totalApps == 0) return

        // Compute how many columns and rows fit on the screen,
        // adding extra for the initial offset
        val visibleColumnsCount = (width / horizontalSpacing).roundToInt() + 4  // +4 for initial offset
        val visibleRowsCount = (height / verticalSpacing).roundToInt() + 4     // +4 for initial offset

        // Convert the pixel-based offsets into "how many hex columns/rows" have we scrolled
        val offsetColsFloat = scrollOffsetX / horizontalSpacing
        val offsetRowsFloat = scrollOffsetY / verticalSpacing

        // Separate integer and fractional parts
        val offsetColsInt = offsetColsFloat.toInt()
        val offsetColsFrac = offsetColsFloat - offsetColsInt
        val offsetRowsInt = offsetRowsFloat.toInt()
        val offsetRowsFrac = offsetRowsFloat - offsetRowsInt

        // Reset for each onDraw call
        currentPathIndex = 0

        // Loop through the "visible window" of rows/columns
        for (r in -2 until visibleRowsCount) { // Start from -2 to account for initial offset
            // Global row in the infinite grid
            val globalRow = r + offsetRowsInt

            for (c in -2 until visibleColumnsCount) { // Start from -2 to account for initial offset
                // Global column in the infinite grid
                val globalCol = c + offsetColsInt

                // Wrap the tile index
                val rawIndex = globalRow * columns + globalCol
                var tileIndex = rawIndex % totalApps
                if (tileIndex < 0) tileIndex += totalApps

                val tile = hexTileList[tileIndex]

                // Calculate the center position for this hex on screen,
                // applying the initial offset
                val baseCenterX = (c - offsetColsFrac) * horizontalSpacing + hexSize + initialOffsetX
                val baseCenterY = (r - offsetRowsFrac) * verticalSpacing + hexSize + initialOffsetY

                // Optional stagger
                val offsetX = if ((globalRow % 2).absoluteValue == 1) hexSize * 1.5f else 0f
                val centerX = baseCenterX + offsetX
                val centerY = baseCenterY

                // Draw the hex shape using a recycled Path object
                val path = obtainPath()
                drawHexagon(canvas, centerX, centerY, path)

                // Draw the icon (if any)
                tile.icon?.let { drawable ->
                    val iconSize = hexSize * 0.6f
                    val left = (centerX - iconSize / 2).toInt()
                    val top = (centerY - iconSize / 2).toInt()
                    val right = (centerX + iconSize / 2).toInt()
                    val bottom = (centerY + iconSize / 2).toInt()
                    drawable.setBounds(left, top, right, bottom)
                    drawable.draw(canvas)
                }

                // Draw the label (optional)
                if (tile.label.isNotEmpty()) {
                    val textY = centerY + hexSize * 0.6f + 24f
                    canvas.drawText(tile.label, centerX, textY, textPaint)
                }
            }
        }
    }

    /**
     * Draws a single hexagon at the provided center coordinates (cx, cy).
     */
    private fun drawHexagon(canvas: Canvas, cx: Float, cy: Float, path: Path) {
        // Each of the 6 corners is 60 degrees apart.
        for (i in 0..5) {
            val angle = Math.toRadians((60 * i).toDouble())
            val x = cx + (hexSize * cos(angle)).toFloat()
            val y = cy + (hexSize * sin(angle)).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        canvas.drawPath(path, hexPaint)
    }

    /**
     * Find which tile index the user tapped, given raw screen coords (x, y).
     * We'll invert the scroll offset logic and wrap using modulus,
     * similar to how we did in onDraw.
     */
    private fun findHexIndex(x: Float, y: Float): Int? {
        if (hexTileList.isEmpty()) return null
        val totalApps = hexTileList.size

        // Convert screen coords to "global" row/col,
        // also considering the initial offset
        val offsetColsFloat = (scrollOffsetX - initialOffsetX) / horizontalSpacing
        val offsetRowsFloat = (scrollOffsetY - initialOffsetY) / verticalSpacing

        val offsetColsInt = offsetColsFloat.toInt()
        val offsetRowsInt = offsetRowsFloat.toInt()
        val offsetColsFrac = offsetColsFloat - offsetColsInt
        val offsetRowsFrac = offsetRowsFloat - offsetRowsInt

        // Adjust for the tap position
        val adjustedCol = ((x - hexSize - initialOffsetX) / horizontalSpacing) + offsetColsFrac
        val adjustedRow = ((y - hexSize - initialOffsetY) / verticalSpacing) + offsetRowsFrac

        val colInt = adjustedCol.toInt() + offsetColsInt
        val rowInt = adjustedRow.toInt() + offsetRowsInt

        // Check if row is odd for the stagger offset
        val isOddRow = (rowInt % 2).absoluteValue == 1
        val offsetX = if (isOddRow) hexSize * 1.5f else 0f

        // Re-check the X coordinate
        val finalX = x - offsetX - initialOffsetX

        // Approximate the column
        val approximateCol = ((finalX - hexSize) / horizontalSpacing) + offsetColsFrac
        val colExactInt = approximateCol.toInt() + offsetColsInt

        // Compute the final tile index
        val rawIndex = rowInt * columns + colExactInt
        var tileIndex = rawIndex % totalApps
        if (tileIndex < 0) tileIndex += totalApps

        return if (tileIndex in 0 until totalApps) tileIndex else null
    }

    // Get a Path object from the pool, or create a new one if needed
    private fun obtainPath(): Path {
        return if (currentPathIndex < hexPathPool.size) {
            val path = hexPathPool[currentPathIndex]
            path.reset() // Important: reset the path before reusing it
            currentPathIndex++
            path
        } else {
            val newPath = Path()
            hexPathPool.add(newPath)
            currentPathIndex++
            newPath
        }
    }
}
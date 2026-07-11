package com.example.rubikscube

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

/**
 * Petit joystick virtuel. Tant qu'il est tenu, il appelle en continu
 * onMove(nx, ny) avec nx,ny ∈ [-1,1] (0,0 au centre).
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onMove: ((Float, Float) -> Unit)? = null

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 255, 255, 255)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 240, 240, 240)
    }

    private var cx = 0f
    private var cy = 0f
    private var baseRadius = 0f
    private var knobRadius = 0f
    private var knobX = 0f
    private var knobY = 0f

    private var nx = 0f
    private var ny = 0f
    private var active = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (active && (nx != 0f || ny != 0f)) {
                onMove?.invoke(nx, ny)
                handler.postDelayed(this, 16)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f; cy = h / 2f
        baseRadius = min(w, h) / 2f - 6f
        knobRadius = baseRadius * 0.45f
        knobX = cx; knobY = cy
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(cx, cy, baseRadius, basePaint)
        canvas.drawCircle(cx, cy, baseRadius, ringPaint)
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { active = true; handler.post(ticker); update(event.x, event.y) }
            MotionEvent.ACTION_MOVE -> update(event.x, event.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> reset()
        }
        return true
    }

    private fun update(x: Float, y: Float) {
        var dx = x - cx
        var dy = y - cy
        val dist = hypot(dx, dy)
        val max = baseRadius - knobRadius
        if (dist > max && dist > 0f) { dx = dx / dist * max; dy = dy / dist * max }
        knobX = cx + dx; knobY = cy + dy
        nx = if (max > 0) dx / max else 0f
        ny = if (max > 0) dy / max else 0f
        invalidate()
    }

    private fun reset() {
        active = false
        nx = 0f; ny = 0f
        knobX = cx; knobY = cy
        invalidate()
    }
}

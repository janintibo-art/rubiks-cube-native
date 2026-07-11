package com.tiboja.cubenova

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Joystick virtuel néon, deux modes :
 *  - Libre : onMove(nx,ny) en continu (rotation fluide).
 *  - Directionnel : onDirection(dx,dy) une fois par poussée (bascule face par face).
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onMove: ((Float, Float) -> Unit)? = null
    var onDirection: ((Int, Int) -> Unit)? = null
    var directional: Boolean = false

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(200, 94, 231, 255)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 94, 231, 255)
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 255, 255, 255)
    }

    private var cx = 0f; private var cy = 0f
    private var baseRadius = 0f; private var knobRadius = 0f
    private var knobX = 0f; private var knobY = 0f
    private var nx = 0f; private var ny = 0f
    private var active = false
    private var fired = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (active && !directional && (nx != 0f || ny != 0f)) {
                onMove?.invoke(nx, ny)
                handler.postDelayed(this, 16)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f; cy = h / 2f
        baseRadius = min(w, h) / 2f - 10f
        knobRadius = baseRadius * 0.42f
        knobX = cx; knobY = cy
        ringPaint.strokeWidth = baseRadius * 0.05f
        glowPaint.strokeWidth = baseRadius * 0.16f
        basePaint.shader = RadialGradient(
            cx, cy, baseRadius,
            intArrayOf(Color.argb(235, 30, 30, 66), Color.argb(205, 12, 12, 28)),
            null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        // Halo extérieur
        glowPaint.color = Color.argb(if (active) 90 else 55, 94, 231, 255)
        canvas.drawCircle(cx, cy, baseRadius, glowPaint)
        // Socle dégradé
        canvas.drawCircle(cx, cy, baseRadius, basePaint)
        // Anneau
        canvas.drawCircle(cx, cy, baseRadius, ringPaint)

        // Petits repères directionnels (haut/bas/gauche/droite)
        val tr = baseRadius * 0.055f
        val d = baseRadius * 0.80f
        canvas.drawCircle(cx, cy - d, tr, tickPaint)
        canvas.drawCircle(cx, cy + d, tr, tickPaint)
        canvas.drawCircle(cx - d, cy, tr, tickPaint)
        canvas.drawCircle(cx + d, cy, tr, tickPaint)

        // Halo du bouton
        knobGlow.shader = RadialGradient(
            knobX, knobY, knobRadius * 1.6f,
            intArrayOf(Color.argb(120, 94, 231, 255), Color.argb(0, 94, 231, 255)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(knobX, knobY, knobRadius * 1.6f, knobGlow)

        // Bouton dégradé
        knobPaint.shader = RadialGradient(
            knobX - knobRadius * 0.3f, knobY - knobRadius * 0.3f, knobRadius * 1.4f,
            intArrayOf(Color.rgb(150, 239, 255), Color.rgb(61, 107, 255)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
        // Reflet
        canvas.drawCircle(knobX - knobRadius * 0.32f, knobY - knobRadius * 0.32f, knobRadius * 0.28f, highlight)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                active = true
                if (!directional) handler.post(ticker)
                update(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> update(event.x, event.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> reset()
        }
        return true
    }

    private fun update(x: Float, y: Float) {
        var dx = x - cx; var dy = y - cy
        val dist = hypot(dx, dy)
        val max = baseRadius - knobRadius
        if (dist > max && dist > 0f) { dx = dx / dist * max; dy = dy / dist * max }
        knobX = cx + dx; knobY = cy + dy
        nx = if (max > 0) dx / max else 0f
        ny = if (max > 0) dy / max else 0f

        if (directional) {
            val mag = hypot(nx, ny)
            if (mag > 0.55f && !fired) {
                if (abs(nx) >= abs(ny)) onDirection?.invoke(if (nx > 0) 1 else -1, 0)
                else onDirection?.invoke(0, if (ny > 0) 1 else -1)
                fired = true
            } else if (mag < 0.25f) {
                fired = false
            }
        }
        invalidate()
    }

    private fun reset() {
        active = false; fired = false
        nx = 0f; ny = 0f
        knobX = cx; knobY = cy
        invalidate()
    }
}

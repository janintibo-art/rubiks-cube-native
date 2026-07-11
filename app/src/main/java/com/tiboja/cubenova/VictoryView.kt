package com.tiboja.cubenova

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animation de victoire : des mini Rubik's cubes isométriques tournoient dans
 * tous les sens en se résolvant progressivement, se figent, puis "VICTOIRE !"
 * apparaît avec un grand smiley. Un tap ferme l'animation (onDone).
 */
class VictoryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onDone: (() -> Unit)? = null

    private val COLORS = intArrayOf(
        Color.rgb(235, 235, 235), Color.rgb(255, 217, 0), Color.rgb(204, 13, 13),
        Color.rgb(255, 115, 0), Color.rgb(0, 77, 230), Color.rgb(0, 153, 38)
    )

    /** Un mini-cube : position, trajectoire, rotation, et ses 27 stickers (3 faces visibles x 9). */
    private inner class MiniCube(val delay: Long) {
        var x = 0f; var y = 0f
        val startX = Random.nextFloat()
        val speedY = 0.10f + Random.nextFloat() * 0.16f     // fraction d'écran / s
        val driftX = (Random.nextFloat() - 0.5f) * 0.10f
        var angle = Random.nextFloat() * 360f
        val spin = (Random.nextFloat() - 0.5f) * 320f       // deg/s
        val size = 0.055f + Random.nextFloat() * 0.045f     // fraction de la largeur
        val wobblePhase = Random.nextFloat() * 6.28f

        // stickers[face 0..2][cell 0..8] : couleur actuelle ; solved = couleur cible de la face
        val target = IntArray(3) { Random.nextInt(6) }.let {
            if (it[0] == it[1]) it[1] = (it[1] + 1) % 6
            if (it[2] == it[0] || it[2] == it[1]) it[2] = (it[0] + it[1] + 3) % 6
            it
        }
        val stickers = Array(3) { f -> IntArray(9) { Random.nextInt(6) } }
        val fixOrder = (0 until 27).shuffled()              // ordre de résolution des stickers
        var frozen = false
    }

    private val cubes = ArrayList<MiniCube>()
    private var startMs = 0L
    private var running = false
    private var textAlpha = 0f
    private var smileyScale = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(200, 10, 10, 20)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5EE7FF")
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        setShadowLayer(24f, 0f, 0f, Color.parseColor("#3D6BFF"))
    }
    private val dimPaint = Paint().apply { color = Color.argb(150, 4, 4, 12) }
    private val path = Path()

    private val SOLVE_MS = 2600f     // durée de résolution
    private val FREEZE_MS = 3000f    // instant où tout se fige
    private val TEXT_MS = 3200f      // apparition du texte

    fun start() {
        cubes.clear()
        repeat(14) { cubes.add(MiniCube(delay = (it * 90L))) }
        startMs = System.currentTimeMillis()
        running = true
        textAlpha = 0f; smileyScale = 0f
        visibility = VISIBLE
        invalidate()
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (running && System.currentTimeMillis() - startMs > TEXT_MS) {
            running = false
            visibility = GONE
            onDone?.invoke()
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        if (!running) return
        val now = System.currentTimeMillis()
        val t = (now - startMs).toFloat()

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        val frozenGlobal = t >= FREEZE_MS
        for (c in cubes) {
            val ct = (t - c.delay).coerceAtLeast(0f)
            if (!frozenGlobal && !c.frozen) {
                val sec = ct / 1000f
                c.x = ((c.startX + c.driftX * sec) % 1f + 1f) % 1f
                c.y = (0.05f + c.speedY * sec + 0.03f * sin(sec * 3f + c.wobblePhase))
                if (c.y > 1.1f) c.y -= 1.15f
                c.angle = (c.angle + c.spin / 60f)
            }
            // Résolution progressive : proportion de stickers "réparés"
            val solveP = (ct / SOLVE_MS).coerceIn(0f, 1f)
            val fixedCount = (solveP * 27).toInt()
            for (k in 0 until fixedCount) {
                val idx = c.fixOrder[k]
                c.stickers[idx / 9][idx % 9] = c.target[idx / 9]
            }
            if (frozenGlobal) c.frozen = true
            drawMiniCube(canvas, c)
        }

        // Texte + smiley
        if (t >= TEXT_MS) {
            val tt = ((t - TEXT_MS) / 400f).coerceIn(0f, 1f)
            textAlpha = tt
            smileyScale = 0.6f + 0.4f * overshoot(tt)

            val cx = width / 2f
            val cy = height * 0.42f

            // Smiley
            paint.shader = null
            val r = min(width, height) * 0.16f * smileyScale
            paint.color = Color.argb((255 * tt).toInt(), 255, 214, 0)
            canvas.drawCircle(cx, cy, r, paint)
            paint.color = Color.argb((255 * tt).toInt(), 40, 30, 0)
            canvas.drawCircle(cx - r * 0.36f, cy - r * 0.25f, r * 0.11f, paint)
            canvas.drawCircle(cx + r * 0.36f, cy - r * 0.25f, r * 0.11f, paint)
            stroke.strokeWidth = r * 0.10f
            stroke.color = Color.argb((255 * tt).toInt(), 40, 30, 0)
            path.reset()
            path.addArc(cx - r * 0.52f, cy - r * 0.30f, cx + r * 0.52f, cy + r * 0.62f, 25f, 130f)
            canvas.drawPath(path, stroke)

            // Texte
            textPaint.alpha = (255 * tt).toInt()
            textPaint.textSize = min(width, height) * 0.11f
            canvas.drawText("VICTOIRE !", cx, cy + r + textPaint.textSize * 1.2f, textPaint)
            textPaint.textSize = min(width, height) * 0.035f
            textPaint.alpha = (200 * tt).toInt()
            canvas.drawText("Touche l'écran pour continuer", cx, cy + r + min(width, height) * 0.11f * 2.1f, textPaint)
        }

        invalidate()
    }

    private fun overshoot(t: Float): Float {
        val s = 2.2f
        val x = t - 1f
        return x * x * ((s + 1) * x + s) + 1f
    }

    /** Dessine un mini Rubik's isométrique (3 faces visibles, 9 stickers chacune). */
    private fun drawMiniCube(canvas: Canvas, c: MiniCube) {
        val px = c.x * width
        val py = c.y * height
        val s = c.size * width

        canvas.save()
        canvas.translate(px, py)
        canvas.rotate(c.angle)

        // Sommets isométriques (origine = centre avant du cube)
        val hx = 0.866f * s   // demi-largeur horizontale
        val hy = 0.5f * s     // demi-hauteur d'une arête inclinée

        // Points clés : T = sommet haut, L/R = coins latéraux, C = centre, B = bas
        // TOP : T(0,-s) , R(hx,-s+hy) , C(0,-s+2hy) , L(-hx,-s+hy)
        drawFace(canvas, c.stickers[0],
            0f, -s, hx, -s + hy, 0f, -s + 2 * hy, -hx, -s + hy)
        // LEFT : L(-hx,-s+hy) , C(0,-s+2hy) , Cb(0, -s+2hy + s) , Lb(-hx, -s+hy + s)
        drawFace(canvas, c.stickers[1],
            -hx, -s + hy, 0f, -s + 2 * hy, 0f, 2 * hy, -hx, hy)
        // RIGHT : C(0,-s+2hy) , R(hx,-s+hy) , Rb(hx, hy) , Cb(0, 2hy)
        drawFace(canvas, c.stickers[2],
            0f, -s + 2 * hy, hx, -s + hy, hx, hy, 0f, 2 * hy)

        canvas.restore()
    }

    /** Face en parallélogramme (p1..p4 dans l'ordre), découpée en 3x3 stickers. */
    private fun drawFace(
        canvas: Canvas, cells: IntArray,
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float
    ) {
        for (row in 0..2) for (col in 0..2) {
            val u0 = col / 3f; val u1 = (col + 1) / 3f
            val v0 = row / 3f; val v1 = (row + 1) / 3f
            fun ix(u: Float, v: Float) = x1 + (x2 - x1) * u + (x4 - x1) * v + (x3 - x2 - x4 + x1) * u * v
            fun iy(u: Float, v: Float) = y1 + (y2 - y1) * u + (y4 - y1) * v + (y3 - y2 - y4 + y1) * u * v
            path.reset()
            path.moveTo(ix(u0, v0), iy(u0, v0))
            path.lineTo(ix(u1, v0), iy(u1, v0))
            path.lineTo(ix(u1, v1), iy(u1, v1))
            path.lineTo(ix(u0, v1), iy(u0, v1))
            path.close()
            paint.color = COLORS[cells[row * 3 + col]]
            canvas.drawPath(path, paint)
            stroke.strokeWidth = 2f
            stroke.color = Color.argb(220, 10, 10, 20)
            canvas.drawPath(path, stroke)
        }
    }
}

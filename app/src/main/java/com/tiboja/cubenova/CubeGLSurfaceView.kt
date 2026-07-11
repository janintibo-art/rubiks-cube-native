package com.tiboja.cubenova

import android.content.Context
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CubeGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    val renderer: CubeRenderer
    var allowOrbit = true   // false en mode directionnel : le cube reste de face

    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0f
    private var startY = 0f

    // Modes de geste
    private var mode = MODE_NONE
    private val HALF = 1.46f          // demi-taille du cube visible (surface extérieure)
    private val TAP_THRESHOLD = 24f   // px : en dessous = simple tap

    // Résultat du picking mémorisé à l'appui (coordonnées centrées, float)
    private val invVPG = FloatArray(16)
    private var hitAxis = 0           // axe de la normale de la face touchée
    private var hitSign = 0
    private var hitGx = 0f
    private var hitGy = 0f
    private var hitGz = 0f
    private var planeVal = 0f

    init {
        setEGLContextClientVersion(2)
        renderer = CubeRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x; startY = event.y
                lastX = event.x; lastY = event.y
                mode = decideMode(event.x, event.y)
                if (mode == MODE_ORBIT && !allowOrbit) mode = MODE_NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == MODE_ORBIT) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    renderer.addDrag(dx * 0.3f, dy * 0.3f)
                }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (mode == MODE_PICK) resolveFaceDrag(event.x, event.y)
                mode = MODE_NONE
            }
        }
        return true
    }

    /** À l'appui : lance un rayon. Si on touche le cube (et rien en cours) → PICK, sinon ORBIT. */
    private fun decideMode(sx: Float, sy: Float): Int {
        if (renderer.isBusy()) return MODE_ORBIT

        val vpg = FloatArray(16)
        if (!renderer.copyVPG(vpg)) return MODE_ORBIT
        if (Matrix.invertM(invVPG, 0, vpg, 0) != true) return MODE_ORBIT

        val o = FloatArray(3)
        val d = FloatArray(3)
        buildRay(sx, sy, o, d)

        // Test rayon / boîte (slabs) pour trouver la face d'entrée
        var tmin = -Float.MAX_VALUE
        var tmax = Float.MAX_VALUE
        var entryAxis = -1
        var entrySign = 0
        for (axis in 0..2) {
            val oa = o[axis]; val da = d[axis]
            if (abs(da) < 1e-6f) {
                if (oa < -HALF || oa > HALF) return MODE_ORBIT
            } else {
                var t1 = (-HALF - oa) / da
                var t2 = (HALF - oa) / da
                var sign = -1
                if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp; sign = 1 }
                if (t1 > tmin) { tmin = t1; entryAxis = axis; entrySign = sign }
                if (t2 < tmax) tmax = t2
                if (tmin > tmax) return MODE_ORBIT
            }
        }
        if (entryAxis < 0 || tmin < 0f) return MODE_ORBIT

        // Point d'entrée → pièce touchée
        val hit = FloatArray(3)
        for (i in 0..2) hit[i] = o[i] + tmin * d[i]

        hitAxis = entryAxis
        hitSign = entrySign
        planeVal = entrySign * HALF

        // Coordonnées centrées (unités "cellule") de la pièce touchée, calées sur le réseau
        val cell = renderer.getCell()
        val half = (renderer.getSize() - 1) / 2f
        fun snapLat(worldCoord: Float): Float {
            val c = worldCoord / cell
            return (c + half).roundToInt() - half
        }
        hitGx = if (entryAxis == 0) entrySign * half else snapLat(hit[0])
        hitGy = if (entryAxis == 1) entrySign * half else snapLat(hit[1])
        hitGz = if (entryAxis == 2) entrySign * half else snapLat(hit[2])

        return MODE_PICK
    }

    /** Au relâchement : calcule l'axe de rotation depuis le glissement et lance la rotation. */
    private fun resolveFaceDrag(ex: Float, ey: Float) {
        if (hypot((ex - startX).toDouble(), (ey - startY).toDouble()) < TAP_THRESHOLD) return

        // Deux rayons (début et fin), intersectés avec le plan de la face → vecteur de glissement 3D
        val w0 = intersectPlane(startX, startY) ?: return
        val w1 = intersectPlane(ex, ey) ?: return

        val d = floatArrayOf(w1[0] - w0[0], w1[1] - w0[1], w1[2] - w0[2])
        // Retire la composante le long de la normale
        d[hitAxis] = 0f
        val len = sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2])
        if (len < 1e-4f) return
        d[0] /= len; d[1] /= len; d[2] /= len

        // Normale de la face
        val n = floatArrayOf(0f, 0f, 0f); n[hitAxis] = hitSign.toFloat()

        // Axe de rotation = n × d
        val a = floatArrayOf(
            n[1] * d[2] - n[2] * d[1],
            n[2] * d[0] - n[0] * d[2],
            n[0] * d[1] - n[1] * d[0]
        )

        // Aligne sur l'axe principal dominant
        val ax = abs(a[0]); val ay = abs(a[1]); val az = abs(a[2])
        val axisIndex: Int
        val sign: Int
        when {
            ax >= ay && ax >= az -> { axisIndex = 0; sign = if (a[0] >= 0) 1 else -1 }
            ay >= ax && ay >= az -> { axisIndex = 1; sign = if (a[1] >= 0) 1 else -1 }
            else -> { axisIndex = 2; sign = if (a[2] >= 0) 1 else -1 }
        }

        val layer = when (axisIndex) { 0 -> hitGx; 1 -> hitGy; else -> hitGz }
        renderer.enqueueMove(axisIndex, layer, sign)
    }

    /** Construit un rayon (origine + direction) en espace-grille depuis un point écran. */
    private fun buildRay(sx: Float, sy: Float, outO: FloatArray, outD: FloatArray) {
        val near = unproject(sx, sy, -1f)
        val far = unproject(sx, sy, 1f)
        for (i in 0..2) outO[i] = near[i]
        val dx = far[0] - near[0]; val dy = far[1] - near[1]; val dz = far[2] - near[2]
        val l = sqrt(dx * dx + dy * dy + dz * dz)
        outD[0] = dx / l; outD[1] = dy / l; outD[2] = dz / l
    }

    private fun intersectPlane(sx: Float, sy: Float): FloatArray? {
        val o = FloatArray(3); val d = FloatArray(3)
        buildRay(sx, sy, o, d)
        val da = d[hitAxis]
        if (abs(da) < 1e-6f) return null
        val t = (planeVal - o[hitAxis]) / da
        return floatArrayOf(o[0] + t * d[0], o[1] + t * d[1], o[2] + t * d[2])
    }

    /** Écran → espace-grille pour une profondeur NDC donnée (-1 near, +1 far). */
    private fun unproject(sx: Float, sy: Float, ndcZ: Float): FloatArray {
        val ndcX = 2f * sx / width - 1f
        val ndcY = 1f - 2f * sy / height
        val clip = floatArrayOf(ndcX, ndcY, ndcZ, 1f)
        val out = FloatArray(4)
        Matrix.multiplyMV(out, 0, invVPG, 0, clip, 0)
        val w = if (out[3] != 0f) out[3] else 1f
        return floatArrayOf(out[0] / w, out[1] / w, out[2] / w)
    }

    companion object {
        private const val MODE_NONE = 0
        private const val MODE_ORBIT = 1
        private const val MODE_PICK = 2
    }
}

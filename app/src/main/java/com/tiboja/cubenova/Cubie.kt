package com.tiboja.cubenova

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.roundToInt

/**
 * Un "cubie" d'un cube N×N×N texturé.
 *
 * Coordonnées "centrées" : chaque axe prend N valeurs { i - (N-1)/2 } pour i in 0..N-1.
 * cell = taille d'une cellule en unités monde (calculée pour garder le cube à taille
 * d'écran constante quelle que soit N).
 *
 * Atlas 3 colonnes x 2 lignes. Face -> cellule (col,row) :
 *   U(+Y)=(0,0)  D(-Y)=(1,0)  F(+Z)=(2,0)  B(-Z)=(0,1)  R(+X)=(1,1)  L(-X)=(2,1)
 */
class Cubie(
    var cx: Float, var cy: Float, var cz: Float,
    private val n: Int,
    private val cell: Float
) {
    val orientation = FloatArray(16)
    val homeX = cx; val homeY = cy; val homeZ = cz   // position d'origine (cube résolu)
    private val vertexBuffer: FloatBuffer
    private val vertexCount: Int

    private data class Face(
        val nx: Int, val ny: Int, val nz: Int,
        val rx: Int, val ry: Int, val rz: Int,
        val ux: Int, val uy: Int, val uz: Int,
        val cellCol: Int, val cellRow: Int
    )

    companion object {
        private val FACES = listOf(
            Face(0, 1, 0,  1, 0, 0,  0, 0, -1,  0, 0), // U +Y
            Face(0, -1, 0, 1, 0, 0,  0, 0, 1,   1, 0), // D -Y
            Face(0, 0, 1,  1, 0, 0,  0, 1, 0,   2, 0), // F +Z
            Face(0, 0, -1, -1, 0, 0, 0, 1, 0,   0, 1), // B -Z
            Face(1, 0, 0,  0, 0, -1, 0, 1, 0,   1, 1), // R +X
            Face(-1, 0, 0, 0, 0, 1,  0, 1, 0,   2, 1)  // L -X
        )
    }

    init {
        Matrix.setIdentityM(orientation, 0)
        val data = buildGeometry()
        vertexCount = data.size / 11
        vertexBuffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(data)
        vertexBuffer.position(0)
    }

    private fun dotC(ax: Int, ay: Int, az: Int) = cx * ax + cy * ay + cz * az

    private fun buildGeometry(): FloatArray {
        val half = (n - 1) / 2f           // valeur centrée max
        val hs = 0.495f * cell            // presque jointifs : le liseré du shader fait la séparation
        val out = ArrayList<Float>()

        for (f in FACES) {
            val outer = kotlin.math.abs(dotC(f.nx, f.ny, f.nz) - half) < 0.01f

            fun corner(sr: Int, su: Int): FloatArray = floatArrayOf(
                (f.nx + sr * f.rx + su * f.ux) * hs,
                (f.ny + sr * f.ry + su * f.uy) * hs,
                (f.nz + sr * f.rz + su * f.uz) * hs
            )
            val TL = corner(-1, 1); val TR = corner(1, 1)
            val BR = corner(1, -1); val BL = corner(-1, -1)

            var uL = 0f; var uR = 0f; var vTop = 0f; var vBot = 0f; var tex = 0f
            if (outer) {
                tex = 1f
                // indices de sticker 0..n-1 le long de R (colonne) et -U (ligne)
                val scol = (dotC(f.rx, f.ry, f.rz) + half).roundToInt()
                val srow = (-dotC(f.ux, f.uy, f.uz) + half).roundToInt()
                uL = f.cellCol / 3f + (scol.toFloat() / n) / 3f
                uR = f.cellCol / 3f + ((scol + 1f) / n) / 3f
                vTop = f.cellRow / 2f + (srow.toFloat() / n) / 2f   // GLUtils : t=0 en haut
                vBot = f.cellRow / 2f + ((srow + 1f) / n) / 2f
            }

            fun push(p: FloatArray, u: Float, v: Float, lu: Float, lv: Float) {
                out.add(p[0]); out.add(p[1]); out.add(p[2])          // position
                out.add(u); out.add(v)                              // UV atlas
                out.add(tex)                                        // 1 = face extérieure
                out.add(f.nx.toFloat()); out.add(f.ny.toFloat()); out.add(f.nz.toFloat())  // normale
                out.add(lu); out.add(lv)                            // UV locale (0..1 dans le sticker)
            }
            // 2 triangles : TL,TR,BR + TL,BR,BL
            push(TL, uL, vTop, 0f, 0f); push(TR, uR, vTop, 1f, 0f); push(BR, uR, vBot, 1f, 1f)
            push(TL, uL, vTop, 0f, 0f); push(BR, uR, vBot, 1f, 1f); push(BL, uL, vBot, 0f, 1f)
        }
        return out.toFloatArray()
    }

    fun draw(posH: Int, uvH: Int, texH: Int, normH: Int, luvH: Int) {
        val stride = 11 * 4
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(posH, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(posH)
        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(uvH, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(uvH)
        vertexBuffer.position(5)
        GLES20.glVertexAttribPointer(texH, 1, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(texH)
        vertexBuffer.position(6)
        GLES20.glVertexAttribPointer(normH, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(normH)
        vertexBuffer.position(9)
        GLES20.glVertexAttribPointer(luvH, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(luvH)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(posH)
        GLES20.glDisableVertexAttribArray(uvH)
        GLES20.glDisableVertexAttribArray(texH)
        GLES20.glDisableVertexAttribArray(normH)
        GLES20.glDisableVertexAttribArray(luvH)
    }
}

package com.example.rubikscube

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Un "cubie" = l'une des 27 petites pièces du Rubik's Cube.
 * Il connaît sa position sur la grille (x,y,z ∈ {-1,0,1}), sa propre
 * orientation (matrice qui accumule les rotations de 90°) et sa géométrie
 * colorée (les faces extérieures sont peintes, les faces internes en noir).
 */
class Cubie(var gx: Int, var gy: Int, var gz: Int, palette: IntArray) {

    val orientation = FloatArray(16)
    private val vertexBuffer: FloatBuffer
    private val vertexCount: Int

    init {
        Matrix.setIdentityM(orientation, 0)

        // palette : [front(+Z), back(-Z), right(+X), left(-X), top(+Y), bottom(-Y)]
        val black = floatArrayOf(0.04f, 0.04f, 0.05f)
        val front  = if (gz == 1)  rgb(palette[0]) else black
        val back   = if (gz == -1) rgb(palette[1]) else black
        val right  = if (gx == 1)  rgb(palette[2]) else black
        val left   = if (gx == -1) rgb(palette[3]) else black
        val top    = if (gy == 1)  rgb(palette[4]) else black
        val bottom = if (gy == -1) rgb(palette[5]) else black

        val data = buildGeometry(front, back, right, left, top, bottom)
        vertexCount = data.size / 6

        vertexBuffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data)
        vertexBuffer.position(0)
    }

    private fun rgb(color: Int): FloatArray = floatArrayOf(
        ((color shr 16) and 0xFF) / 255f,
        ((color shr 8) and 0xFF) / 255f,
        (color and 0xFF) / 255f
    )

    /** Construit 6 faces (2 triangles chacune). 6 floats par sommet : x,y,z,r,g,b. */
    private fun buildGeometry(
        f: FloatArray, bk: FloatArray, r: FloatArray,
        l: FloatArray, t: FloatArray, bo: FloatArray
    ): FloatArray {
        val h = 0.46f // demi-taille (< 0.5 → petit espace noir entre les pièces)
        val out = ArrayList<Float>()

        fun quad(
            ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float,
            cx: Float, cy: Float, cz: Float, dx: Float, dy: Float, dz: Float, c: FloatArray
        ) {
            val verts = arrayOf(
                floatArrayOf(ax, ay, az), floatArrayOf(bx, by, bz), floatArrayOf(cx, cy, cz),
                floatArrayOf(ax, ay, az), floatArrayOf(cx, cy, cz), floatArrayOf(dx, dy, dz)
            )
            for (v in verts) {
                out.add(v[0]); out.add(v[1]); out.add(v[2])
                out.add(c[0]); out.add(c[1]); out.add(c[2])
            }
        }

        // +Z (front)
        quad(-h, -h, h, h, -h, h, h, h, h, -h, h, h, f)
        // -Z (back)
        quad(h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, bk)
        // +X (right)
        quad(h, -h, h, h, -h, -h, h, h, -h, h, h, h, r)
        // -X (left)
        quad(-h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, l)
        // +Y (top)
        quad(-h, h, h, h, h, h, h, h, -h, -h, h, -h, t)
        // -Y (bottom)
        quad(-h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, bo)

        return out.toFloatArray()
    }

    fun draw(positionHandle: Int, colorHandle: Int) {
        val stride = 6 * 4
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)

        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(colorHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
}

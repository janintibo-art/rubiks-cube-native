package com.example.rubikscube

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.roundToInt

class CubeRenderer : GLSurfaceView.Renderer {

    // Un mouvement en attente : quelle face + sens (+1 / -1)
    data class Move(val face: Char, val dir: Int)

    private val moveQueue = ConcurrentLinkedQueue<Move>()
    @Volatile private var resetRequested = false

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpHandle = 0

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val global = FloatArray(16)   // rotation globale (au doigt)
    private val temp = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    private var cubies = buildCubies()

    // Rotation au doigt (deltas déposés par le thread UI, consommés ici)
    @Volatile private var dragX = 0f
    @Volatile private var dragY = 0f

    // Animation d'une face
    private var animating = false
    private var animFace = 'U'
    private var animDir = 1
    private var animAngle = 0f          // angle courant en degrés
    private val animAxis = FloatArray(3)
    private var animLayer = 0

    private val spacing = 1.0f

    private fun buildCubies(): MutableList<Cubie> {
        val list = ArrayList<Cubie>(27)
        for (x in -1..1) for (y in -1..1) for (z in -1..1)
            list.add(Cubie(x, y, z))
        return list
    }

    // Appelé depuis le thread UI
    fun enqueueMove(face: Char, dir: Int) = moveQueue.add(Move(face, dir))
    fun requestReset() { resetRequested = true }
    fun addDrag(dx: Float, dy: Float) { dragX += dx; dragY += dy }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.05f, 0.05f, 0.07f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        val vs = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SRC)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SRC)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)

        positionHandle = GLES20.glGetAttribLocation(program, "aPos")
        colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMVP")

        Matrix.setIdentityM(global, 0)
        // Petite inclinaison initiale pour voir 3 faces
        Matrix.rotateM(global, 0, -30f, 1f, 0f, 0f)
        Matrix.rotateM(global, 0, -35f, 0f, 1f, 0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projection, 0, 45f, ratio, 1f, 100f)
        Matrix.setLookAtM(view, 0, 0f, 0f, 9f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (resetRequested) {
            resetRequested = false
            moveQueue.clear()
            animating = false
            animAngle = 0f
            cubies = buildCubies()
        }

        // Applique la rotation au doigt
        if (dragX != 0f || dragY != 0f) {
            Matrix.setIdentityM(temp, 0)
            Matrix.rotateM(temp, 0, dragY, 1f, 0f, 0f)
            Matrix.rotateM(temp, 0, dragX, 0f, 1f, 0f)
            Matrix.multiplyMM(global, 0, temp, 0, global, 0)
            dragX = 0f; dragY = 0f
        }

        // Lance le prochain mouvement si libre
        if (!animating) {
            moveQueue.poll()?.let { startAnimation(it.face, it.dir) }
        }

        if (animating) advanceAnimation()

        GLES20.glUseProgram(program)

        val vp = temp // réutilise temp comme (projection * view)
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0)
        val vpGlobal = FloatArray(16)
        Matrix.multiplyMM(vpGlobal, 0, vp, 0, global, 0)

        val animRot = FloatArray(16)
        Matrix.setIdentityM(animRot, 0)
        if (animating) {
            Matrix.setRotateM(animRot, 0, animAngle, animAxis[0], animAxis[1], animAxis[2])
        }

        for (c in cubies) {
            // model = translation(grille) * orientation
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, c.gx * spacing, c.gy * spacing, c.gz * spacing)
            Matrix.multiplyMM(model, 0, model, 0, c.orientation, 0)

            // Si la pièce est dans la couche animée, applique la rotation d'animation autour de l'origine
            val inLayer = animating && layerCoord(c) == animLayer
            val world = FloatArray(16)
            if (inLayer) {
                Matrix.multiplyMM(world, 0, animRot, 0, model, 0)
            } else {
                System.arraycopy(model, 0, world, 0, 16)
            }

            Matrix.multiplyMM(mvp, 0, vpGlobal, 0, world, 0)
            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)
            c.draw(positionHandle, colorHandle)
        }
    }

    private fun axisOf(face: Char): Int = when (face) {
        'R', 'L' -> 0
        'U', 'D' -> 1
        else -> 2 // F, B
    }

    private fun layerValueOf(face: Char): Int = when (face) {
        'R', 'U', 'F' -> 1
        else -> -1 // L, D, B
    }

    private fun layerCoord(c: Cubie): Int = when (axisOf(animFace)) {
        0 -> c.gx
        1 -> c.gy
        else -> c.gz
    }

    private fun startAnimation(face: Char, dir: Int) {
        animating = true
        animFace = face
        animDir = dir
        animAngle = 0f
        animLayer = layerValueOf(face)
        animAxis[0] = 0f; animAxis[1] = 0f; animAxis[2] = 0f
        animAxis[axisOf(face)] = 1f
    }

    private fun advanceAnimation() {
        val target = 90f * animDir
        val step = 9f * animDir // ~10 frames
        animAngle += step
        if (abs(animAngle) >= 90f) {
            animAngle = target
            finishAnimation()
        }
    }

    /** Verrouille la rotation : met à jour position + orientation des pièces de la couche. */
    private fun finishAnimation() {
        val rot = FloatArray(16)
        Matrix.setRotateM(rot, 0, 90f * animDir, animAxis[0], animAxis[1], animAxis[2])

        for (c in cubies) {
            if (layerCoord(c) != animLayer) continue

            // Nouvelle position sur la grille
            val p = floatArrayOf(c.gx.toFloat(), c.gy.toFloat(), c.gz.toFloat(), 1f)
            val np = FloatArray(4)
            Matrix.multiplyMV(np, 0, rot, 0, p, 0)
            c.gx = np[0].roundToInt()
            c.gy = np[1].roundToInt()
            c.gz = np[2].roundToInt()

            // Nouvelle orientation (on empile la rotation dans la matrice de la pièce)
            val newOri = FloatArray(16)
            Matrix.multiplyMM(newOri, 0, rot, 0, c.orientation, 0)
            System.arraycopy(newOri, 0, c.orientation, 0, 16)
        }

        animating = false
        animAngle = 0f
    }

    private fun loadShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SRC = """
            uniform mat4 uMVP;
            attribute vec4 aPos;
            attribute vec3 aColor;
            varying vec3 vColor;
            void main() {
                vColor = aColor;
                gl_Position = uMVP * aPos;
            }
        """

        private const val FRAGMENT_SRC = """
            precision mediump float;
            varying vec3 vColor;
            void main() {
                gl_FragColor = vec4(vColor, 1.0);
            }
        """
    }
}

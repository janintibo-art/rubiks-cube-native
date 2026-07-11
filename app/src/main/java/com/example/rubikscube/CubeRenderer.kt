package com.example.rubikscube

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.roundToInt

class CubeRenderer(private val context: Context) : GLSurfaceView.Renderer {

    // layer = valeur centrée de la couche (peut être demi-entière pour N pair)
    data class Move(val axis: Int, val layer: Float, val dir: Int)

    private val moveQueue = ConcurrentLinkedQueue<Move>()
    @Volatile private var resetRequested = false

    @Volatile private var size = 3
    @Volatile private var pendingSize = 3
    private var cell = cellFor(3)

    private var program = 0
    private var posHandle = 0
    private var uvHandle = 0
    private var texFlagHandle = 0
    private var mvpHandle = 0
    private var samplerHandle = 0

    private var textureId = 0
    @Volatile private var pendingAtlas: String = Themes.ATLAS[Themes.DEFAULT]
    private var loadedAtlas: String? = null

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val global = FloatArray(16)
    private val temp = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    private val vpgSnapshot = FloatArray(16)
    private val vpgLock = Any()
    @Volatile private var vpgReady = false

    private var cubies = buildCubies()

    @Volatile private var dragX = 0f
    @Volatile private var dragY = 0f

    private var animating = false
    private var animAxisIndex = 1
    private var animLayer = 0f
    private var animDir = 1
    private var animAngle = 0f
    private val animAxis = FloatArray(3)

    // Le cube conserve toujours ~2.92 unités monde de large (surface à ±1.46)
    private fun cellFor(n: Int): Float = 1.46f / (n / 2f - 0.04f)

    private fun buildCubies(): MutableList<Cubie> {
        val n = size
        val half = (n - 1) / 2f
        val list = ArrayList<Cubie>()
        for (i in 0 until n) for (j in 0 until n) for (k in 0 until n) {
            // ne garder que les cubies de surface (au moins un indice à 0 ou n-1)
            val surface = i == 0 || i == n - 1 || j == 0 || j == n - 1 || k == 0 || k == n - 1
            if (!surface) continue
            list.add(Cubie(i - half, j - half, k - half, n, cell))
        }
        return list
    }

    // --- API thread UI ---
    fun enqueueMove(axis: Int, layer: Float, dir: Int) = moveQueue.add(Move(axis, layer, dir))
    fun requestReset() { resetRequested = true }
    fun setTheme(index: Int) { pendingAtlas = Themes.ATLAS[index] }
    fun setSize(n: Int) { pendingSize = n; resetRequested = true }
    fun getSize(): Int = size
    fun getCell(): Float = cell
    fun addDrag(dx: Float, dy: Float) { dragX += dx; dragY += dy }
    fun isBusy(): Boolean = animating || moveQueue.isNotEmpty()

    fun copyVPG(out: FloatArray): Boolean {
        if (!vpgReady) return false
        synchronized(vpgLock) { System.arraycopy(vpgSnapshot, 0, out, 0, 16) }
        return true
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.05f, 0.05f, 0.07f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        val vs = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SRC)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SRC)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)

        posHandle = GLES20.glGetAttribLocation(program, "aPos")
        uvHandle = GLES20.glGetAttribLocation(program, "aUV")
        texFlagHandle = GLES20.glGetAttribLocation(program, "aTex")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMVP")
        samplerHandle = GLES20.glGetUniformLocation(program, "uTexture")

        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        textureId = ids[0]
        loadedAtlas = null

        Matrix.setIdentityM(global, 0)
        Matrix.rotateM(global, 0, -30f, 1f, 0f, 0f)
        Matrix.rotateM(global, 0, -35f, 0f, 1f, 0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projection, 0, 45f, ratio, 1f, 100f)
        Matrix.setLookAtM(view, 0, 0f, 0f, 9f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    private fun loadAtlas(asset: String) {
        try {
            val bmp: Bitmap = context.assets.open(asset).use { BitmapFactory.decodeStream(it) }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
            bmp.recycle()
            loadedAtlas = asset
        } catch (e: Exception) { }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (loadedAtlas != pendingAtlas) loadAtlas(pendingAtlas)

        if (resetRequested) {
            resetRequested = false
            if (pendingSize != size) { size = pendingSize; cell = cellFor(size) }
            moveQueue.clear()
            animating = false
            animAngle = 0f
            cubies = buildCubies()
        }

        if (dragX != 0f || dragY != 0f) {
            Matrix.setIdentityM(temp, 0)
            Matrix.rotateM(temp, 0, dragY, 1f, 0f, 0f)
            Matrix.rotateM(temp, 0, dragX, 0f, 1f, 0f)
            Matrix.multiplyMM(global, 0, temp, 0, global, 0)
            dragX = 0f; dragY = 0f
        }

        if (!animating) moveQueue.poll()?.let { startAnimation(it.axis, it.layer, it.dir) }
        if (animating) advanceAnimation()

        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(samplerHandle, 0)

        val vp = FloatArray(16)
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0)
        val vpGlobal = FloatArray(16)
        Matrix.multiplyMM(vpGlobal, 0, vp, 0, global, 0)
        synchronized(vpgLock) { System.arraycopy(vpGlobal, 0, vpgSnapshot, 0, 16) }
        vpgReady = true

        val animRot = FloatArray(16)
        Matrix.setIdentityM(animRot, 0)
        if (animating) Matrix.setRotateM(animRot, 0, animAngle, animAxis[0], animAxis[1], animAxis[2])

        for (c in cubies) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, c.cx * cell, c.cy * cell, c.cz * cell)
            Matrix.multiplyMM(model, 0, model, 0, c.orientation, 0)

            val inLayer = animating && abs(coordOnAxis(c, animAxisIndex) - animLayer) < 0.01f
            val world = FloatArray(16)
            if (inLayer) Matrix.multiplyMM(world, 0, animRot, 0, model, 0)
            else System.arraycopy(model, 0, world, 0, 16)

            Matrix.multiplyMM(mvp, 0, vpGlobal, 0, world, 0)
            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)
            c.draw(posHandle, uvHandle, texFlagHandle)
        }
    }

    private fun coordOnAxis(c: Cubie, axis: Int): Float = when (axis) {
        0 -> c.cx; 1 -> c.cy; else -> c.cz
    }

    private fun startAnimation(axis: Int, layer: Float, dir: Int) {
        animating = true
        animAxisIndex = axis; animLayer = layer; animDir = dir; animAngle = 0f
        animAxis[0] = 0f; animAxis[1] = 0f; animAxis[2] = 0f
        animAxis[axis] = 1f
    }

    private fun advanceAnimation() {
        val target = 90f * animDir
        animAngle += 9f * animDir
        if (abs(animAngle) >= 90f) { animAngle = target; finishAnimation() }
    }

    private fun snap(v: Float): Float {
        val half = (size - 1) / 2f
        return (v + half).roundToInt() - half
    }

    private fun finishAnimation() {
        val rot = FloatArray(16)
        Matrix.setRotateM(rot, 0, 90f * animDir, animAxis[0], animAxis[1], animAxis[2])
        for (c in cubies) {
            if (abs(coordOnAxis(c, animAxisIndex) - animLayer) >= 0.01f) continue
            val p = floatArrayOf(c.cx, c.cy, c.cz, 1f)
            val np = FloatArray(4)
            Matrix.multiplyMV(np, 0, rot, 0, p, 0)
            c.cx = snap(np[0]); c.cy = snap(np[1]); c.cz = snap(np[2])
            val newOri = FloatArray(16)
            Matrix.multiplyMM(newOri, 0, rot, 0, c.orientation, 0)
            System.arraycopy(newOri, 0, c.orientation, 0, 16)
        }
        animating = false; animAngle = 0f
    }

    private fun loadShader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src); GLES20.glCompileShader(s)
        return s
    }

    companion object {
        private const val VERTEX_SRC = """
            uniform mat4 uMVP;
            attribute vec4 aPos;
            attribute vec2 aUV;
            attribute float aTex;
            varying vec2 vUV;
            varying float vTex;
            void main() { vUV = aUV; vTex = aTex; gl_Position = uMVP * aPos; }
        """
        private const val FRAGMENT_SRC = """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec2 vUV;
            varying float vTex;
            void main() {
                if (vTex > 0.5) gl_FragColor = texture2D(uTexture, vUV);
                else gl_FragColor = vec4(0.02, 0.02, 0.03, 1.0);
            }
        """
    }
}

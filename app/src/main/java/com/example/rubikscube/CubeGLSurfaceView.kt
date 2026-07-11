package com.example.rubikscube

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent

class CubeGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    val renderer: CubeRenderer

    private var lastX = 0f
    private var lastY = 0f

    init {
        setEGLContextClientVersion(2)
        renderer = CubeRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                lastX = event.x
                lastY = event.y
                // 0.3 = sensibilité de rotation
                renderer.addDrag(dx * 0.3f, dy * 0.3f)
            }
        }
        return true
    }
}

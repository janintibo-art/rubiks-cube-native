package com.example.rubikscube

import android.app.Activity
import android.os.Bundle
import android.widget.Button

class MainActivity : Activity() {

    private lateinit var glView: CubeGLSurfaceView
    private var clockwise = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        glView = findViewById(R.id.glView)

        val btnDir = findViewById<Button>(R.id.btnDir)
        btnDir.setOnClickListener {
            clockwise = !clockwise
            btnDir.text = if (clockwise) "Sens: horaire" else "Sens: anti-horaire"
        }

        // Boutons de face
        mapOf(
            R.id.btnU to 'U', R.id.btnD to 'D', R.id.btnL to 'L',
            R.id.btnR to 'R', R.id.btnF to 'F', R.id.btnB to 'B'
        ).forEach { (id, face) ->
            findViewById<Button>(id).setOnClickListener {
                glView.renderer.enqueueMove(face, if (clockwise) 1 else -1)
            }
        }

        findViewById<Button>(R.id.btnScramble).setOnClickListener {
            val faces = charArrayOf('U', 'D', 'L', 'R', 'F', 'B')
            repeat(20) {
                val f = faces.random()
                val d = if (Math.random() > 0.5) 1 else -1
                glView.renderer.enqueueMove(f, d)
            }
        }

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            glView.renderer.requestReset()
        }
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
    }
}

package com.example.rubikscube

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var glView: CubeGLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        glView = findViewById(R.id.glView)

        // Joystick -> rotation de la vue (vitesse modérée)
        val joystick = findViewById<JoystickView>(R.id.joystick)
        joystick.onMove = { nx, ny ->
            glView.renderer.addDrag(nx * 3.2f, ny * 3.2f)
        }

        findViewById<Button>(R.id.btnTheme).setOnClickListener { showThemeChooser() }

        findViewById<Button>(R.id.btnLevel).setOnClickListener { showLevelChooser() }

        findViewById<Button>(R.id.btnScramble).setOnClickListener { scramble() }

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            glView.renderer.requestReset()
        }
    }

    private fun scramble() {
        val n = glView.renderer.getSize()
        val half = (n - 1) / 2f
        val layers = FloatArray(n) { it - half }        // couches centrées
        val moves = when (n) { 2 -> 12; 3 -> 20; 4 -> 30; else -> 40 }
        repeat(moves) {
            val axis = (0..2).random()
            val layer = layers.random()
            glView.renderer.enqueueMove(axis, layer, if (Math.random() > 0.5) 1 else -1)
        }
    }

    private fun showLevelChooser() {
        val labels = arrayOf("Facile — 2×2", "Normal — 3×3", "Difficile — 4×4", "Extrême — 5×5")
        val sizes = intArrayOf(2, 3, 4, 5)
        AlertDialog.Builder(this)
            .setTitle("Niveau de difficulté")
            .setItems(labels) { _, which ->
                glView.renderer.setSize(sizes[which])
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showThemeChooser() {
        val grid = GridView(this).apply {
            numColumns = 2
            verticalSpacing = 20
            horizontalSpacing = 20
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#12121a"))
        }
        grid.adapter = ThemeAdapter()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Choisir un thème")
            .setView(grid)
            .setNegativeButton("Fermer", null)
            .create()

        grid.setOnItemClickListener { _, _, position, _ ->
            glView.renderer.setTheme(position)
            glView.renderer.requestReset()
            dialog.dismiss()
        }
        dialog.show()
    }

    private inner class ThemeAdapter : BaseAdapter() {
        override fun getCount() = Themes.NAMES.size
        override fun getItem(p: Int) = Themes.NAMES[p]
        override fun getItemId(p: Int) = p.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val ctx = parent!!.context
            val container = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            val img = ImageView(ctx)
            val side = (130 * resources.displayMetrics.density).toInt()
            img.layoutParams = LinearLayout.LayoutParams(side, side)
            img.scaleType = ImageView.ScaleType.CENTER_CROP
            try {
                assets.open(Themes.THUMBS[position]).use {
                    img.setImageBitmap(BitmapFactory.decodeStream(it))
                }
            } catch (_: Exception) {}

            val label = TextView(ctx).apply {
                text = Themes.NAMES[position]
                setTextColor(Color.WHITE)
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 6, 0, 0)
            }
            container.addView(img)
            container.addView(label)
            return container
        }
    }

    override fun onResume() { super.onResume(); glView.onResume() }
    override fun onPause() { super.onPause(); glView.onPause() }
}

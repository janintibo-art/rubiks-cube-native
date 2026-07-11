package com.example.rubikscube

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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

        findViewById<Button>(R.id.btnTheme).setOnClickListener { showThemeChooser() }

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
                glView.renderer.enqueueMove(faces.random(), if (Math.random() > 0.5) 1 else -1)
            }
        }

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            glView.renderer.requestReset()
        }
    }

    /** Boîte de dialogue : grille des 20 thèmes (aperçu des planches). */
    private fun showThemeChooser() {
        val grid = GridView(this).apply {
            numColumns = 3
            verticalSpacing = 16
            horizontalSpacing = 16
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
            glView.renderer.setPalette(Themes.PALETTES[position])
            dialog.dismiss()
        }
        dialog.show()
    }

    /** Adapter qui affiche une vignette par thème (image de planche ou pastilles de couleur). */
    private inner class ThemeAdapter : BaseAdapter() {
        override fun getCount() = Themes.PALETTES.size
        override fun getItem(p: Int) = Themes.PALETTES[p]
        override fun getItemId(p: Int) = p.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val ctx = parent!!.context
            val container = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            val img = ImageView(ctx)
            val side = (110 * resources.displayMetrics.density).toInt()
            img.layoutParams = LinearLayout.LayoutParams(side, side)
            img.scaleType = ImageView.ScaleType.CENTER_CROP

            val thumb = Themes.THUMBS[position]
            if (thumb != null) {
                try {
                    assets.open(thumb).use { img.setImageBitmap(BitmapFactory.decodeStream(it)) }
                } catch (e: Exception) {
                    img.setImageDrawable(swatchDrawable(position))
                }
            } else {
                img.setImageDrawable(swatchDrawable(position))
            }

            val label = TextView(ctx).apply {
                text = Themes.NAMES[position]
                setTextColor(Color.WHITE)
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, 6, 0, 0)
            }

            container.addView(img)
            container.addView(label)
            return container
        }

        /** Vignette de secours : dégradé des 6 couleurs du thème. */
        private fun swatchDrawable(position: Int): GradientDrawable {
            val pal = Themes.PALETTES[position]
            fun opaque(c: Int) = c or 0xFF000000.toInt()
            return GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(opaque(pal[4]), opaque(pal[0]), opaque(pal[2]),
                           opaque(pal[1]), opaque(pal[3]), opaque(pal[5]))
            ).apply { cornerRadius = 12f }
        }
    }

    override fun onResume() { super.onResume(); glView.onResume() }
    override fun onPause() { super.onPause(); glView.onPause() }
}

package com.example.rubikscube

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private lateinit var txtTimer: TextView
    private lateinit var txtMoves: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            updateHud()
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        glView = findViewById(R.id.glView)
        txtTimer = findViewById(R.id.txtTimer)
        txtMoves = findViewById(R.id.txtMoves)

        val startSize = intent.getIntExtra("startSize", 3)
        if (startSize in 2..5) glView.renderer.setSize(startSize)

        val joystick = findViewById<JoystickView>(R.id.joystick)
        joystick.onMove = { nx, ny -> glView.renderer.addDrag(nx * 3.2f, ny * 3.2f) }

        findViewById<Button>(R.id.btnTheme).setOnClickListener { showThemeChooser() }
        findViewById<Button>(R.id.btnLevel).setOnClickListener { showLevelChooser() }
        findViewById<Button>(R.id.btnScramble).setOnClickListener { glView.renderer.scramble() }
        findViewById<Button>(R.id.btnReset).setOnClickListener { glView.renderer.requestReset() }
    }

    private fun updateHud() {
        txtTimer.text = Stats.formatTime(glView.renderer.elapsedMs())
        txtMoves.text = "${glView.renderer.moveCount()} coups"

        val win = glView.renderer.consumeWin() ?: return
        val timeMs = win[0]; val moves = win[1].toInt(); val size = win[2].toInt()
        val report = Stats.recordWin(this, size, timeMs, moves)
        showVictory(size, timeMs, moves, report)
    }

    private fun showVictory(size: Int, timeMs: Long, moves: Int, report: Stats.WinReport) {
        val sb = StringBuilder()
        sb.append("Cube ${size}×${size} résolu !\n\n")
        sb.append("⏱ Temps : ${Stats.formatTime(timeMs)}")
        if (report.newRecordTime) sb.append("  🏆 Record !")
        sb.append("\n🔢 Coups : $moves")
        if (report.newAchievements.isNotEmpty()) {
            sb.append("\n\n✨ Succès débloqués :\n")
            report.newAchievements.forEach { sb.append("• $it\n") }
        }
        AlertDialog.Builder(this)
            .setTitle("🎉 Bravo !")
            .setMessage(sb.toString().trim())
            .setPositiveButton("Rejouer") { _, _ -> glView.renderer.scramble() }
            .setNegativeButton("Menu") { _, _ -> finish() }
            .setCancelable(true)
            .show()
    }

    private fun showLevelChooser() {
        val labels = arrayOf("Facile — 2×2", "Normal — 3×3", "Difficile — 4×4", "Extrême — 5×5")
        val sizes = intArrayOf(2, 3, 4, 5)
        AlertDialog.Builder(this)
            .setTitle("Niveau de difficulté")
            .setItems(labels) { _, which -> glView.renderer.setSize(sizes[which]) }
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
                assets.open(Themes.THUMBS[position]).use { img.setImageBitmap(BitmapFactory.decodeStream(it)) }
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

    override fun onResume() { super.onResume(); glView.onResume(); handler.post(ticker) }
    override fun onPause() { super.onPause(); glView.onPause(); handler.removeCallbacks(ticker) }
}

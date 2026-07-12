package com.tiboja.cubenova

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
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var glView: CubeGLSurfaceView
    private lateinit var txtTimer: TextView
    private lateinit var txtMoves: TextView
    private lateinit var menuPanel: View
    private lateinit var hud: View
    private lateinit var joystick: JoystickView
    private lateinit var miView: Button
    private lateinit var btnUndo: Button

    private var dailyMode = false
    private var dailySeed = 0L
    private var directional = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() { updateHud(); handler.postDelayed(this, 100) }
    }

    private fun dialog() = AlertDialog.Builder(this, R.style.NeonDialog)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        glView = findViewById(R.id.glView)
        txtTimer = findViewById(R.id.txtTimer)
        txtMoves = findViewById(R.id.txtMoves)
        menuPanel = findViewById(R.id.menuPanel)
        hud = findViewById(R.id.hud)
        joystick = findViewById(R.id.joystick)
        miView = findViewById(R.id.miView)

        Sound.init(this)
        glView.renderer.onUserMove = { Sound.move() }

        // Joystick
        joystick.onMove = { nx, ny -> glView.renderer.addDrag(nx * 3.2f, ny * 3.2f) }
        joystick.onDirection = { dx, dy ->
            if (dx != 0) glView.renderer.snapView(1, if (dx > 0) -1 else 1)   // gauche/droite → axe Y
            else glView.renderer.snapView(0, if (dy > 0) 1 else -1)           // haut/bas → axe X
        }

        // Mode de vue mémorisé
        directional = Stats.viewDirectional(this)
        applyViewMode(initial = true)

        // Menu rétractable
        findViewById<Button>(R.id.btnMenu).setOnClickListener { toggleMenu() }
        findViewById<Button>(R.id.miLevel).setOnClickListener { closeMenu(); showLevelChooser() }
        findViewById<Button>(R.id.miTheme).setOnClickListener { closeMenu(); showThemeChooser() }
        miView.setOnClickListener { toggleViewMode() }
        findViewById<Button>(R.id.miChallenges).setOnClickListener { closeMenu(); showChallenges() }
        findViewById<Button>(R.id.miDaily).setOnClickListener { closeMenu(); startDaily() }
        findViewById<Button>(R.id.miLeaderboard).setOnClickListener { closeMenu(); showLeaderboard() }
        findViewById<Button>(R.id.miAchievements).setOnClickListener { closeMenu(); showAchievements() }
        findViewById<Button>(R.id.miSettings).setOnClickListener { closeMenu(); showSettings() }
        findViewById<Button>(R.id.miGraphics).setOnClickListener {
            closeMenu(); GraphicsDialog.show(this, glView.renderer)
        }

        // Réglages graphiques persistés
        Stats.applyGfx(this, glView.renderer)

        // Actions
        btnUndo = findViewById(R.id.btnUndo)
        btnUndo.setOnClickListener { glView.renderer.undo() }

        val btnScramble = findViewById<Button>(R.id.btnScramble)
        btnScramble.setOnClickListener {
            if (dailyMode) glView.renderer.requestChallenge(3, dailySeed) else glView.renderer.scramble()
        }
        val btnReset = findViewById<Button>(R.id.btnReset)
        btnReset.setOnClickListener { confirmReset() }

        // Appui long : rappelle le rôle de chaque icône
        fun tip(v: View, label: String) {
            v.setOnLongClickListener {
                Toast.makeText(this, label, Toast.LENGTH_SHORT).show(); true
            }
        }
        tip(btnUndo, "Annuler le dernier coup")
        tip(btnScramble, "Mélanger")
        tip(btnReset, "Réinitialiser")

        // Aperçu du cube résolu : maintenir 👁 affiche les 6 faces cibles
        val peekPanel = findViewById<View>(R.id.peekPanel)
        val peekImage = findViewById<ImageView>(R.id.peekImage)
        findViewById<Button>(R.id.btnPeek).setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    try {
                        assets.open(Themes.ATLAS[glView.renderer.currentTheme]).use {
                            peekImage.setImageBitmap(BitmapFactory.decodeStream(it))
                        }
                        peekPanel.visibility = View.VISIBLE
                    } catch (_: Exception) {}
                    true
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    peekPanel.visibility = View.GONE
                    v.performClick()
                    true
                }
                else -> true
            }
        }

        // Lancement
        if (intent.getBooleanExtra("daily", false)) {
            dailyMode = true
            dailySeed = Stats.dailySeed()
            glView.renderer.requestChallenge(3, dailySeed)
        } else {
            val startSize = intent.getIntExtra("startSize", 3)
            if (startSize in 2..5) glView.renderer.setSize(startSize)
        }

        // Aide (une seule fois)
        if (!Stats.isHelpSeen(this)) {
            Stats.setHelpSeen(this)
            dialog()
                .setTitle("Comment jouer")
                .setMessage(
                    "• Glisse sur le cube pour tourner une face.\n" +
                    "• Joystick pour pivoter la vue.\n" +
                    "• Menu ☰ : Niveau, Thème, mode de Vue, Défis, Classement, Succès.\n\n" +
                    "Mode Vue :\n" +
                    "  – Libre : rotation fluide.\n" +
                    "  – Directionnel : le cube reste de face et bascule face par face.\n\n" +
                    "Mélange, puis remets l'image en ordre !"
                )
                .setPositiveButton("C'est parti !", null)
                .show()
        }
    }

    // ---------- Menu rétractable ----------
    private fun toggleMenu() {
        val open = menuPanel.visibility != View.VISIBLE
        menuPanel.visibility = if (open) View.VISIBLE else View.GONE
        hud.visibility = if (open) View.GONE else View.VISIBLE   // évite le chevauchement
    }
    private fun closeMenu() { menuPanel.visibility = View.GONE; hud.visibility = View.VISIBLE }

    private fun startDaily() {
        dailyMode = true
        dailySeed = Stats.dailySeed()
        glView.renderer.requestChallenge(3, dailySeed)
    }

    // ---------- Mode de vue ----------
    private fun toggleViewMode() {
        directional = !directional
        Stats.setViewDirectional(this, directional)
        applyViewMode(initial = false)
    }
    private fun applyViewMode(initial: Boolean) {
        joystick.directional = directional
        glView.allowOrbit = !directional
        miView.text = if (directional) "🧭  Vue : Directionnel" else "🧭  Vue : Libre"
        if (directional) glView.renderer.setViewFlat() else if (!initial) glView.renderer.setViewIso()
    }

    private var lastFacesChecked = 0

    private fun updateHud() {
        txtTimer.text = Stats.formatTime(glView.renderer.elapsedMs())
        txtMoves.text = "${glView.renderer.moveCount()} coups"

        // Succès "N faces complétées" en temps réel
        val faces = glView.renderer.maxFacesReached
        if (faces > lastFacesChecked) {
            lastFacesChecked = faces
            val newly = Stats.recordFaces(this, faces)
            if (newly.isNotEmpty()) {
                Toast.makeText(this, "⭐ Succès : ${newly.last()}", Toast.LENGTH_SHORT).show()
            }
        }

        // Bouton Annuler : actif seulement s'il y a un coup à défaire
        val can = glView.renderer.canUndo()
        if (btnUndo.isEnabled != can) {
            btnUndo.isEnabled = can
            btnUndo.alpha = if (can) 1f else 0.4f
        }

        val win = glView.renderer.consumeWin() ?: return
        Sound.win()
        val timeMs = win[0]; val moves = win[1].toInt(); val size = win[2].toInt()
        val isDaily = dailyMode && size == 3
        val report = Stats.recordWin(this, size, timeMs, moves, isDaily)

        // Animation de victoire, puis dialogue de score au tap
        val victory = findViewById<VictoryView>(R.id.victoryView)
        victory.onDone = { showVictory(size, timeMs, moves, report, isDaily) }
        victory.start()
    }

    private fun showVictory(size: Int, timeMs: Long, moves: Int, report: Stats.WinReport, isDaily: Boolean) {
        val sb = StringBuilder()
        if (isDaily) sb.append("🗓 Défi du jour réussi !\n\n")
        else sb.append("Cube ${size}×${size} résolu !\n\n")
        sb.append("⏱ Temps : ${Stats.formatTime(timeMs)}")
        if (report.newRecordTime || report.dailyRecord) sb.append("  🏆 Record !")
        sb.append("\n🔢 Coups : $moves")
        sb.append("\n\n⚡ +${report.xpGained} XP   💎 +${report.gemsGained}")
        if (report.streak > 0) {
            sb.append("\n\n🔥 Série : ${report.streak} jour${if (report.streak > 1) "s" else ""}")
            if (report.streakBonus > 0) sb.append("   (+${report.streakBonus} 💎 bonus)")
        }
        if (report.leveledUp) sb.append("\n🎖 Niveau ${Stats.level(this)} atteint !")
        if (report.newChallenges.isNotEmpty()) {
            sb.append("\n\n🎯 Défis réussis :\n")
            report.newChallenges.forEach { sb.append("• $it\n") }
        }
        if (report.newAchievements.isNotEmpty()) {
            sb.append("\n✨ Succès débloqués :\n")
            report.newAchievements.forEach { sb.append("• $it\n") }
        }
        dialog()
            .setTitle("🎉 Bravo !")
            .setMessage(sb.toString().trim())
            .setPositiveButton("Rejouer") { _, _ ->
                if (dailyMode) glView.renderer.requestChallenge(3, dailySeed) else glView.renderer.scramble()
            }
            .setNegativeButton("Menu") { _, _ -> finish() }
            .setCancelable(true)
            .show()
    }

    private fun showLevelChooser() {
        val labels = arrayOf("Facile — 2×2", "Normal — 3×3", "Difficile — 4×4", "Extrême — 5×5")
        val sizes = intArrayOf(2, 3, 4, 5)
        dialog()
            .setTitle("Niveau de difficulté")
            .setItems(labels) { _, which ->
                dailyMode = false
                glView.renderer.setSize(sizes[which])
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showChallenges() {
        val sb = StringBuilder()
        for ((id, label) in Stats.CHALLENGES) {
            sb.append(if (Stats.challengeDone(this, id)) "✅ " else "⬜ ").append(label).append("\n")
        }
        dialog().setTitle("🎯 Défis").setMessage(sb.toString().trim())
            .setPositiveButton("OK", null).show()
    }

    private fun showLeaderboard() {
        val names = mapOf(2 to "Facile 2×2", 3 to "Normal 3×3", 4 to "Difficile 4×4", 5 to "Extrême 5×5")
        val sb = StringBuilder()
        for (n in 2..5) {
            sb.append("${names[n]}\n   ⏱ ${Stats.formatTime(Stats.bestTime(this, n))}")
            val m = Stats.bestMoves(this, n)
            sb.append("   |   🔢 ${if (m < 0) "—" else m}\n\n")
        }
        dialog().setTitle("🏆 Classement").setMessage(sb.toString().trim())
            .setPositiveButton("OK", null).show()
    }

    private fun showAchievements() {
        val sb = StringBuilder()
        for ((id, label) in Stats.ACHIEVEMENTS) {
            sb.append(if (Stats.isUnlocked(this, id)) "✅ " else "🔒 ").append(label).append("\n")
        }
        dialog().setTitle("⭐ Succès").setMessage(sb.toString().trim())
            .setPositiveButton("OK", null).show()
    }

    private fun confirmReset() {
        // Partie en cours = chrono lancé et cube non résolu
        if (!glView.renderer.isTiming()) {
            glView.renderer.requestReset()
            return
        }
        dialog()
            .setTitle("Réinitialiser ?")
            .setMessage("La partie en cours sera abandonnée (temps et coups perdus). Continuer ?")
            .setPositiveButton("Réinitialiser") { _, _ -> glView.renderer.requestReset() }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showSettings() {
        SettingsDialog.show(this)
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
        val d = dialog().setTitle("Thèmes  (💎 ${Stats.gems(this)})").setView(grid)
            .setNegativeButton("Fermer", null).create()
        grid.setOnItemClickListener { _, _, position, _ ->
            if (Stats.themeUnlocked(this, position)) {
                glView.renderer.setTheme(position)
                glView.renderer.requestReset()
                d.dismiss()
            } else {
                confirmBuyTheme(position, d)
            }
        }
        d.show()
    }

    private fun confirmBuyTheme(position: Int, parent: android.app.AlertDialog) {
        val gems = Stats.gems(this)
        if (gems < Stats.THEME_PRICE) {
            dialog()
                .setTitle("💎 Gemmes insuffisantes")
                .setMessage("Ce thème coûte ${Stats.THEME_PRICE} 💎 et tu en as $gems.\n\n" +
                        "Gagne des gemmes en résolvant des cubes et le Défi du jour !")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        dialog()
            .setTitle("Débloquer « ${Themes.NAMES[position]} » ?")
            .setMessage("Prix : ${Stats.THEME_PRICE} 💎   (tu as $gems 💎)")
            .setPositiveButton("Débloquer") { _, _ ->
                if (Stats.buyTheme(this, position)) {
                    Sound.win()
                    glView.renderer.setTheme(position)
                    glView.renderer.requestReset()
                    parent.dismiss()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
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
                val locked = !Stats.themeUnlocked(this@MainActivity, position)
                text = if (locked) "🔒 ${Themes.NAMES[position]} — ${Stats.THEME_PRICE}💎"
                       else Themes.NAMES[position]
                setTextColor(if (locked) Color.parseColor("#9999CC") else Color.WHITE)
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 6, 0, 0)
            }
            if (!Stats.themeUnlocked(this@MainActivity, position)) img.alpha = 0.35f
            container.addView(img)
            container.addView(label)
            return container
        }
    }

    override fun onResume() {
        super.onResume(); glView.onResume(); handler.post(ticker)
        Music.start(this)
    }

    override fun onPause() {
        super.onPause(); glView.onPause(); handler.removeCallbacks(ticker)
        Music.pause()
    }
}

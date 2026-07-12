package com.tiboja.cubenova

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import kotlin.math.min

/**
 * Écran d'accueil CubeNova. On affiche l'image de présentation en fond (fitCenter)
 * et on superpose des zones tactiles transparentes sur les boutons dessinés.
 * Les positions sont exprimées en fractions de l'image, donc ça reste aligné
 * quelle que soit la taille de l'écran.
 */
class MenuActivity : Activity() {

    // Zone cliquable : left, top, right, bottom en fraction de l'image (0..1)
    private data class Hotspot(val l: Float, val t: Float, val r: Float, val b: Float, val action: () -> Unit)

    private lateinit var root: FrameLayout
    private lateinit var bg: ImageView
    private val hotspots = ArrayList<Hotspot>()
    private val views = ArrayList<View>()

    // Barre de stats réelle (Niveau / XP / Gemmes)
    private lateinit var statsBar: LinearLayout
    private lateinit var txtLevel: TextView
    private lateinit var xpBar: ProgressBar
    private lateinit var txtXp: TextView
    private lateinit var txtGems: TextView
    private val STATS_RECT = floatArrayOf(0.05f, 0.905f, 0.95f, 0.977f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        bg = ImageView(this)
        bg.scaleType = ImageView.ScaleType.FIT_CENTER
        bg.setImageResource(R.drawable.menu_bg)
        root.addView(bg, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContentView(root)

        defineHotspots()

        // Crée une vue transparente cliquable par zone
        for (h in hotspots) {
            val v = View(this)
            v.setOnClickListener { h.action() }
            root.addView(v, FrameLayout.LayoutParams(1, 1))
            views.add(v)
        }

        buildStatsBar()

        root.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() { layoutHotspots() }
            })
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
        Sound.init(this)
        Music.start(this)
    }

    override fun onPause() {
        super.onPause()
        Music.pause()
    }

    private fun buildStatsBar() {
        val cyan = Color.parseColor("#5EE7FF")
        statsBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.panel_bg)
            setPadding(28, 14, 28, 14)
        }
        txtLevel = TextView(this).apply {
            setTextColor(cyan); textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val mid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.leftMargin = 24; lp.rightMargin = 24
            layoutParams = lp
        }
        xpBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = Stats.XP_PER_LEVEL.toInt()
        }
        txtXp = TextView(this).apply { setTextColor(Color.parseColor("#AFAFE0")); textSize = 12f }
        mid.addView(xpBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        mid.addView(txtXp)
        txtGems = TextView(this).apply {
            setTextColor(Color.parseColor("#B388FF")); textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        statsBar.addView(txtLevel)
        statsBar.addView(mid)
        statsBar.addView(txtGems)
        root.addView(statsBar, FrameLayout.LayoutParams(1, 1))
        refreshStats()
    }

    private fun refreshStats() {
        if (!::txtLevel.isInitialized) return
        txtLevel.text = "⭐ Niv. ${Stats.level(this)}"
        xpBar.progress = Stats.xpInLevel(this).toInt()
        val s = Stats.streak(this)
        txtXp.text = if (s > 0 && Stats.streakAlive(this))
            "${Stats.xpInLevel(this)} / ${Stats.XP_PER_LEVEL} XP   🔥 $s j"
        else
            "${Stats.xpInLevel(this)} / ${Stats.XP_PER_LEVEL} XP"
        txtGems.text = "💎 ${Stats.gems(this)}"
    }

    private fun defineHotspots() {
        // Colonne gauche
        hotspots.add(Hotspot(0.01f, 0.545f, 0.17f, 0.625f) { showChallenges() })       // Défis
        hotspots.add(Hotspot(0.01f, 0.632f, 0.17f, 0.712f) { showLeaderboard() })      // Classement
        hotspots.add(Hotspot(0.01f, 0.715f, 0.17f, 0.795f) { showAchievements() })     // Succès
        // Colonne droite
        hotspots.add(Hotspot(0.83f, 0.545f, 0.99f, 0.625f) { chooseMode() })          // Modes
        hotspots.add(Hotspot(0.83f, 0.632f, 0.99f, 0.712f) { play(null) })            // Entraînement
        hotspots.add(Hotspot(0.83f, 0.715f, 0.99f, 0.795f) { openSettings() })        // Paramètres
        // Boutons centraux
        hotspots.add(Hotspot(0.24f, 0.783f, 0.76f, 0.856f) { play(null) })            // JOUER
        hotspots.add(Hotspot(0.30f, 0.858f, 0.70f, 0.903f) { showDaily() }) // Défis du jour
    }

    private fun layoutHotspots() {
        val vw = bg.width; val vh = bg.height
        val dr = bg.drawable ?: return
        if (vw == 0 || vh == 0) return
        val iw = dr.intrinsicWidth.toFloat(); val ih = dr.intrinsicHeight.toFloat()
        val scale = min(vw / iw, vh / ih)
        val dw = iw * scale; val dh = ih * scale
        val ox = (vw - dw) / 2f; val oy = (vh - dh) / 2f
        for (i in hotspots.indices) {
            val h = hotspots[i]
            val lp = views[i].layoutParams as FrameLayout.LayoutParams
            lp.leftMargin = (ox + h.l * dw).toInt()
            lp.topMargin = (oy + h.t * dh).toInt()
            lp.width = ((h.r - h.l) * dw).toInt()
            lp.height = ((h.b - h.t) * dh).toInt()
            views[i].layoutParams = lp
        }

        // Barre de stats sur la zone libérée en bas
        if (::statsBar.isInitialized) {
            val lp = statsBar.layoutParams as FrameLayout.LayoutParams
            lp.leftMargin = (ox + STATS_RECT[0] * dw).toInt()
            lp.topMargin = (oy + STATS_RECT[1] * dh).toInt()
            lp.width = ((STATS_RECT[2] - STATS_RECT[0]) * dw).toInt()
            lp.height = ((STATS_RECT[3] - STATS_RECT[1]) * dh).toInt()
            statsBar.layoutParams = lp
        }
    }

    private fun play(size: Int?) {
        val i = Intent(this, MainActivity::class.java)
        if (size != null) i.putExtra("startSize", size)
        startActivity(i)
    }

    private fun chooseMode() {
        val labels = arrayOf("Facile — 2×2", "Normal — 3×3", "Difficile — 4×4", "Extrême — 5×5")
        val sizes = intArrayOf(2, 3, 4, 5)
        AlertDialog.Builder(this, R.style.NeonDialog)
            .setTitle("Modes")
            .setItems(labels) { _, which -> play(sizes[which]) }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun openSettings() {
        SettingsDialog.show(this)
    }

    private fun showChallenges() {
        val sb = StringBuilder()
        for ((id, label) in Stats.CHALLENGES) {
            sb.append(if (Stats.challengeDone(this, id)) "✅ " else "⬜ ")
            sb.append(label).append("\n")
        }
        AlertDialog.Builder(this, R.style.NeonDialog)
            .setTitle("🎯 Défis")
            .setMessage(sb.toString().trim())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDaily() {
        val done = Stats.dailyDone(this)
        val best = Stats.dailyBestTime(this)
        val moves = Stats.dailyBestMoves(this)
        val streak = Stats.streak(this)
        val alive = Stats.streakAlive(this)
        val sb = StringBuilder()
        sb.append("Un mélange 3×3 identique pour tous, chaque jour.\n\n")

        if (streak > 0 && alive) {
            sb.append("🔥 Série en cours : $streak jour${if (streak > 1) "s" else ""}\n")
            if (!done) {
                sb.append("Relève le défi aujourd'hui pour la prolonger !\n")
                sb.append("Bonus si tu réussis : +${Stats.streakBonus(streak + 1)} 💎\n")
            }
        } else if (streak > 0) {
            sb.append("💔 Série interrompue (record : ${Stats.bestStreak(this)} jours)\n")
            sb.append("Réussis le défi du jour pour repartir !\n")
        } else {
            sb.append("Réussis le défi chaque jour pour bâtir une série 🔥\n")
        }
        sb.append("\n")

        if (done) {
            sb.append("✅ Réussi aujourd'hui !\n")
            sb.append("⏱ Ton meilleur temps : ${Stats.formatTime(best)}\n")
            sb.append("🔢 Coups : ${if (moves < 0) "—" else moves}")
        } else {
            sb.append("Pas encore résolu aujourd'hui.")
        }

        AlertDialog.Builder(this, R.style.NeonDialog)
            .setTitle("🗓 Défi du jour")
            .setMessage(sb.toString().trim())
            .setPositiveButton(if (done) "Rejouer" else "Jouer") { _, _ ->
                val i = Intent(this, MainActivity::class.java)
                i.putExtra("daily", true)
                startActivity(i)
            }
            .setNegativeButton("Fermer", null)
            .show()
    }

    private fun showLeaderboard() {
        val sb = StringBuilder()
        val names = mapOf(2 to "Facile 2×2", 3 to "Normal 3×3", 4 to "Difficile 4×4", 5 to "Extrême 5×5")
        for (n in 2..5) {
            val t = Stats.bestTime(this, n)
            val m = Stats.bestMoves(this, n)
            sb.append("${names[n]}\n")
            sb.append("   Meilleur temps : ${Stats.formatTime(t)}")
            sb.append("   |   Coups : ${if (m < 0) "—" else m}\n\n")
        }
        AlertDialog.Builder(this, R.style.NeonDialog)
            .setTitle("🏆 Classement")
            .setMessage(sb.toString().trim())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAchievements() {
        val sb = StringBuilder()
        for ((id, label) in Stats.ACHIEVEMENTS) {
            sb.append(if (Stats.isUnlocked(this, id)) "✅ " else "🔒 ")
            sb.append(label).append("\n")
        }
        AlertDialog.Builder(this, R.style.NeonDialog)
            .setTitle("⭐ Succès")
            .setMessage(sb.toString().trim())
            .setPositiveButton("OK", null)
            .show()
    }
}

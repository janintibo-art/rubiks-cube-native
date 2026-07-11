package com.example.rubikscube

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
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

        root.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() { layoutHotspots() }
            })
    }

    private fun defineHotspots() {
        // Colonne gauche
        hotspots.add(Hotspot(0.01f, 0.545f, 0.17f, 0.625f) { soon("Défis") })
        hotspots.add(Hotspot(0.01f, 0.632f, 0.17f, 0.712f) { soon("Classement") })
        hotspots.add(Hotspot(0.01f, 0.715f, 0.17f, 0.795f) { soon("Succès") })
        // Colonne droite
        hotspots.add(Hotspot(0.83f, 0.545f, 0.99f, 0.625f) { chooseMode() })          // Modes
        hotspots.add(Hotspot(0.83f, 0.632f, 0.99f, 0.712f) { play(null) })            // Entraînement
        hotspots.add(Hotspot(0.83f, 0.715f, 0.99f, 0.795f) { openSettings() })        // Paramètres
        // Boutons centraux
        hotspots.add(Hotspot(0.24f, 0.783f, 0.76f, 0.856f) { play(null) })            // JOUER
        hotspots.add(Hotspot(0.30f, 0.858f, 0.70f, 0.903f) { soon("Défis du jour") }) // Défis du jour
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
    }

    private fun play(size: Int?) {
        val i = Intent(this, MainActivity::class.java)
        if (size != null) i.putExtra("startSize", size)
        startActivity(i)
    }

    private fun chooseMode() {
        val labels = arrayOf("Facile — 2×2", "Normal — 3×3", "Difficile — 4×4", "Extrême — 5×5")
        val sizes = intArrayOf(2, 3, 4, 5)
        AlertDialog.Builder(this)
            .setTitle("Modes")
            .setItems(labels) { _, which -> play(sizes[which]) }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun openSettings() {
        AlertDialog.Builder(this)
            .setTitle("Paramètres")
            .setMessage("CubeNova\nVersion 1.0\n\nLe thème et le niveau se choisissent aussi en jeu (boutons 🎨 et 🎚).")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun soon(name: String) {
        Toast.makeText(this, "$name — bientôt disponible 🚧", Toast.LENGTH_SHORT).show()
    }
}

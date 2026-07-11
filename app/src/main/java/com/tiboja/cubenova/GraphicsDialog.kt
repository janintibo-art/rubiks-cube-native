package com.tiboja.cubenova

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

/**
 * Réglages graphiques : curseurs appliqués EN DIRECT sur le cube,
 * + bouton "Réglages d'usine". Annuler restaure l'état d'avant ouverture.
 */
object GraphicsDialog {

    private val LABELS = arrayOf("☀ Luminosité", "✨ Brillance", "🪞 Reflets", "◧ Relief des bords", "🌌 Halo de fond")

    fun show(act: Activity, renderer: CubeRenderer) {
        val ctx = act
        val cyan = Color.parseColor("#5EE7FF")
        val txt = Color.parseColor("#EAEAFF")

        // Valeurs à l'ouverture (pour Annuler)
        val before = IntArray(Stats.GFX_KEYS.size) { Stats.gfx(ctx, Stats.GFX_KEYS[it]) }

        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
        }
        val bars = ArrayList<SeekBar>()

        for (i in Stats.GFX_KEYS.indices) {
            val label = TextView(ctx).apply {
                setTextColor(cyan); textSize = 14f
                setPadding(0, if (i == 0) 4 else 20, 0, 2)
                text = LABELS[i]
            }
            val bar = SeekBar(ctx).apply {
                max = 100
                progress = Stats.gfx(ctx, Stats.GFX_KEYS[i])
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                        if (!fromUser) return
                        Stats.setGfx(ctx, Stats.GFX_KEYS[i], p)
                        Stats.applyGfx(ctx, renderer)   // aperçu en direct
                    }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
            }
            bars.add(bar)
            content.addView(label)
            content.addView(bar, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        val hint = TextView(ctx).apply {
            setTextColor(Color.parseColor("#8888B8")); textSize = 12f
            setPadding(0, 18, 0, 0)
            text = "Les curseurs s'appliquent en direct sur le cube. 50 % = réglage d'origine."
        }
        content.addView(hint)

        val scroll = ScrollView(ctx).apply { addView(content) }

        AlertDialog.Builder(act, R.style.NeonDialog)
            .setTitle("🎛 Graphismes")
            .setView(scroll)
            .setPositiveButton("OK", null)
            .setNeutralButton("Réglages d'usine") { _, _ ->
                Stats.resetGfx(ctx)
                Stats.applyGfx(ctx, renderer)
            }
            .setNegativeButton("Annuler") { _, _ ->
                for (i in Stats.GFX_KEYS.indices) Stats.setGfx(ctx, Stats.GFX_KEYS[i], before[i])
                Stats.applyGfx(ctx, renderer)
            }
            .show()
    }
}

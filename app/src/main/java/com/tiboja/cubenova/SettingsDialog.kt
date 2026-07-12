package com.tiboja.cubenova

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * Fenêtre Paramètres partagée : Son, Vibration, Musique (+ volume, + piste).
 */
object SettingsDialog {

    fun show(act: Activity) {
        val ctx = act
        val cyan = Color.parseColor("#5EE7FF")
        val txt = Color.parseColor("#EAEAFF")

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        fun check(label: String, initial: Boolean): CheckBox =
            CheckBox(ctx).apply {
                text = label
                isChecked = initial
                setTextColor(txt)
                textSize = 16f
            }

        val cbSound = check("🔊  Effets sonores", Stats.soundOn(ctx))
        val cbVibrate = check("📳  Vibration", Stats.vibrateOn(ctx))
        val cbMusic = check("🎵  Musique de fond", Stats.musicOn(ctx))

        root.addView(cbSound)
        root.addView(cbVibrate)
        root.addView(cbMusic)

        // Volume musique
        val volLabel = TextView(ctx).apply {
            setTextColor(cyan); textSize = 14f
            setPadding(0, 18, 0, 4)
            text = "Volume : ${Stats.musicVolume(ctx)} %"
        }
        val vol = SeekBar(ctx).apply {
            max = 100
            progress = Stats.musicVolume(ctx)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    volLabel.text = "Volume : $p %"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        root.addView(volLabel)
        root.addView(vol, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Choix de piste
        val trackLabel = TextView(ctx).apply {
            setTextColor(cyan); textSize = 14f
            setPadding(0, 18, 0, 6)
            text = "Piste"
        }
        root.addView(trackLabel)

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val trackBtns = ArrayList<TextView>()
        fun paint(selected: Int) {
            for (i in trackBtns.indices) {
                trackBtns[i].setBackgroundResource(R.drawable.btn_neon)
                trackBtns[i].alpha = if (i == selected) 1f else 0.45f
            }
        }
        for (i in Music.TRACKS.indices) {
            val b = TextView(ctx).apply {
                text = Music.TRACK_NAMES[i]
                setTextColor(txt)
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 22, 0, 22)
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginEnd = 10
                layoutParams = lp
                setOnClickListener {
                    Music.selectTrack(ctx, i)
                    paint(i)
                }
            }
            trackBtns.add(b)
            row.addView(b)
        }
        paint(Music.currentTrack())
        root.addView(row)

        // Version du build (pour vérifier que l'APK installé est bien le dernier)
        val version = TextView(ctx).apply {
            setTextColor(Color.parseColor("#8888B8")); textSize = 11f
            setPadding(0, 20, 0, 0)
            val v = try {
                ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
            } catch (_: Exception) { "?" }
            text = "CubeNova v$v"
        }
        root.addView(version)

        AlertDialog.Builder(act, R.style.NeonDialog)
            .setTitle("⚙ Paramètres")
            .setView(root)
            .setPositiveButton("Enregistrer") { _, _ ->
                Stats.setSoundOn(ctx, cbSound.isChecked)
                Stats.setVibrateOn(ctx, cbVibrate.isChecked)
                Stats.setMusicOn(ctx, cbMusic.isChecked)
                Stats.setMusicVolume(ctx, vol.progress)
                Sound.refresh(ctx)
                Music.refresh(ctx)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}

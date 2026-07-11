package com.tiboja.cubenova

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sons générés par le code (aucun fichier nécessaire) + vibrations.
 * - clic court à chaque rotation
 * - petit jingle à la victoire
 * Les fichiers WAV sont synthétisés dans le cache puis joués via SoundPool
 * (faible latence, superposition possible). Prêt à recevoir de vrais fichiers plus tard.
 */
object Sound {
    private var pool: SoundPool? = null
    private var clickId = 0
    private var winId = 0
    private var vibrator: Vibrator? = null

    @Volatile var soundOn = true
    @Volatile var vibrateOn = true

    private const val RATE = 22050

    fun init(ctx: Context) {
        refresh(ctx)
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
        try {
            clickId = pool!!.load(makeClick(ctx).absolutePath, 1)
            winId = pool!!.load(makeWin(ctx).absolutePath, 1)
        } catch (_: Exception) {}
        @Suppress("DEPRECATION")
        vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun refresh(ctx: Context) {
        soundOn = Stats.soundOn(ctx)
        vibrateOn = Stats.vibrateOn(ctx)
    }

    fun move() {
        if (soundOn) pool?.play(clickId, 0.45f, 0.45f, 1, 0, 1f)
        if (vibrateOn) vibrate(12)
    }

    fun win() {
        if (soundOn) pool?.play(winId, 0.85f, 0.85f, 1, 0, 1f)
        if (vibrateOn) vibratePattern(longArrayOf(0, 30, 50, 70))
    }

    private fun vibrate(ms: Long) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") v.vibrate(ms)
    }

    private fun vibratePattern(p: LongArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(VibrationEffect.createWaveform(p, -1))
        else @Suppress("DEPRECATION") v.vibrate(p, -1)
    }

    // ---------- Synthèse WAV ----------
    private fun makeClick(ctx: Context): File {
        val n = RATE * 40 / 1000  // 40 ms
        val data = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val env = exp(-t * 55.0)                 // décroissance rapide
            val s = sin(2 * PI * 1150 * t) * 0.5 + sin(2 * PI * 2300 * t) * 0.2
            data[i] = (s * env * 32767).toInt().coerceIn(-32767, 32767).toShort()
        }
        return writeWav(ctx, "click.wav", data)
    }

    private fun makeWin(ctx: Context): File {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.5) // Do Mi Sol Do
        val noteMs = 130
        val nn = RATE * noteMs / 1000
        val data = ShortArray(nn * notes.size)
        var idx = 0
        for (f in notes) {
            for (i in 0 until nn) {
                val t = i.toDouble() / RATE
                val env = (exp(-t * 6.0)) * (1 - exp(-t * 120.0)) // attaque douce + tenue
                val s = (sin(2 * PI * f * t) * 0.6 + sin(2 * PI * f * 2 * t) * 0.15)
                data[idx++] = (s * env * 32767).toInt().coerceIn(-32767, 32767).toShort()
            }
        }
        return writeWav(ctx, "win.wav", data)
    }

    private fun writeWav(ctx: Context, name: String, samples: ShortArray): File {
        val f = File(ctx.cacheDir, name)
        val byteRate = RATE * 2
        val dataSize = samples.size * 2
        FileOutputStream(f).use { out ->
            fun wI(v: Int) { out.write(v and 0xff); out.write((v shr 8) and 0xff); out.write((v shr 16) and 0xff); out.write((v shr 24) and 0xff) }
            fun wS(v: Int) { out.write(v and 0xff); out.write((v shr 8) and 0xff) }
            out.write("RIFF".toByteArray()); wI(36 + dataSize); out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray()); wI(16); wS(1); wS(1)   // PCM, mono
            wI(RATE); wI(byteRate); wS(2); wS(16)
            out.write("data".toByteArray()); wI(dataSize)
            for (s in samples) wS(s.toInt())
        }
        return f
    }
}

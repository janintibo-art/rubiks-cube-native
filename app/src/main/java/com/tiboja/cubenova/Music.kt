package com.tiboja.cubenova

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Musique de fond : lecture en boucle sur une playlist (assets/music),
 * enchaînement automatique, pause quand l'app passe en arrière-plan.
 */
object Music {

    val TRACKS = arrayOf("music/track_1.mp3", "music/track_2.mp3", "music/track_3.mp3")
    val TRACK_NAMES = arrayOf("Piste 1", "Piste 2", "Piste 3")

    private var player: MediaPlayer? = null
    private var current = 0
    private var prepared = false
    @Volatile private var enabled = true
    private var volume = 0.5f

    fun start(ctx: Context) {
        enabled = Stats.musicOn(ctx)
        volume = Stats.musicVolume(ctx) / 100f
        if (!enabled) { stop(); return }
        if (player != null) { resume(); return }
        current = Stats.musicTrack(ctx).coerceIn(0, TRACKS.size - 1)
        play(ctx, current)
    }

    private fun play(ctx: Context, index: Int) {
        stop()
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            ctx.assets.openFd(TRACKS[index]).use { afd ->
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mp.setVolume(volume, volume)
            mp.setOnPreparedListener {
                prepared = true
                if (enabled) it.start()
            }
            // Enchaîne sur la piste suivante à la fin
            mp.setOnCompletionListener {
                current = (current + 1) % TRACKS.size
                Stats.setMusicTrack(ctx, current)
                play(ctx, current)
            }
            mp.prepareAsync()
            player = mp
        } catch (_: Exception) {
            player = null
        }
    }

    /** Change de piste immédiatement. */
    fun selectTrack(ctx: Context, index: Int) {
        current = index.coerceIn(0, TRACKS.size - 1)
        Stats.setMusicTrack(ctx, current)
        if (enabled) play(ctx, current)
    }

    fun currentTrack(): Int = current

    fun pause() {
        try { if (prepared) player?.takeIf { it.isPlaying }?.pause() } catch (_: Exception) {}
    }

    fun resume() {
        try { if (prepared && enabled) player?.takeIf { !it.isPlaying }?.start() } catch (_: Exception) {}
    }

    fun stop() {
        try { player?.release() } catch (_: Exception) {}
        player = null
        prepared = false
    }

    /** Applique les réglages (on/off + volume) sans couper la lecture si possible. */
    fun refresh(ctx: Context) {
        enabled = Stats.musicOn(ctx)
        volume = Stats.musicVolume(ctx) / 100f
        if (!enabled) { stop(); return }
        if (player == null) { start(ctx); return }
        try { player?.setVolume(volume, volume) } catch (_: Exception) {}
        resume()
    }
}

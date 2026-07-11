package com.tiboja.cubenova

import android.content.Context
import java.util.Calendar

/**
 * Stockage local (SharedPreferences) : records, succès, défis, défi du jour.
 */
object Stats {
    private const val PREF = "cubenova"

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // --- Succès : id -> libellé ---
    val ACHIEVEMENTS = listOf(
        "first" to "Premier cube résolu",
        "solve_2" to "Facile vaincu (2×2)",
        "solve_3" to "Normal vaincu (3×3)",
        "solve_4" to "Difficile vaincu (4×4)",
        "solve_5" to "Extrême vaincu (5×5)",
        "speed_3" to "3×3 en moins de 60 s",
        "few_3" to "3×3 en moins de 40 coups",
        "all" to "Toutes les tailles résolues"
    )

    // --- Défis (objectifs) : id -> libellé ---
    val CHALLENGES = listOf(
        "c_2" to "Résoudre un 2×2",
        "c_3_time" to "3×3 en moins de 2 min",
        "c_3_moves" to "3×3 en moins de 60 coups",
        "c_4" to "Résoudre un 4×4",
        "c_daily" to "Résoudre le Défi du jour",
        "c_5" to "Résoudre un 5×5"
    )

    fun bestTime(ctx: Context, size: Int): Long = p(ctx).getLong("time_$size", -1L)
    fun bestMoves(ctx: Context, size: Int): Int = p(ctx).getInt("moves_$size", -1)

    // ---------- Progression : XP / Niveau / Gemmes ----------
    const val XP_PER_LEVEL = 1000L
    fun totalXp(ctx: Context): Long = p(ctx).getLong("xp", 0L)
    fun gems(ctx: Context): Long = p(ctx).getLong("gems", 0L)
    fun level(ctx: Context): Int = (totalXp(ctx) / XP_PER_LEVEL).toInt() + 1
    fun xpInLevel(ctx: Context): Long = totalXp(ctx) % XP_PER_LEVEL

    fun isUnlocked(ctx: Context, id: String) = p(ctx).getBoolean("ach_$id", false)
    fun challengeDone(ctx: Context, id: String) = p(ctx).getBoolean("chal_$id", false)

    // Aide au premier lancement
    fun isHelpSeen(ctx: Context) = p(ctx).getBoolean("help_seen", false)
    fun setHelpSeen(ctx: Context) = p(ctx).edit().putBoolean("help_seen", true).apply()

    // Mode de vue (false = libre, true = directionnel)
    fun viewDirectional(ctx: Context) = p(ctx).getBoolean("view_dir", false)
    fun setViewDirectional(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("view_dir", v).apply()

    // Son / vibration (activés par défaut)
    fun soundOn(ctx: Context) = p(ctx).getBoolean("sound_on", true)
    fun vibrateOn(ctx: Context) = p(ctx).getBoolean("vibrate_on", true)
    fun setSoundOn(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("sound_on", v).apply()
    fun setVibrateOn(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("vibrate_on", v).apply()

    // Musique de fond
    fun musicOn(ctx: Context) = p(ctx).getBoolean("music_on", true)
    fun setMusicOn(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("music_on", v).apply()
    fun musicVolume(ctx: Context) = p(ctx).getInt("music_vol", 50)          // 0..100
    fun setMusicVolume(ctx: Context, v: Int) = p(ctx).edit().putInt("music_vol", v.coerceIn(0, 100)).apply()
    fun musicTrack(ctx: Context) = p(ctx).getInt("music_track", 0)
    fun setMusicTrack(ctx: Context, v: Int) = p(ctx).edit().putInt("music_track", v).apply()

    // ---------- Défi du jour ----------
    /** Clé du jour au format AAAAMMJJ (locale). */
    fun todayKey(): Long {
        val c = Calendar.getInstance()
        return (c.get(Calendar.YEAR) * 10000L
                + (c.get(Calendar.MONTH) + 1) * 100L
                + c.get(Calendar.DAY_OF_MONTH))
    }

    fun dailySeed(): Long = todayKey()
    fun dailyDone(ctx: Context): Boolean = p(ctx).getBoolean("daily_done_${todayKey()}", false)
    fun dailyBestTime(ctx: Context): Long = p(ctx).getLong("daily_time_${todayKey()}", -1L)
    fun dailyBestMoves(ctx: Context): Int = p(ctx).getInt("daily_moves_${todayKey()}", -1)

    /**
     * Enregistre une victoire. Renvoie les succès et défis nouvellement débloqués
     * et si c'est un nouveau record de temps.
     */
    data class WinReport(
        val newRecordTime: Boolean,
        val newAchievements: List<String>,
        val newChallenges: List<String>,
        val dailyRecord: Boolean,
        val xpGained: Int,
        val gemsGained: Int,
        val leveledUp: Boolean
    )

    fun recordWin(ctx: Context, size: Int, timeMs: Long, moves: Int, isDaily: Boolean): WinReport {
        val pr = p(ctx)
        val e = pr.edit()

        val levelBefore = level(ctx)

        // Récompenses XP / gemmes
        val baseXp = when (size) { 2 -> 30; 3 -> 60; 4 -> 120; else -> 200 }
        val xpGain = baseXp + if (isDaily) 50 else 0
        val gemGain = size * 2 + if (isDaily) 5 else 0
        e.putLong("xp", pr.getLong("xp", 0L) + xpGain)
        e.putLong("gems", pr.getLong("gems", 0L) + gemGain)

        // Records par taille
        val prevT = pr.getLong("time_$size", -1L)
        val recordTime = prevT < 0 || timeMs < prevT
        if (recordTime) e.putLong("time_$size", timeMs)
        val prevM = pr.getInt("moves_$size", -1)
        if (prevM < 0 || moves < prevM) e.putInt("moves_$size", moves)

        // Défi du jour
        var dailyRecord = false
        if (isDaily) {
            val dk = todayKey()
            e.putBoolean("daily_done_$dk", true)
            val prevDT = pr.getLong("daily_time_$dk", -1L)
            if (prevDT < 0 || timeMs < prevDT) {
                e.putLong("daily_time_$dk", timeMs)
                e.putInt("daily_moves_$dk", moves)
                dailyRecord = true
            }
        }

        // Succès
        val newAch = ArrayList<String>()
        fun unlockAch(id: String) {
            if (!pr.getBoolean("ach_$id", false)) {
                e.putBoolean("ach_$id", true)
                newAch.add(ACHIEVEMENTS.first { it.first == id }.second)
            }
        }
        unlockAch("first")
        unlockAch("solve_$size")
        if (size == 3 && timeMs < 60_000) unlockAch("speed_3")
        if (size == 3 && moves < 40) unlockAch("few_3")

        // Défis
        val newChal = ArrayList<String>()
        fun unlockChal(id: String) {
            if (!pr.getBoolean("chal_$id", false)) {
                e.putBoolean("chal_$id", true)
                newChal.add(CHALLENGES.first { it.first == id }.second)
            }
        }
        if (size == 2) unlockChal("c_2")
        if (size == 4) unlockChal("c_4")
        if (size == 5) unlockChal("c_5")
        if (size == 3 && timeMs < 120_000) unlockChal("c_3_time")
        if (size == 3 && moves < 60) unlockChal("c_3_moves")
        if (isDaily) unlockChal("c_daily")

        e.apply() // applique avant de tester "toutes les tailles"

        if (isUnlocked(ctx, "solve_2") && isUnlocked(ctx, "solve_3") &&
            isUnlocked(ctx, "solve_4") && isUnlocked(ctx, "solve_5")) {
            if (!isUnlocked(ctx, "all")) {
                pr.edit().putBoolean("ach_all", true).apply()
                newAch.add(ACHIEVEMENTS.first { it.first == "all" }.second)
            }
        }
        val leveledUp = level(ctx) > levelBefore
        return WinReport(recordTime, newAch, newChal, dailyRecord, xpGain, gemGain, leveledUp)
    }

    fun formatTime(ms: Long): String {
        if (ms < 0) return "—"
        val totalTenths = ms / 100
        val m = totalTenths / 600
        val s = (totalTenths / 10) % 60
        val d = totalTenths % 10
        return if (m > 0) "%d:%02d.%d".format(m, s, d) else "%d.%d s".format(s, d)
    }
}

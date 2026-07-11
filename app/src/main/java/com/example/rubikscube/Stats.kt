package com.example.rubikscube

import android.content.Context

/**
 * Stockage local (SharedPreferences) : meilleurs temps/coups par taille + succès.
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

    fun bestTime(ctx: Context, size: Int): Long = p(ctx).getLong("time_$size", -1L)
    fun bestMoves(ctx: Context, size: Int): Int = p(ctx).getInt("moves_$size", -1)

    fun isUnlocked(ctx: Context, id: String) = p(ctx).getBoolean("ach_$id", false)

    /**
     * Enregistre une victoire. Renvoie la liste des succès NOUVELLEMENT débloqués
     * et indique si c'est un nouveau record de temps.
     */
    data class WinReport(val newRecordTime: Boolean, val newAchievements: List<String>)

    fun recordWin(ctx: Context, size: Int, timeMs: Long, moves: Int): WinReport {
        val pr = p(ctx)
        val e = pr.edit()

        // Records
        val prevT = pr.getLong("time_$size", -1L)
        val recordTime = prevT < 0 || timeMs < prevT
        if (recordTime) e.putLong("time_$size", timeMs)
        val prevM = pr.getInt("moves_$size", -1)
        if (prevM < 0 || moves < prevM) e.putInt("moves_$size", moves)

        // Succès
        val newly = ArrayList<String>()
        fun unlock(id: String) {
            if (!pr.getBoolean("ach_$id", false)) {
                e.putBoolean("ach_$id", true)
                newly.add(ACHIEVEMENTS.first { it.first == id }.second)
            }
        }
        unlock("first")
        unlock("solve_$size")
        if (size == 3 && timeMs < 60_000) unlock("speed_3")
        if (size == 3 && moves < 40) unlock("few_3")
        e.apply() // applique avant de tester "all"

        if (isUnlocked(ctx, "solve_2") && isUnlocked(ctx, "solve_3") &&
            isUnlocked(ctx, "solve_4") && isUnlocked(ctx, "solve_5")) {
            if (!isUnlocked(ctx, "all")) {
                pr.edit().putBoolean("ach_all", true).apply()
                newly.add(ACHIEVEMENTS.first { it.first == "all" }.second)
            }
        }
        return WinReport(recordTime, newly)
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

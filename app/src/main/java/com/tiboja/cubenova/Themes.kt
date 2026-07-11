package com.tiboja.cubenova

object Themes {
    val NAMES = arrayOf(
        "Classique", "Mangas", "Elfes", "Yggdrasil", "Zen", "Punk",
        "Smileys", "Jungle", "Saisons", "Or", "Émeraude", "Crépuscule"
    )
    val ATLAS = Array(12) { "atlas/theme_%02d.jpg".format(it) }
    val THUMBS = Array(12) { "thumbs/theme_%02d.jpg".format(it) }
    const val DEFAULT = 0
}

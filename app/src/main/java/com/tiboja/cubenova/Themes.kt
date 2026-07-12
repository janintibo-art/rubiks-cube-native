package com.tiboja.cubenova

object Themes {
    val NAMES = arrayOf(
        "Classique", "Mangas", "Elfes", "Yggdrasil", "Zen", "Punk",
        "Smileys", "Jungle", "Saisons", "Or", "Émeraude", "Crépuscule",
        "Nature", "Espace", "Espace fun", "Abstrait", "Kawaii", "Délire"
    )
    val ATLAS = Array(18) { "atlas/theme_%02d.jpg".format(it) }
    val THUMBS = Array(18) { "thumbs/theme_%02d.jpg".format(it) }
    const val DEFAULT = 0
}

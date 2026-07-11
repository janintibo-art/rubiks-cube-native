package com.tiboja.cubenova

object Themes {
    val NAMES = arrayOf(
        "Classique", "Manga", "Elfe", "Yggdrasil", "Zen", "Punk", "Smile délire", "Nature folie", "Nature couleurs", "Lot 1", "Lot 2", "Cube 6"
    )
    val ATLAS = Array(12) { "atlas/theme_%02d.jpg".format(it) }
    val THUMBS = Array(12) { "thumbs/theme_%02d.jpg".format(it) }
    const val DEFAULT = 0
}

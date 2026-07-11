package com.example.rubikscube

object Themes {
    val NAMES = arrayOf(
        "Classique", "Emoji", "Monstres", "Bonbons", "Bureau", "Nature 1", "Nature 2", "Nature 3", "Nature 4", "Nature 5", "Nature 6", "Nature 7", "Nature 8", "Nature 9", "Nature 10", "Nature 11", "Nature 12", "Nature 13", "Nature 14", "Nature 15", "Nature 16", "Nature 17", "Nature 18", "Nature 19", "Nature 20"
    )
    val ATLAS = Array(25) { "atlas/theme_%02d.jpg".format(it) }
    val THUMBS = Array(25) { "thumbs/theme_%02d.jpg".format(it) }
    const val DEFAULT = 0
}

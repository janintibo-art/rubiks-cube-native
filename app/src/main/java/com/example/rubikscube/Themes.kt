package com.example.rubikscube

/**
 * 20 thèmes de couleurs extraits des 20 planches.
 * Chaque thème = 6 couleurs 0xRRGGBB dans l'ordre :
 * front(+Z), back(-Z), right(+X), left(-X), top(+Y), bottom(-Y).
 */
object Themes {

    // Thème classique (Rubik's standard)
    val CLASSIC = intArrayOf(0xCC0D0D.toInt(), 0xFF7300.toInt(), 0x004DE6.toInt(), 0x009926.toInt(), 0xEBEBEB.toInt(), 0xFFD900.toInt())

    val PALETTES: Array<IntArray> = arrayOf(
        CLASSIC,
        intArrayOf(0x01F903.toInt(), 0xB81C03.toInt(), 0xFDD51D.toInt(), 0xF78E10.toInt(), 0xF4F1E3.toInt(), 0x0A3789.toInt()),
        intArrayOf(0x02F804.toInt(), 0xCC762E.toInt(), 0xBED8BD.toInt(), 0x05629B.toInt(), 0x8F1010.toInt(), 0xF4DF76.toInt()),
        intArrayOf(0x03FA05.toInt(), 0xEB841C.toInt(), 0xFCE277.toInt(), 0xC01811.toInt(), 0x0BC9B1.toInt(), 0xC5CB7D.toInt()),
        intArrayOf(0x0AF20B.toInt(), 0xE03104.toInt(), 0xC6A147.toInt(), 0xAAE770.toInt(), 0x3D388F.toInt(), 0x1A985F.toInt()),
        intArrayOf(0x10F812.toInt(), 0xD8A827.toInt(), 0xB92F17.toInt(), 0xDED39E.toInt(), 0x2C6C5D.toInt(), 0xBB7118.toInt()),
        intArrayOf(0xCEAA31.toInt(), 0x03FA04.toInt(), 0x846411.toInt(), 0x11464E.toInt(), 0x7A1D12.toInt(), 0xF4DB66.toInt()),
        intArrayOf(0x02F80D.toInt(), 0xC06C1F.toInt(), 0x281509.toInt(), 0xF5D56D.toInt(), 0xA42E11.toInt(), 0x2A455A.toInt()),
        intArrayOf(0xFDFDF4.toInt(), 0x0AF617.toInt(), 0xDA1511.toInt(), 0x117AA7.toInt(), 0xE68A3D.toInt(), 0x361945.toInt()),
        intArrayOf(0x02FA05.toInt(), 0xF5C950.toInt(), 0x0A0D2D.toInt(), 0xC35B11.toInt(), 0x720C04.toInt(), 0x135067.toInt()),
        intArrayOf(0x08E206.toInt(), 0xF9DB46.toInt(), 0x00579C.toInt(), 0x9D0C0B.toInt(), 0x908212.toInt(), 0x5FE65D.toInt()),
        intArrayOf(0xF7C41D.toInt(), 0x02DE0D.toInt(), 0xD5671D.toInt(), 0xFBF29F.toInt(), 0x175B78.toInt(), 0x6CC626.toInt()),
        intArrayOf(0x03D712.toInt(), 0xF5CF04.toInt(), 0xF6F3E8.toInt(), 0x294469.toInt(), 0x9B0817.toInt(), 0x95822B.toInt()),
        intArrayOf(0x06F70A.toInt(), 0xEBE7AC.toInt(), 0xD47426.toInt(), 0xDBC143.toInt(), 0x18717B.toInt(), 0x8E1B1C.toInt()),
        intArrayOf(0x12E514.toInt(), 0xE97D09.toInt(), 0xF9E160.toInt(), 0x11698E.toInt(), 0x9D1812.toInt(), 0x88B64C.toInt()),
        intArrayOf(0xF8CE20.toInt(), 0x03E81A.toInt(), 0xFDEB77.toInt(), 0x418D90.toInt(), 0x880F0A.toInt(), 0x9F5E21.toInt()),
        intArrayOf(0x06F905.toInt(), 0xCBE45C.toInt(), 0x1F48B4.toInt(), 0xA8871B.toInt(), 0x7A0B0A.toInt(), 0x7A0B0A.toInt()),
        intArrayOf(0xE1A519.toInt(), 0x02F803.toInt(), 0x2561CE.toInt(), 0xFBEB69.toInt(), 0xFD2808.toInt(), 0x27B24E.toInt()),
        intArrayOf(0x05F804.toInt(), 0xFAF9F6.toInt(), 0x0A85B6.toInt(), 0xA21219.toInt(), 0xB99D1F.toInt(), 0xF7DB4A.toInt()),
        intArrayOf(0x02EE07.toInt(), 0x0B5A65.toInt(), 0xD6650C.toInt(), 0xBF2409.toInt(), 0x4E2714.toInt(), 0xC2BE48.toInt()),
        intArrayOf(0x03F107.toInt(), 0xF28807.toInt(), 0x0A3598.toInt(), 0xCD1A0A.toInt(), 0xF9DA59.toInt(), 0x10785A.toInt()),
    )

    // Noms affichés (index 0 = classique, puis planches 1..20)
    val NAMES: Array<String> = arrayOf(
        "Classique",
        "Thème 1",
        "Thème 2",
        "Thème 3",
        "Thème 4",
        "Thème 5",
        "Thème 6",
        "Thème 7",
        "Thème 8",
        "Thème 9",
        "Thème 10",
        "Thème 11",
        "Thème 12",
        "Thème 13",
        "Thème 14",
        "Thème 15",
        "Thème 16",
        "Thème 17",
        "Thème 18",
        "Thème 19",
        "Thème 20",
    )

    // Miniature associée dans assets/themes (null = pas d'image pour le classique)
    val THUMBS: Array<String?> = arrayOf(
        null,
        "themes/theme_01.jpg",
        "themes/theme_02.jpg",
        "themes/theme_03.jpg",
        "themes/theme_04.jpg",
        "themes/theme_05.jpg",
        "themes/theme_06.jpg",
        "themes/theme_07.jpg",
        "themes/theme_08.jpg",
        "themes/theme_09.jpg",
        "themes/theme_10.jpg",
        "themes/theme_11.jpg",
        "themes/theme_12.jpg",
        "themes/theme_13.jpg",
        "themes/theme_14.jpg",
        "themes/theme_15.jpg",
        "themes/theme_16.jpg",
        "themes/theme_17.jpg",
        "themes/theme_18.jpg",
        "themes/theme_19.jpg",
        "themes/theme_20.jpg",
    )
}

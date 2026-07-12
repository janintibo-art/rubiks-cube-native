package com.tiboja.cubenova

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/**
 * Écran de démarrage : le logo est affiché par le thème (windowBackground),
 * donc il apparaît instantanément, puis on passe au menu après un court délai.
 */
class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
        }, 900)
    }
}

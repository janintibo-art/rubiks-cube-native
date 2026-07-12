plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tiboja.cubenova"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tiboja.cubenova"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.3"
    }

    // Signature release : activée seulement si les variables d'environnement
    // sont présentes (elles le sont dans GitHub Actions via les Secrets).
    // En local, l'absence de ces variables laisse un build non signé.
    signingConfigs {
        create("release") {
            val ksFile = System.getenv("KEYSTORE_FILE")
            if (ksFile != null) {
                storeFile = file(ksFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (System.getenv("KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Aucune dépendance externe : OpenGL ES + SDK Android suffisent.
}

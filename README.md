# 🧊 Rubik's Cube 3D — natif Kotlin

Un Rubik's Cube 3D **100% natif Android**, écrit en **Kotlin** avec **OpenGL ES 2.0**
(pas de WebView, pas de moteur externe). L'APK est compilé **automatiquement** par
GitHub Actions.

## ✨ Fonctionnalités
- Rendu 3D natif OpenGL ES (27 pièces, faces colorées).
- **Tourne une face en glissant directement dessus** (picking 3D par lancer de rayon).
- **Joystick** dédié pour pivoter la vue (plus de rotation accidentelle de ligne).
- **12 thèmes texturés** (bouton 🎨) : Classique + Manga, Elfe, Yggdrasil, Zen, Punk, Smile, Nature folie, Nature couleurs, Lot 1, Lot 2, Cube 6.
- **4 niveaux de difficulté** (bouton 🎚) : Facile 2×2, Normal 3×3, Difficile 4×4, Extrême 5×5.
- Les motifs sont collés en vraies textures : l'image se mélange comme un vrai Rubik's.
- Mélange aléatoire + réinitialisation.
- Icône d'application personnalisée.
- Aucune dépendance externe → build léger.

## 📁 Arborescence
```
rubiks-native/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── .gitignore
├── .github/workflows/build-apk.yml     # compile l'APK tout seul
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/rubikscube/
        │   ├── MainActivity.kt          # branche les boutons
        │   ├── CubeGLSurfaceView.kt     # gère le toucher
        │   ├── CubeRenderer.kt          # caméra + animation + logique
        │   └── Cubie.kt                 # géométrie d'une pièce
        └── res/
            ├── layout/activity_main.xml
            └── values/{strings,styles}.xml
```

## 🎮 Comment jouer
- **Glisse sur une face du cube** → cette face (ou la tranche touchée) tourne dans le sens du glissement.
- **Glisse dans le vide autour du cube** → tu fais pivoter la vue.
- Les **boutons** en bas restent disponibles (U/D/L/R/F/B, sens, mélange, reset).

## 🤖 Obtenir l'APK
1. Crée un dépôt GitHub et envoie ces fichiers (commandes Termux plus bas).
2. Onglet **Actions** → le workflow *Build APK* démarre au push.
3. Une fois ✅, ouvre le job → **Artifacts** → télécharge `rubiks-cube-3d-apk`.
4. Décompresse le `.zip` : ton `.apk` est dedans. Installe-le (autorise les
   « sources inconnues » sur Android).

> L'APK est en *debug* (non signé pour le Play Store, mais installable directement).

## 🧪 Compiler en local (facultatif)
Avec Android Studio : ouvre le dossier, laisse-le télécharger le SDK, puis Run.
En ligne de commande (SDK + JDK 17 installés) :
```bash
gradle assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

## 📜 Licence
MIT

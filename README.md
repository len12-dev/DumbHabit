# DumbHabit

Application Android de suivi d'habitudes et de routines, au design « écran LCD » (sauge, cadres épais, monospace).

## Fonctionnalités

- **Habitudes** : cocher chaque jour, jours actifs (L M M J V S D), rappel quotidien optionnel.
- **Routines** : suite d'étapes ordonnées, chacune avec un minuteur optionnel (ex. « Se brosser les dents · 2:00 ») ou un simple chronomètre.
- **Routine en direct** : pendant une routine, une notification persistante affiche l'étape courante, le chrono et les boutons **Valider** (étape faite) et **Passer** (étape ignorée). L'écran de l'app affiche la même progression.
- **Rappels** : notification quotidienne pour chaque habitude ou routine, reprogrammée après redémarrage.
- **Réglages** : thème clair, sombre ou automatique (suit le système).

## Stack

- Kotlin, Jetpack Compose (Material 3 pour les composants de base, styles maison pour l'apparence)
- Room (habitudes, routines, historique)
- AlarmManager (rappels), foreground service `specialUse` (routine en direct)
- minSdk 26, targetSdk 35

## Structure

```
app/src/main/java/com/dumbhabit/app/
├── DumbHabitApp.kt          # Application : base de données, canal de notifications
├── data/                    # Entités Room, DAO, dates et formats
├── reminders/               # Planification des rappels + receivers (rappel, boot)
├── routine/
│   ├── RoutineEngine.kt     # État de la routine en cours (Valider / Passer)
│   └── RoutineService.kt    # Notification persistante en direct
└── ui/
    ├── MainActivity.kt, AppNav.kt, AppViewModel.kt
    ├── theme/               # Palette clair / sombre, typographie monospace
    ├── components/Pixel.kt  # Cadres, cases, boutons, sélecteurs de jours et d'heure
    └── screens/             # Accueil, habitudes, routines, exécution, réglages
```

## Ouvrir et lancer

1. Ouvrir le dossier dans Android Studio (Ladybug ou plus récent).
2. Laisser Gradle synchroniser ; le wrapper est généré par Android Studio si absent.
3. Lancer sur un appareil ou un émulateur Android 8.0+.

## Pistes pour la suite

- Curseurs « Teinte » et « Contraste » des réglages (captures d'origine).
- Live Updates natifs d'Android 16 (ProgressStyle) à la place de la notification standard.
- Widget d'accueil listant les habitudes du jour.
- Statistiques de série (jours consécutifs).

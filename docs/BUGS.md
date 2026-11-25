# Bugs connus

## Ouverts

- **Chip Pomodoro bloqué dans la barre d'état** : après avoir lancé le Pomodoro, la barre d'état (à gauche de l'heure) affiche une icône ✓ et « 25:00 » fixe, alors que le minuteur ne tourne pas. Probablement une notification live Pomodoro restée active après l'arrêt, ou un texte court non mis à jour. À vérifier : état de `PomodoroEngine` et `renderLive` quand on met en pause, réinitialise ou que la phase se termine.

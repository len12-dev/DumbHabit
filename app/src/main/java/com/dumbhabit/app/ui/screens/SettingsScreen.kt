package com.dumbhabit.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dumbhabit.app.ui.AppViewModel
import com.dumbhabit.app.ui.components.PixelBox
import com.dumbhabit.app.ui.components.PixelButton
import com.dumbhabit.app.ui.components.PixelSegmented
import com.dumbhabit.app.ui.components.ScreenHeader
import com.dumbhabit.app.ui.theme.ColorMode
import com.dumbhabit.app.ui.theme.pixel

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val p = pixel()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(title = "Réglages", onBack = onBack)

        PixelBox(label = "Écran") {
            Text("Couleurs", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            PixelSegmented(
                options = listOf(
                    ColorMode.LIGHT to "Clair",
                    ColorMode.DARK to "Sombre",
                    ColorMode.FOLLOW_SYSTEM to "Auto",
                ),
                selected = vm.colorMode,
                onSelect = vm::setColorMode,
            )
        }

        PixelBox(label = "Notifications") {
            Text(
                "Rappels d'habitudes et de routines, et routine en direct avec les boutons Valider et Passer.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.muted,
            )
            Spacer(Modifier.height(12.dp))
            PixelButton(
                text = "Ouvrir les réglages",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                },
            )
        }

        PixelBox(label = "À propos") {
            Text("DumbHabit 0.1.0", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

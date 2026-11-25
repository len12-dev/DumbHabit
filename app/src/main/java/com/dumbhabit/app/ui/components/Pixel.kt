package com.dumbhabit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dumbhabit.app.data.minutesToLabel
import com.dumbhabit.app.ui.theme.pixel

/** Cadre épais avec un petit label en capitales, comme les blocs de l'écran d'accueil. */
@Composable
fun PixelBox(
    modifier: Modifier = Modifier,
    label: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = pixel()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, p.ink)
            .padding(14.dp),
    ) {
        if (label != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                    color = p.muted,
                )
                Spacer(Modifier.weight(1f))
                trailing?.invoke()
            }
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "<",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(end = 12.dp),
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            PixelButton(text = action, onClick = onAction)
        }
    }
}

@Composable
fun PixelCheck(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val p = pixel()
    Box(
        modifier = modifier
            .size(24.dp)
            .border(2.dp, p.ink)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = p.ink,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val p = pixel()
    val fg = if (filled) p.background else p.ink
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .border(2.dp, p.ink)
            .background(if (filled) p.ink else p.background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            color = fg,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PixelChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = pixel()
    Box(
        modifier = modifier
            .border(2.dp, p.ink)
            .background(if (selected) p.ink else p.background)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) p.background else p.ink,
        )
    }
}

@Composable
fun <T> PixelSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            PixelButton(
                text = label,
                onClick = { onSelect(value) },
                filled = value == selected,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Ligne pointillée séparatrice. */
@Composable
fun PixelDivider(modifier: Modifier = Modifier) {
    val p = pixel()
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .drawBehind {
                drawLine(
                    color = p.ink,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                )
            },
    )
}

@Composable
fun PixelNavItem(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = pixel()
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(56.dp).height(6.dp).background(p.ink))
    }
}

@Composable
fun PixelStepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PixelButton(text = "−", onClick = onMinus, enabled = minusEnabled)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 96.dp)
                .padding(horizontal = 10.dp),
        )
        PixelButton(text = "+", onClick = onPlus, enabled = plusEnabled)
    }
}

@Composable
fun PixelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val p = pixel()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.ink),
        cursorBrush = SolidColor(p.ink),
        modifier = modifier
            .border(2.dp, p.ink)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = p.muted)
            }
            inner()
        },
    )
}

/** Sélecteur des jours actifs. `mask` : bit 0 = lundi ... bit 6 = dimanche. */
@Composable
fun DaysPicker(mask: Int, onChange: (Int) -> Unit) {
    val labels = listOf("L", "M", "M", "J", "V", "S", "D")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            val bit = 1 shl i
            PixelChip(
                text = label,
                selected = (mask and bit) != 0,
                onClick = { onChange(mask xor bit) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Rappel quotidien optionnel, réglé par pas de 15 minutes. */
@Composable
fun ReminderField(minutes: Int?, onChange: (Int?) -> Unit) {
    val p = pixel()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "RAPPEL",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = p.muted,
        )
        if (minutes == null) {
            PixelButton(text = "Ajouter un rappel", onClick = { onChange(8 * 60) })
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelStepper(
                    value = minutesToLabel(minutes),
                    onMinus = { onChange((minutes - 15 + 1440) % 1440) },
                    onPlus = { onChange((minutes + 15) % 1440) },
                )
                Spacer(Modifier.weight(1f))
                PixelButton(text = "Retirer", onClick = { onChange(null) })
            }
        }
    }
}

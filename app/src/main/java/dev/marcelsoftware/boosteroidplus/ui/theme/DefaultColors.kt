package dev.marcelsoftware.boosteroidplus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
class DefaultColors(
    val titleColor: Color,
    val iconColor: Color,
    val subtitleColor: Color,
)

@Composable
fun AppColors(
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
): DefaultColors =
    DefaultColors(
        titleColor,
        iconColor,
        subtitleColor,
    )

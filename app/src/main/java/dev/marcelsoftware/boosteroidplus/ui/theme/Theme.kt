package dev.marcelsoftware.boosteroidplus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.t8rin.dynamic.theme.ColorTuple
import com.t8rin.dynamic.theme.DynamicTheme
import com.t8rin.dynamic.theme.PaletteStyle
import com.t8rin.dynamic.theme.calculateSecondaryColor
import com.t8rin.dynamic.theme.calculateSurfaceColor
import com.t8rin.dynamic.theme.calculateTertiaryColor
import com.t8rin.dynamic.theme.rememberAppColorTuple
import com.t8rin.dynamic.theme.rememberDynamicThemeState

@Composable
fun BoosteroidTheme(content: @Composable () -> Unit) {
    val primaryColor = Color(0xFFB1D18A)
    val isDarkTheme = isSystemInDarkTheme()

    val colorTuple =
        rememberAppColorTuple(
            defaultColorTuple =
                ColorTuple(
                    primary = primaryColor,
                    secondary = primaryColor.calculateSecondaryColor().toColor(),
                    tertiary = primaryColor.calculateTertiaryColor().toColor(),
                    surface = primaryColor.calculateSurfaceColor().toColor(),
                ),
            dynamicColor = false,
            darkTheme = isDarkTheme,
        )

    MaterialTheme {
        DynamicTheme(
            state = rememberDynamicThemeState(colorTuple),
            defaultColorTuple = colorTuple,
            dynamicColor = false,
            isDarkTheme = isDarkTheme,
            amoledMode = false,
            style = PaletteStyle.TonalSpot,
            content = content,
        )
    }
}

private fun Int.toColor() = Color(this)

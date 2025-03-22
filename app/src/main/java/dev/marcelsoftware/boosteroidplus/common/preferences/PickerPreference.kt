package dev.marcelsoftware.boosteroidplus.common.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.marcelsoftware.boosteroidplus.common.BottomSheetHostState
import dev.marcelsoftware.boosteroidplus.ui.theme.AppColors

@Composable
fun <T> PickerPreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    selectedIndex: Int,
    options: List<T>,
    optionLabel: (T) -> String,
    selectedLabel: (T) -> String = { optionLabel(it) },
    onOptionSelected: (Int) -> Unit,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    bottomSheetHostState: BottomSheetHostState,
) {
    val displayText =
        if (selectedIndex >= 0 && selectedIndex < options.size) {
            selectedLabel(options[selectedIndex])
        } else {
            ""
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = {
                        if (enabled) {
                            bottomSheetHostState.showBottomSheet(
                                title = title,
                                content = {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        options.forEachIndexed { index, option ->
                                            Row(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            onOptionSelected(index)
                                                            bottomSheetHostState.hideBottomSheet()
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                RadioButton(
                                                    selected = index == selectedIndex,
                                                    onClick = {
                                                        onOptionSelected(index)
                                                        bottomSheetHostState.hideBottomSheet()
                                                    },
                                                )

                                                Text(
                                                    text = optionLabel(option),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.padding(start = 16.dp),
                                                )
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    },
                )
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(modifier = Modifier.padding(end = 16.dp)) {
                icon()
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) AppColors().titleColor else AppColors().titleColor.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) AppColors().subtitleColor else AppColors().subtitleColor.copy(alpha = 0.6f),
            )
        }

        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.6f),
        )
    }
}

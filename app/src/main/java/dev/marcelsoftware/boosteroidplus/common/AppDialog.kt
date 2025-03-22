package dev.marcelsoftware.boosteroidplus.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t8rin.modalsheet.FullscreenPopup
import dev.marcelsoftware.boosteroidplus.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Stable
open class AppDialogState {
    private val mutex = Mutex()

    var isVisible by mutableStateOf(false)
        private set

    suspend fun showDialog() =
        mutex.withLock {
            isVisible = true
        }

    suspend fun hideDialog() =
        mutex.withLock {
            isVisible = false
        }
}

@Composable
fun rememberAppCronDialogState() = remember { AppDialogState() }

@Composable
fun AppDialog(
    onDismissRequest: (() -> Unit)? = null,
    state: AppDialogState,
    header: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    if (state.isVisible) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            FullscreenPopup(
                onDismiss = onDismissRequest,
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(0.8f))
                            .clickable {
                                scope.launch {
                                    state.hideDialog()
                                    onDismissRequest?.invoke()
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier =
                            Modifier
                                .padding(horizontal = 32.dp)
                                .clickable(enabled = false) {}
                                .border(
                                    width = 0.5.dp,
                                    color =
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                            .compositeOver(MaterialTheme.colorScheme.outlineVariant),
                                    shape = AlertDialogDefaults.shape,
                                ),
                        shape = AlertDialogDefaults.shape,
                        color = AlertDialogDefaults.containerColor,
                        tonalElevation = AlertDialogDefaults.TonalElevation,
                        contentColor = AlertDialogDefaults.textContentColor,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            header?.let {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                    content = it,
                                )
                                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                            }
                            content()
                            FlowRow(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .safeDrawingPadding(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        space = 8.dp,
                                        alignment = Alignment.End,
                                    ),
                            ) {
                                OutlinedButton(
                                    modifier =
                                        Modifier
                                            .shadow(
                                                elevation = 0.dp,
                                                shape = ButtonDefaults.shape,
                                            ),
                                    onClick = {
                                        scope.launch {
                                            state.hideDialog()
                                            onDismissRequest?.invoke()
                                        }
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            contentColor = contentColorFor(MaterialTheme.colorScheme.primary),
                                            containerColor = MaterialTheme.colorScheme.primary,
                                        ),
                                ) {
                                    Text(
                                        text = stringResource(R.string.action_ok),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppDialogHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
    )
}

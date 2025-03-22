package dev.marcelsoftware.boosteroidplus.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Stable
class BottomSheetHostState {
    private val _currentContent: MutableState<(@Composable () -> Unit)?> = mutableStateOf(null)
    internal val currentContent: (@Composable () -> Unit)? get() = _currentContent.value

    private val _currentTitle: MutableState<String> = mutableStateOf("")
    internal val currentTitle: String get() = _currentTitle.value

    private val _isVisible = mutableStateOf(false)
    val isVisible: Boolean get() = _isVisible.value

    fun showBottomSheet(
        title: String,
        content: @Composable () -> Unit,
    ) {
        _currentTitle.value = title
        _currentContent.value = content
        _isVisible.value = true
    }

    fun hideBottomSheet() {
        _isVisible.value = false
    }
}

@Composable
fun rememberBottomSheetHostState(): BottomSheetHostState {
    return remember { BottomSheetHostState() }
}

@Composable
fun BottomSheetHost(state: BottomSheetHostState) {
    if (state.isVisible && state.currentContent != null) {
        DraggableBottomSheet(
            title = state.currentTitle,
            onDismiss = { state.hideBottomSheet() },
            content = { state.currentContent?.invoke() },
        )
    }
}

@Composable
private fun DraggableBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var sheetHeight by remember { mutableStateOf(0) }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            ),
    )

    val draggableState =
        rememberDraggableState { delta ->
            if (delta > 0) {
                dragOffset += delta
            } else if (dragOffset > 0) {
                dragOffset += delta
                if (dragOffset < 0) dragOffset = 0f
            }
        }

    val dismissThreshold = sheetHeight * 0.25f

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures { onDismiss() }
                    },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size -> sheetHeight = size.height }
                        .offset { IntOffset(0, animatedDragOffset.roundToInt()) }
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = draggableState,
                            onDragStopped = {
                                if (dragOffset > dismissThreshold) {
                                    scope.launch {
                                        dragOffset = sheetHeight.toFloat()
                                        onDismiss()
                                    }
                                } else {
                                    scope.launch {
                                        dragOffset = 0f
                                    }
                                }
                            },
                        )
                        .clickable(enabled = false) { },
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .width(32.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = { onDismiss() },
                                        )
                                    },
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                        ) {
                            content()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onDismiss,
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

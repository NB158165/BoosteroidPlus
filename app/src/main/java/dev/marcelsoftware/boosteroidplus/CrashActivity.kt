package dev.marcelsoftware.boosteroidplus

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.BrandGithub
import compose.icons.tablericons.Clipboard
import compose.icons.tablericons.ClipboardCheck
import compose.icons.tablericons.Code
import compose.icons.tablericons.InfoCircle
import compose.icons.tablericons.Refresh
import compose.icons.tablericons.X
import dev.marcelsoftware.boosteroidplus.common.BottomSheetHost
import dev.marcelsoftware.boosteroidplus.common.ToastHost
import dev.marcelsoftware.boosteroidplus.common.rememberBottomSheetHostState
import dev.marcelsoftware.boosteroidplus.common.rememberToastHostState
import dev.marcelsoftware.boosteroidplus.ui.theme.AppColors
import dev.marcelsoftware.boosteroidplus.ui.theme.BoosteroidTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CrashActivity : ComponentActivity() {
    companion object {
        const val EXTRA_ERROR = "error_stack_trace"

        fun createIntent(
            context: Context,
            throwable: Throwable,
        ): Intent {
            return Intent(context, CrashActivity::class.java).apply {
                putExtra(EXTRA_ERROR, throwable.stackTraceToString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorString =
            intent.getStringExtra(EXTRA_ERROR)
                ?: getString(R.string.crash_no_details)

        setContent {
            BoosteroidTheme {
                CrashScreen(
                    errorInfo = errorString,
                    onRestartClick = {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    },
                    onReportClick = {
                        val intent =
                            Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://github.com/MarcelM264/Boosteroid-Plus/issues/new")
                            }
                        startActivity(intent)
                    },
                )
            }
        }
    }
}

@Composable
fun CrashScreen(
    errorInfo: String,
    onRestartClick: () -> Unit,
    onReportClick: () -> Unit,
) {
    val context = LocalContext.current
    val bottomSheetHostState = rememberBottomSheetHostState()
    val coroutineScope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val toastHostState = rememberToastHostState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TablerIcons.X,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.crash_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.crash_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Button(
                    onClick = onRestartClick,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Icon(
                        imageVector = TablerIcons.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(R.string.crash_restart_button))
                }

                OutlinedButton(
                    onClick = onReportClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = TablerIcons.BrandGithub,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(stringResource(R.string.crash_report_button))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val crashDetailsTitle = stringResource(R.string.crash_details_title)
            val crashCopiedToClipboard = stringResource(R.string.crash_copied_to_clipboard)

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        bottomSheetHostState.showBottomSheet(
                            title = crashDetailsTitle,
                            content = {
                                ErrorDetailsContent(
                                    errorInfo = errorInfo,
                                    onCopyClick = {
                                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Error Stack Trace", errorInfo)
                                        clipboardManager.setPrimaryClip(clip)
                                        copied = true
                                        coroutineScope.launch {
                                            toastHostState.showToast(crashCopiedToClipboard, icon = TablerIcons.InfoCircle)
                                        }
                                    },
                                    copied = copied,
                                )
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = TablerIcons.Code,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(R.string.crash_show_details_button))
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.crash_version_format, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        BottomSheetHost(state = bottomSheetHostState)
        ToastHost(toastHostState)
    }

    LaunchedEffect(copied) {
        if (copied) {
            delay(3000)
            copied = false
        }
    }
}

@Composable
fun ErrorDetailsContent(
    errorInfo: String,
    onCopyClick: () -> Unit,
    copied: Boolean,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.crash_stack_trace_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )

                IconButton(onClick = onCopyClick) {
                    Icon(
                        imageVector = if (copied) TablerIcons.ClipboardCheck else TablerIcons.Clipboard,
                        contentDescription = null,
                        tint = if (copied) MaterialTheme.colorScheme.primary else AppColors().iconColor,
                    )
                }
            }

            Divider()

            Spacer(modifier = Modifier.height(8.dp))

            if (errorInfo.isNotBlank()) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = errorInfo,
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                        modifier =
                            Modifier
                                .padding(vertical = 8.dp),
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.crash_no_details),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

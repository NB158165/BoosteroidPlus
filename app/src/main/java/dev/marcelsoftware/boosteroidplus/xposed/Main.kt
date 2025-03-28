package dev.marcelsoftware.boosteroidplus.xposed

import android.app.Activity
import android.app.AndroidAppHelper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.SeekBar
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import dev.marcelsoftware.boosteroidplus.BuildConfig
import dev.marcelsoftware.boosteroidplus.ResolutionManager
import dev.marcelsoftware.boosteroidplus.common.XAppPrefs
import dev.marcelsoftware.boosteroidplus.common.preferences.PrefKeys
import org.luckypray.dexkit.DexKitBridge
import kotlin.text.Regex

class Main : IXposedHookLoadPackage {
    companion object {
        const val TAG = "Boosteroid+"
        val prefs: XAppPrefs = XAppPrefs()

        val enabled
            get() = prefs.getBoolean(PrefKeys.ENABLED, false)
        val unlockFrameRate
            get() = enabled && prefs.getBoolean(PrefKeys.UNLOCK_FPS, false)
        val unlockBitRate
            get() = enabled && prefs.getBoolean(PrefKeys.UNLOCK_BITRATE, false)
        val extendOverNotch: Boolean
            get() = enabled && prefs.getBoolean(PrefKeys.EXTEND_INTO_NOTCH, true)
        val resolution: Int
            get() = if (enabled) prefs.getInt(PrefKeys.RESOLUTION, 0) else -1
        val aspectRatio: Int
            get() = if (enabled) prefs.getInt(PrefKeys.ASPECT_RATIO, -1) else -1

        var streamActivity: String = "com.boosteroid.streaming.UI.StreamActivity"
        var startActivity: String = "com.boosteroid.streaming.UI.StartActivity"
        var packageName: String = "com.boosteroid.streaming"

        init {
            System.loadLibrary("dexkit")
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == BuildConfig.APPLICATION_ID) {
            hookMethod(
                "dev.marcelsoftware.boosteroidplus.common.XAppPrefs\$Companion",
                lpparam.classLoader,
                "isModuleEnabled",
            ) {
                before { params ->
                    params.result = true
                }
            }
        }
        if (!lpparam.packageName.startsWith("com.boosteroid")) return

        if (lpparam.packageName == "com.boosteroidtv.streaming") {
            streamActivity = "com.boosteroidtv.streaming.UI.tv.StreamActivity"
            startActivity = "com.boosteroidtv.streamcom.boosteroidtv.streaming.network.wss.modeling.UI.tv.StartActivity"
        }

        packageName = lpparam.packageName

        Log.i(TAG, "Handling package: ${lpparam.packageName}")

        DexKitBridge.create(lpparam.appInfo.sourceDir).use { dexKitBridge ->
            Log.d(TAG, "DexKit bridge created successfully")
            hookBitrate()
            hookFrameRate(lpparam.classLoader)
            hookResolution(dexKitBridge, lpparam.classLoader)
            hookNotch(lpparam.classLoader)
            hookUri(lpparam.classLoader)
        }
    }
}

private fun hookBitrate() {
    Log.d(Main.TAG, "Setting up bitrate hooks")

    hookMethod(
        SeekBar::class.java,
        "setOnSeekBarChangeListener",
        SeekBar.OnSeekBarChangeListener::class.java,
    ) {
        before { param ->
            if (Main.enabled) {
                val seekBar = param.thisObject as SeekBar
                val oldMax = seekBar.max
                seekBar.max = 80
                Log.d(Main.TAG, "Modified SeekBar max value from $oldMax to 80")
            }
        }

        after { param ->
            if (!Main.enabled) {
                val seekBar = param.thisObject as SeekBar
                val oldMax = seekBar.max
                if (oldMax > 25) {
                    seekBar.max = 25
                    Log.d(Main.TAG, "Reset SeekBar max value from $oldMax to 25")
                }
            }
        }
    }
}

private fun hookFrameRate(classLoader: ClassLoader) {
    Log.d(Main.TAG, "Setting up frame rate hooks")

    hookMethod(
        "${Main.packageName}.network.wss.model.ParamsModel",
        classLoader,
        "setBitrate_max",
        Int::class.java,
    ) {
        before { param ->
            val bitRate = param.args[0] as Int

            if (!Main.unlockBitRate) {
                val defaultBitRate = 25 * 1000000
                if (bitRate > defaultBitRate) {
                    param.args[0] = defaultBitRate
                    Log.i(Main.TAG, "Limiting bitrate from $bitRate to default maximum: $defaultBitRate")
                } else {
                    Log.d(Main.TAG, "Bitrate within limits: $bitRate (max: $defaultBitRate)")
                }
            } else {
                Log.i(Main.TAG, "Allowing unlimited bitrate: $bitRate")
            }
        }

        after { param ->
            if (Main.unlockFrameRate) {
                val context = AndroidAppHelper.currentApplication().applicationContext
                val resolutionManager = ResolutionManager(context)
                val refreshRate = resolutionManager.displayRefreshRate

                val thisObject = param.thisObject
                XposedHelpers.callMethod(thisObject, "setFramerate_max", refreshRate)
                Log.i(Main.TAG, "Unlocked frame rate: Set maximum refresh rate to $refreshRate Hz")
            } else {
                Log.d(Main.TAG, "Frame rate unlocking disabled, keeping default settings")
            }
        }
    }
}

private fun hookResolution(
    dexKitBridge: DexKitBridge,
    classLoader: ClassLoader,
) {
    Log.d(Main.TAG, "Setting up resolution hooks")

    val foundMethods =
        dexKitBridge.findMethod {
            matcher {
                usingStrings("User-Agent")
                callerMethods {
                    add {
                        usingStrings("&y=")
                    }
                }
            }
        }

    if (foundMethods.isEmpty()) {
        Log.e(Main.TAG, "Failed to find method for resolution hook")
        return
    }

    Log.d(Main.TAG, "Found ${foundMethods.size} methods for resolution hook")

    foundMethods.singleOrNull()?.hook(classLoader) {
        before { param ->
            if (Main.resolution == -1) {
                Log.d(Main.TAG, "Native resolution selected, using app's default scaling")
                return@before
            }
            val context = AndroidAppHelper.currentApplication().applicationContext
            val resolutionManager = ResolutionManager(context)

            val originalWss = param.args[0] as String

            val selectedAspectRatio =
                if (Main.aspectRatio >= 0 && Main.aspectRatio < ResolutionManager.AspectRatio.entries.size) {
                    ResolutionManager.AspectRatio.entries[Main.aspectRatio]
                } else {
                    null
                }

            val availableResolutions = resolutionManager.getResolutionsForAspectRatio(selectedAspectRatio)

            val resIndex = Main.resolution.coerceIn(0, availableResolutions.lastIndex)
            val selectedResolution = availableResolutions[resIndex]

            Log.d(Main.TAG, "Using resolution index: $resIndex")
            Log.d(Main.TAG, "Selected aspect ratio: ${selectedAspectRatio?.label ?: "Native"}")

            val modifiedWss =
                originalWss
                    .replace(Regex("x=(\\d+)")) { matchResult ->
                        val oldWidth = matchResult.groupValues[1]
                        "x=${selectedResolution.first}".also {
                            Log.d(Main.TAG, "Changed width from $oldWidth to ${selectedResolution.first}")
                        }
                    }
                    .replace(Regex("y=(\\d+)")) { matchResult ->
                        val oldHeight = matchResult.groupValues[1]
                        "y=${selectedResolution.second}".also {
                            Log.d(Main.TAG, "Changed height from $oldHeight to ${selectedResolution.second}")
                        }
                    }

            param.args[0] = "$modifiedWss&tv=1"
            Log.i(Main.TAG, "Set resolution to ${selectedResolution.first} × ${selectedResolution.second}")
        }
    }
}

private fun hookNotch(classLoader: ClassLoader) {
    Log.d(Main.TAG, "Setting up notch display hooks")

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        Log.d(Main.TAG, "Skipping notch hooks - device API level too low (requires Android P or higher)")
        return
    }

    hookMethod(
        Main.streamActivity,
        classLoader,
        "onCreate",
        Bundle::class.java,
    ) {
        after { params ->
            val activity = params.thisObject as Activity

            if (!Main.extendOverNotch) {
                val oldMode = activity.window.attributes.layoutInDisplayCutoutMode
                activity.window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
                Log.i(Main.TAG, "Changed cutout mode from $oldMode to LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER")
            } else {
                Log.d(Main.TAG, "Using default cutout behavior - content can extend into notch area")
            }
        }
    }
}

fun hookUri(classLoader: ClassLoader) {
    hookMethod(Main.startActivity, classLoader, "onCreate", Bundle::class.java) {
        after { params ->
            val activity = params.thisObject as Activity
            val intent = activity.intent
            if (Intent.ACTION_VIEW == intent.action) {
                val data = intent.data ?: return@after

                handleBooleanQueryParam(data, PrefKeys.ENABLED)
                handleBooleanQueryParam(data, PrefKeys.UNLOCK_FPS)
                handleBooleanQueryParam(data, PrefKeys.UNLOCK_BITRATE)
                handleBooleanQueryParam(data, PrefKeys.EXTEND_INTO_NOTCH)

                handleIntQueryParam(data, PrefKeys.RESOLUTION)
                handleIntQueryParam(data, PrefKeys.ASPECT_RATIO)
            }
        }
    }
}

private fun handleBooleanQueryParam(
    data: Uri,
    prefKey: String,
) {
    data.getQueryParameter(prefKey)?.let { param ->
        Main.prefs.putBoolean(prefKey, param.equals("true", ignoreCase = true))
    }
}

private fun handleIntQueryParam(
    data: Uri,
    prefKey: String,
) {
    data.getQueryParameter(prefKey)?.let { param ->
        param.toIntOrNull()?.let { value ->
            Main.prefs.putInt(prefKey, value)
        }
    }
}

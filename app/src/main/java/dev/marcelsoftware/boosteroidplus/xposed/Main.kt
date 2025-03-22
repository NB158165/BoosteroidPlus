package dev.marcelsoftware.boosteroidplus.xposed

import android.app.AndroidAppHelper
import android.util.Log
import android.widget.SeekBar
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import dev.marcelsoftware.boosteroidplus.BuildConfig
import dev.marcelsoftware.boosteroidplus.ResolutionManager
import dev.marcelsoftware.boosteroidplus.common.preferences.PrefKeys
import org.luckypray.dexkit.DexKitBridge
import kotlin.text.Regex

class Main : IXposedHookLoadPackage, IXposedHookZygoteInit {
    companion object {
        const val TAG = "Boosteroid+"
        val prefs: XSharedPreferences =
            XSharedPreferences(
                BuildConfig.APPLICATION_ID,
                "ksp_default-${BuildConfig.APPLICATION_ID}",
            )

        val enabled
            get() = prefs.getKsBoolean(PrefKeys.ENABLED, false)
        val unlockFrameRate
            get() = enabled && prefs.getKsBoolean(PrefKeys.UNLOCK_FPS, false)
        val unlockBitRate
            get() = enabled && prefs.getKsBoolean(PrefKeys.UNLOCK_BITRATE, false)
        val resolution: Int
            get() = if (enabled) prefs.getKsInt(PrefKeys.RESOLUTION, 0) else -1
        val aspectRatio: Int
            get() = if (enabled) prefs.getKsInt(PrefKeys.ASPECT_RATIO, -1) else -1

        init {
            System.loadLibrary("dexkit")
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        Log.d(TAG, "Initialized with preferences at: ${prefs.file.absolutePath}")
        Log.d(TAG, enabled.toString())
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.boosteroid.streaming") return

        Log.i(TAG, "Handling package: ${lpparam.packageName}")

        DexKitBridge.create(lpparam.appInfo.sourceDir).use { dexKitBridge ->
            Log.d(TAG, "DexKit bridge created successfully")
            hookBitrate()
            hookFrameRate(lpparam.classLoader)
            hookResolution(dexKitBridge, lpparam.classLoader)
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
        "com.boosteroid.streaming.network.wss.model.ParamsModel",
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

    if (Main.resolution == -1) {
        Log.d(Main.TAG, "Native resolution selected, using app's default scaling")
        return
    }

    foundMethods.singleOrNull()?.hook(classLoader) {
        before { param ->
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

private fun XSharedPreferences.getKsBoolean(
    key: String,
    default: Boolean,
) = this.getString(key, default.toString()) != "false"

private fun XSharedPreferences.getKsInt(
    key: String,
    default: Int,
) = this.getString(key, default.toString())?.toIntOrNull() ?: default

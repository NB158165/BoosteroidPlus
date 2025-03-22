package dev.marcelsoftware.boosteroidplus

import android.content.Context
import android.graphics.Point
import android.util.Log
import android.view.WindowManager
import dev.marcelsoftware.boosteroidplus.xposed.Main
import kotlin.math.roundToInt

@Suppress("DEPRECATION")
class ResolutionManager(private val context: Context) {
    private val displayDimensions =
        Point().also {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getRealSize(it)
        }

    init {
        Log.d(Main.TAG, "Raw display dimensions - x: ${displayDimensions.x}, y: ${displayDimensions.y}")
    }

    val nativeWidth =
        if (displayDimensions.x > 0) {
            displayDimensions.x.coerceAtLeast(displayDimensions.y).coerceAtLeast(1920).also {
                Log.d(Main.TAG, "Calculated nativeWidth: $it")
            }
        } else {
            1920
        }

    val nativeHeight =
        if (displayDimensions.y > 0) {
            displayDimensions.x.coerceAtMost(displayDimensions.y).coerceAtLeast(1080).also {
                Log.d(Main.TAG, "Calculated nativeHeight: $it")
            }
        } else {
            1080
        }

    val displayRefreshRate: Int =
        run {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val refreshRate = display.refreshRate
            refreshRate.roundToInt()
        }

    fun getResolutionsForAspectRatio(aspectRatio: AspectRatio?): List<Pair<Int, Int>> {
        if (aspectRatio == null) {
            val nativeRatio = nativeWidth.toFloat() / nativeHeight.toFloat()
            return listOf(
                Pair(nativeWidth, nativeHeight),
                Pair(1920, (1920 / nativeRatio).roundToInt()),
                Pair(2560, (2560 / nativeRatio).roundToInt()),
                Pair(3840, (3840 / nativeRatio).roundToInt()),
            )
        }

        val resolutions = aspectRatio.getResolutions().sortedByDescending { it.first }

        val nativeAdjustedHeight = (nativeWidth / aspectRatio.ratio).roundToInt()
        return listOf(Pair(nativeWidth, nativeAdjustedHeight)) + resolutions
    }

    fun getDisplayStringForResolution(
        resolution: Pair<Int, Int>,
        isNative: Boolean = false,
    ): String {
        val (width, height) = resolution

        val displayName =
            when {
                isNative -> "Native"
                width == nativeWidth -> "Native (Adjusted)"
                width >= 3840 -> "4K"
                width >= 3200 -> "3K"
                width >= 2560 -> "2K/QHD"
                width >= 1920 -> "Full HD"
                width >= 1280 -> "HD"
                else -> "${width}p"
            }

        return "$displayName ($width × $height)"
    }

    enum class AspectRatio(val ratio: Float, val label: String) {
        RATIO_2_37_1(2.37f, "2.37:1") {
            override fun getResolutions(): List<Pair<Int, Int>> =
                listOf(
                    Pair(1920, 810),
                )
        },
        RATIO_21_9(21f / 9f, "21:9") {
            override fun getResolutions(): List<Pair<Int, Int>> =
                listOf(
                    Pair(3440, 1440),
                    Pair(2560, 1080),
                    Pair(2400, 1080),
                    Pair(1920, 864),
                    Pair(1600, 720),
                )
        },
        RATIO_16_9(16f / 9f, "16:9") {
            override fun getResolutions(): List<Pair<Int, Int>> =
                listOf(
                    Pair(3840, 2160),
                    Pair(3200, 1800),
                    Pair(3200, 1440),
                    Pair(2560, 1440),
                    Pair(2048, 1152),
                    Pair(1920, 1080),
                    Pair(1600, 900),
                    Pair(1366, 768),
                    Pair(1360, 768),
                    Pair(1280, 720),
                )
        },
        RATIO_16_10(16f / 10f, "16:10") {
            override fun getResolutions(): List<Pair<Int, Int>> =
                listOf(
                    Pair(3360, 2100),
                    Pair(3320, 2160),
                    Pair(2940, 1912),
                    Pair(2732, 2048),
                    Pair(2560, 1600),
                    Pair(2160, 1350),
                    Pair(2048, 1330),
                    Pair(2048, 1152),
                    Pair(1920, 1200),
                    Pair(1800, 1168),
                    Pair(1680, 1050),
                    Pair(1440, 900),
                    Pair(1280, 800),
                )
        },
        RATIO_4_3(4f / 3f, "4:3") {
            override fun getResolutions(): List<Pair<Int, Int>> =
                listOf(
                    Pair(2048, 1536),
                    Pair(1920, 1536),
                    Pair(1800, 1350),
                    Pair(1600, 1200),
                    Pair(1440, 1152),
                    Pair(1400, 1050),
                    Pair(1280, 960),
                    Pair(1152, 864),
                    Pair(1024, 768),
                    Pair(800, 600),
                )
        },
        RATIO_5_4(5f / 4f, "5:4") {
            override fun getResolutions(): List<Pair<Int, Int>> =
                listOf(
                    Pair(2560, 2048),
                    Pair(1600, 1024),
                    Pair(1280, 1024),
                )
        }, ;

        abstract fun getResolutions(): List<Pair<Int, Int>>
    }
}

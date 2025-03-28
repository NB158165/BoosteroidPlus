package dev.marcelsoftware.boosteroidplus.common

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import de.robv.android.xposed.XSharedPreferences
import dev.marcelsoftware.boosteroidplus.BuildConfig
import dev.marcelsoftware.boosteroidplus.common.preferences.PrefKeys
import dev.marcelsoftware.boosteroidplus.xposed.Main
import kotlin.reflect.KProperty

class XAppPrefs {
    companion object {
        fun isModuleEnabled() = false
    }

    private var isRootless: Boolean = false

    private fun getRootlessSharedPreferences(): SharedPreferences {
        return AndroidAppHelper.currentApplication().applicationContext.getSharedPreferences("boosteroid_plus", Context.MODE_PRIVATE);
    }

    private val xSharedPreferences: XSharedPreferences = XSharedPreferences(BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID)

    fun getBoolean(
        key: String,
        defaultValue: Boolean,
    ): Boolean {
        xSharedPreferences.reload()
        return if (isRootless) getRootlessSharedPreferences().getBoolean(key, defaultValue) else xSharedPreferences.getBoolean(key, defaultValue)
    }

    fun getInt(
        key: String,
        defaultValue: Int,
    ): Int {
        xSharedPreferences.reload()
        return if (isRootless) getRootlessSharedPreferences().getInt(key, defaultValue) else xSharedPreferences.getInt(key, defaultValue)
    }

    fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        isRootless = true
        getRootlessSharedPreferences().edit(commit = true) {
            putBoolean(key, value)
        }
    }

    fun putInt(
        key: String,
        value: Int,
    ) {
        isRootless = true
        getRootlessSharedPreferences().edit(commit = true) {
            putInt(key, value)
        }
    }
}

@Stable
class BooleanPreference(
    private val preferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Boolean,
) : MutableState<Boolean> {
    private val state = mutableStateOf(preferences.getBoolean(key, defaultValue))

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): Boolean {
        return value
    }

    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        newValue: Boolean,
    ) {
        value = newValue
    }

    override var value: Boolean
        get() = state.value
        set(newValue) {
            if (state.value != newValue) {
                preferences.edit {
                    putBoolean(key, newValue)
                }
                state.value = newValue
            }
        }

    override fun component1(): Boolean = value

    override fun component2(): (Boolean) -> Unit = { value = it }
}

@Stable
class IntPreference(
    private val preferences: SharedPreferences,
    private val key: String,
    private val defaultValue: Int,
) : MutableState<Int> {
    private val state = mutableIntStateOf(preferences.getInt(key, defaultValue))

    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): Int {
        return value
    }

    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        newValue: Int,
    ) {
        value = newValue
    }

    override var value: Int
        get() = state.intValue
        set(newValue) {
            if (state.intValue != newValue) {
                preferences.edit {
                    putInt(key, newValue)
                }
                state.intValue = newValue
            }
        }

    override fun component1(): Int = value

    override fun component2(): (Int) -> Unit = { value = it }
}

@Composable
@SuppressLint("WorldReadableFiles")
fun rememberPreferences(): SharedPreferences {
    val context = LocalContext.current
    return remember {
        @Suppress("DEPRECATION")
        context.getSharedPreferences(
            BuildConfig.APPLICATION_ID,
            if (XAppPrefs.isModuleEnabled()) Context.MODE_WORLD_READABLE else Context.MODE_PRIVATE,
        )
    }
}

@Composable
fun rememberBooleanPreference(
    key: String,
    defaultValue: Boolean = false,
): BooleanPreference {
    val prefs = rememberPreferences()
    return remember(key) {
        BooleanPreference(prefs, key, defaultValue)
    }
}

@Composable
fun rememberIntPreference(
    key: String,
    defaultValue: Int = 0,
): IntPreference {
    val prefs = rememberPreferences()
    return remember(key) {
        IntPreference(prefs, key, defaultValue)
    }
}

package dev.marcelsoftware.boosteroidplus

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.cioccarellia.ksprefs.KsPrefs

class App : Application() {
    @SuppressLint("WorldReadableFiles")
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var appContext: Context
        val prefs by lazy {
            KsPrefs(appContext) {
                mode = Context.MODE_WORLD_READABLE
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        appContext = this
    }
}

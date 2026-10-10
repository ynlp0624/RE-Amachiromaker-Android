package cc.nekoha.amachiromaker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import cc.nekoha.amachiromaker.data.AssetStore
import cc.nekoha.amachiromaker.data.KEY_THEME
import cc.nekoha.amachiromaker.data.MakerState
import cc.nekoha.amachiromaker.ui.AmachiromakerTheme
import cc.nekoha.amachiromaker.ui.MakerScreen

const val PREFS_NAME = "amachiromaker"
const val KEY_NOTICE = "noticeAccepted"

private const val BAR_LIGHT = 0xFFDCEBFB.toInt()
private const val BAR_DARK = 0xFF0D1822.toInt()

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val store = AssetStore(applicationContext)
        val state = MakerState(store, prefs)

        setContent {
            var themeMode by remember { mutableStateOf(prefs.getString(KEY_THEME, "system") ?: "system") }
            var noticeAccepted by remember { mutableStateOf(prefs.getBoolean(KEY_NOTICE, false)) }

            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            val view = LocalView.current
            SideEffect {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                window.statusBarColor = if (darkTheme) BAR_DARK else BAR_LIGHT
                window.navigationBarColor = if (darkTheme) BAR_DARK else BAR_LIGHT
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            LaunchedEffect(Unit) {
                if (!state.isReady) state.bootstrap()
            }

            AmachiromakerTheme(darkTheme = darkTheme) {
                MakerScreen(
                    state = state,
                    store = store,
                    darkTheme = darkTheme,
                    onToggleTheme = {
                        val next = if (darkTheme) "light" else "dark"
                        themeMode = next
                        prefs.edit().putString(KEY_THEME, next).apply()
                    },
                    noticeAccepted = noticeAccepted,
                    onAcceptNotice = {
                        noticeAccepted = true
                        prefs.edit().putBoolean(KEY_NOTICE, true).apply()
                    }
                )
            }
        }
    }
}

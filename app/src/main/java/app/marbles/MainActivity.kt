package app.marbles

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marbles.data.Settings
import app.marbles.notify.Alarms
import app.marbles.ui.MarblesRoot
import app.marbles.ui.theme.MarblesTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A dark table under a lamp: light icons in the bars.
        val style = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        if (savedInstanceState == null) opened(intent)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            MarblesTheme {
                // Wait for settings so the welcome screen doesn't flash for a family already here.
                settings?.let { MarblesRoot(it) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        opened(intent)
    }

    /** A tapped reminder: remember where it wants to go. */
    private fun opened(intent: Intent?) {
        val open = intent?.getStringExtra(Alarms.EXTRA_OPEN) ?: return
        container.open.value = open to intent.getLongExtra(Alarms.EXTRA_CHILD, 0)
    }
}

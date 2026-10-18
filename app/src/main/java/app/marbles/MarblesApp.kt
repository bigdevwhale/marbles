package app.marbles

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.marbles.data.DemoSeed
import app.marbles.data.MarblesDatabase
import app.marbles.data.SettingsStore
import app.marbles.data.Vault
import app.marbles.notify.Alarms
import app.marbles.usage.Usage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = MarblesDatabase.create(context)
    val settings = SettingsStore(context)
    val vault = Vault(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Where a tapped reminder wants to go ("idea:pancakes", "plan", "letter", …) and for which child. */
    val open = MutableStateFlow<Pair<String, Long>?>(null)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch {
            block()
            changed()
        }
    }

    /** Something changed: the next reminder may be a different one now. */
    suspend fun changed() = Alarms.reschedule(context)
}

class MarblesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Alarms.ensureChannels(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@MarblesApp, container)
            val s = container.settings.now()
            // A reboot clears the quiet clock of time under way; bring it back.
            if (s.session.active) {
                val kids = container.db.dao().children().filter { ",${it.id}," in s.session.kids }
                Alarms.showTogether(this@MarblesApp, s.session, kids.joinToString(", ") { it.name })
            }
            runCatching { Usage.record(this@MarblesApp, container.db.dao(), s) }
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as MarblesApp).container

/** Resources in the in-app language, for receivers that AppCompat doesn't localise before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

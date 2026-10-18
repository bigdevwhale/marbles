package app.marbles

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import app.marbles.data.DemoSeed
import app.marbles.data.Settings
import app.marbles.ui.MarblesRoot
import app.marbles.ui.theme.MarblesTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the main screens with the demo family to PNGs (run with `-Prender`): a look without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class RenderScreens {
    @get:Rule
    val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val out = File(System.getProperty("render.out") ?: "build/art", "screens").apply { mkdirs() }
    private val demo = File(System.getProperty("render.photos") ?: "src/demo/assets/demo")

    private fun settle(ms: Long = 3000) {
        rule.mainClock.autoAdvance = false
        var t = 0L
        while (t < ms) {
            Thread.sleep(60)
            rule.mainClock.advanceTimeBy(100)
            t += 100
        }
    }

    private fun shot(name: String) {
        // Draw the window by hand: capturing waits for a frame, which a paused clock never gives.
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        rule.mainClock.advanceTimeByFrame()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val root = rule.activity.window.decorView
        val b = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(b))
        File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tap(text: String, ms: Long = 2500, exact: Boolean = false) {
        settle(600)
        rule.onAllNodesWithText(text, substring = !exact, useUnmergedTree = exact)[0].performClick()
        settle(ms)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
    }

    /** Scrolls the screen by a swipe: scrolling to a node waits for an idle the paused clock never gives. */
    private fun swipe(times: Int = 1) {
        repeat(times) {
            rule.onRoot().performTouchInput { swipeUp(startY = bottom * 0.8f, endY = bottom * 0.25f, durationMillis = 300) }
            settle(800)
        }
    }

    private fun back() {
        rule.onAllNodesWithContentDescription("Back")[0].performClick()
        settle(1500)
    }

    @Test
    fun screens() {
        val app = ApplicationProvider.getApplicationContext<MarblesApp>()
        runBlocking {
            DemoSeed.seedIfEmpty(app, app.container, list = { demo.list()?.toList().orEmpty() }) { name -> File(demo, name.removePrefix("demo/")).inputStream() }
        }
        rule.setContent {
            val s: Settings? by app.container.settings.settings.collectAsState(initial = null)
            MarblesTheme { s?.let { MarblesRoot(it) } }
        }
        settle(5000)
        shot("1-jar")
        swipe(2)
        shot("2-jar-tray")
        tap("Tonight", exact = true)
        shot("3-tonight")
        swipe(2)
        shot("3b-tonight-more")
        swipe(3)
        shot("3c-tonight-end")
        tap("Mirror", exact = true)
        shot("4-mirror")
        swipe(2)
        shot("4b-mirror-more")
        tap("Kept", exact = true)
        shot("5-memories")
        tap("The month's letter")
        settle(2000)
        shot("6-letter")
        back()
        tap("Jar", exact = true)
        tap("Petya", exact = true)
        shot("7-jar-petya")
        tap("Petya", exact = true)
        shot("8-child")
        back()
        rule.onAllNodesWithContentDescription("Together")[0].performClick()
        settle(2500)
        shot("9-start")
    }
}

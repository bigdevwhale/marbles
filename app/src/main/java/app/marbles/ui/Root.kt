package app.marbles.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.marbles.BuildConfig
import app.marbles.R
import app.marbles.container
import app.marbles.data.Child
import app.marbles.data.Evening
import app.marbles.data.Interest
import app.marbles.data.Plan
import app.marbles.data.Settings
import app.marbles.data.Together
import app.marbles.domain.Jar
import app.marbles.domain.Week
import app.marbles.domain.Weeks
import app.marbles.notify.Alarms
import app.marbles.ui.child.ChildScreen
import app.marbles.ui.components.MarbleDot
import app.marbles.ui.components.Look
import app.marbles.ui.components.rememberNow
import app.marbles.ui.deck.DeckScreen
import app.marbles.ui.jar.JarTab
import app.marbles.ui.jar.WeekScreen
import app.marbles.ui.letter.LetterScreen
import app.marbles.ui.log.LogScreen
import app.marbles.ui.memories.MemoriesTab
import app.marbles.ui.mirror.MirrorTab
import app.marbles.ui.onboarding.OnboardingScreen
import app.marbles.ui.plan.PlanScreen
import app.marbles.ui.settings.SettingsScreen
import app.marbles.ui.theme.Manrope
import app.marbles.ui.theme.U
import app.marbles.ui.together.StartSheet
import app.marbles.ui.together.TogetherScreen
import app.marbles.usage.Usage
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class Tab { JAR, TONIGHT, MIRROR, MEMORIES }

/** Everything stored, plus what is derived from it, recomputed every minute or on change. */
@Immutable
class House(
    val children: List<Child>,
    val together: List<Together>,
    val interests: List<Interest>,
    val plans: List<Plan>,
    val evenings: List<Evening>,
    val settings: Settings,
    val now: Long,
) {
    val zone: ZoneId = ZoneId.systemDefault()
    val today: LocalDate = LocalDate.now(zone)
    private val trays = HashMap<Long, List<Week>>()

    fun child(id: Long) = children.firstOrNull { it.id == id }
    fun entry(id: Long) = together.firstOrNull { it.id == id }
    fun kidsOf(t: Together) = t.kidIds.mapNotNull { child(it) }
    fun kidsOf(p: Plan) = p.kidIds.mapNotNull { child(it) }

    fun of(id: Long): List<Together> = together.filter { it.of(id) }
    fun interestsOf(id: Long) = interests.filter { it.childId == id }
    fun age(c: Child) = Jar.age(c.birthday, today).years

    val startedAt: LocalDate get() = Jar.date(if (settings.startedAt > 0) settings.startedAt else now, zone)

    /** Every week since the jar was started, oldest first, this week last. */
    fun tray(c: Child): List<Week> = trays.getOrPut(c.id) { Weeks.tray(c, together, startedAt, settings.weekGoal, today, zone) }
    fun thisWeek(c: Child): Week = tray(c).last()

    /** The children still under eighteen, the ones the jar is about. */
    val young: List<Child> get() = children.filter { !Jar.grown(it, today) }
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController, val start: () -> Unit) {
    fun child(id: Long) = nav.navigate("child/$id") { launchSingleTop = true }
    fun log(id: Long, kid: Long = 0, idea: String = "", question: String = "") = nav.navigate("log/$id?kid=$kid&idea=$idea&question=$question")
    fun week(child: Long, monday: LocalDate) = nav.navigate("week/$child/${monday.toEpochDay()}")
    fun deck(child: Long, rituals: Boolean = false) = nav.navigate("deck/$child?rituals=$rituals") { launchSingleTop = true }
    fun letter(month: YearMonth, year: Boolean = false) = nav.navigate("letter/${month.year}/${month.monthValue}?year=$year")
    fun plan(kid: Long = 0, alone: Boolean = false, idea: String = "") = nav.navigate("plan?kid=$kid&alone=$alone&idea=$idea") { launchSingleTop = true }
    fun together() = nav.navigate("together") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun home() { nav.popBackStack("main", false) }
    fun back() { nav.popBackStack() }
}

@Composable
fun MarblesRoot(settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(Tab.JAR) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }
    val dao = container.db.dao()

    val children by dao.observeChildren().collectAsStateWithLifecycle(initialValue = null)
    val together by dao.observeTogether().collectAsStateWithLifecycle(initialValue = null)
    val interests by dao.observeInterests().collectAsStateWithLifecycle(initialValue = null)
    val plans by dao.observePlans().collectAsStateWithLifecycle(initialValue = null)
    val evenings by dao.observeEvenings().collectAsStateWithLifecycle(initialValue = null)
    val tick = rememberNow(60_000)
    val house = remember(children, together, interests, plans, evenings, settings, tick) {
        House(children.orEmpty(), together.orEmpty(), interests.orEmpty(), plans.orEmpty(), evenings.orEmpty(), settings, System.currentTimeMillis())
    }
    var picked by rememberSaveable { mutableLongStateOf(0L) }
    val child = house.child(picked) ?: house.young.firstOrNull() ?: house.children.firstOrNull()
    var sheet by remember { mutableStateOf(false) }
    val go = remember(nav) { Go(nav, start = { sheet = true }) }

    // Every time the app comes forward, keep the evenings the phone still remembers.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        container.scope.launch { runCatching { Usage.record(context, dao, container.settings.now()) } }
    }
    // Opened while time together is under way: straight back to its clock.
    LaunchedEffect(Unit) { if (settings.onboarded && settings.session.active && container.open.value == null) go.together() }
    // Onboarded elsewhere (a restored backup, the demo seed): leave the welcome screens.
    // Keyed on the route too: the demo seed can finish before the nav graph has its first destination.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }
    // A tapped reminder.
    val open by container.open.collectAsStateWithLifecycle()
    LaunchedEffect(open, house.children.isNotEmpty()) {
        val (where, who) = open ?: return@LaunchedEffect
        if (house.children.isEmpty()) return@LaunchedEffect
        container.open.value = null
        if (who > 0) picked = who
        val k = house.child(who) ?: child ?: return@LaunchedEffect
        go.home()
        when {
            where.startsWith("idea:") -> go.deck(k.id)
            where == "together" -> if (house.settings.session.active) go.together() else sheet = true
            where == "plan" -> go.plan()
            where == "letter" -> go.letter(YearMonth.from(house.today).minusMonths(1))
            where == "child" -> go.child(k.id)
        }
    }

    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.scope.launch { container.changed() }
    }
    LaunchedEffect(house.together.isNotEmpty()) {
        // Asked once there is something to be reminded about: after the first time together.
        if (house.together.isNotEmpty() && android.os.Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(context) && !BuildConfig.DEMO) {
            askNotify.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(U.c.bg),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            composable("welcome") {
                OnboardingScreen { k ->
                    container.write {
                        val id = db.dao().insert(k)
                        this.settings.update { it.copy(onboarded = true, startedAt = System.currentTimeMillis()) }
                        picked = id
                    }
                    nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
                }
            }
            composable("main") {
                if (child == null) {
                    // Every child removed: back to adding one.
                    LaunchedEffect(house.children.isEmpty()) { if (children != null && house.children.isEmpty()) go.child(0) }
                    Box(Modifier.fillMaxSize().background(U.c.bg))
                } else MainTabs(tab, { tab = it }, go, house, child) { picked = it }
            }
            composable("child/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                ChildScreen(e.arguments?.getLong("id") ?: 0L, house, go) { picked = it }
            }
            composable(
                "log/{id}?kid={kid}&idea={idea}&question={question}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("kid") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("idea") { type = NavType.StringType; defaultValue = "" },
                    navArgument("question") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { e ->
                val a = e.arguments
                LogScreen(a?.getLong("id") ?: 0L, a?.getLong("kid") ?: 0L, a?.getString("idea").orEmpty(), a?.getString("question").orEmpty(), house, go)
            }
            composable(
                "week/{child}/{monday}",
                arguments = listOf(navArgument("child") { type = NavType.LongType }, navArgument("monday") { type = NavType.LongType }),
            ) { e ->
                WeekScreen(e.arguments?.getLong("child") ?: 0L, LocalDate.ofEpochDay(e.arguments?.getLong("monday") ?: 0L), house, go)
            }
            composable(
                "deck/{child}?rituals={rituals}",
                arguments = listOf(navArgument("child") { type = NavType.LongType }, navArgument("rituals") { type = NavType.BoolType; defaultValue = false }),
            ) { e ->
                DeckScreen(e.arguments?.getLong("child") ?: 0L, e.arguments?.getBoolean("rituals") ?: false, house, go)
            }
            composable(
                "letter/{y}/{m}?year={year}",
                arguments = listOf(navArgument("y") { type = NavType.IntType }, navArgument("m") { type = NavType.IntType }, navArgument("year") { type = NavType.BoolType; defaultValue = false }),
            ) { e ->
                val a = e.arguments
                LetterScreen(YearMonth.of(a?.getInt("y") ?: 2026, a?.getInt("m") ?: 1), a?.getBoolean("year") ?: false, house, go)
            }
            composable(
                "plan?kid={kid}&alone={alone}&idea={idea}",
                arguments = listOf(
                    navArgument("kid") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("alone") { type = NavType.BoolType; defaultValue = false },
                    navArgument("idea") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { e ->
                val a = e.arguments
                PlanScreen(a?.getLong("kid") ?: 0L, a?.getBoolean("alone") ?: false, a?.getString("idea").orEmpty(), house, go)
            }
            composable("together") { TogetherScreen(house, go) }
            composable("settings") { SettingsScreen(house, go) }
        }

        if (sheet && child != null) StartSheet(house, child, go, onDismiss = { sheet = false })
    }
}

fun openAppSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, house: House, child: Child, onPick: (Long) -> Unit) {
    val running = house.settings.session.active
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.JAR -> JarTab(house, child, go, onPick)
                Tab.TONIGHT -> app.marbles.ui.tonight.TonightTab(house, child, go, onPick)
                Tab.MIRROR -> MirrorTab(house, go)
                Tab.MEMORIES -> MemoriesTab(house, child, go, onPick)
            }
        }
        HorizontalDivider(color = U.c.line)
        Row(
            Modifier.fillMaxWidth().background(U.c.surface).navigationBarsPadding().height(66.dp).padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(null, stringResource(R.string.tab_jar), tab == Tab.JAR) { onTab(Tab.JAR) }
            NavItem(Icons.Rounded.LightMode, stringResource(R.string.tab_tonight), tab == Tab.TONIGHT) { onTab(Tab.TONIGHT) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.offset(y = (-10).dp).size(62.dp).shadow(8.dp, CircleShape).clip(CircleShape)
                        .background(if (running) U.c.lamp else U.c.accent)
                        .border(3.dp, U.c.surface, CircleShape)
                        .clickable { if (running) go.together() else go.start() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Timer, stringResource(R.string.together_start), Modifier.size(28.dp), tint = U.c.bg) }
            }
            NavItem(Icons.Rounded.Contrast, stringResource(R.string.tab_mirror), tab == Tab.MIRROR) { onTab(Tab.MIRROR) }
            NavItem(Icons.Rounded.AutoStories, stringResource(R.string.tab_memories), tab == Tab.MEMORIES) { onTab(Tab.MEMORIES) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector?, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.accent else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.accentSoft else Color.Transparent).padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(22.dp), tint = color)
            else MarbleDot(Look(if (selected) U.c.accent else U.c.faint, if (selected) 1f else 0.6f, 0.3f), size = 22.dp)
        }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Manrope, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
}

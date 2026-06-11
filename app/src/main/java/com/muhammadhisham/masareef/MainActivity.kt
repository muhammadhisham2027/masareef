package com.muhammadhisham.masareef

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.muhammadhisham.masareef.data.Budget
import com.muhammadhisham.masareef.data.Expense
import com.muhammadhisham.masareef.data.ExpenseDb
import com.muhammadhisham.masareef.data.toLocalDate
import com.muhammadhisham.masareef.ui.AddExpenseSheet
import com.muhammadhisham.masareef.ui.Amber500
import com.muhammadhisham.masareef.ui.BiometricHelper
import com.muhammadhisham.masareef.ui.BudgetsScreen
import com.muhammadhisham.masareef.ui.CsvExport
import com.muhammadhisham.masareef.ui.ExpensesScreen
import com.muhammadhisham.masareef.ui.MasareefDark
import com.muhammadhisham.masareef.ui.MasareefLight
import com.muhammadhisham.masareef.ui.OnboardingScreen
import com.muhammadhisham.masareef.ui.SettingsScreen
import com.muhammadhisham.masareef.ui.StatsScreen
import java.time.YearMonth

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MasareefApp(activity = this) }
    }
}

@Composable
fun MasareefApp(activity: ComponentActivity) {
    val context   = LocalContext.current
    val prefs     = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val db        = remember { ExpenseDb.get(context) }

    // ── Persistent state ─────────────────────────────────────────────────────
    var expenses  by remember { mutableStateOf(db.getAll()) }
    var budgets   by remember { mutableStateOf(
        db.getBudgets(YearMonth.now().monthValue, YearMonth.now().year)
    ) }
    var darkMode  by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var bioEnabled by remember { mutableStateOf(prefs.getBoolean("bio_enabled", false)) }
    var onboarded by remember { mutableStateOf(prefs.getBoolean("onboarded", false)) }
    var unlocked  by remember { mutableStateOf(!bioEnabled) }
    var showAdd   by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Expense?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val refresh = {
        expenses = db.getAll()
        budgets  = db.getBudgets(YearMonth.now().monthValue, YearMonth.now().year)
    }

    // Refresh on resume (catches SMS captured in background)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Auto-generate recurring expenses at month start
    LaunchedEffect(Unit) {
        val lastRun = prefs.getString("recurring_run", "")
        val nowKey  = YearMonth.now().toString()
        if (lastRun != nowKey) {
            val recurring = expenses.filter { it.isRecurring }
            val thisMonth = YearMonth.now()
            recurring.forEach { template ->
                val alreadyExists = expenses.any {
                    it.merchant == template.merchant &&
                    it.category == template.category &&
                    YearMonth.from(it.toLocalDate()) == thisMonth &&
                    it.source == "recurring"
                }
                if (!alreadyExists) {
                    db.insertExpense(template.copy(
                        id        = 0L,
                        timestamp = System.currentTimeMillis(),
                        source    = "recurring",
                        smsHash   = null
                    ))
                }
            }
            prefs.edit().putString("recurring_run", nowKey).apply()
            refresh()
        }
    }

    MaterialTheme(colorScheme = if (darkMode) MasareefDark else MasareefLight) {

        // ── Onboarding ───────────────────────────────────────────────────────
        if (!onboarded) {
            OnboardingScreen(onDone = {
                prefs.edit().putBoolean("onboarded", true).apply()
                onboarded = true
            })
            return@MaterialTheme
        }

        // ── Biometric lock screen ────────────────────────────────────────────
        if (!unlocked) {
            LaunchedEffect(Unit) {
                if (activity is androidx.fragment.app.FragmentActivity) {
                    BiometricHelper.authenticate(
                        activity  = activity,
                        onSuccess = { unlocked = true },
                        onFail    = { /* keep locked */ }
                    )
                } else { unlocked = true }
            }
            Box {} // blank screen while auth dialog shows
            return@MaterialTheme
        }

        // ── Main scaffold ────────────────────────────────────────────────────
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    listOf(
                        Triple(0, Icons.AutoMirrored.Filled.List, "Expenses"),
                        Triple(1, Icons.Filled.BarChart,           "Stats"),
                        Triple(2, Icons.Filled.Savings,            "Budgets"),
                        Triple(3, Icons.Filled.Settings,           "Settings")
                    ).forEach { (idx, icon, label) ->
                        NavigationBarItem(
                            selected = selectedTab == idx,
                            onClick  = { selectedTab = idx },
                            icon     = { Icon(icon, null) },
                            label    = { Text(label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = selectedTab == 0,
                    enter   = fadeIn() + slideInVertically { it },
                    exit    = fadeOut() + slideOutVertically { it }
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showAdd = true },
                        icon    = { Icon(Icons.Filled.Add, null) },
                        text    = { Text("Add expense") }
                    )
                }
            }
        ) { pad ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 12 })
                        .togetherWith(fadeOut(tween(150)))
                },
                modifier = Modifier.padding(pad),
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    0 -> ExpensesScreen(
                        expenses = expenses,
                        onDelete = { db.deleteExpense(it.id); refresh() },
                        onEdit   = { editTarget = it }
                    )
                    1 -> StatsScreen(expenses)
                    2 -> BudgetsScreen(
                        expenses = expenses,
                        budgets  = budgets,
                        onSave   = { db.upsertBudget(it); refresh() },
                        onDelete = {
                            db.deleteBudget(it.category, it.month, it.year); refresh()
                        }
                    )
                    else -> SettingsScreen(
                        expenses         = expenses,
                        darkMode         = darkMode,
                        biometricEnabled = bioEnabled,
                        onToggleDark     = { v ->
                            darkMode = v
                            prefs.edit().putBoolean("dark_mode", v).apply()
                        },
                        onToggleBiometric = { v ->
                            bioEnabled = v
                            prefs.edit().putBoolean("bio_enabled", v).apply()
                        },
                        onDataChanged = refresh,
                        onExportCsv   = { CsvExport.share(context, expenses) }
                    )
                }
            }
        }

        // ── Add / Edit sheet ─────────────────────────────────────────────────
        if (showAdd || editTarget != null) {
            AddExpenseSheet(
                existing  = editTarget,
                onDismiss = { showAdd = false; editTarget = null },
                onSave    = { expense ->
                    if (expense.id == 0L) db.insertExpense(expense)
                    else db.updateExpense(expense)
                    refresh()
                    showAdd    = false
                    editTarget = null
                }
            )
        }
    }
}

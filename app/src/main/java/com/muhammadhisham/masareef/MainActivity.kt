package com.muhammadhisham.masareef

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.muhammadhisham.masareef.data.Expense
import com.muhammadhisham.masareef.data.ExpenseDb
import com.muhammadhisham.masareef.data.toLocalDate
import com.muhammadhisham.masareef.ui.AddExpenseSheet
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

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MasareefApp(activity = this) }
    }
}

@Composable
fun MasareefApp(activity: FragmentActivity) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val db = remember { ExpenseDb.get(context) }

    // ── Persistent state ─────────────────────────────────────────────────────
    var expenses by remember { mutableStateOf(db.getAll()) }
    var budgets by remember {
        mutableStateOf(db.getBudgets(YearMonth.now().monthValue, YearMonth.now().year))
    }
    var darkMode   by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var bioEnabled by remember { mutableStateOf(prefs.getBoolean("bio_enabled", false)) }
    var onboarded  by remember { mutableStateOf(prefs.getBoolean("onboarded", false)) }
    var unlocked   by remember { mutableStateOf(!prefs.getBoolean("bio_enabled", false)) }
    var showAdd    by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Expense?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val refresh = {
        expenses = db.getAll()
        budgets = db.getBudgets(YearMonth.now().monthValue, YearMonth.now().year)
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

    // ── Recurring expenses: generate once per month, no duplicates ──────────
    LaunchedEffect(Unit) {
        val nowKey = YearMonth.now().toString()
        if (prefs.getString("recurring_run", "") != nowKey) {
            val thisMonth = YearMonth.now()
            val all = db.getAll()

            // De-duplicate templates: a template + last month's generated copy
            // share merchant/category/amount, so keep only the newest of each.
            val templates = all
                .filter { it.isRecurring }
                .groupBy { Triple(it.merchant.lowercase(), it.category, it.amount) }
                .mapNotNull { (_, group) -> group.maxByOrNull { it.timestamp } }

            templates.forEach { template ->
                val coveredThisMonth = all.any {
                    it.merchant.equals(template.merchant, ignoreCase = true) &&
                    it.category == template.category &&
                    YearMonth.from(it.toLocalDate()) == thisMonth
                }
                if (!coveredThisMonth) {
                    db.insertExpense(
                        template.copy(
                            id = 0L,
                            timestamp = System.currentTimeMillis(),
                            source = "recurring",
                            smsHash = null
                        )
                    )
                }
            }
            prefs.edit().putString("recurring_run", nowKey).apply()
            refresh()
        }
    }

    MaterialTheme(colorScheme = if (darkMode) MasareefDark else MasareefLight) {

        // ── Onboarding (first launch) ────────────────────────────────────────
        if (!onboarded) {
            OnboardingScreen(onDone = {
                prefs.edit().putBoolean("onboarded", true).apply()
                onboarded = true
            })
            return@MaterialTheme
        }

        // ── Biometric lock screen (with retry — never a dead end) ───────────
        if (!unlocked) {
            val tryAuth = {
                BiometricHelper.authenticate(
                    activity = activity,
                    onSuccess = { unlocked = true },
                    onFail = { /* stay locked; user can retry below */ }
                )
            }
            LaunchedEffect(Unit) {
                // If biometrics were removed from the device, don't lock the user out.
                if (!BiometricHelper.isAvailable(context)) unlocked = true
                else tryAuth()
            }
            Box(
                Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Lock, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Masareef is locked",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Verify your identity to continue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = tryAuth) { Text("Unlock") }
                }
            }
            return@MaterialTheme
        }

        // ── Main scaffold ────────────────────────────────────────────────────
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar {
                    listOf(
                        Triple(0, Icons.AutoMirrored.Filled.List, "Expenses"),
                        Triple(1, Icons.Filled.BarChart, "Stats"),
                        Triple(2, Icons.Filled.Savings, "Budgets"),
                        Triple(3, Icons.Filled.Settings, "Settings")
                    ).forEach { (idx, icon, label) ->
                        NavigationBarItem(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            icon = { Icon(icon, null) },
                            label = { Text(label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = selectedTab == 0,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showAdd = true },
                        icon = { Icon(Icons.Filled.Add, null) },
                        text = { Text("Add expense") }
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
                        onEdit = { editTarget = it }
                    )
                    1 -> StatsScreen(expenses)
                    2 -> BudgetsScreen(
                        expenses = expenses,
                        budgets = budgets,
                        onSave = { db.upsertBudget(it); refresh() },
                        onDelete = { db.deleteBudget(it.category, it.month, it.year); refresh() }
                    )
                    else -> SettingsScreen(
                        expenses = expenses,
                        darkMode = darkMode,
                        biometricEnabled = bioEnabled,
                        onToggleDark = { v ->
                            darkMode = v
                            prefs.edit().putBoolean("dark_mode", v).apply()
                        },
                        onToggleBiometric = { v ->
                            bioEnabled = v
                            prefs.edit().putBoolean("bio_enabled", v).apply()
                        },
                        onDataChanged = refresh,
                        onExportCsv = { CsvExport.share(context, expenses) },
                        onAddExpense = { db.insertExpense(it); refresh() }
                    )
                }
            }
        }

        // ── Add / Edit sheet ─────────────────────────────────────────────────
        if (showAdd || editTarget != null) {
            AddExpenseSheet(
                existing = editTarget,
                onDismiss = { showAdd = false; editTarget = null },
                onSave = { expense ->
                    if (expense.id == 0L) db.insertExpense(expense)
                    else db.updateExpense(expense)
                    refresh()
                    showAdd = false
                    editTarget = null
                }
            )
        }
    }
}

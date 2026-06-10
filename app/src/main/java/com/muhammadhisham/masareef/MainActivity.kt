package com.muhammadhisham.masareef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.muhammadhisham.masareef.data.ExpenseDb
import com.muhammadhisham.masareef.ui.AddExpenseSheet
import com.muhammadhisham.masareef.ui.ExpensesScreen
import com.muhammadhisham.masareef.ui.SettingsScreen
import com.muhammadhisham.masareef.ui.StatsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MasareefApp()
        }
    }
}

private val MasareefColors = lightColorScheme(
    primary = Color(0xFF2D6A4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F3DC),
    onPrimaryContainer = Color(0xFF1B4332),
    secondary = Color(0xFF40916C),
    background = Color(0xFFFDF8F0),
    surface = Color(0xFFFFFFFF)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasareefApp() {
    val context = LocalContext.current
    val db = remember { ExpenseDb.get(context) }
    var expenses by remember { mutableStateOf(db.getAll()) }
    val refresh: () -> Unit = { expenses = db.getAll() }

    // Refresh whenever the app comes back to the foreground,
    // so SMS captured in the background show up immediately.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSheet by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = MasareefColors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Masareef", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Expenses") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                        label = { Text("Stats") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Settings") }
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add expense")
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> ExpensesScreen(
                        expenses = expenses,
                        onDelete = { expense ->
                            db.deleteExpense(expense.id)
                            refresh()
                        }
                    )
                    1 -> StatsScreen(expenses)
                    else -> SettingsScreen(onDataChanged = refresh)
                }
            }

            if (showAddSheet) {
                AddExpenseSheet(
                    onDismiss = { showAddSheet = false },
                    onSave = { expense ->
                        db.insertExpense(expense)
                        refresh()
                        showAddSheet = false
                    }
                )
            }
        }
    }
}

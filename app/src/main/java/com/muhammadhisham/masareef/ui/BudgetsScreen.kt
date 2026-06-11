package com.muhammadhisham.masareef.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.muhammadhisham.masareef.data.Budget
import com.muhammadhisham.masareef.data.Categories
import com.muhammadhisham.masareef.data.Expense
import com.muhammadhisham.masareef.data.formatAmount
import com.muhammadhisham.masareef.data.toLocalDate
import java.time.YearMonth

@Composable
fun BudgetsScreen(
    expenses: List<Expense>,
    budgets: List<Budget>,
    onSave: (Budget) -> Unit,
    onDelete: (Budget) -> Unit
) {
    val thisMonth = YearMonth.now()
    val monthExpenses = expenses.filter { YearMonth.from(it.toLocalDate()) == thisMonth }

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, "Add budget")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Monthly Budgets",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text("${thisMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${thisMonth.year}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(4.dp))

            if (budgets.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💰", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text("No budgets set",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("Tap + to set a spending limit per category",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            budgets.forEach { budget ->
                val spent = monthExpenses
                    .filter { it.category == budget.category }
                    .sumOf { it.amount }
                val fraction = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat() else 0f
                val over = spent > budget.limitAmount
                val warn = fraction >= 0.8f && !over
                val barColor = when {
                    over -> MaterialTheme.colorScheme.error
                    warn -> Amber500
                    else -> categoryColor(budget.category)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (over)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryBadge(budget.category, size = 38.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(budget.category,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold)
                                    Text("Limit: ${formatAmount(budget.limitAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (over || warn) {
                                    Icon(Icons.Filled.Warning, null,
                                        tint = if (over) MaterialTheme.colorScheme.error else Amber500,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                IconButton(
                                    onClick = { onDelete(budget) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, "Delete",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        AnimatedProgressBar(
                            fraction = fraction.coerceAtMost(1f),
                            color = barColor,
                            modifier = Modifier.fillMaxWidth().height(10.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${formatAmount(spent)} spent",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (over) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (over) "Over by ${formatAmount(spent - budget.limitAmount)}"
                                else "${formatAmount(budget.limitAmount - spent)} left",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (over) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showDialog) {
        AddBudgetDialog(
            existingCategories = budgets.map { it.category }.toSet(),
            onDismiss = { showDialog = false },
            onSave = { budget ->
                onSave(budget)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddBudgetDialog(
    existingCategories: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Budget) -> Unit
) {
    val available = Categories.all.filter { it !in existingCategories }
    var selectedCat by remember { mutableStateOf(available.firstOrNull() ?: "") }
    var limitText   by remember { mutableStateOf("") }
    val limit = limitText.toDoubleOrNull()
    val now = YearMonth.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (available.isEmpty()) {
                    Text("All categories already have a budget this month.",
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        available.forEach { cat ->
                            FilterChip(
                                selected = selectedCat == cat,
                                onClick = { selectedCat = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Monthly limit") },
                        prefix = { Text("EGP ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (limit != null && limit > 0 && selectedCat.isNotBlank()) {
                        onSave(Budget(
                            category    = selectedCat,
                            limitAmount = limit,
                            month       = now.monthValue,
                            year        = now.year
                        ))
                    }
                },
                enabled = available.isNotEmpty() && limit != null && limit > 0
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

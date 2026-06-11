package com.muhammadhisham.masareef.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.muhammadhisham.masareef.data.Categories
import com.muhammadhisham.masareef.data.Expense

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseSheet(
    existing: Expense? = null,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit
) {
    val isEdit = existing != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var amountText by remember { mutableStateOf(existing?.amount?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "") }
    var merchant   by remember { mutableStateOf(existing?.merchant ?: "") }
    var category   by remember { mutableStateOf(existing?.category ?: Categories.OTHER) }
    var note       by remember { mutableStateOf(existing?.note ?: "") }
    var recurring  by remember { mutableStateOf(existing?.isRecurring ?: false) }

    // Auto-guess category from merchant as user types
    LaunchedEffect(merchant) {
        if (!isEdit && merchant.length > 2) {
            val guessed = Categories.guess(merchant, "")
            if (guessed != Categories.OTHER) category = guessed
        }
    }

    val amount = amountText.replace(",", "").toDoubleOrNull()
    val isValid = amount != null && amount > 0.0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                if (isEdit) "Edit expense" else "New expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(20.dp))

            // Amount field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                prefix = { Text("EGP ", fontWeight = FontWeight.Medium) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                isError = amountText.isNotBlank() && amount == null
            )
            Spacer(Modifier.height(12.dp))

            // Merchant field
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Where? (e.g. Carrefour)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // Note field
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))

            // Category chips
            Text("Category", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Categories.all.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text(c) },
                        leadingIcon = {
                            Icon(
                                categoryIcon(c),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                                tint = if (category == c)
                                    categoryColor(c)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Recurring toggle
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Repeat, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Column {
                        Text("Recurring", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                        Text("Auto-generated every month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(checked = recurring, onCheckedChange = { recurring = it })
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        Expense(
                            id          = existing?.id ?: 0L,
                            amount      = amount ?: return@Button,
                            merchant    = merchant.trim().ifBlank { "Manual entry" },
                            category    = category,
                            timestamp   = existing?.timestamp ?: System.currentTimeMillis(),
                            source      = existing?.source ?: "manual",
                            note        = note.trim(),
                            isRecurring = recurring,
                            smsHash     = existing?.smsHash
                        )
                    )
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEdit) "Save changes" else "Add expense",
                    style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

package com.muhammadhisham.masareef.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.muhammadhisham.masareef.data.Expense
import com.muhammadhisham.masareef.data.formatAmountFull
import com.muhammadhisham.masareef.sms.InboxScanner
import com.muhammadhisham.masareef.sms.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SMS_PERMS = arrayOf(
    Manifest.permission.READ_SMS,
    Manifest.permission.RECEIVE_SMS
)

private fun hasSmsPerms(context: Context) = SMS_PERMS.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun SettingsScreen(
    expenses: List<Expense>,
    darkMode: Boolean,
    biometricEnabled: Boolean,
    onToggleDark: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onDataChanged: () -> Unit,
    onExportCsv: () -> Unit,
    onAddExpense: (Expense) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var smsEnabled by remember {
        mutableStateOf(prefs.getBoolean("sms_enabled", false) && hasSmsPerms(context))
    }
    var status   by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun saveSms(v: Boolean) {
        smsEnabled = v
        prefs.edit().putBoolean("sms_enabled", v).apply()
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        if (res.values.all { it }) {
            saveSms(true); status = "SMS auto-tracking enabled."
        } else {
            status = "Permission denied. Enable SMS access in system settings."
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // ── Appearance ───────────────────────────────────────────────────────
        SettingsSection(title = "Appearance") {
            SettingsToggleRow(
                icon = Icons.Filled.DarkMode,
                title = "Dark mode",
                subtitle = "Easy on the eyes at night",
                checked = darkMode,
                onToggle = onToggleDark
            )
        }

        // ── Security ─────────────────────────────────────────────────────────
        SettingsSection(title = "Security") {
            SettingsToggleRow(
                icon = Icons.Filled.Fingerprint,
                title = "Biometric lock",
                subtitle = "Require fingerprint / Face ID to open",
                checked = biometricEnabled,
                onToggle = onToggleBiometric
            )
        }

        // ── SMS ──────────────────────────────────────────────────────────────
        SettingsSection(title = "SMS Auto-tracking") {
            SettingsToggleRow(
                icon = Icons.Filled.Message,
                title = "Enable SMS tracking",
                subtitle = "Detect bank debit SMS automatically (Arabic + English)",
                checked = smsEnabled,
                onToggle = { wanted ->
                    if (!wanted) { saveSms(false); status = "SMS tracking off." }
                    else if (hasSmsPerms(context)) { saveSms(true); status = "SMS tracking on." }
                    else permLauncher.launch(SMS_PERMS)
                }
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (!hasSmsPerms(context)) { permLauncher.launch(SMS_PERMS); return@Button }
                    scanning = true
                    status = "Scanning inbox…"
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            InboxScanner.scan(context, days = 90)
                        }
                        scanning = false
                        status = buildString {
                            append("Scanned ${result.scanned} SMS · imported ${result.imported} expense(s).")
                            if (result.scanned > 0 && result.imported == 0) {
                                append(" If a bank SMS was missed, paste it in the Parser Tester below to see why.")
                            }
                        }
                        onDataChanged()
                    }
                },
                enabled = !scanning,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (scanning) "Scanning…" else "Scan inbox (last 90 days)") }
        }

        // ── Parser Tester ────────────────────────────────────────────────────
        SettingsSection(title = "SMS Parser Tester") {
            var testSms by remember { mutableStateOf("") }
            var testOutcome by remember { mutableStateOf<SmsParser.Result?>(null) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.BugReport, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Paste a bank SMS to see exactly how Masareef reads it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = testSms,
                onValueChange = { testSms = it; testOutcome = null },
                label = { Text("Bank SMS text") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { testOutcome = SmsParser.parseDetailed(testSms) },
                enabled = testSms.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Test parser") }

            when (val outcome = testOutcome) {
                is SmsParser.Result.Success -> {
                    val p = outcome.parsed
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "✓ Recognized as an expense",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Amount: ${formatAmountFull(p.amount)}\n" +
                                "Merchant: ${p.merchant ?: "(not detected)"}\n" +
                                "Category: ${p.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = {
                                val now = System.currentTimeMillis()
                                onAddExpense(
                                    Expense(
                                        amount = p.amount,
                                        merchant = p.merchant ?: "Bank SMS",
                                        category = p.category,
                                        timestamp = now,
                                        source = "sms",
                                        smsHash = SmsParser.hash(testSms, now)
                                    )
                                )
                                status = "Expense added from tested SMS."
                                testSms = ""
                                testOutcome = null
                            }) { Text("Add as expense") }
                        }
                    }
                }
                is SmsParser.Result.Rejected -> {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "✗ Skipped: ${outcome.reason}",
                            Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                null -> {}
            }
        }

        // ── Data ─────────────────────────────────────────────────────────────
        SettingsSection(title = "Data") {
            OutlinedButton(
                onClick = onExportCsv,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Export all expenses to CSV")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${expenses.size} expense(s) on device · everything stored locally, nothing uploaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        status?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    it,
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Text(
            "Privacy: SMS text is read only to extract the amount, merchant, and category. " +
            "The message itself is never stored, and no data ever leaves your phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "Masareef v2.1 · Built in Cairo 🇪🇬",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                icon, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

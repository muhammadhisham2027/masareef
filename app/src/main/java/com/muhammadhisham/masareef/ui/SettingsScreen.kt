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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.muhammadhisham.masareef.sms.InboxScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SMS_PERMISSIONS = arrayOf(
    Manifest.permission.READ_SMS,
    Manifest.permission.RECEIVE_SMS
)

private fun hasSmsPermissions(context: Context): Boolean =
    SMS_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

@Composable
fun SettingsScreen(onDataChanged: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    var smsEnabled by remember {
        mutableStateOf(prefs.getBoolean("sms_enabled", false) && hasSmsPermissions(context))
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun saveEnabled(value: Boolean) {
        smsEnabled = value
        prefs.edit().putBoolean("sms_enabled", value).apply()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            saveEnabled(true)
            statusMessage = "SMS auto-tracking is on."
        } else {
            statusMessage = "Permission denied. You can grant SMS access from system settings."
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "SMS auto-tracking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Automatically record expenses from your bank's transaction SMS (English and Arabic).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = smsEnabled,
                    onCheckedChange = { wanted ->
                        if (!wanted) {
                            saveEnabled(false)
                            statusMessage = "SMS auto-tracking is off."
                        } else if (hasSmsPermissions(context)) {
                            saveEnabled(true)
                            statusMessage = "SMS auto-tracking is on."
                        } else {
                            permissionLauncher.launch(SMS_PERMISSIONS)
                        }
                    }
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Import past transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Scan your SMS inbox for bank transactions from the last 30 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (!hasSmsPermissions(context)) {
                            permissionLauncher.launch(SMS_PERMISSIONS)
                        } else {
                            scanning = true
                            statusMessage = "Scanning inbox…"
                            scope.launch {
                                val count = withContext(Dispatchers.IO) {
                                    InboxScanner.scan(context, days = 30)
                                }
                                scanning = false
                                statusMessage = "Imported $count new expense(s) from SMS."
                                onDataChanged()
                            }
                        }
                    },
                    enabled = !scanning
                ) {
                    Text(if (scanning) "Scanning…" else "Scan inbox")
                }
            }
        }

        statusMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            "Privacy: everything stays on your phone. SMS text is read only to extract the amount, " +
                "merchant, and category — the message itself is never stored, and nothing is ever uploaded.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

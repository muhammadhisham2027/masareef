package com.muhammadhisham.masareef.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.muhammadhisham.masareef.data.Expense
import com.muhammadhisham.masareef.data.ExpenseDb

/**
 * Listens for incoming SMS and records bank transactions automatically
 * when SMS auto-tracking is enabled in Settings.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sms_enabled", false)) return

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read SMS from intent", e)
            return
        } ?: return

        // Multipart SMS arrive as several parts from the same sender — join them.
        val bySender = messages.filterNotNull().groupBy { it.displayOriginatingAddress ?: "" }
        for ((sender, parts) in bySender) {
            val body = parts.joinToString("") { it.messageBody ?: "" }
            if (body.isBlank()) continue

            val parsed = SmsParser.parse(body) ?: continue
            val timestamp = parts.first().timestampMillis

            val rowId = ExpenseDb.get(context).insertExpense(
                Expense(
                    amount = parsed.amount,
                    merchant = parsed.merchant ?: sender,
                    category = parsed.category,
                    timestamp = timestamp,
                    source = "sms",
                    smsHash = SmsParser.hash(body, timestamp)
                )
            )
            if (rowId != -1L) Log.i(TAG, "Captured expense from SMS")
        }
    }

    private companion object {
        const val TAG = "MasareefSms"
    }
}

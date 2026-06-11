package com.muhammadhisham.masareef.sms

import android.content.Context
import android.provider.Telephony
import com.muhammadhisham.masareef.data.Expense
import com.muhammadhisham.masareef.data.ExpenseDb
import java.util.concurrent.TimeUnit

/**
 * One-off scan of the SMS inbox to import past bank transactions.
 * Requires READ_SMS permission. Returns the number of new expenses imported.
 */
object InboxScanner {

    fun scan(context: Context, days: Int = 30): Int {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        var imported = 0

        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ADDRESS),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(cutoff.toString()),
            "${Telephony.Sms.DATE} DESC"
        ) ?: return 0

        val db = ExpenseDb.get(context)
        cursor.use {
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val addrIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)

            while (it.moveToNext()) {
                val body = it.getString(bodyIdx) ?: continue
                val timestamp = it.getLong(dateIdx)
                val sender = it.getString(addrIdx) ?: ""

                val parsed = SmsParser.parse(body) ?: continue
                val rowId = db.insertExpense(
                    Expense(
                        amount = parsed.amount,
                        merchant = parsed.merchant ?: sender,
                        category = parsed.category,
                        timestamp = timestamp,
                        source = "sms",
                        smsHash = SmsParser.hash(body, timestamp)
                    )
                )
                if (rowId != -1L) imported++
            }
        }
        return imported
    }
}

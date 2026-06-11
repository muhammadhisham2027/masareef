package com.muhammadhisham.masareef.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.muhammadhisham.masareef.data.Expense
import com.muhammadhisham.masareef.data.toLocalDate
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvExport {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    fun share(context: Context, expenses: List<Expense>) {
        try {
            val csv = buildString {
                appendLine("Date,Merchant,Category,Amount (EGP),Source,Note,Recurring")
                expenses
                    .sortedByDescending { it.timestamp }
                    .forEach { e ->
                        val date = e.toLocalDate().format(dateFmt)
                        val merchant = e.merchant.replace(",", " ")
                        val note     = e.note.replace(",", " ")
                        appendLine("$date,$merchant,${e.category},${
                            String.format(Locale.US, "%.2f", e.amount)
                        },${e.source},$note,${if (e.isRecurring) "yes" else "no"}")
                    }
            }

            val file = File(context.cacheDir, "masareef_export.csv")
            file.writeText(csv)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Masareef — Expense Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share expenses CSV"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

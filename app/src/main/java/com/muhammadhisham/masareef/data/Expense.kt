package com.muhammadhisham.masareef.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class Expense(
    val id: Long = 0L,
    val amount: Double,
    val merchant: String,
    val category: String,
    val timestamp: Long,
    val source: String,           // "manual" | "sms"
    val note: String = "",
    val isRecurring: Boolean = false,
    val smsHash: String? = null
)

data class Budget(
    val id: Long = 0L,
    val category: String,
    val limitAmount: Double,
    val month: Int,   // 1-12
    val year: Int
)

fun Expense.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

fun formatAmount(value: Double): String =
    "EGP " + String.format(Locale.US, "%,.0f", value)

fun formatAmountFull(value: Double): String =
    "EGP " + String.format(Locale.US, "%,.2f", value)

object Categories {
    const val OTHER = "Other"

    val all = listOf(
        "Food", "Groceries", "Transport", "Shopping",
        "Bills", "Health", "Entertainment", OTHER
    )

    private val keywords = mapOf(
        "Food" to listOf(
            "talabat", "mcdonald", "kfc", "pizza", "burger", "restaurant",
            "cafe", "coffee", "elmenus", "breadfast", "مطعم", "كافيه", "قهوة",
            "totkati", "hardees", "popeyes", "subway", "shawarma"
        ),
        "Groceries" to listOf(
            "carrefour", "spinneys", "hyper", "seoudi", "gourmet", "kazyon",
            "metro market", "بقالة", "سوبر", "ماركت", "fathalla", "oscar"
        ),
        "Transport" to listOf(
            "uber", "careem", "swvl", "indrive", "fuel", "petrol", "benzin",
            "بنزين", "مواصلات", "تاكسي", "parking", "toll"
        ),
        "Bills" to listOf(
            "vodafone", "orange", "etisalat", "telecom", "electricity",
            "internet", "فاتورة", "كهرباء", "مياه", "gas", "we eg", "nile sat"
        ),
        "Health" to listOf(
            "pharmacy", "hospital", "clinic", "lab", "صيدلية", "مستشفى",
            "عيادة", "معمل", "seif", "doctor", "dawaya"
        ),
        "Entertainment" to listOf(
            "netflix", "spotify", "cinema", "playstation", "steam", "anghami",
            "سينما", "viu", "shahid", "osn", "game", "roblox"
        ),
        "Shopping" to listOf(
            "amazon", "noon", "jumia", "zara", "ikea", "shein", "city stars",
            "mall", "h&m", "pull&bear", "aldo", "mothercare"
        )
    )

    fun guess(merchant: String, body: String): String {
        val text = (merchant + " " + body).lowercase()
        for ((category, words) in keywords) {
            if (words.any { text.contains(it) }) return category
        }
        return OTHER
    }
}

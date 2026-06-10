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
    val source: String,          // "manual" or "sms"
    val smsHash: String? = null  // used to avoid importing the same SMS twice
)

fun Expense.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

fun formatAmount(value: Double): String =
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
            "cafe", "coffee", "elmenus", "breadfast", "مطعم", "كافيه", "قهوة"
        ),
        "Groceries" to listOf(
            "carrefour", "spinneys", "hyper", "seoudi", "gourmet", "kazyon",
            "metro market", "بقالة", "سوبر", "ماركت"
        ),
        "Transport" to listOf(
            "uber", "careem", "swvl", "indrive", "fuel", "petrol", "benzin",
            "بنزين", "مواصلات", "تاكسي"
        ),
        "Bills" to listOf(
            "vodafone", "orange", "etisalat", "telecom", "electricity",
            "internet", "فاتورة", "كهرباء", "مياه"
        ),
        "Health" to listOf(
            "pharmacy", "hospital", "clinic", "lab", "صيدلية", "مستشفى", "عيادة", "معمل"
        ),
        "Entertainment" to listOf(
            "netflix", "spotify", "cinema", "playstation", "steam", "anghami", "سينما"
        ),
        "Shopping" to listOf(
            "amazon", "noon", "jumia", "zara", "ikea", "shein", "city stars", "mall"
        )
    )

    /** Guess a category from merchant name and SMS body keywords. */
    fun guess(merchant: String, body: String): String {
        val text = (merchant + " " + body).lowercase()
        for ((category, words) in keywords) {
            if (words.any { text.contains(it) }) return category
        }
        return OTHER
    }
}

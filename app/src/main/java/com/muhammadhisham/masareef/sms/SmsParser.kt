package com.muhammadhisham.masareef.sms

import com.muhammadhisham.masareef.data.Categories

/**
 * Parses bank transaction SMS (English and Arabic) into an expense.
 * Only the extracted amount/merchant/category is ever stored —
 * the SMS text itself is never saved.
 */
object SmsParser {

    data class Parsed(
        val amount: Double,
        val merchant: String?,
        val category: String
    )

    // "EGP 250.00" / "جنيه 250"
    private val currencyFirst = Regex(
        """(?:EGP|LE|L\.E\.?|USD|SAR|AED|جنيه|ج\.م|ريال|درهم)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // "250.00 EGP" / "250 جنيه"
    private val currencyLast = Regex(
        """([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(?:EGP|LE|L\.E\.?|USD|SAR|AED|جنيه|ج\.م|ريال|درهم)""",
        RegexOption.IGNORE_CASE
    )

    // "at CARREFOUR" / "لدى كارفور" / "في ..."
    private val merchantRegex = Regex(
        """(?:\bat\b|@|لدى|في)\s+([\w&.\-' \u0600-\u06FF]{2,40})""",
        RegexOption.IGNORE_CASE
    )

    private val debitKeywords = listOf(
        "purchase", "debited", "debit", "spent", "paid", "payment",
        "withdraw", "pos ", "transaction",
        "خصم", "شراء", "سحب", "دفع", "مدين", "عملية"
    )

    private val creditKeywords = listOf(
        "credited", "deposit", "received", "refund", "reversal", "salary",
        "إيداع", "ايداع", "استرداد", "إضافة", "اضافة", "راتب"
    )

    private val skipKeywords = listOf(
        "otp", "one-time", "one time password", "verification code",
        "do not share", "رمز التحقق", "كود التفعيل", "لا تشارك"
    )

    fun parse(body: String): Parsed? {
        val lower = body.lowercase()

        // Ignore OTP / verification messages entirely.
        if (skipKeywords.any { lower.contains(it) }) return null

        // Must look like a debit, and not a credit/refund.
        if (creditKeywords.any { lower.contains(it) }) return null
        if (debitKeywords.none { lower.contains(it) }) return null

        val match = currencyFirst.find(body) ?: currencyLast.find(body) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val merchant = merchantRegex.find(body)
            ?.groupValues?.get(1)
            ?.substringBefore('\n')
            ?.trim()
            ?.trimEnd('.', ',', ';')
            ?.takeIf { it.isNotBlank() }

        return Parsed(
            amount = amount,
            merchant = merchant,
            category = Categories.guess(merchant ?: "", body)
        )
    }

    /** Stable de-duplication key so the same SMS is never imported twice. */
    fun hash(body: String, timestamp: Long): String =
        "${body.hashCode()}_$timestamp"
}

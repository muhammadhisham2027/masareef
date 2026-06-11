package com.muhammadhisham.masareef.sms

import com.muhammadhisham.masareef.data.Categories

/**
 * Parses bank transaction SMS (English and Arabic) into an expense.
 * Only the extracted amount/merchant/category is ever stored —
 * the SMS text itself is never saved.
 *
 * v2.1 overhaul:
 *  - Normalizes Arabic-Indic digits (٠١٢٣٤٥٦٧٨٩ / ۰-۹) and the Arabic
 *    decimal separator (٫) to ASCII before matching — many Egyptian banks
 *    send amounts in Arabic numerals.
 *  - Much wider debit vocabulary (was used / sent / transferred / تم استخدام …).
 *  - Accepts keyword-less messages when a currency amount appears together
 *    with card/account context (بطاقة، حساب، POS، wallet …).
 *  - parseDetailed() returns a human-readable rejection reason so the
 *    in-app Parser Tester can explain exactly why a message was skipped.
 */
object SmsParser {

    data class Parsed(
        val amount: Double,
        val merchant: String?,
        val category: String
    )

    sealed class Result {
        data class Success(val parsed: Parsed) : Result()
        data class Rejected(val reason: String) : Result()
    }

    // ── Regexes (run on normalized text) ─────────────────────────────────────

    private const val CUR =
        """(?:EGP|LE|L\.E\.?|USD|EUR|SAR|AED|KWD|QAR|جنيها|جنيه|ج\.م|جم|ريال|درهم|دولار|يورو)"""
    private const val NUM = """([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""

    // "EGP 250.00" / "جنيه 250"
    private val currencyFirst = Regex("""$CUR\s*$NUM""", RegexOption.IGNORE_CASE)

    // "250.00 EGP" / "250 جنيه"
    private val currencyLast = Regex("""$NUM\s*$CUR""", RegexOption.IGNORE_CASE)

    // Fallback: "amount of 250.00" / "مبلغ 250" (only trusted when a debit word exists)
    private val amountKeyword = Regex(
        """(?:amount of|amount|value of|مبلغ|بمبلغ|قيمة|بقيمة)\s*:?\s*$NUM""",
        RegexOption.IGNORE_CASE
    )

    // "Merchant: Hyper One" / "التاجر: ..."
    private val merchantLabel = Regex(
        """(?:merchant\s*:?|التاجر\s*:?)\s+([\w&.\-' \u0600-\u06FF]{2,40})""",
        RegexOption.IGNORE_CASE
    )

    // "at CARREFOUR" / "لدى كارفور" / "عند ..." / "في ..."
    private val merchantAt = Regex(
        """(?:\bat\b|@|لدى|عند|في)\s+([\w&.\-' \u0600-\u06FF]{2,40})""",
        RegexOption.IGNORE_CASE
    )

    // "sent to John" / "transfer to X" / "إلى فلان"
    private val merchantTo = Regex(
        """(?:sent to|transferred to|transfer to|paid to|إلى|الى)\s+([\w&.\-' \u0600-\u06FF]{2,40})""",
        RegexOption.IGNORE_CASE
    )

    // ── Vocabulary ───────────────────────────────────────────────────────────

    private val debitKeywords = listOf(
        // English
        "purchase", "purchased", "debited", "debit", "spent", "paid", "payment",
        "withdraw", "withdrawal", "withdrawn", "pos ", "transaction", "was used",
        "has been used", "charged", "deducted", "sent", "transferred", "transfer of",
        // Arabic
        "خصم", "شراء", "سحب", "دفع", "مدين", "عملية", "استخدام", "مشتريات",
        "مسحوب", "مدفوع", "إرسال", "ارسال", "تحويل صادر", "حولت"
    )

    private val creditKeywords = listOf(
        "credited", "deposit", "deposited", "received from", "you received",
        "refund", "refunded", "reversal", "salary", "cashback",
        "إيداع", "ايداع", "استرداد", "راتب", "تحويل وارد", "استلمت",
        "تم اضافة", "تم إضافة", "أضيف", "اضيف"
    )

    // Card/account context — lets us accept messages whose wording we've
    // never seen, as long as money clearly moved on a card or account.
    private val contextWords = listOf(
        "card", "credit card", "debit card", "account", "atm", "wallet",
        "instapay", "balance", "بطاقة", "بطاقتك", "حساب", "حسابك",
        "محفظة", "محفظتك", "رصيد", "رصيدك"
    )

    private val skipKeywords = listOf(
        "otp", "one-time", "one time password", "verification code", "passcode",
        "do not share", "never share", "رمز التحقق", "كود التحقق",
        "كود التفعيل", "لا تشارك", "الرمز السري"
    )

    // ── Normalization ────────────────────────────────────────────────────────

    /** Converts Arabic-Indic and Eastern Arabic-Indic digits + separators to ASCII. */
    fun normalize(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(
                when (ch) {
                    in '٠'..'٩' -> '0' + (ch - '٠')   // Arabic-Indic digits
                    in '۰'..'۹' -> '0' + (ch - '۰')   // Eastern Arabic-Indic digits
                    '٫'         -> '.'                 // Arabic decimal separator
                    '٬'         -> ','                 // Arabic thousands separator
                    else        -> ch
                }
            )
        }
        return sb.toString()
    }

    // ── Parsing ──────────────────────────────────────────────────────────────

    fun parse(body: String): Parsed? =
        (parseDetailed(body) as? Result.Success)?.parsed

    fun parseDetailed(rawBody: String): Result {
        if (rawBody.isBlank()) return Result.Rejected("Empty message")

        val body = normalize(rawBody)
        val lower = body.lowercase()

        if (skipKeywords.any { lower.contains(it) }) {
            return Result.Rejected("OTP / verification message — skipped for safety")
        }

        val hasDebit = debitKeywords.any { lower.contains(it) }
        val hasCredit = creditKeywords.any { lower.contains(it) }

        // Pure credit (incoming money) with no spending wording → not an expense.
        if (hasCredit && !hasDebit) {
            return Result.Rejected("Looks like incoming money (deposit/refund), not an expense")
        }

        val amountMatch = currencyFirst.find(body)
            ?: currencyLast.find(body)
            ?: (if (hasDebit) amountKeyword.find(body) else null)
            ?: return Result.Rejected(
                "No amount found — looked for EGP / LE / جنيه / جم next to a number"
            )

        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull()
            ?: return Result.Rejected("Amount could not be read as a number")
        if (amount <= 0.0) return Result.Rejected("Amount is zero")

        // Unknown wording is fine as long as the message clearly involves
        // a card / account / wallet.
        val hasContext = contextWords.any { lower.contains(it) }
        if (!hasDebit && !hasContext) {
            return Result.Rejected(
                "No spending indicators (purchase / خصم / card / بطاقة …) found"
            )
        }

        val merchant = extractMerchant(body)
        return Result.Success(
            Parsed(
                amount = amount,
                merchant = merchant,
                category = Categories.guess(merchant ?: "", body)
            )
        )
    }

    private fun extractMerchant(body: String): String? {
        val match = merchantLabel.find(body)
            ?: merchantAt.find(body)
            ?: merchantTo.find(body)
            ?: return null
        var s = match.groupValues[1].substringBefore('\n')

        // Cut trailing date/sentence fragments: "CARREFOUR on 12/06", "X بتاريخ ..."
        for (cut in listOf(" on ", " بتاريخ", " في تاريخ", " dated", " date ")) {
            val i = s.indexOf(cut, ignoreCase = true)
            if (i > 0) s = s.substring(0, i)
        }

        s = s.trim().trimEnd('.', ',', ';', ':', '-')
        if (s.length < 2) return null

        val l = s.lowercase()
        // "to your account" / "في حسابك" → not a merchant
        if (l.startsWith("your") || l.startsWith("حساب") || l.startsWith("رصيد")) return null
        return s
    }

    /** Stable de-duplication key so the same SMS is never imported twice. */
    fun hash(body: String, timestamp: Long): String =
        "${body.hashCode()}_$timestamp"
}

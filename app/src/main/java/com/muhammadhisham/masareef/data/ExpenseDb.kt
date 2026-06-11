package com.muhammadhisham.masareef.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ExpenseDb private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE expenses (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount REAL NOT NULL,
                merchant TEXT NOT NULL,
                category TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                source TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                is_recurring INTEGER NOT NULL DEFAULT 0,
                sms_hash TEXT UNIQUE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE budgets (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                limit_amount REAL NOT NULL,
                month INTEGER NOT NULL,
                year INTEGER NOT NULL,
                UNIQUE(category, month, year)
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE expenses ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE expenses ADD COLUMN is_recurring INTEGER NOT NULL DEFAULT 0")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS budgets (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT,
                    category TEXT NOT NULL,
                    limit_amount REAL NOT NULL,
                    month INTEGER NOT NULL,
                    year INTEGER NOT NULL,
                    UNIQUE(category, month, year)
                )
            """.trimIndent())
        }
    }

    // ── Expenses ──────────────────────────────────────────────────────────────

    fun insertExpense(e: Expense): Long {
        val values = ContentValues().apply {
            put("amount", e.amount)
            put("merchant", e.merchant)
            put("category", e.category)
            put("timestamp", e.timestamp)
            put("source", e.source)
            put("note", e.note)
            put("is_recurring", if (e.isRecurring) 1 else 0)
            put("sms_hash", e.smsHash)
        }
        return writableDatabase.insertWithOnConflict(
            "expenses", null, values, SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun updateExpense(e: Expense) {
        val values = ContentValues().apply {
            put("amount", e.amount)
            put("merchant", e.merchant)
            put("category", e.category)
            put("timestamp", e.timestamp)
            put("note", e.note)
            put("is_recurring", if (e.isRecurring) 1 else 0)
        }
        writableDatabase.update("expenses", values, "_id = ?", arrayOf(e.id.toString()))
    }

    fun deleteExpense(id: Long) {
        writableDatabase.delete("expenses", "_id = ?", arrayOf(id.toString()))
    }

    fun getAll(): List<Expense> {
        val list = mutableListOf<Expense>()
        val cursor = readableDatabase.query(
            "expenses",
            arrayOf("_id","amount","merchant","category","timestamp","source","note","is_recurring","sms_hash"),
            null, null, null, null, "timestamp DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(Expense(
                    id          = it.getLong(0),
                    amount      = it.getDouble(1),
                    merchant    = it.getString(2),
                    category    = it.getString(3),
                    timestamp   = it.getLong(4),
                    source      = it.getString(5),
                    note        = it.getString(6) ?: "",
                    isRecurring = it.getInt(7) == 1,
                    smsHash     = it.getString(8)
                ))
            }
        }
        return list
    }

    // ── Budgets ───────────────────────────────────────────────────────────────

    fun upsertBudget(b: Budget) {
        val values = ContentValues().apply {
            put("category", b.category)
            put("limit_amount", b.limitAmount)
            put("month", b.month)
            put("year", b.year)
        }
        writableDatabase.insertWithOnConflict("budgets", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteBudget(category: String, month: Int, year: Int) {
        writableDatabase.delete("budgets",
            "category=? AND month=? AND year=?",
            arrayOf(category, month.toString(), year.toString()))
    }

    fun getBudgets(month: Int, year: Int): List<Budget> {
        val list = mutableListOf<Budget>()
        val cursor = readableDatabase.query(
            "budgets",
            arrayOf("_id","category","limit_amount","month","year"),
            "month=? AND year=?",
            arrayOf(month.toString(), year.toString()),
            null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(Budget(
                    id          = it.getLong(0),
                    category    = it.getString(1),
                    limitAmount = it.getDouble(2),
                    month       = it.getInt(3),
                    year        = it.getInt(4)
                ))
            }
        }
        return list
    }

    companion object {
        private const val DB_NAME = "masareef.db"
        private const val DB_VERSION = 2

        @Volatile private var instance: ExpenseDb? = null

        fun get(context: Context): ExpenseDb =
            instance ?: synchronized(this) {
                instance ?: ExpenseDb(context).also { instance = it }
            }
    }
}

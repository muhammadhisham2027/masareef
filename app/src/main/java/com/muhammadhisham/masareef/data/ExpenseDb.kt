package com.muhammadhisham.masareef.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ExpenseDb private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE expenses (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount REAL NOT NULL,
                merchant TEXT NOT NULL,
                category TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                source TEXT NOT NULL,
                sms_hash TEXT UNIQUE
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No migrations yet (version 1).
    }

    /** Returns the new row id, or -1 if the SMS was already imported (duplicate hash). */
    fun insertExpense(e: Expense): Long {
        val values = ContentValues().apply {
            put("amount", e.amount)
            put("merchant", e.merchant)
            put("category", e.category)
            put("timestamp", e.timestamp)
            put("source", e.source)
            put("sms_hash", e.smsHash)
        }
        return writableDatabase.insertWithOnConflict(
            "expenses", null, values, SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun deleteExpense(id: Long) {
        writableDatabase.delete("expenses", "_id = ?", arrayOf(id.toString()))
    }

    fun getAll(): List<Expense> {
        val list = mutableListOf<Expense>()
        val cursor = readableDatabase.query(
            "expenses",
            arrayOf("_id", "amount", "merchant", "category", "timestamp", "source", "sms_hash"),
            null, null, null, null,
            "timestamp DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Expense(
                        id = it.getLong(0),
                        amount = it.getDouble(1),
                        merchant = it.getString(2),
                        category = it.getString(3),
                        timestamp = it.getLong(4),
                        source = it.getString(5),
                        smsHash = it.getString(6)
                    )
                )
            }
        }
        return list
    }

    companion object {
        private const val DB_NAME = "masareef.db"
        private const val DB_VERSION = 1

        @Volatile
        private var instance: ExpenseDb? = null

        fun get(context: Context): ExpenseDb =
            instance ?: synchronized(this) {
                instance ?: ExpenseDb(context).also { instance = it }
            }
    }
}

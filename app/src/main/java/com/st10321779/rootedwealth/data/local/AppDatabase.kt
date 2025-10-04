package com.st10321779.rootedwealth.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.st10321779.rootedwealth.data.local.dao.CategoryDao
import com.st10321779.rootedwealth.data.local.dao.ExpenseDao
import com.st10321779.rootedwealth.data.local.entity.Category
import com.st10321779.rootedwealth.data.local.entity.Expense
import com.st10321779.rootedwealth.data.local.entity.Income
import com.st10321779.rootedwealth.data.local.dao.IncomeDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Expense::class, Income::class, Category::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rooted_wealth_db"
                )
                    .addCallback(AppDatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedDefaultCategories(database.categoryDao())
                }
            }
        }

        suspend fun seedDefaultCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                Category(name = "Groceries", icon = "ic_groceries", color = "#4CAF50", isDefault = true),
                Category(name = "Transport", icon = "ic_transport", color = "#2196F3", isDefault = true),
                Category(name = "Entertainment", icon = "ic_entertainment", color = "#9C27B0", isDefault = true),
                Category(name = "Rent", icon = "ic_rent", color = "#FF9800", isDefault = true),
                Category(name = "Utilities", icon = "ic_utilities", color = "#FFC107", isDefault = true),
                Category(name = "Takeout", icon = "ic_takeout", color = "#E91E63", isDefault = true),
                Category(name = "Health", icon = "ic_health", color = "#F44336", isDefault = true),
                Category(name = "Education", icon = "ic_education", color = "#009688", isDefault = true),
                Category(name = "Other", icon = "ic_other", color = "#607D8B", isDefault = true)
            )
            categoryDao.insertAll(defaultCategories)
        }
    }
}
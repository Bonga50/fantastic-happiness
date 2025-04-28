package com.example.allocate_optimize_track

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Category::class, Expense::class], version = 5, exportSchema = true) // Add Expense, increment version
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao // **** ADD DAO GETTER ****

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // *** DEFINE THE MIGRATION from version 2 to 3 ***
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the 'expenses' table
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS `expenses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `userId` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `amount` REAL NOT NULL,
                `date` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `photoUri` TEXT,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """)
                // Create the indexes for expenses table
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_categoryId` ON `expenses` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_userId` ON `expenses` (`userId`)")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new monthlyGoal column to the categories table
                // Adjust data type (REAL for Double/Float, INTEGER for Int/Long)
                // Adjust NULLABILITY (e.g., add 'NOT NULL DEFAULT 0.0' if non-nullable in Kotlin)
                db.execSQL("ALTER TABLE categories ADD COLUMN monthlyGoal REAL") // Assuming monthlyGoal is nullable Double?
                // Example if monthlyGoal was a non-nullable Double:
                // db.execSQL("ALTER TABLE categories ADD COLUMN monthlyGoal REAL NOT NULL DEFAULT 0.0")
                // Example if monthlyGoal was a nullable Int?
                // db.execSQL("ALTER TABLE categories ADD COLUMN monthlyGoal INTEGER")
            }
        }

        // Add previous migrations if any (e.g., MIGRATION_1_2)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    // *** ADD THE NEW MIGRATION ***
                    .fallbackToDestructiveMigration()
                    //.addMigrations( MIGRATION_3_4,MIGRATION_3_4) // Add all migrations needed
                    // Or use .fallbackToDestructiveMigration() for development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
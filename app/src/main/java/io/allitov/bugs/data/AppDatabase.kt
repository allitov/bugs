package io.allitov.bugs.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** База данных игры: игроки и их результаты раундов. */
@Database(
    entities = [PlayerEntity::class, ScoreEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao

    abstract fun scoreDao(): ScoreDao

    companion object {
        private const val DATABASE_NAME = "bugs.db"

        /** Версия 2: добавлена длительность раунда в рекорды; старым записям присваивается значение по умолчанию. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE scores ADD COLUMN roundDurationSeconds INTEGER NOT NULL DEFAULT 120"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        /** Возвращает синглтон базы данных, создавая её при первом обращении. */
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}

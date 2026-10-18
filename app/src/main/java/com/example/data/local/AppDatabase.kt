package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ApplicationDao
import com.example.data.local.dao.DematDao
import com.example.data.local.dao.IpoDao
import com.example.data.local.entity.DematAccountEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.IpoApplicationEntity
import com.example.data.local.entity.IpoEntity
import com.example.data.local.entity.StatusEventEntity
import com.example.data.local.entity.UpiAccountEntity

@Database(
    entities = [
        FamilyMemberEntity::class,
        DematAccountEntity::class,
        UpiAccountEntity::class,
        IpoEntity::class,
        IpoApplicationEntity::class,
        StatusEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ipoDao(): IpoDao
    abstract fun dematDao(): DematDao
    abstract fun applicationDao(): ApplicationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "narada_ipo_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

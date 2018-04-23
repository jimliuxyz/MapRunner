package com.jimliuxyz.maprunner.handset.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context


/**
 * Created by jimliu on 2018/3/1.
 */
@Database(entities = [RunRec::class, RunDir::class], version = 12)
abstract class RecDatabase : RoomDatabase() {

    abstract fun getDao(): RecDao

    companion object {
        private var INSTANCE: RecDatabase? = null
        private var lock = Any()

        fun getInstance(context: Context): RecDatabase {
            synchronized(lock) {
                return INSTANCE ?: Room.databaseBuilder(context.applicationContext,
                        RecDatabase::class.java, "app.db")
                        .fallbackToDestructiveMigration()
                        .build().also { INSTANCE = it }
            }
        }
    }
}

package com.example.airpollutionapp.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.airpollutionapp.room.dao.StationDao
import com.example.airpollutionapp.room.entity.StationEntity

@Database(entities = [StationEntity::class], version = 6)
abstract class LocalDB:RoomDatabase() {

    companion object {
        private var INSTANCE:LocalDB? = null

        @Synchronized
        fun getDatabase(context: Context):LocalDB? {
            if (INSTANCE == null){
                INSTANCE = Room.databaseBuilder(context.applicationContext,LocalDB::class.java, "LocalDB").build()
            }
            return INSTANCE
        }
    }

    abstract fun stationDao():StationDao

}
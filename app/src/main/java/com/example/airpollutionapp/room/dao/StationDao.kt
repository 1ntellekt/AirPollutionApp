package com.example.airpollutionapp.room.dao

import androidx.room.*
import com.example.airpollutionapp.models.Station
import com.example.airpollutionapp.room.entity.StationEntity

@Dao
interface StationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addStation(station: StationEntity)

    @Query("SELECT * FROM Station ORDER BY id")
    fun getAllStations():List<StationEntity>

    @Query("DELETE FROM Station")
    fun deleteAllStations()

}
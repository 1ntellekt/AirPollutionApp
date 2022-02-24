package com.example.airpollutionapp.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Station")
data class StationEntity(
    @PrimaryKey(autoGenerate = true)
    var id:Int=0,
    @ColumnInfo(name = "name")
    var name: String,
    @ColumnInfo(name = "longitude")
    var longitude:Double,
    @ColumnInfo(name = "latitude")
    var latitude:Double,
    @ColumnInfo(name = "address")
    var address: String,
    @ColumnInfo(name = "city")
    var city: String,

    @ColumnInfo(name = "co")
    var co: Double,
    @ColumnInfo(name = "no")
    var no: Double,
    @ColumnInfo(name = "no2")
    var no2: Double,
    @ColumnInfo(name = "so2")
    var so2: Double,
    @ColumnInfo(name = "nh3")
    var nh3: Double,
    @ColumnInfo(name = "pm10")
    var pm10: Double,
    @ColumnInfo(name = "pm25")
    var pm25: Double
)

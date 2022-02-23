package com.example.airpollutionapp.models


import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable


data class Station(
    val id:Int? = null,
    val name: String ="",
    val longitude:Double=0.0,
    val latitude:Double=0.0,
    val address: String ="",
    val city: String ="",
    val components: MutableMap<String,Double> = mutableMapOf()
):Serializable
package com.example.airpollutionapp.map

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.airpollutionapp.APP_ACTIVITY
import com.example.airpollutionapp.models.Station
import com.google.android.gms.maps.model.PolygonOptions
import org.json.JSONObject

class MapsViewModel(application: Application):AndroidViewModel(application) {

    private var requestQueue: RequestQueue = Volley.newRequestQueue(APP_ACTIVITY)


    fun getStationsPolygon(polygonOpt: PolygonOptions, onSuccess:(List<Station>)->Unit, onFail:(String)->Unit){
        val url = "https://air-pollution-app-q1.herokuapp.com/polygon"
        val strList = mutableListOf<String>()
        polygonOpt.points.forEach {
            strList.add("\"${it.latitude} ${it.longitude}\"")
        }
        strList.add(strList[0])

        val params = mutableMapOf<String,Any>()
        params["polygon"] = strList

        val jsonObjectRequest = object : JsonObjectRequest(Request.Method.POST,url, JSONObject(params.toString()),{ response ->

            if (response.getBoolean("status")){
                val stationList = mutableListOf<Station>()

                val stationsJsonArray = response.getJSONArray("stations")

                for (i in 0 until stationsJsonArray.length()){
                    val jsonStation = stationsJsonArray.getJSONObject(i)
                    val station = Station(
                        id = jsonStation.getInt("id"),
                        name = jsonStation.getString("name"),
                        address = jsonStation.getString("address"),
                        longitude = jsonStation.getDouble("longitude"),
                        latitude = jsonStation.getDouble("latitude"),
                        city = jsonStation.getString("city")
                    ).also { st->
                        st.apply {
                            components["co"] = jsonStation.getDouble("co")
                            components["no"] = jsonStation.getDouble("no")
                            components["no2"] = jsonStation.getDouble("no2")
                            components["so2"] = jsonStation.getDouble("so2")
                            components["nh3"] = jsonStation.getDouble("nh3")
                            components["pm10"] = jsonStation.getDouble("pm10")
                            components["pm2.5"] = jsonStation.getDouble("pm2_5")
                        }
                    }
                    stationList.add(station)
                }
                onSuccess(stationList)
            }

            Log.i("tag_poly", "message: ${response.getString("message")}")

        },{ error->
                Log.e("tag_poly", "error: ${error.message.toString()}")
                onFail(error.message.toString())
        } )
        {
            override fun getHeaders(): MutableMap<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers.put("Content-Type", "application/json; charset=utf-8")
                headers.put("Accept", "application/json")
                return headers
            }
        }
        requestQueue.add(jsonObjectRequest)
    }


}
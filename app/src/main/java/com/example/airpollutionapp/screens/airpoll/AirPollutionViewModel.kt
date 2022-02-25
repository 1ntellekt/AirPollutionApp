package com.example.airpollutionapp.screens.airpoll

import android.app.Application
import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.airpollutionapp.APP_ACTIVITY
import com.example.airpollutionapp.datePattern
import com.example.airpollutionapp.models.Station
import com.example.airpollutionapp.models.WindInstance
import com.example.airpollutionapp.room.database.LocalDB
import com.example.airpollutionapp.room.entity.StationEntity
import com.example.airpollutionapp.setInitData
import com.example.airpollutionapp.showToast
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AirPollutionViewModel(application: Application):AndroidViewModel(application) {

    var stationLiveData:MutableLiveData<List<Station>> = MutableLiveData()
    private var requestQueue: RequestQueue = Volley.newRequestQueue(APP_ACTIVITY)
    private val mDatabase = FirebaseFirestore.getInstance()
    private val stations = mDatabase.collection("stations")
    private val listStationFromUrl = mutableListOf<Station>()

    fun getAllStationsOpenWeather(onSuccess:()->Unit, onFail:(String)->Unit) {
        val url = "http://atmosphera.kz:4004/stations"
         viewModelScope.launch(Dispatchers.IO){
             listStationFromUrl.clear()
             val jsonObjectRequest = JsonArrayRequest(Request.Method.GET,url,null,{ jsonArray->
                 for (i in 0 until jsonArray.length()) {
                     val jsonObject = jsonArray.getJSONObject(i)

                     val station = Station(
                         name = jsonObject.getString("nameRu"),
                         longitude = jsonObject.getDouble("longitude"),
                         latitude = jsonObject.getDouble("latitude"),
                         address = jsonObject.getString("addressRu"),
                         city = jsonObject.getString("cityEn")
                     )

                     if(station.city == "Ust-Kamenogorsk") {
                         if (!station.name.contains("AirKaz") && !station.name.contains("(ручной)"))
                         {
                             getComponents(station)
                                 // listStationFromUrl.add(station)
                         }
                     }
                 }

                 Log.i("tagSize","size: ${listStationFromUrl.size}")

                 viewModelScope.launch(Dispatchers.Main){
                    // stationLiveData.postValue(listStationFromUrl)
                     onSuccess()
                 }

             }, { error->
                 Log.e("tag","${error.message}")
                 viewModelScope.launch(Dispatchers.Main){
                     stationLiveData.postValue(null)
                     onFail(error.message.toString())
                 }
                 //showToast("Error get from URL response {Stations}!")
             })
                  requestQueue.add(jsonObjectRequest)
                 //

            /*             val umzFactory = Station(name = "Завод УМЗ", latitude = 49.97953973052293,
                 longitude = 82.62973320762704, address = "пр.Абая 102" , city = "Ust-Kamenogorsk")
             getComponents(umzFactory)

             val tmkFactory = Station(name = "Завод TMK", latitude = 50.029055501159924,
                 longitude =82.7635794686513, address = "Менделеева" , city = "Ust-Kamenogorsk")
             getComponents(tmkFactory)

             val armFactory = Station(name = "Завод Арматурный", latitude = 50.01350531781607,
                 longitude = 82.67060095475433, address = "Машиностроителей, 1/7" , city = "Ust-Kamenogorsk")
             getComponents(armFactory)

             val keramFactory = Station(name = "Завод Керамзитовый", latitude = 50.00259325716911,
                 longitude = 82.6327498686513, address = "ул. Примыкание, 1" , city = "Ust-Kamenogorsk")
             getComponents(keramFactory)



             onSuccess()*/

         }
    }

    private fun getComponents( station: Station){
        val lat = station.latitude
        val lon = station.longitude
        val apiKey = "e3fc044955e04204c19ed28f90ea68cf"
        val url = "http://api.openweathermap.org/data/2.5/air_pollution/forecast?lat=$lat&lon=$lon&appid=$apiKey"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET,url,null,{ jsonObj->

            val jsonArrayList = jsonObj.getJSONArray("list")

            val jsonObjCompFirst = jsonArrayList.getJSONObject(0)
            val jsonComp = jsonObjCompFirst.getJSONObject("components")

            station.apply {
                components["co"] = jsonComp.getDouble("co")// оксид углерода
                components["no"] = jsonComp.getDouble("no")// оксид азота
                components["no2"] = jsonComp.getDouble("no2")// диоксид азота
                components["so2"] = jsonComp.getDouble("so2")// диоксид серы
                components["pm2.5"] = jsonComp.getDouble("pm2_5")// взвешенные частицы pm2.5
                components["pm10"] = jsonComp.getDouble("pm10") // взвешенные частицы pm10
                components["nh3"] = jsonComp.getDouble("nh3") // аммиак
            }

            listStationFromUrl.add(station)

            if(station.components.isNotEmpty()){
                stationLiveData.postValue(listStationFromUrl)
            }


            Log.i("tagComp",": $station")
            //getColorOfPollution(listStationFromUrl[i].components)

        }, { error->
            Log.e("tag","${error.message}")
            showToast("Error get from URL response {Components Air}!")
        })
        requestQueue.add(jsonObjectRequest)
    }

    fun saveDataDBFire(){
        viewModelScope.launch(Dispatchers.IO){
        stations.get().addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.forEach {
                    it.reference.delete()
                }

            Handler().postDelayed({

                    for (item in listStationFromUrl) {
                        Log.i("tagFire","$item")
                        stations.document().set(mapOf
                            (
                            "name" to item.name,
                            "city" to item.city,
                            "address" to item.address,
                            "longitude" to item.longitude,
                            "latitude" to item.latitude,
                            "components" to item.components
                        )
                        )
                            .addOnFailureListener {
                                Log.e("tag", "${it.message}")
                            }
                    }
                setInitData(SimpleDateFormat(datePattern, Locale.getDefault()).format(Date()))
            },4000)
        }
       }

    }

    fun getAllStationsDbFire(onSuccess: () -> Unit, onFail: (String) -> Unit){
        viewModelScope.launch(Dispatchers.IO){
            stations.get().addOnSuccessListener { snaps ->
                listStationFromUrl.clear()
                for (doc in snaps){
                    val station = doc.toObject(Station::class.java)
                    listStationFromUrl.add(station)
                    Log.i("tagFire","${doc.data}")
                }

                if (listStationFromUrl.isNotEmpty())
                stationLiveData.postValue(listStationFromUrl)

                viewModelScope.launch(Dispatchers.Main){
                    onSuccess()
                }

            }.addOnFailureListener {
                viewModelScope.launch(Dispatchers.Main){
                    onFail(it.message.toString())
                }
            }
        }
    }

    fun getWindOfCity(onSuccess: () -> Unit,onFail: (String) -> Unit){
        val lat = 50.00926222222222
        val lon = 82.56449888888889
        val apiKey = "e3fc044955e04204c19ed28f90ea68cf"
        val url = "https://api.openweathermap.org/data/2.5/onecall?lat=$lat&lon=$lon&exclude=hourly&appid=$apiKey"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null, { response->
          val currentJSONObj = response.getJSONObject("current")
            WindInstance.speed = currentJSONObj.getInt("wind_speed")
            WindInstance.deg = currentJSONObj.getInt("wind_deg")
            onSuccess()
        },{ error->
            Log.e("tag","${error.message}")
           // viewModelScope.launch(Dispatchers.Main){
                //LiveData.postValue(null)
                onFail(error.message.toString())
        })
        requestQueue.add(jsonObjectRequest)
    }

    fun getAllStationsAPI(onSuccess:()->Unit, onFail:(String)->Unit){
        val url = "https://air-pollution-app-q1.herokuapp.com/stations"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url,null, { response ->

          val statusRes =  response.getBoolean("status")
          val message = response.getString("message")

            if(statusRes){

                listStationFromUrl.clear()

                val jsonStationsArray = response.getJSONArray("stations")
                for (i in 0 until jsonStationsArray.length()) {
                    val jsonStation = jsonStationsArray.getJSONObject(i)
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
                    listStationFromUrl.add(station)
                }

                if (listStationFromUrl.isNotEmpty()){
                    stationLiveData.postValue(listStationFromUrl)
                }

                Log.i("tagAPI", message)
                onSuccess()

            }

        }, { error->
            Log.e("tagApi", error.message.toString())
            stationLiveData.postValue(null)
            onFail(error.message.toString())
        })
        requestQueue.add(jsonObjectRequest)
    }

    fun saveStationsAPI(){
        val url = "https://air-pollution-app-q1.herokuapp.com/allstations"
        val stringRequest = StringRequest(Request.Method.DELETE, url, { response ->
            val jsonObject = JSONObject(response)
            val statusReq = jsonObject.getBoolean("status")

            if (statusReq){
                for (i in 0 until listStationFromUrl.size){
                    Handler().postDelayed({
                        addStationToAPI(i,listStationFromUrl[i])
                    },200)
                }
            }

                 setInitData(SimpleDateFormat(datePattern, Locale.getDefault()).format(Date()))

        },{ error ->
            Log.e("tagAPI","DELETE ALL Stations ${error.message.toString()}")
           // onFail(error.message.toString())
        })
        requestQueue.add(stringRequest)
    }

    private fun addStationToAPI(ind:Int , station: Station){
        val url = "https://air-pollution-app-q1.herokuapp.com/stations"

        val params:MutableMap<String, String> = mutableMapOf()
        params["name"] = station.name
        params["address"] = station.address
        params["city"] = station.city
        params["latitude"] = station.latitude.toString()
        params["longitude"] = station.longitude.toString()

        params["co"] = station.components["co"].toString()
        params["no"] = station.components["no"].toString()
        params["no2"] = station.components["no2"].toString()
        params["so2"] = station.components["so2"].toString()
        params["nh3"] = station.components["nh3"].toString()
        params["pm10"] = station.components["pm10"].toString()
        params["pm2_5"] = station.components["pm2.5"].toString()

        val stringRequest = object : StringRequest(Request.Method.POST, url, { response ->
            val jsonObject = JSONObject(response)

            if (jsonObject.getBoolean("status")){
                val insertId = jsonObject.getInt("added_id")
                listStationFromUrl.set(ind,station.copy(id = insertId))
                if (listStationFromUrl.isNotEmpty()){
                    Log.i("tagSt", "${listStationFromUrl[ind]}")
                    stationLiveData.postValue(listStationFromUrl)
                }
                Log.i("tagAPI", "GET on $insertId| $station")
            }

        }, {error ->
            Log.e("tagAPI","ADD Station ${error.message.toString()}")
        }){
            override fun getParams(): MutableMap<String, String> {
                return params
            }
        }
        requestQueue.add(stringRequest)
    }

    fun getStationLocalDB(onSuccess: () -> Unit) {

        try {
            viewModelScope.launch(Dispatchers.Default){
                    listStationFromUrl.clear()
                    val stations = mutableListOf<Station>()
                    LocalDB.getDatabase(getApplication())?.stationDao()?.getAllStations()?.forEach { stationEntity ->
                        val compMap = mutableMapOf<String, Double>()
                        compMap["co"] = stationEntity.co
                        compMap["no"] = stationEntity.no
                        compMap["no2"] = stationEntity.no2
                        compMap["so2"] = stationEntity.so2
                        compMap["nh3"] = stationEntity.nh3
                        compMap["pm10"] = stationEntity.pm10
                        compMap["pm2.5"] = stationEntity.pm25
                        val station = Station(
                            name = stationEntity.name,
                            address = stationEntity.address,
                            city = stationEntity.city,
                            longitude = stationEntity.longitude,
                            latitude = stationEntity.latitude,
                            components = compMap
                        )
                        stations.add(station)
                    }
                    Log.i("tagLocal", "size list from db: ${stations.size}")
                    if (stations.isNotEmpty()) {
                        listStationFromUrl.addAll(stations)
                        stationLiveData.postValue(listStationFromUrl)
                    }

                    viewModelScope.launch(Dispatchers.Main){
                        onSuccess()
                    }
            }
        } catch (e:Exception){
            Log.e("tagE", "error: ${e.message.toString()}")
        }


    }

    fun saveStationsToLocalDB() {
        viewModelScope.launch(Dispatchers.Default){

        try{
            LocalDB.getDatabase(getApplication())?.stationDao()?.deleteAllStations()
                listStationFromUrl.forEach { station ->

                    val stationEntity = StationEntity(name = station.name, longitude = station.longitude,
                        latitude = station.latitude, address = station.address, city = station.city,
                        co = station.components["co"]!!, no = station.components["no"]!!, no2 = station.components["no2"]!!, so2 = station.components["so2"]!!,
                        pm25 = station.components["pm2.5"]!!, pm10 = station.components["pm10"]!!, nh3 = station.components["nh3"]!!)

                        LocalDB.getDatabase(APP_ACTIVITY)?.stationDao()?.addStation(stationEntity)
                }

                Log.i("tagL", "local size list : ${LocalDB.getDatabase(APP_ACTIVITY)?.stationDao()?.getAllStations()?.size}")
                //setInitData(SimpleDateFormat(datePattern, Locale.getDefault()).format(Date()))

        }catch (e:Exception){
            //e.printStackTrace()
            Log.e("tagL", "error: ${e.message.toString()}")
        }


        }
    }

    fun getDistanceFromAPI(isKM:Boolean,station1: Station, station2: Station,onSuccess: (Double) -> Unit, onFail: (String) -> Unit){

        val params  = mutableMapOf<String, String>()
        params["lat1"] = station1.latitude.toString()
        params["lng1"] = station1.longitude.toString()
        params["lat2"] = station2.latitude.toString()
        params["lng2"] = station2.longitude.toString()

        if (isKM){
            params["unit"] = "km"
        }

        val url = "https://air-pollution-app-q1.herokuapp.com/distance"
        val stringRequest = object : StringRequest(Request.Method.POST, url,  {

             val jsonObject = JSONObject(it)
            onSuccess(jsonObject.getDouble("distance"))

        }, {
            Log.e("tagD", "error post distance: ${it.message.toString()}")
            onFail(it.message.toString())
        }) {
            override fun getParams(): MutableMap<String, String>? {
                return params
            }
        }
        requestQueue.add(stringRequest)
    }

}
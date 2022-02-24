package com.example.airpollutionapp

import android.app.ProgressDialog
import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.widget.Toast
import com.example.airpollutionapp.models.WindInstance
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.data.geojson.GeoJsonLayer

lateinit var APP_ACTIVITY:MainActivity
lateinit var progressDialog:ProgressDialog
const val datePattern = "yyyy.MM.dd HH:mm:ss"
const val testDate = "2000.01.20 12:00:00"

fun showToast(msg:String){
    Toast.makeText(APP_ACTIVITY,msg,Toast.LENGTH_SHORT).show()
}


fun getUserInitBool():Boolean{
    return APP_ACTIVITY.getSharedPreferences("user",MODE_PRIVATE).getBoolean("init_user",false)
}

fun setUserInitBool(init:Boolean) {
    val editPref = APP_ACTIVITY.getSharedPreferences("user", MODE_PRIVATE).edit()
    editPref.putBoolean("init_user",init)
    editPref.apply()
}

fun setWindPref(){
    val editPref = APP_ACTIVITY.getSharedPreferences("wind", MODE_PRIVATE).edit()
    editPref.putFloat("speed", WindInstance.speed.toFloat())
    editPref.putFloat("deg", WindInstance.deg.toFloat())
    editPref.apply()
}

fun initWindByPref() {
    val prefs = APP_ACTIVITY.getSharedPreferences("wind", MODE_PRIVATE)
    WindInstance.speed = prefs.getFloat("speed",0.0f).toInt()
    WindInstance.deg = prefs.getFloat("deg",0.0f).toInt()
}


fun showProgressDialog(title:String){
    progressDialog.setTitle(title)
    progressDialog.show()
}

fun closeProgressDialog(){
    if (progressDialog.isShowing)
        progressDialog.dismiss()
}


fun getInitData(): String? {
    return APP_ACTIVITY.getSharedPreferences("data", MODE_PRIVATE).getString("timeInit", testDate)
}

fun setInitData(time:String){
  val edit = APP_ACTIVITY.getSharedPreferences("data", MODE_PRIVATE).edit()
    edit.putString("timeInit",time)
    edit.apply()
}


 fun getColorOfPollution(mapComponents:MutableMap<String,Double>) :MutableMap<String,String> {

    val componentMap = mutableMapOf<String,String>()

    mapComponents.forEach{ component ->

        when(component.key){

            "co"->{
                when(component.value){
                    in 0.0..999.9 -> componentMap[component.key] = "green"
                    in 1000.0..2499.9 -> componentMap[component.key] = "yellow"
                    in 2500.0..4999.9 -> componentMap[component.key] = "orange"
                    else -> componentMap[component.key] = "red"
                }
            }

            "no2"->{
                when(component.value){
                    in 0.0..39.9 -> componentMap[component.key] = "green"
                    in 40.0..99.9 -> componentMap[component.key] = "yellow"
                    in 100.0..199.9 -> componentMap[component.key] = "orange"
                    else -> componentMap[component.key] = "red"
                }
            }

            "so2"->{
                when(component.value){
                    in 0.0..99.9 -> componentMap[component.key] = "green"
                    in 100.0..249.9 -> componentMap[component.key] = "yellow"
                    in 250.0..499.9 -> componentMap[component.key] = "orange"
                    else -> componentMap[component.key] = "red"
                }
            }

            "no"->{
                when(component.value){
                    in 0.0..79.9 -> componentMap[component.key] = "green"
                    in 80.0..199.9 -> componentMap[component.key] = "yellow"
                    in 200.0..399.9 -> componentMap[component.key] = "orange"
                    else -> componentMap[component.key] = "red"
                }
            }

            "nh3"->{
                when(component.value){
                    in 0.0..400.9 -> componentMap[component.key] = "green"
                    in 401.0..800.9 -> componentMap[component.key] = "yellow"
                    in 801.0..1200.9 -> componentMap[component.key] = "orange"
                    else -> componentMap[component.key] = "red"
                }
            }

            "pm2.5" -> {
                when(component.value){
                    in 0.0..31.9 -> componentMap[component.key] = "green"
                    in 32.0..79.9 -> componentMap[component.key] = "yellow"
                    in 80.0..159.9 -> componentMap[component.key] = "orange"
                    else -> componentMap[component.key] = "red"
                }
            }

            "pm10" -> {
                when(component.value){
                    in 0.0..59.9 -> componentMap[component.key] = "green"
                    in 60.0..139.9 -> componentMap[component.key] = "yellow"
                    in 140.0..299.9 -> componentMap[component.key] = "orange"
                    else -> componentMap[component.key] = "red"
                }
            }

        }

    }

    //Log.i("tag", componentMap.toString())
    return componentMap
}

fun getDescriptionPollution(componentStr:String):String{
    return when(componentStr){
        "co" -> APP_ACTIVITY.getString(R.string.CO)
        "no" -> APP_ACTIVITY.getString(R.string.NO)
        "nh3" -> APP_ACTIVITY.getString(R.string.NH3)
        "no2" -> APP_ACTIVITY.getString(R.string.NO2)
        "so2" -> APP_ACTIVITY.getString(R.string.SO2)
        "pm2.5" -> APP_ACTIVITY.getString(R.string.PM2_5)
        "pm10" -> APP_ACTIVITY.getString(R.string.PM10)
        else -> APP_ACTIVITY.getString(R.string.CO)
    }
}

fun getColorOfName(colorName:String):Int{
    return when(colorName){
        "red" -> Color.argb(70,255,0,0)
        "green" -> Color.argb(70,0,255,0)
        "yellow" -> Color.argb(70,255,255,0)
        "orange" -> Color.argb(70,255,165,0)
        else -> {Color.argb(70,0,255,0)}
    }
}

fun getWindWithDegrees():String{
    return when(WindInstance.deg){
       in 0..22 -> "Ю"
        in 23..67 -> "ЮЗ"
        in 68..112 -> "З"
        in 113..157 -> "СЗ"
        in 158..202 -> "С"
        in 203..247 -> "СВ"
        in 248..292 -> "В"
        in 293..337 -> "ЮВ"
        else -> "Ю"
    }
}

fun getWindName():String{
    return when(WindInstance.deg){
        in 0..22 -> "С"
        in 23..67 -> "СВ"
        in 68..112 -> "В"
        in 113..157 -> "ЮВ"
        in 158..202 -> "Ю"
        in 203..247 -> "ЮЗ"
        in 248..292 -> "З"
        in 293..337 -> "СЗ"
        else -> "С"
}
}

fun getCoordinateAreaName(areaName:String):LatLng{
    return when(areaName){
        "Abai" -> {
            LatLng(49.0819945,79.50520885170317)
        }
        "Altai" -> {
            LatLng(49.82404265,84.00252872514564)
        }
        "Ayagoz" -> {
            LatLng(47.828935099999995,79.28794126870397)
        }
        "Beskara" -> {
            LatLng(51.06266275,78.98379276309502)
        }
        "Borod" -> {
            LatLng(50.78037035,80.76292696853781)
        }
        "Glub" -> {
            LatLng(50.446683300000004,82.89993631706531)
        }
        "Jarmin" -> {
            LatLng(49.259948949999995,81.42059982649938)
        }
        "Katon" -> {
            LatLng(49.213886,84.508766)
        }
        "Kokpek" -> {
            LatLng(48.822366849999995,82.68666983738916)
        }
        "Kurshim" -> {
            LatLng(48.4572135,84.86434859596294)
        }
        "Shaman" -> {
            LatLng(50.5572813,82.13809346314846)
        }
        "Tarb" -> {
            LatLng(47.7450649,82.89393907422081)
        }
        "Ulan" -> {
            LatLng(49.748705900000004,82.32742282896285)
        }
        "Uzhar" -> {
            LatLng(46.6242091,81.73770095587159)
        }
        "Semey" -> {
            LatLng(50.4049863,80.2491787)
        }
        else -> {LatLng(49.50115765,72.70046605651511)}
    }
}

fun getNameArea(areaName: String):String{
    return when(areaName){
        "Abai" -> {
            "Абайский район"
        }
        "Altai" -> {
            "Алтайский район"
        }
        "Ayagoz" -> {
            "Аягозский район"
        }
        "Beskara" -> {
            "Бескарагайский район"
        }
        "Borod" -> {
            "Бородулихинский район"
        }
        "Glub" -> {
            "Глубоковский район"
        }
        "Jarmin" -> {
            "Жарминский район"
        }
        "Katon" -> {
            "Катон-Карагайский район"
        }
        "Kokpek" -> {
            "Кокпектинский район"
        }
        "Kurshim" -> {
            "Курчумский район"
        }
        "Shaman" -> {
            "Шемонаихинский район"
        }
        "Tarb" -> {
            "Тарбагатайский район"
        }
        "Ulan" -> {
            "Уланский район"
        }
        "Uzhar" -> {
            "Урджарский район"
        }
        "Semey" -> {
            "Семей"
        }
        else -> {"Семей"}
    }
}

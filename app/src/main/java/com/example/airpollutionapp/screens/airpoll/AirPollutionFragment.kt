package com.example.airpollutionapp.screens.airpoll

import android.app.Dialog
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.airpollutionapp.*
import com.example.airpollutionapp.databinding.FragmentAirPollutionBinding
import com.example.airpollutionapp.models.Station
import com.example.airpollutionapp.models.WindInstance
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*


class AirPollutionFragment : Fragment() {

    private lateinit var binding:FragmentAirPollutionBinding
    private lateinit var requestQueue: RequestQueue
    private lateinit var adapterSpinner:ArrayAdapter<String>
    private lateinit var mViewModel: AirPollutionViewModel
    private val listStationFromUrl = mutableListOf<Station>()
    private lateinit var mObserver: Observer<List<Station>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }

        requestQueue = Volley.newRequestQueue(requireContext())
        adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item,
            arrayOf(
                "CO - Оксид углерода",
                "NO2 - Диоксид азота",
                "SO2 - Диоксид серы",
                "NH3 - Аммиак",
                "PM10 - Взвешанные частицы",
                "PM2.5 - Взвешанные частицы",
                "NO - Оксид азота"
            )
        )

        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mViewModel = ViewModelProvider(this).get(AirPollutionViewModel::class.java)

        mObserver = Observer {
            if (it.isNotEmpty()) {
                listStationFromUrl.clear()
                listStationFromUrl.addAll(it)
                //showToast("(observer)list size: ${it.size}")
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentAirPollutionBinding.inflate(inflater,container,false)
        return binding.root
    }

    private var firstDelay = 3000

    private val spinnerListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val str = binding.spinnerComponent.selectedItem.toString().substringBefore(" -")
                .lowercase(Locale.getDefault())
            //showProgressDialog("Loading data (select other $str)...")
            //Handler().postDelayed({
                Log.i("setFrag", " size list {listener} = ${listStationFromUrl.size}")
            setArgsToMap(str)
               // firstDelay -= (firstDelay-300)
            // closeProgressDialog()
            //}, firstDelay.toLong())
        }
        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("Not yet implemented")
        }
    }

    override fun onStart() {
        super.onStart()

        binding.apply {

            btnExit.setOnClickListener {
                setUserInitBool(false)
                FirebaseAuth.getInstance().signOut()
                APP_ACTIVITY.mNavController.navigate(R.id.action_airPollutionFragment_to_signInFragment)
            }

            btnDownload.setOnClickListener {
                mViewModel.saveStationsToLocalDB()
                setWindPref()
            }

            btnWindChange.setOnClickListener {
                showPopupMenu()
            }

            spinnerComponent.adapter = adapterSpinner
        }

        mViewModel.stationLiveData.observe(this,mObserver)

        mViewModel.getWindOfCity({
            //Log.i("wind_tag", "deg: speed:")
            binding.tvWind.text = "Скорость ветра: ${WindInstance.speed} Направление(в градусах):${WindInstance.deg} ${getWindName()}"
        },{})

        showProgressDialog("Loading data from URL....")

        if (isNetworkConnected()){
            getDataWithInternet()
        } else {
            getDataWithLocal()
        }

    }

    private fun getDataWithLocal() {
        initWindByPref()
        binding.tvWind.text = "Скорость ветра: ${WindInstance.speed} Направление(в градусах):${WindInstance.deg} ${getWindName()}"
        mViewModel.getStationLocalDB {
            try {
                showToast("Get data from local db")
                if (listStationFromUrl.isEmpty()){
                    Snackbar.make(APP_ACTIVITY.findViewById(android.R.id.content),"Local DB is empty!", Snackbar.LENGTH_INDEFINITE).show()
                } else {
                    binding.spinnerComponent.apply {
                         onItemSelectedListener = spinnerListener
                         setSelection(adapterSpinner.count-1)
                    }
                }
                closeProgressDialog()
            } catch (e:Exception) {
                Log.e("tag", "error ${e.message.toString()}")
            }
        }
    }

    private fun getDataWithInternet(){
        val timeSavedDate = SimpleDateFormat(datePattern, Locale.getDefault()).parse(getInitData())
        if (getInitData()== testDate){
            mViewModel.getAllStationsOpenWeather({
                showToast("Get data from openweather")
                mViewModel.saveStationsAPI()
                binding.spinnerComponent.apply {
                    onItemSelectedListener = spinnerListener
                    setSelection(adapterSpinner.count-1)
                }
                closeProgressDialog()
            },{
                showToast("Error get from URL response {Stations}!")
            })
        } else {
            if (checkTimeInPrefs(timeSavedDate!!)){
                mViewModel.getAllStationsOpenWeather({
                    showToast("Get data from openweather")
                    mViewModel.saveStationsAPI()
                    Handler().postDelayed({
                        binding.spinnerComponent.apply {
                            onItemSelectedListener = spinnerListener
                            setSelection(adapterSpinner.count-1)
                        }},3000)
                    closeProgressDialog()
                },{
                    showToast("Error get from URL response $it!")
                })
            } else {
                mViewModel.getAllStationsAPI({
                    showToast("Get data from api")
                    binding.spinnerComponent.apply {
                        onItemSelectedListener = spinnerListener
                        setSelection(adapterSpinner.count-1)
                    }
                    closeProgressDialog()
                },{
                    showToast("Error get from DB Firebase $it!")
                })
            }
        }
    }

    private fun isNetworkConnected(): Boolean {
        val cm = APP_ACTIVITY.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
        return cm!!.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    private fun showPopupMenu() {
        val popupMenu = PopupMenu(context,binding.btnWindChange)
        popupMenu.inflate(R.menu.layout_popup_menu)
        popupMenu.setOnMenuItemClickListener { item -> popupMenuItemClicked(item) }
        popupMenu.show()
    }

    private fun popupMenuItemClicked(menuItem: MenuItem):Boolean {
        when(menuItem.itemId){
            R.id.idMenuSpeed -> {
                showBottomDialog(1)
            }
            R.id.idMenuSpeedDeg -> {
                showBottomDialog(2)
            }
        }
        return true
    }

    private fun showBottomDialog(typeEdit: Int) {
        val dialog = Dialog(APP_ACTIVITY)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_sheet_layout)

        val edSpeed = dialog.findViewById<EditText>(R.id.edSpeed)
        val edDegree = dialog.findViewById<EditText>(R.id.edDegree)
        val btnOkChange = dialog.findViewById<Button>(R.id.btnOkChange)

        if (typeEdit == 1) {
            edDegree.visibility = View.GONE
            btnOkChange.setOnClickListener {
                if (edSpeed.text.toString().isNotEmpty()){
                    WindInstance.speed = edSpeed.text.toString().toInt()
                    updateUIWindow()
                }
                dialog.dismiss()
            }
        } else {
            edSpeed.visibility = View.GONE
            btnOkChange.setOnClickListener {
                if (edDegree.text.toString().isNotEmpty()){
                    WindInstance.deg = edDegree.text.toString().toInt()
                    updateUIWindow()
                }
                dialog.dismiss()
            }
        }
        dialog.show()
        dialog.window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
    }

    private fun updateUIWindow(){
        binding.tvWind.text = "Скорость ветра: ${WindInstance.speed} Направление(в градусах):${WindInstance.deg} ${getWindName()}"
        setArgsToMap(str = binding.spinnerComponent.selectedItem.toString().substringBefore(" -").lowercase(Locale.getDefault()))
    }

    private fun setArgsToMap(str: String) {
        if (listStationFromUrl.isNotEmpty()){
            binding.tvTextComponent.visibility = View.VISIBLE
            binding.tvTextComponent.text = getDescriptionPollution(str)
            val bundle = Bundle()
            bundle.putString("component", str)
            bundle.putSerializable("stations",listStationFromUrl as Serializable)
            //UserInstance.stations.addAll(listStationFromUrl)
           // showToast("set fragment map")
            setFragment(MapsFragment().also { it.arguments = bundle })
        } else {
            showToast("not set fragment map, because list is empty!")
        }
    }

    private fun checkTimeInPrefs(timeSavedDate: Date): Boolean {
        val now = Date()
        val diff = now.time - timeSavedDate.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return (days >= 0 && minutes >= 4 && hours >= 0 && seconds >= 0)
    }

    private fun setFragment(fragment: Fragment) {
        val fragTransaction = parentFragmentManager.beginTransaction()
        fragTransaction.replace(R.id.frameLayout,fragment).commit()
    }

/*    private fun getAllStations() {
        val URL_API = "http://atmosphera.kz:4004/stations"
        val jsonObjectRequest = JsonArrayRequest(Request.Method.GET,URL_API,null,{ jsonArray->

            var k = 0

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
                    listStationFromUrl.add(station)
                    getComponents(k,station)
                    k++
                }

            }
            if (progressDialog.isShowing){
                progressDialog.dismiss()
            }

            Log.i("tag","size: ${listStationFromUrl.size}")

        }, { error->
            Log.e("tag","${error.message}")
            showToast("Error get from URL response {Stations}!")
        })

        requestQueue.add(jsonObjectRequest)
    }

    //http://api.openweathermap.org/data/2.5/air_pollution/forecast?lat=49.94481805555555&lon=82.65334027777779&appid=e3fc044955e04204c19ed28f90ea68cf

   private fun getComponents(i:Int, station: Station){
        val lat = station.latitude
        val lon = station.longitude
        val apiKey = "e3fc044955e04204c19ed28f90ea68cf"
        val url = "http://api.openweathermap.org/data/2.5/air_pollution/forecast?lat=$lat&lon=$lon&appid=$apiKey"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET,url,null,{ jsonObj->

            val jsonArrayList = jsonObj.getJSONArray("list")

            val jsonObjCompFirst = jsonArrayList.getJSONObject(0)
            val jsonComp = jsonObjCompFirst.getJSONObject("components")

            listStationFromUrl[i].apply {
                components["co"] = jsonComp.getInt("co")// оксид углерода
                components["no"] = jsonComp.getInt("no")// оксид азота
                components["no2"] = jsonComp.getInt("no2")// диоксид азота
                components["so2"] = jsonComp.getInt("so2")// диоксид серы
                components["pm2.5"] = jsonComp.getInt("pm2_5")// взвешенные частицы pm2.5
                components["pm10"] = jsonComp.getInt("pm10") // взвешенные частицы pm10
                components["nh3"] = jsonComp.getInt("nh3") // аммиак
            }

            Log.i("tag","$i: ${listStationFromUrl[i]}")
            //getColorOfPollution(listStationFromUrl[i].components)

        }, { error->
            Log.e("tag","${error.message}")
            showToast("Error get from URL response {Components Air}!")
        })

        requestQueue.add(jsonObjectRequest)
    }*/

    override fun onDestroyView() {
        super.onDestroyView()
        mViewModel.stationLiveData.removeObserver(mObserver)
    }

}

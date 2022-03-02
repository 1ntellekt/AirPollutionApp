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
import androidx.appcompat.widget.SwitchCompat
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
                /*listStationFromUrl.forEach {
                    Log.i("tagSt", "st: $it")
                }*/
            }

            btnDownload.setOnClickListener {
                mViewModel.saveStationsToLocalDB()
                setWindPref()
            }

            btnPopupMenu.setOnClickListener {
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

        if (isNetworkConnected()) {
            getDataWithInternet()
        } else {
            getDataWithLocal()
        }
    }

    private fun getDataWithLocal(){
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
        val popupMenu = PopupMenu(context,binding.btnPopupMenu)
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
            R.id.idDistanceStations -> {
                showTopDialog()
            }
        }
        return true
    }

    private fun showBottomDialog(typeEdit: Int) {
        val dialog = context?.let { Dialog(it) }
        dialog?.let {
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
    }

    private fun showTopDialog(){
        val dialog = context?.let { Dialog(it) }

        dialog?.let {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.top_sheet_layout)
            val spStation1  = dialog.findViewById<Spinner>(R.id.spStation1)
            val spStation2  = dialog.findViewById<Spinner>(R.id.spStation2)
            val tvRez = dialog.findViewById<TextView>(R.id.tvRezDistance)
            val switch = dialog.findViewById<SwitchCompat>(R.id.swKm)
            val btnDistance = dialog.findViewById<Button>(R.id.btnDistance)

            val arrayAdapter = ArrayAdapter(requireContext(),android.R.layout.simple_spinner_item,listStationFromUrl.map { "${it.name} ${it.address}" })
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spStation1.adapter = arrayAdapter
            spStation2.adapter = arrayAdapter

            var station1: Station? = null
            var station2: Station? = null

            val listener1 = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    station1 = listStationFromUrl[position]
                    //Log.i("tagSP", "spinner 1: ${station1.toString()}")
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }

            val listener2 = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    station2 = listStationFromUrl[position]
                    //Log.i("tagSP", "spinner 2: ${station2.toString()}")
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }

            spStation1.onItemSelectedListener = listener1
            spStation2.onItemSelectedListener = listener2

            btnDistance.setOnClickListener {
                if (station1 != null && station2 != null){
                    if (station1!!.id != station2!!.id){
                       // Log.i("tagSP", "btn click popup menu: ${station1.toString()} || ${station2.toString()}")
                        mViewModel.getDistanceFromAPI(switch.isChecked, station1!!, station2!!, {
                            tvRez.visibility = View.VISIBLE
                            if (switch.isChecked)
                            tvRez.text = "Итоговая дистанция: $it (km)"
                            else tvRez.text = "Итоговая дистанция: $it (meters)"
                        }, {
                            showToast(it)
                        })
                    }
                }
            }

            dialog.show()
            dialog.window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
            dialog.window?.setGravity(Gravity.TOP)
        }

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
        return (days >= 1 && minutes >= 4 && hours >= 0 && seconds >= 0)
    }

    private fun setFragment(fragment: Fragment) {
        val fragTransaction = parentFragmentManager.beginTransaction()
        fragTransaction.replace(R.id.frameLayout,fragment).commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mViewModel.stationLiveData.removeObserver(mObserver)
    }

}

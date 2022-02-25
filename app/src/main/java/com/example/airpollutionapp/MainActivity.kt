package com.example.airpollutionapp

import android.app.ProgressDialog
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.airpollutionapp.models.UserInstance
import java.security.Permission
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    lateinit var mNavController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mNavController = Navigation.findNavController(this, R.id.nav_host_fragment)
        APP_ACTIVITY = this
        progressDialog = ProgressDialog(APP_ACTIVITY)
    }

    override fun onStart() {
        super.onStart()
        checkLocationPermission()
    }

    private fun checkLocationPermission(){
        when {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                launchPermissionReq.launch(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION))
            }
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                launchPermissionReq.launch(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION))
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showToast("We need fine location permission!")
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                showToast("We need coarse location permission!")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //UserInstance.stations.clear()
    }

    private val launchPermissionReq = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            if (it[android.Manifest.permission.ACCESS_FINE_LOCATION] == true && it[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true){
                showToast("Permission granted!")
            } else {
                showToast("Permission denied!")
            }
    }


}
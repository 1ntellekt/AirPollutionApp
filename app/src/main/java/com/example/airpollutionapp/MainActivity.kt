package com.example.airpollutionapp

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.airpollutionapp.models.UserInstance

class MainActivity : AppCompatActivity() {

    lateinit var mNavController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mNavController = Navigation.findNavController(this, R.id.nav_host_fragment)
        APP_ACTIVITY = this
        progressDialog = ProgressDialog(APP_ACTIVITY)
    }

    override fun onDestroy() {
        super.onDestroy()
        //UserInstance.stations.clear()
    }


}
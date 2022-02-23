package com.example.airpollutionapp.screens.signup

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.airpollutionapp.APP_ACTIVITY
import com.example.airpollutionapp.R
import com.example.airpollutionapp.databinding.FragmentSignUpBinding
import com.example.airpollutionapp.setUserInitBool
import com.example.airpollutionapp.showToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpFragment : Fragment() {

    private val mDatabaseStore:FirebaseFirestore = FirebaseFirestore.getInstance()
    private val users = mDatabaseStore.collection("users")
    private val fireAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var binding: FragmentSignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSignUpBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        binding.apply {

            btnSignUp.setOnClickListener {
                if (edEmail.text.toString().isNotEmpty()&&edPassword.text.toString().isNotEmpty()&&edPhone.text.toString().isNotEmpty()&&edRegion.text.toString().isNotEmpty()){
                    fireAuth.createUserWithEmailAndPassword(edEmail.text.toString(),edPassword.text.toString())
                        .addOnSuccessListener {
                            users.document(fireAuth.currentUser!!.uid).set(mapOf(
                                "id" to fireAuth.currentUser!!.uid,
                                "email" to edEmail.text.toString(),
                                "password" to edPassword.text.toString(),
                                "phone" to edPhone.text.toString(),
                                "region" to edRegion.text.toString()
                            )).addOnSuccessListener {
                                showToast("Sign up success!")
                                setUserInitBool(true)
                                APP_ACTIVITY.mNavController.navigate(R.id.action_signUpFragment_to_airPollutionFragment)
                            }.addOnFailureListener {
                                showToast("${it.message}")
                            }
                        }
                        .addOnFailureListener { showToast("error sign up!") }
                }
            }

        }

    }

}
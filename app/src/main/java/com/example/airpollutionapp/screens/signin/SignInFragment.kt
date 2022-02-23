package com.example.airpollutionapp.screens.signin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.airpollutionapp.*
import com.example.airpollutionapp.databinding.FragmentSignInBinding
import com.google.firebase.auth.FirebaseAuth

class SignInFragment : Fragment() {

    private val fireAuth:FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var binding:FragmentSignInBinding

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
        binding = FragmentSignInBinding.inflate(inflater, container,false)
         return binding.root
    }


    override fun onStart() {
        super.onStart()

        if(getUserInitBool()){
            APP_ACTIVITY.mNavController.navigate(R.id.action_signInFragment_to_airPollutionFragment)
        }

        binding.apply {

            btnSignIn.setOnClickListener {
                if(edEmail.text.toString().isNotEmpty()&&edPassword.text.toString().isNotEmpty()){
                    fireAuth.signInWithEmailAndPassword(edEmail.text.toString(),edPassword.text.toString())
                        .addOnSuccessListener {
                            showToast("Sign in success!")
                            setUserInitBool(true)
                            APP_ACTIVITY.mNavController.navigate(R.id.action_signInFragment_to_airPollutionFragment)
                        }
                        .addOnFailureListener {
                            showToast("Sign in error:${it.message}!")
                        }
                }
            }

            tvToggle.setOnClickListener {
                APP_ACTIVITY.mNavController.navigate(R.id.action_signInFragment_to_signUpFragment)
            }

        }

    }

}
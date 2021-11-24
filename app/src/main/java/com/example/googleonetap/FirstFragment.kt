package com.example.googleonetap

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.IntentSenderRequest
import androidx.navigation.fragment.findNavController
import com.example.googleonetap.databinding.FragmentFirstBinding
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.lang.Exception
import com.google.android.gms.common.api.ApiException

import com.google.android.gms.auth.api.identity.SignInCredential

import androidx.activity.result.contract.ActivityResultContracts

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.CommonStatusCodes


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var oneTapClient: SignInClient

    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var signUpRequest: BeginSignInRequest

    private var showOneTapUI = true


    private val loginResultHandler = registerForActivityResult(
        StartIntentSenderForResult()
    ) { result: ActivityResult ->
        // handle intent result here
        if (result.resultCode == Activity.RESULT_OK) {
            var credential: SignInCredential? = null
            try {
                credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                val username = credential.id
                val password = credential.password
                if (idToken != null) {
                    // Got an ID token from Google. Use it to authenticate
                    // with your backend.
                    Log.d(TAG, "Got ID token.")
                } else if (password != null) {
                    // Got a saved username and password. Use them to authenticate
                    // with your backend.
                    Log.d(TAG, "Got password.")
                }

                val controller = findNavController()
                controller.navigate(R.id.action_FirstFragment_to_SecondFragment)
                
            } catch (e: ApiException) {
                when (e.statusCode) {
                    CommonStatusCodes.CANCELED -> {
                        Log.d(TAG, "One-tap dialog was closed.")
                        // Don't re-prompt the user.
                        showOneTapUI = false
                    }
                    CommonStatusCodes.NETWORK_ERROR -> {
                        Log.d(TAG, "One-tap encountered a network error.")
                        // Try again or just ignore.
                    }
                    else -> {
                        Log.d(
                            TAG, "Couldn't get credential from result." +
                                    " (${e.localizedMessage})"
                        )
                    }
                }

            }
        }
    }

    private val signUpResultHandler = registerForActivityResult(
        StartIntentSenderForResult()
    ) { result: ActivityResult ->
        // handle intent result here
        if (result.resultCode == Activity.RESULT_OK) {
            var credential: SignInCredential? = null
            try {
                credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                when {
                    idToken != null -> {
                        // Got an ID token from Google. Use it to authenticate
                        // with your backend.
                        Log.d(TAG, "Got ID token.")
                    }
                    else -> {
                        // Shouldn't happen.
                        Log.d(TAG, "No ID token!")
                    }
                }
            } catch (e: ApiException) {
                when (e.statusCode) {
                    CommonStatusCodes.CANCELED -> {
                        Log.d(TAG, "One-tap dialog was closed.")
                        // Don't re-prompt the user.
                        showOneTapUI = false
                    }
                    CommonStatusCodes.NETWORK_ERROR -> {
                        Log.d(TAG, "One-tap encountered a network error.")
                        // Try again or just ignore.
                    }
                    else -> {
                        Log.d(
                            TAG, "Couldn't get credential from result." +
                                    " (${e.localizedMessage})"
                        )
                    }
                }

            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        oneTapClient = Identity.getSignInClient(activity as MainActivity)



        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(
                BeginSignInRequest.PasswordRequestOptions.builder()
                    .setSupported(true)
                    .build()
            )
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.google_one_tap_server))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build()

        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.google_one_tap_server))
                    // Show all accounts on the device.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            signIn()
        }
    }


    private fun signIn() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(
                activity as MainActivity
            ) {
                try {
                    loginResultHandler.launch(
                        IntentSenderRequest.Builder(
                            it.pendingIntent
                                .intentSender
                        ).build()
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(activity as MainActivity) {
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Log.d(TAG, it.localizedMessage.orEmpty());
                signUp()
            }
    }

    private fun signUp() {
        oneTapClient.beginSignIn(signUpRequest)
            .addOnSuccessListener(activity as MainActivity) {
                try {
                    signUpResultHandler.launch(
                        IntentSenderRequest.Builder(it.pendingIntent.intentSender).build()
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(activity as MainActivity) { e ->
                // No Google Accounts found. Just continue presenting the signed-out UI.
                Log.d(TAG, e.localizedMessage.orEmpty())
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
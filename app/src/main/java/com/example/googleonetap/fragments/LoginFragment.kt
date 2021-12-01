package com.example.googleonetap.fragments


import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.IntentSender
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.googleonetap.CIPHERTEXT_WRAPPER
import com.example.googleonetap.MainActivity
import com.example.googleonetap.R
import com.example.googleonetap.SHARED_PREFS_FILENAME
import com.example.googleonetap.auth.*
import com.example.googleonetap.databinding.FragmentLoginBinding
import com.example.googleonetap.models.Credentials
import com.example.googleonetap.viewModels.LoginViewModel
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @Inject
    lateinit var authManager: AuthManager

    private val loginViewModel: LoginViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private lateinit var oneTapClient: SignInClient

    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var signUpRequest: BeginSignInRequest

    private var showOneTapUI = true

    private lateinit var biometricPrompt: BiometricPrompt
    private val cryptographyManager = CryptographyManager()
    private val ciphertextWrapper
        get() = cryptographyManager.getCiphertextWrapperFromSharedPrefs(
            requireContext(),
            SHARED_PREFS_FILENAME,
            Context.MODE_PRIVATE,
            CIPHERTEXT_WRAPPER
        )


    private val loginResultHandler = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
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
                    lifecycleScope.launch {
                        delay(100)
                        loginViewModel.loginGoogleToken(idToken)
                    }
                } else if (password != null) {
                    // Got a saved username and password. Use them to authenticate
                    // with your backend.
                    Log.d(TAG, "Got password.")
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

    private val signUpResultHandler = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
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
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

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

        loginViewModel.credentialsResult.observe(viewLifecycleOwner, Observer {
            val state = it ?: return@Observer
            when (state.valid) {
                true -> loginViewModel.loginNewToken(it.credentials)
                false -> {
                    binding.emailInput.error = it.emailError
                    binding.passwordInput.error = it.passwordError
                }
            }
        })

        val canAuthenticate = BiometricManager.from(requireContext()).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            binding.authBiometric.visibility = View.VISIBLE
            binding.authBiometric.setOnClickListener {
                if (ciphertextWrapper != null) {
                    showBiometricPromptForDecryption()
                } else {
                    //startActivity(Intent(this, EnableBiometricLoginActivity::class.java))
                }
            }
        } else {
            binding.authBiometric.visibility = View.INVISIBLE
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.passwordInput.editText?.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    val email = binding.emailInput.editText?.text.toString()
                    val password = binding.passwordInput.editText?.text.toString()
                    loginViewModel.validateCredentials(Credentials(email, password))
                }
            }
            false
        }

        binding.authEmail.setOnClickListener {
            val email = binding.emailInput.editText?.text.toString()
            val password = binding.passwordInput.editText?.text.toString()
            loginViewModel.validateCredentials(Credentials(email, password))
        }

        binding.emailInput.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.emailInput.error = ""
            }
        })

        binding.passwordInput.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.passwordInput.error = ""
            }
        })
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

    private fun showBiometricPromptForDecryption() {
        ciphertextWrapper?.let { textWrapper ->
            val secretKeyName = getString(R.string.secret_key_name)
            val cipher = cryptographyManager.getInitializedCipherForDecryption(
                secretKeyName, textWrapper.initializationVector
            )
            biometricPrompt =
                BiometricPromptUtils.createBiometricPrompt(
                    requireActivity() as AppCompatActivity,
                    ::decryptServerTokenFromStorage
                )
            val promptInfo =
                BiometricPromptUtils.createPromptInfo(requireActivity() as AppCompatActivity)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun decryptServerTokenFromStorage(authResult: BiometricPrompt.AuthenticationResult) {
        ciphertextWrapper?.let { textWrapper ->
            authResult.cryptoObject?.cipher?.let {
                val plaintext =
                    cryptographyManager.decryptData(textWrapper.ciphertext, it)
                Log.i("TOKEN", plaintext)
                loginViewModel.loginOldToken(plaintext)
                // Now that you have the token, you can query server for everything else
                // the only reason we call this fakeToken is because we didn't really get it from
                // the server. In your case, you will have gotten it from the server the first time
                // and therefore, it's a real token.
                /// updateApp(getString(R.string.already_signedin))
            }
        }
    }
}
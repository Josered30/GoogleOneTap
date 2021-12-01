package com.example.googleonetap.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.googleonetap.CIPHERTEXT_WRAPPER
import com.example.googleonetap.MainActivity
import com.example.googleonetap.R
import com.example.googleonetap.SHARED_PREFS_FILENAME
import com.example.googleonetap.auth.*
import com.example.googleonetap.databinding.FragmentHomeBinding
import com.example.googleonetap.models.Credentials
import com.example.googleonetap.viewModels.HomeViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    @Inject
    lateinit var authManager: AuthManager

    private val homeViewModel: HomeViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var cryptographyManager: CryptographyManager
    private lateinit var oneTapClient: SignInClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        if (!authManager.checkToken()) {
            authManager.changeState(User("", AuthState.UN_AUTH))
            val navController = findNavController()
            navController.navigate(R.id.action_to_LoginFragment)
        }

        homeViewModel.credentialsResult.observe(viewLifecycleOwner, Observer {
            val state = it ?: return@Observer
            when (state.valid) {
                true -> showBiometricPromptForEncryption()
                false -> {
                    binding.emailBiometricInput.error = it.emailError
                    binding.passwordBiometricInput.error = it.passwordError
                }
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.authorize.setOnClickListener {
            val email = binding.emailBiometricInput.editText?.text.toString()
            val password = binding.passwordBiometricInput.editText?.text.toString()
            homeViewModel.validateCredentials(Credentials(email, password))
        }

        oneTapClient = Identity.getSignInClient(activity as MainActivity)
//        if (context != null) {
//            val d = GoogleSignIn.getLastSignedInAccount(context)
//            Log.i("AUTH", "fvfds")
//        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showBiometricPromptForEncryption() {
        val canAuthenticate = BiometricManager.from(requireContext()).canAuthenticate()
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val secretKeyName = getString(R.string.secret_key_name)
            cryptographyManager = CryptographyManager()
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            val biometricPrompt =
                BiometricPromptUtils.createBiometricPrompt(
                    requireActivity() as AppCompatActivity,
                    ::encryptAndStoreServerToken
                )
            val promptInfo =
                BiometricPromptUtils.createPromptInfo(requireActivity() as AppCompatActivity)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun encryptAndStoreServerToken(authResult: BiometricPrompt.AuthenticationResult) {
        authResult.cryptoObject?.cipher?.apply {
            val user = authManager.currentValue
            user.token.let { token ->
                Log.d("AUTH", "The token from server is $token")
                val encryptedServerTokenWrapper = cryptographyManager.encryptData(token, this)
                cryptographyManager.persistCiphertextWrapperToSharedPrefs(
                    encryptedServerTokenWrapper,
                    requireContext(),
                    SHARED_PREFS_FILENAME,
                    Context.MODE_PRIVATE,
                    CIPHERTEXT_WRAPPER
                )
            }
        }
    }
}
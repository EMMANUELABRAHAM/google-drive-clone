package com.agape.googledriveclone.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.agape.googledriveclone.databinding.FragmentLoginBinding
import com.agape.googledriveclone.utils.AppUtils.showToast
import com.agape.googledriveclone.viewmodel.DriveViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var viewModel: DriveViewModel
    private lateinit var signInLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var authLauncher: ActivityResultLauncher<IntentSenderRequest>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[DriveViewModel::class.java]
        lifecycleScope.launch{
            if (viewModel.isSignedIn()) {
                val action = LoginFragmentDirections.actionLoginFragmentToFeatureFragment()
                findNavController().navigate(action)
            }
        }

        signInLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                val data = result.data ?: return@registerForActivityResult
                data.dataString?.let { Log.d("GD Clone", it) }
                viewModel.getSignInResult(data, getSignInClient())
            }

        authLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                val data = result.data ?: return@registerForActivityResult
                "Auth Result".showToast(requireContext())
                try {
                    val authorize = Identity.getAuthorizationClient(requireActivity())
                    authorize.getAuthorizationResultFromIntent(data)
                } catch (e: Exception) {
                    "Unexpected error occurred".showToast(requireContext())
                    Log.e("GD Clone", "Exception", e)
                }
            }

        initializeObservers()
        binding.signInButton.setOnClickListener {
            signIn()
        }
    }

    private fun initializeObservers() {
        viewModel.showToast.observe(viewLifecycleOwner) {
            it.showToast(requireContext())
        }

        viewModel.initiateAuthorization.observe(viewLifecycleOwner) { initializedValue ->
            if (initializedValue == 1) {
                val authorize = Identity.getAuthorizationClient(requireActivity())
                lifecycleScope.launch(Dispatchers.IO) {
                    val authorizationResult = viewModel.authorizeGoogleDrive(authorize)
                    authorizationResult?.pendingIntent?.let {
                        authLauncher.launch(
                            IntentSenderRequest.Builder(it.intentSender)
                                .build()
                        )
                    }
                    withContext(Dispatchers.Main){
                        moveToFeatureScreen()
                    }
                }
            }
        }
    }

    private fun getSignInClient(): SignInClient {
        return Identity.getSignInClient(requireActivity())
    }

    private fun moveToFeatureScreen(){
            val action = LoginFragmentDirections.actionLoginFragmentToFeatureFragment()
            findNavController().navigate(action)
    }

    private fun signIn() {
        lifecycleScope.launch {
            val result = viewModel.signInGoogle(getSignInClient())
            result?.let {
                signInLauncher.launch(IntentSenderRequest.Builder(it).build())
            }
        }
    }
}

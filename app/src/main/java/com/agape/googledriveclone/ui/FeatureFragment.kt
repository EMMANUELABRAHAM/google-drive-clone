package com.agape.googledriveclone.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.agape.googledriveclone.R
import com.agape.googledriveclone.databinding.FragmentFeatureBinding
import com.agape.googledriveclone.viewmodel.DriveViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import kotlinx.coroutines.launch

class FeatureFragment : Fragment() {

    private lateinit var binding: FragmentFeatureBinding
    private lateinit var viewModel: DriveViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeatureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            requireActivity()
        )[DriveViewModel::class.java]

        binding.buttonSignOut.setOnClickListener {
            signOut()
        }

        binding.buttonUploadFile.setOnClickListener {
        }

        binding.buttonListFiles.setOnClickListener {
            val action = FeatureFragmentDirections.actionFeatureFragmentToFileListFragment()
            findNavController().navigate(action)
        }
    }

    private fun getSignInClient(): SignInClient {
        return Identity.getSignInClient(requireActivity())
    }

    private fun signOut() {
        lifecycleScope.launch {
            viewModel.signOut(getSignInClient())
            findNavController().navigate(R.id.action_featureFragment_to_loginFragment)
        }
    }
}

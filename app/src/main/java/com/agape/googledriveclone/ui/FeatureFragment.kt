package com.agape.googledriveclone.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.agape.googledriveclone.R
import com.agape.googledriveclone.databinding.FragmentFeatureBinding
import com.agape.googledriveclone.utils.AppUtils.showToast
import com.agape.googledriveclone.utils.FileUtils
import com.agape.googledriveclone.viewmodel.DriveViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch

class FeatureFragment : Fragment() {

    private lateinit var binding: FragmentFeatureBinding
    private lateinit var viewModel: DriveViewModel
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

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

        initializeObservers()

        binding.buttonSignOut.setOnClickListener {
            signOut()
        }

        binding.buttonUploadFile.setOnClickListener {
            checkPermissionsAndPickFile()
        }

        binding.buttonListFiles.setOnClickListener {
            val action = FeatureFragmentDirections.actionFeatureFragmentToFileListFragment()
            findNavController().navigate(action)
        }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allGranted = permissions.values.all { it }
                if (allGranted) {
                    pickFile()
                } else {
                    "Permission denied".showToast(requireContext())
                }
            }

        filePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        lifecycleScope.launch {
                            val fileDetails = FileUtils.getFileDetails(uri, requireContext())
                            val drive = viewModel.getGoogleDrive(getGoogleCredential())
                            if (fileDetails != null && drive != null) {
                                viewModel.uploadFileToDrive(drive, fileDetails)
                            }
                        }
                    }
                }
            }
    }

    private fun initializeObservers() {
        viewModel.showToast.observe(viewLifecycleOwner) {
            it.showToast(requireContext())
        }
    }

    private fun pickFile() {
        val intent = FileUtils.getPickFileIntent()
        filePickerLauncher.launch(intent)
    }

    private fun checkPermissionsAndPickFile() {
        val permissions = viewModel.getPermissionListForFileUpload()
        permissionLauncher.launch(permissions)
    }


    private fun getGoogleCredential(): GoogleAccountCredential {
        return GoogleAccountCredential.usingOAuth2(requireContext(), listOf(DriveScopes.DRIVE_FILE))
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

package com.agape.googledriveclone.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.agape.googledriveclone.databinding.FragmentFileListBinding
import com.agape.googledriveclone.model.GoogleDriveFile
import com.agape.googledriveclone.ui.adapter.FileListAdapter
import com.agape.googledriveclone.utils.AppUtils.showToast
import com.agape.googledriveclone.viewmodel.DriveViewModel
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileListFragment : Fragment() {

    private lateinit var binding: FragmentFileListBinding
    private lateinit var viewModel: DriveViewModel
    private lateinit var adapter: FileListAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFileListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            requireActivity()
        )[DriveViewModel::class.java]

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            var files = viewModel.getGoogleDrive(getGoogleCredential())?.let { it1 ->
                viewModel.listFiles(it1)
            }
            if (files == null){
                files = emptyList()
            }

            adapter = FileListAdapter(files, this@FileListFragment::openFile, this@FileListFragment::downloadFile)

            binding.recyclerView.adapter = adapter
            Log.d("GD Clone", files.toString())
        }
    }

    private fun openFile(file: GoogleDriveFile) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://drive.google.com/file/d/${file.id}/view")
        }
        startActivity(intent)
    }

    private fun downloadFile(file: GoogleDriveFile) {
        file.id.let { fileId ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val outputStream = requireContext().openFileOutput(file.name, Context.MODE_PRIVATE)
                    getGoogleDrive()?.files()?.get(fileId)?.executeMediaAndDownloadTo(outputStream)
                    withContext(Dispatchers.Main){
                        "File downloaded successfully".showToast(requireContext())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main){
                        "Error downloading file".showToast(requireContext())                }
                }
            }
        }
    }

    private fun getGoogleDrive(): Drive?{
        return viewModel.getGoogleDrive(getGoogleCredential())
    }
    private fun getGoogleCredential(): GoogleAccountCredential {
        return GoogleAccountCredential.usingOAuth2(requireContext(), listOf(DriveScopes.DRIVE_FILE))
    }
}

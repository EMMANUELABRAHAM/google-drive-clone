package com.agape.googledriveclone.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.agape.googledriveclone.databinding.FragmentFileListBinding
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class FileListFragment : Fragment() {

    private lateinit var binding: FragmentFileListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFileListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        driveViewModel.files.observe(viewLifecycleOwner, Observer { files ->
//            binding.textViewFiles.text = files.joinToString("\n") { "${it.name} (${it.id})" }
//        })
//
//        driveViewModel.listFiles()
    }
}

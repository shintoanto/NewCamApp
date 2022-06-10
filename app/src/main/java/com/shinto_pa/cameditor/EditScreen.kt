package com.shinto_pa.cameditor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.shinto_pa.cameditor.databinding.EditScreenFragmentBinding

class EditScreen : Fragment() {

    private lateinit var binding: EditScreenFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EditScreenFragmentBinding.inflate(inflater, container, false)



        return binding.root
    }

}
package com.example.tutoriamovil2.Estudiante

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.tutoriamovil2.Login.Login
import com.example.tutoriamovil2.databinding.FragmentPerfilEstudianteBinding

class PerfilEstudianteFragment : Fragment() {

    private var _binding: FragmentPerfilEstudianteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PerfilEstudianteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPerfilEstudianteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCerrarSesion.setOnClickListener {
            binding.btnCerrarSesion.isEnabled = false
            viewModel.cerrarSesion()
        }

        viewModel.sesionCerrada.observe(viewLifecycleOwner) { cerrada ->
            if (cerrada) volverAlLogin()
        }
    }

    private fun volverAlLogin() {
        val intent = Intent(requireContext(), Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

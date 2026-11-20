package com.example.tutoriamovil2.Tutor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.example.tutoriamovil2.Estudiante.EstadoPerfil
import com.example.tutoriamovil2.Login.Login
import com.example.tutoriamovil2.base.BaseFragment
import com.example.tutoriamovil2.databinding.FragmentTutorPerfilBinding

class TutorPerfilFragment : BaseFragment<FragmentTutorPerfilBinding>(FragmentTutorPerfilBinding::inflate) {

    private val viewModel: TutorPerfilViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCerrarSesionTutor.setOnClickListener {
            binding.btnCerrarSesionTutor.isEnabled = false
            viewModel.cerrarSesion()
        }

        viewModel.sesionCerrada.observe(viewLifecycleOwner) { cerrada ->
            if (cerrada) volverAlLogin()
        }

        viewModel.estadoPerfil.observe(viewLifecycleOwner) { estado ->
            binding.progressTutor.isVisible = estado is EstadoPerfil.Cargando
            when (estado) {
                is EstadoPerfil.Exito -> {
                    binding.tvNombreTutor.text = estado.usuario.name
                    binding.tvCorreoTutor.text = estado.usuario.email
                }
                is EstadoPerfil.Error ->
                    Toast.makeText(requireContext(), estado.mensaje, Toast.LENGTH_SHORT).show()
                is EstadoPerfil.Cargando -> Unit
            }
        }

        viewModel.cargarPerfil()
    }

    private fun volverAlLogin() {
        val intent = Intent(requireContext(), Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}

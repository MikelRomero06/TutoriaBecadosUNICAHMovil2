package com.example.tutoriamovil2.Estudiante

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.tutoriamovil2.Login.Login
import com.example.tutoriamovil2.databinding.FragmentPerfilEstudianteBinding
import com.example.tutoriamovil2.red.dto.UsuarioDto

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

        viewModel.estadoPerfil.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is EstadoPerfil.Cargando -> {
                    binding.progressBarPerfil.isVisible = true
                }
                is EstadoPerfil.Exito -> {
                    binding.progressBarPerfil.isVisible = false
                    mostrarDatosUsuario(estado.usuario)
                }
                is EstadoPerfil.Error -> {
                    binding.progressBarPerfil.isVisible = false
                    Toast.makeText(requireContext(), estado.mensaje, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.cargarPerfil()
    }

    private fun mostrarDatosUsuario(usuario: UsuarioDto) {
        binding.tvNombreEstudiante.text = usuario.name.ifBlank { "Sin nombre" }
        binding.tvEmailEstudiante.text = usuario.email.ifBlank { "Sin correo" }
        binding.tvRolEstudiante.text = (usuario.rol.ifBlank { "Estudiante" }).replaceFirstChar { it.uppercase() }

        binding.tvCarreraEstudiante.text = if (usuario.carreraId != null) {
            "Carrera #${usuario.carreraId}"
        } else {
            "Sin especificar"
        }

        binding.tvCuentaEstudiante.text = usuario.id.toString()
        binding.tvTelefonoEstudiante.text = if (!usuario.telefono.isNullOrBlank()) usuario.telefono else "Sin registrar"
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


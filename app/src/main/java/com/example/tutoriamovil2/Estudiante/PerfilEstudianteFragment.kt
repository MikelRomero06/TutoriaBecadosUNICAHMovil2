package com.example.tutoriamovil2.Estudiante

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.Login.Login
import com.example.tutoriamovil2.databinding.FragmentPerfilEstudianteBinding
import com.example.tutoriamovil2.red.dto.UsuarioDto

class PerfilEstudianteFragment : Fragment() {

    private var _binding: FragmentPerfilEstudianteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PerfilEstudianteViewModel by viewModels()

    private var carreraId: Long? = null
    private var carreraNombre: String? = null

    // Selector de fotos del sistema: no pide permisos de almacenamiento.
    private val elegirImagen = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.subirFoto(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPerfilEstudianteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPostularme.setOnClickListener {
            findNavController().navigate(
                R.id.action_perfilEstudiante_to_postulacion,
                bundleOf(
                    EstudiantePostulacion.ARG_CARRERA_ID to (carreraId ?: EstudiantePostulacion.SIN_CARRERA),
                    EstudiantePostulacion.ARG_CARRERA_NOMBRE to carreraNombre,
                ),
            )
        }

        binding.btnEditarPerfil.setOnClickListener { elegirFoto() }
        binding.ivFotoPerfil.setOnClickListener { elegirFoto() }

        viewModel.estadoFoto.observe(viewLifecycleOwner) { pintarEstadoFoto(it) }

        binding.btnCerrarSesion.setOnClickListener {
            binding.btnCerrarSesion.isEnabled = false
            viewModel.cerrarSesion()
        }

        viewModel.sesionCerrada.observe(viewLifecycleOwner) { cerrada ->
            if (cerrada) volverAlLogin()
        }

        viewModel.fotoPerfil.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                binding.ivFotoPerfil.scaleType = ImageView.ScaleType.CENTER_CROP
                binding.ivFotoPerfil.setImageBitmap(bitmap)
            }
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
        viewModel.cargarSolicitudFoto()
    }

    private fun elegirFoto() {
        if (viewModel.estadoFoto.value is EstadoFoto.Subiendo) return
        elegirImagen.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun pintarEstadoFoto(estado: EstadoFoto) {
        val subiendo = estado is EstadoFoto.Subiendo
        binding.btnEditarPerfil.isEnabled = !subiendo
        binding.btnEditarPerfil.setText(if (subiendo) R.string.perfil_foto_subiendo else R.string.perfil_cambiar_foto)
        binding.tvFotoEstado.isVisible = estado is EstadoFoto.Pendiente
        when (estado) {
            is EstadoFoto.Aplicada -> {
                Toast.makeText(requireContext(), R.string.perfil_foto_aplicada, Toast.LENGTH_SHORT).show()
                viewModel.fotoAvisada()
            }
            is EstadoFoto.Pendiente -> if (estado.recienEnviada) {
                Toast.makeText(requireContext(), R.string.perfil_foto_pendiente_toast, Toast.LENGTH_LONG).show()
                viewModel.fotoAvisada()
            }
            is EstadoFoto.Error -> {
                Toast.makeText(requireContext(), estado.mensaje, Toast.LENGTH_LONG).show()
                viewModel.fotoAvisada()
            }
            else -> Unit
        }
    }

    private fun mostrarDatosUsuario(usuario: UsuarioDto) {
        carreraId = usuario.carreraId
        carreraNombre = usuario.carrera?.nombre
        // Quien ya es tutor aprobado no necesita postularse otra vez.
        binding.btnPostularme.isVisible = !usuario.esTutorAprobado
        binding.tvNombreEstudiante.text = usuario.name.ifBlank { "Sin nombre" }
        binding.tvEmailEstudiante.text = usuario.email.ifBlank { "Sin correo" }
        binding.tvRolEstudiante.text = (usuario.rol.ifBlank { "Estudiante" }).replaceFirstChar { it.uppercase() }

        binding.tvCarreraEstudiante.text = usuario.carrera?.nombre
            ?: if (usuario.carreraId != null) "Carrera #${usuario.carreraId}" else "Sin especificar"

        binding.tvCuentaEstudiante.text = usuario.id.toString()
        binding.tvTelefonoEstudiante.text = if (!usuario.telefono.isNullOrBlank()) usuario.telefono else "Sin registrar"

        viewModel.cargarFoto(usuario.fotoUrl)
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


package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentEstudiantePerfilTutorBinding
import com.example.tutoriamovil2.red.CargadorFotos
import com.example.tutoriamovil2.red.dto.TutorDto
import com.google.android.material.chip.Chip

/** Perfil público de un tutor. Recibe el id en el argumento [ARG_TUTOR_ID]. */
class EstudiantePerfilTutor : Fragment() {

    private var _binding: FragmentEstudiantePerfilTutorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PerfilTutorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEstudiantePerfilTutorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        val tutorId = arguments?.getLong(ARG_TUTOR_ID, -1L) ?: -1L
        if (tutorId <= 0) {
            Toast.makeText(requireContext(), "No se encontró el tutor.", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // La agenda por bloques todavía no está publicada en el servidor: se muestra el aviso vacío.
        mostrarAgendaVacia()

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            binding.progress.isVisible = estado is EstadoPerfilTutor.Cargando
            when (estado) {
                is EstadoPerfilTutor.Cargando -> Unit
                is EstadoPerfilTutor.Exito -> mostrar(estado.tutor)
                is EstadoPerfilTutor.Error -> {
                    binding.tvNombre.text = "Tutor no disponible"
                    binding.tvCarreraTel.text = estado.mensaje
                }
            }
        }

        viewModel.cargar(tutorId)
    }

    private fun mostrar(tutor: TutorDto) {
        binding.tvNombre.text = tutor.name?.ifBlank { null } ?: "Tutor"
        binding.tvCarreraTel.text = tutor.carrera?.nombre.orEmpty()

        val esNuevo = tutor.nuevo == true || (tutor.totalCalificaciones ?: 0) < 3
        if (esNuevo) {
            binding.tvRating.text = "Nuevo"
            binding.tvCalificaciones.text = "menos de 3 calificaciones"
        } else {
            binding.tvRating.text = String.format(java.util.Locale.US, "%.1f", tutor.promedio ?: 0.0)
            val total = tutor.totalCalificaciones ?: 0
            binding.tvCalificaciones.text = if (total == 1) "1 calificación" else "$total calificaciones"
        }

        binding.chipsMaterias.removeAllViews()
        tutor.materias.orEmpty().forEach { materia ->
            val chip = Chip(requireContext()).apply {
                text = materia.nombre
                isCheckable = false
                isClickable = false
            }
            binding.chipsMaterias.addView(chip)
        }

        binding.tvBio.text = tutor.bio.orEmpty()
        binding.tvBio.isVisible = !tutor.bio.isNullOrBlank()
        val cumplidas = tutor.totalCumplidas ?: 0
        binding.tvCumplidas.isVisible = cumplidas > 0
        binding.tvCumplidas.text = if (cumplidas == 1) "1 tutoría dada" else "$cumplidas tutorías dadas"

        CargadorFotos.mostrar(viewLifecycleOwner.lifecycleScope, binding.ivFoto, tutor.fotoUrl, ladoMaximo = 900)
    }

    private fun mostrarAgendaVacia() {
        binding.rvFechas.isVisible = false
        binding.rvBloques.isVisible = false
        binding.leyendaBloques.isVisible = false
        binding.tvTituloFecha.isVisible = false
        binding.tvTituloBloques.text = "Horarios"
        binding.tvSinBloques.isVisible = true
        binding.tvSinBloques.text = "Los horarios de este tutor todavía no están disponibles para reservar."
        binding.cardLugar.isVisible = false
        binding.btnSolicitar.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_TUTOR_ID = "tutor_id"
    }
}

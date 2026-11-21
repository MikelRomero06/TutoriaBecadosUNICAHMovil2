package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
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

    private var tutor: TutorDto? = null
    private var bloquesDelDia: List<BloqueUi> = emptyList()

    private val diaAdapter = DiaAdapter { viewModel.elegirFecha(it) }
    private val bloqueAdapter = BloqueAdapter { viewModel.elegirBloque(it) }

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

        binding.rvFechas.adapter = diaAdapter
        binding.rvBloques.adapter = bloqueAdapter
        binding.btnSolicitar.setOnClickListener { irASolicitar(tutorId) }
        actualizarBoton(null)

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

        viewModel.agenda.observe(viewLifecycleOwner) { pintarAgenda() }
        viewModel.fecha.observe(viewLifecycleOwner) { pintarAgenda() }
        viewModel.bloque.observe(viewLifecycleOwner) { bloque ->
            bloqueAdapter.actualizar(bloquesDelDia, bloque?.clave)
            actualizarBoton(bloque)
        }

        viewModel.cargar(tutorId)
        viewModel.cargarAgenda(tutorId)
    }

    private fun pintarAgenda() {
        when (val agenda = viewModel.agenda.value) {
            null, EstadoAgenda.Cargando -> Unit
            is EstadoAgenda.Error -> mostrarSinBloques(agenda.mensaje)
            is EstadoAgenda.Exito -> {
                if (agenda.bloques.isEmpty()) {
                    mostrarSinBloques("Este tutor todavía no tiene horarios para reservar.")
                    return
                }
                val fecha = viewModel.fecha.value
                val fechas = agenda.bloques.map { it.fecha }.distinct()
                bloquesDelDia = agenda.bloques.filter { it.fecha == fecha }

                binding.tvTituloFecha.isVisible = true
                binding.rvFechas.isVisible = true
                binding.tvTituloBloques.text = "Bloques del día"
                binding.leyendaBloques.isVisible = true
                binding.rvBloques.isVisible = true
                binding.tvSinBloques.isVisible = false

                diaAdapter.actualizar(fechas, fecha)
                bloqueAdapter.actualizar(bloquesDelDia, viewModel.bloque.value?.clave)
            }
        }
    }

    private fun mostrarSinBloques(mensaje: String) {
        binding.rvFechas.isVisible = false
        binding.rvBloques.isVisible = false
        binding.leyendaBloques.isVisible = false
        binding.tvTituloFecha.isVisible = false
        binding.tvTituloBloques.text = "Horarios"
        binding.tvSinBloques.isVisible = true
        binding.tvSinBloques.text = mensaje
        binding.cardLugar.isVisible = false
        actualizarBoton(null)
    }

    /** "Solicitar" solo está activo con un bloque libre elegido. */
    private fun actualizarBoton(bloque: BloqueUi?) {
        binding.btnSolicitar.isEnabled = bloque != null
        binding.btnSolicitar.text = if (bloque == null) "Elegí un bloque para solicitar" else "Solicitar tutoría"
        binding.cardLugar.isVisible = bloque != null
        if (bloque != null) {
            binding.tvLugar.text = "${modalidadTexto(bloque.modalidad)} · ${bloque.lugarOEnlace.orEmpty()}".trimEnd(' ', '·')
            binding.tvFechaHora.text = "${fechaLarga(bloque.fecha)} · ${rangoHoras(bloque.horaInicio, bloque.horaFin)}"
        }
    }

    private fun irASolicitar(tutorId: Long) {
        val bloque = viewModel.bloque.value ?: return
        findNavController().navigate(
            R.id.action_perfilTutor_to_solicitarTutoria,
            bundleOf(
                EstudianteSolicitarTutoria.ARG_TUTOR_ID to tutorId,
                EstudianteSolicitarTutoria.ARG_TUTOR_NOMBRE to (tutor?.name ?: "Tutor"),
                EstudianteSolicitarTutoria.ARG_TUTOR_FOTO to tutor?.fotoUrl,
                EstudianteSolicitarTutoria.ARG_FECHA to bloque.fecha,
                EstudianteSolicitarTutoria.ARG_HORA_INICIO to bloque.horaInicio,
                EstudianteSolicitarTutoria.ARG_HORA_FIN to bloque.horaFin,
                EstudianteSolicitarTutoria.ARG_MODALIDAD to bloque.modalidad,
                EstudianteSolicitarTutoria.ARG_LUGAR to bloque.lugarOEnlace,
            ),
        )
    }

    private fun mostrar(tutor: TutorDto) {
        this.tutor = tutor
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_TUTOR_ID = "tutor_id"
    }
}

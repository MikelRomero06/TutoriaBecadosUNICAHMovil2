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

    private var tutorId: Long = -1L
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

        tutorId = arguments?.getLong(ARG_TUTOR_ID, -1L) ?: -1L
        if (tutorId <= 0) {
            Toast.makeText(requireContext(), R.string.perfil_tutor_id_invalido, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        binding.rvFechas.adapter = diaAdapter
        binding.rvBloques.adapter = bloqueAdapter
        binding.btnSolicitar.setOnClickListener { irASolicitar() }
        actualizarBoton(null)

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is EstadoPerfilTutor.Cargando -> Unit
                is EstadoPerfilTutor.Exito -> mostrar(estado.tutor)
                is EstadoPerfilTutor.Error -> {
                    binding.tvNombre.setText(R.string.perfil_tutor_no_disponible)
                    binding.tvCarreraTel.text = estado.mensaje
                }
            }
            actualizarCarga()
        }

        viewModel.agenda.observe(viewLifecycleOwner) {
            pintarAgenda()
            actualizarCarga()
        }
        viewModel.fecha.observe(viewLifecycleOwner) { pintarAgenda() }
        viewModel.bloque.observe(viewLifecycleOwner) { bloque ->
            bloqueAdapter.actualizar(bloquesDelDia, bloque?.clave)
            actualizarBoton(bloque)
        }

        viewModel.cargar(tutorId)
        viewModel.cargarAgenda(tutorId)
    }

    /** El indicador de carga cubre tanto el perfil como la agenda. */
    private fun actualizarCarga() {
        binding.progress.isVisible = viewModel.estado.value is EstadoPerfilTutor.Cargando ||
            viewModel.agenda.value is EstadoAgenda.Cargando
    }

    /** Agenda: cargando · error (tocar para reintentar) · sin horarios · lista de fechas y bloques. */
    private fun pintarAgenda() {
        when (val agenda = viewModel.agenda.value) {
            null -> Unit
            EstadoAgenda.Cargando -> mostrarMensajeAgenda(getString(R.string.perfil_cargando_agenda))
            is EstadoAgenda.Error -> mostrarMensajeAgenda(
                getString(R.string.perfil_error_agenda, agenda.mensaje),
                alTocar = { viewModel.cargarAgenda(tutorId) },
            )
            is EstadoAgenda.Exito -> {
                if (agenda.bloques.isEmpty()) {
                    mostrarMensajeAgenda(getString(R.string.perfil_sin_horarios))
                    return
                }
                val fecha = viewModel.fecha.value
                bloquesDelDia = agenda.bloques.filter { it.fecha == fecha }

                binding.tvTituloFecha.isVisible = true
                binding.rvFechas.isVisible = true
                binding.tvTituloBloques.setText(R.string.perfil_bloques_dia)
                binding.leyendaBloques.isVisible = true
                binding.rvBloques.isVisible = bloquesDelDia.isNotEmpty()
                binding.tvSinBloques.isVisible = bloquesDelDia.isEmpty()
                binding.tvSinBloques.setText(R.string.perfil_sin_bloques_dia)
                binding.tvSinBloques.setOnClickListener(null)
                binding.tvSinBloques.isClickable = false

                diaAdapter.actualizar(agenda.bloques.map { it.fecha }.distinct(), fecha)
                bloqueAdapter.actualizar(bloquesDelDia, viewModel.bloque.value?.clave)
            }
        }
    }

    private fun mostrarMensajeAgenda(mensaje: String, alTocar: (() -> Unit)? = null) {
        binding.rvFechas.isVisible = false
        binding.rvBloques.isVisible = false
        binding.leyendaBloques.isVisible = false
        binding.tvTituloFecha.isVisible = false
        binding.tvTituloBloques.setText(R.string.perfil_horarios)
        binding.tvSinBloques.isVisible = true
        binding.tvSinBloques.text = mensaje
        binding.tvSinBloques.setOnClickListener(alTocar?.let { accion -> View.OnClickListener { accion() } })
        binding.tvSinBloques.isClickable = alTocar != null
        binding.cardLugar.isVisible = false
        actualizarBoton(null)
    }

    /** "Solicitar" solo está activo con un bloque libre elegido. */
    private fun actualizarBoton(bloque: BloqueUi?) {
        binding.btnSolicitar.isEnabled = bloque != null
        binding.btnSolicitar.setText(if (bloque == null) R.string.btn_solicitar_elegir else R.string.btn_solicitar)
        binding.cardLugar.isVisible = bloque != null
        if (bloque != null) {
            binding.tvLugar.text = modalidadYLugar(requireContext(), bloque.modalidad, bloque.lugarOEnlace)
            binding.tvFechaHora.text = fechaYHora(requireContext(), bloque.fecha, bloque.horaInicio, bloque.horaFin)
        }
    }

    private fun irASolicitar() {
        val bloque = viewModel.bloque.value ?: return
        findNavController().navigate(
            R.id.action_perfilTutor_to_solicitarTutoria,
            bundleOf(
                EstudianteSolicitarTutoria.ARG_TUTOR_ID to tutorId,
                EstudianteSolicitarTutoria.ARG_TUTOR_NOMBRE to (tutor?.name ?: getString(R.string.perfil_tutor_default)),
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
        binding.tvNombre.text = tutor.name?.ifBlank { null } ?: getString(R.string.perfil_tutor_default)
        binding.tvCarreraTel.text = tutor.carrera?.nombre.orEmpty()

        val total = tutor.totalCalificaciones ?: 0
        val esNuevo = tutor.nuevo == true || total < 3
        if (esNuevo) {
            binding.tvRating.setText(R.string.perfil_nuevo)
            binding.tvCalificaciones.setText(R.string.perfil_menos_3_calificaciones)
        } else {
            binding.tvRating.text = String.format(java.util.Locale.US, "%.1f", tutor.promedio ?: 0.0)
            binding.tvCalificaciones.text = resources.getQuantityString(R.plurals.perfil_calificaciones, total, total)
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
        binding.tvCumplidas.text = resources.getQuantityString(R.plurals.perfil_tutorias_dadas, cumplidas, cumplidas)

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

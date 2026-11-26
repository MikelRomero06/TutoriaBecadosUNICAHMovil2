package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.base.entrarConRebote
import com.example.tutoriamovil2.databinding.FragmentEstudianteTutoriaSolicitadaBinding
import com.example.tutoriamovil2.red.CargadorFotos
import com.example.tutoriamovil2.red.dto.TutoriaDto
import com.google.gson.Gson

/** Detalle de una tutoría del estudiante (recibida como JSON en `ARG_TUTORIA`), con opción de cancelar o reportar. */
class EstudianteTutoriaSolicitada : Fragment() {

    private var _binding: FragmentEstudianteTutoriaSolicitadaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEstudianteTutoriaSolicitadaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) binding.iconoExito.entrarConRebote()
        val tutoria = Gson().fromJson(requireArguments().getString(ARG_TUTORIA), TutoriaDto::class.java)

        binding.tvTitulo.text = getString(R.string.detalle_titulo)
        binding.tvSubtitulo.text = estiloEstado(requireContext(), tutoria.estado).texto
        binding.tvTutor.text = tutoria.tutor?.name?.ifBlank { null } ?: getString(R.string.detalle_tutor)
        binding.tvMateria.text = listOfNotNull(tutoria.materia?.nombre, tutoria.tema?.ifBlank { null })
            .joinToString(" · ")
        binding.tvFecha.text = tutoria.fecha?.let { fechaLarga(it) }.orEmpty()
        binding.tvHoraModalidad.text = listOf(
            horario(tutoria.horaInicio, tutoria.horaFin),
            textoModalidad(requireContext(), tutoria.modalidad),
            tutoria.lugarOEnlace.orEmpty(),
        ).filter { it.isNotBlank() }.joinToString(" · ")
        CargadorFotos.mostrar(viewLifecycleOwner.lifecycleScope, binding.ivFoto, tutoria.tutor?.fotoUrl)

        // El layout viene del flujo de reserva: aquí el botón principal es "Cancelar tutoría" antes de
        // que empiece, y "Reportar inasistencia" una vez empezada (dentro del plazo).
        val nav = findNavController()
        when {
            puedeCancelar(tutoria.estado) && !yaEmpezo(tutoria) -> {
                binding.btnVerTutorias.text = getString(R.string.cancelar_titulo)
                binding.btnVerTutorias.setOnClickListener { nav.irACancelar(tutoria.id) }
            }
            puedeReportar(tutoria) -> {
                binding.btnVerTutorias.text = getString(R.string.agenda_reportar)
                binding.btnVerTutorias.setOnClickListener { nav.irAReportar(tutoria.id, esTutor = false) }
            }
            else -> binding.btnVerTutorias.isVisible = false
        }

        binding.btnVolver.text = getString(R.string.detalle_volver)
        binding.btnVolver.setOnClickListener { findNavController().popBackStack() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

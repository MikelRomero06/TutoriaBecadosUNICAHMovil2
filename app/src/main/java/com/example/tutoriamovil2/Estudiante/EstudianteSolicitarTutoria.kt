package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentEstudianteSolicitarTutoriaBinding
import com.example.tutoriamovil2.red.dto.MateriaDto
import com.example.tutoriamovil2.red.dto.SolicitudPeticion

/** Formulario de solicitud: materia, tema y descripción del bloque elegido en el perfil. */
class EstudianteSolicitarTutoria : Fragment() {

    private var _binding: FragmentEstudianteSolicitarTutoriaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SolicitarTutoriaViewModel by viewModels()

    private var materias: List<MateriaDto> = emptyList()
    private var materiaElegida: MateriaDto? = null

    private val tutorId get() = requireArguments().getLong(ARG_TUTOR_ID)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEstudianteSolicitarTutoriaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvTutor.text = args.getString(ARG_TUTOR_NOMBRE)
        binding.tvFechaHora.text = "${fechaLarga(args.getString(ARG_FECHA)!!)} · " +
            rangoHoras(args.getInt(ARG_HORA_INICIO), args.getInt(ARG_HORA_FIN))
        binding.tvLugar.text = "${modalidadTexto(args.getString(ARG_MODALIDAD))} · ${args.getString(ARG_LUGAR).orEmpty()}"
            .trimEnd(' ', '·')

        binding.acMateria.setOnItemClickListener { _, _, pos, _ ->
            materiaElegida = materias.getOrNull(pos)
            binding.tilMateria.error = null
        }
        binding.etTema.doAfterChange { binding.tilTema.error = null }
        binding.etDescripcion.doAfterChange { binding.tilDescripcion.error = null }
        binding.btnConfirmar.setOnClickListener { enviar() }

        viewModel.materias.observe(viewLifecycleOwner) { lista ->
            materias = lista
            binding.acMateria.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, lista.map { it.nombre.orEmpty() }),
            )
        }
        viewModel.estado.observe(viewLifecycleOwner) { manejar(it) }
        viewModel.cargarMaterias(tutorId)
    }

    private fun enviar() {
        val tema = binding.etTema.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim()
        binding.tvError.isVisible = false

        val errMateria = if (materiaElegida == null) "Elegí una materia de la lista." else null
        val errTema = when {
            tema.isEmpty() -> "El tema es obligatorio."
            tema.length < 5 -> "Escribí al menos 5 caracteres."
            tema.length > 100 -> "Máximo 100 caracteres."
            else -> null
        }
        val errDescripcion = when {
            descripcion.isEmpty() -> "Contale al tutor qué necesitás repasar."
            descripcion.length < 20 -> "Escribí al menos 20 caracteres (${descripcion.length}/20)."
            descripcion.length > 500 -> "Máximo 500 caracteres."
            else -> null
        }
        binding.tilMateria.error = errMateria
        binding.tilTema.error = errTema
        binding.tilDescripcion.error = errDescripcion
        if (errMateria != null || errTema != null || errDescripcion != null) return

        viewModel.enviar(SolicitudPeticion(
            tutorId = tutorId,
            fecha = requireArguments().getString(ARG_FECHA)!!,
            horaInicio = requireArguments().getInt(ARG_HORA_INICIO),
            materiaId = materiaElegida!!.id,
            tema = tema,
            descripcion = descripcion,
        ))
    }

    private fun manejar(estado: EstadoSolicitud) {
        binding.progress.isVisible = estado is EstadoSolicitud.Enviando
        binding.btnConfirmar.isEnabled = estado !is EstadoSolicitud.Enviando
        when (estado) {
            EstadoSolicitud.Inactivo, EstadoSolicitud.Enviando -> Unit
            is EstadoSolicitud.Error -> {
                binding.tilTema.error = estado.errores["tema"]?.firstOrNull()
                binding.tilDescripcion.error = estado.errores["descripcion"]?.firstOrNull()
                binding.tilMateria.error = estado.errores["materia_id"]?.firstOrNull()
                val general = (estado.errores["fecha"] ?: estado.errores["hora_inicio"])?.firstOrNull() ?: estado.mensaje
                binding.tvError.text = general
                binding.tvError.isVisible = true
            }
            is EstadoSolicitud.Exito -> {
                val a = requireArguments()
                viewModel.limpiarEstado()
                findNavController().navigate(
                    R.id.action_solicitarTutoria_to_tutoriaConfirmacion,
                    bundleOf(
                        TutoriaConfirmacion.ARG_TUTOR_NOMBRE to a.getString(ARG_TUTOR_NOMBRE),
                        TutoriaConfirmacion.ARG_TUTOR_FOTO to a.getString(ARG_TUTOR_FOTO),
                        TutoriaConfirmacion.ARG_MATERIA to materiaElegida?.nombre,
                        TutoriaConfirmacion.ARG_FECHA to a.getString(ARG_FECHA),
                        TutoriaConfirmacion.ARG_HORA_INICIO to a.getInt(ARG_HORA_INICIO),
                        TutoriaConfirmacion.ARG_HORA_FIN to a.getInt(ARG_HORA_FIN),
                        TutoriaConfirmacion.ARG_MODALIDAD to a.getString(ARG_MODALIDAD),
                    ),
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_TUTOR_ID = "tutor_id"
        const val ARG_TUTOR_NOMBRE = "tutor_nombre"
        const val ARG_TUTOR_FOTO = "tutor_foto"
        const val ARG_FECHA = "fecha"
        const val ARG_HORA_INICIO = "hora_inicio"
        const val ARG_HORA_FIN = "hora_fin"
        const val ARG_MODALIDAD = "modalidad"
        const val ARG_LUGAR = "lugar"
    }
}

private fun android.widget.EditText.doAfterChange(block: () -> Unit) =
    addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        override fun afterTextChanged(s: android.text.Editable?) = block()
    })

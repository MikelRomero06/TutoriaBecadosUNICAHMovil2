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
        binding.tvFechaHora.text = fechaYHora(
            requireContext(), args.getString(ARG_FECHA)!!, args.getInt(ARG_HORA_INICIO), args.getInt(ARG_HORA_FIN),
        )
        binding.tvLugar.text = modalidadYLugar(requireContext(), args.getString(ARG_MODALIDAD), args.getString(ARG_LUGAR))

        // Al volver de otra pestaña o rotar, el campo recupera su texto solo; la materia se recupera por su id.
        val materiaGuardada = savedInstanceState?.takeIf { it.containsKey(ESTADO_MATERIA_ID) }?.getLong(ESTADO_MATERIA_ID)

        // Se busca por nombre porque la lista del desplegable puede venir filtrada por el texto del campo.
        binding.acMateria.setOnItemClickListener { padre, _, pos, _ ->
            val nombre = padre.getItemAtPosition(pos) as? String
            materiaElegida = materias.firstOrNull { it.nombre.orEmpty() == nombre }
            binding.tilMateria.error = null
        }
        binding.etTema.doAfterChange { binding.tilTema.error = null }
        binding.etDescripcion.doAfterChange { binding.tilDescripcion.error = null }
        binding.btnConfirmar.setOnClickListener { enviar() }

        viewModel.materias.observe(viewLifecycleOwner) { lista ->
            materias = lista
            if (materiaElegida == null) materiaElegida = lista.firstOrNull { it.id == materiaGuardada }
            binding.acMateria.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, lista.map { it.nombre.orEmpty() }),
            )
            actualizarFormulario()
        }
        viewModel.cargandoMaterias.observe(viewLifecycleOwner) { actualizarFormulario() }
        viewModel.errorMaterias.observe(viewLifecycleOwner) { actualizarFormulario() }
        viewModel.estado.observe(viewLifecycleOwner) { manejar(it) }
        viewModel.cargarMaterias(tutorId)
    }

    /**
     * Estados de la carga de materias:
     * cargando (campo deshabilitado) · error (mensaje + reintento al confirmar) · vacío (tutor sin materias) · listo.
     */
    private fun actualizarFormulario() {
        val cargando = viewModel.cargandoMaterias.value == true
        val error = viewModel.errorMaterias.value
        val sinMaterias = !cargando && error == null && materias.isEmpty()
        val enviando = viewModel.estado.value is EstadoSolicitud.Enviando

        binding.acMateria.isEnabled = !cargando && materias.isNotEmpty()
        binding.progress.isVisible = cargando || enviando
        binding.btnConfirmar.isEnabled = !cargando && !enviando && !sinMaterias

        when {
            cargando -> mostrarAviso(getString(R.string.solicitar_materias_cargando))
            error != null -> mostrarAviso(getString(R.string.solicitar_materias_error, error))
            sinMaterias -> mostrarAviso(getString(R.string.solicitar_sin_materias))
            viewModel.estado.value !is EstadoSolicitud.Error -> binding.tvError.isVisible = false
        }
    }

    private fun mostrarAviso(texto: String) {
        binding.tvError.text = texto
        binding.tvError.isVisible = true
    }

    private fun enviar() {
        // Si las materias fallaron al cargar, "Confirmar" reintenta en vez de validar contra una lista vacía.
        if (materias.isEmpty()) {
            viewModel.cargarMaterias(tutorId, forzar = true)
            return
        }

        val tema = binding.etTema.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim()
        binding.tvError.isVisible = false

        val errMateria = if (materiaElegida == null) getString(R.string.err_materia) else null
        val errTema = when {
            tema.isEmpty() -> getString(R.string.err_tema_vacio)
            tema.length < TEMA_MIN -> getString(R.string.err_tema_corto, TEMA_MIN)
            tema.length > TEMA_MAX -> getString(R.string.err_tema_largo, TEMA_MAX)
            else -> null
        }
        val errDescripcion = when {
            descripcion.isEmpty() -> getString(R.string.err_desc_vacia)
            descripcion.length < DESC_MIN -> getString(R.string.err_desc_corta, DESC_MIN, descripcion.length)
            descripcion.length > DESC_MAX -> getString(R.string.err_desc_larga, DESC_MAX)
            else -> null
        }
        binding.tilMateria.error = errMateria
        binding.tilTema.error = errTema
        binding.tilDescripcion.error = errDescripcion
        if (errMateria != null || errTema != null || errDescripcion != null) return

        viewModel.enviar(
            SolicitudPeticion(
                tutorId = tutorId,
                fecha = requireArguments().getString(ARG_FECHA)!!,
                horaInicio = requireArguments().getInt(ARG_HORA_INICIO),
                materiaId = materiaElegida!!.id,
                tema = tema,
                descripcion = descripcion,
            ),
        )
    }

    private fun manejar(estado: EstadoSolicitud) {
        actualizarFormulario()
        when (estado) {
            EstadoSolicitud.Inactivo, EstadoSolicitud.Enviando -> Unit
            is EstadoSolicitud.Error -> {
                binding.tilTema.error = estado.errores["tema"]?.firstOrNull()
                binding.tilDescripcion.error = estado.errores["descripcion"]?.firstOrNull()
                binding.tilMateria.error = estado.errores["materia_id"]?.firstOrNull()
                mostrarAviso((estado.errores["fecha"] ?: estado.errores["hora_inicio"])?.firstOrNull() ?: estado.mensaje)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        materiaElegida?.let { outState.putLong(ESTADO_MATERIA_ID, it.id) }
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

        private const val ESTADO_MATERIA_ID = "materia_elegida_id"
        private const val TEMA_MIN = 5
        private const val TEMA_MAX = 100
        private const val DESC_MIN = 20
        private const val DESC_MAX = 500
    }
}

private fun android.widget.EditText.doAfterChange(block: () -> Unit) =
    addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
        override fun afterTextChanged(s: android.text.Editable?) = block()
    })

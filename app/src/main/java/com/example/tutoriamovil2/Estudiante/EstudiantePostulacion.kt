package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentEstudiantePostulacionBinding
import com.example.tutoriamovil2.red.dto.MateriaDto
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable

/**
 * Postulación a tutor, abierta desde el perfil del estudiante. Pide las materias de su carrera
 * (al menos una) y una presentación opcional; administración revisa y aprueba o rechaza.
 */
class EstudiantePostulacion : Fragment() {

    private var _binding: FragmentEstudiantePostulacionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostulacionViewModel by viewModels()

    private val carreraId get() = requireArguments().getLong(ARG_CARRERA_ID, SIN_CARRERA)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEstudiantePostulacionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnEnviar.setOnClickListener { enviar() }
        binding.etBio.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.tilBio.error = null
            }
        })

        viewModel.materias.observe(viewLifecycleOwner) { pintarMaterias(it) }
        viewModel.cargandoMaterias.observe(viewLifecycleOwner) { actualizar() }
        viewModel.errorMaterias.observe(viewLifecycleOwner) { actualizar() }
        viewModel.estado.observe(viewLifecycleOwner) { manejar(it) }

        if (carreraId != SIN_CARRERA) viewModel.cargarMaterias(carreraId)
        actualizar()
    }

    /** Un chip por materia de la carrera; se pueden elegir varias. */
    private fun pintarMaterias(materias: List<MateriaDto>) {
        // No se repinta si el usuario ya marcó algo (rotación): el ViewModel solo vuelve a emitir al recargar.
        binding.chipsMaterias.removeAllViews()
        materias.forEach { materia ->
            val chip = Chip(requireContext()).apply {
                setChipDrawable(ChipDrawable.createFromAttributes(context, null, 0, R.style.Widget_TutorU_Chip))
                setTextColor(ContextCompat.getColorStateList(context, R.color.sel_chip_text))
                text = materia.nombre.orEmpty()
                isCheckable = true
                tag = materia.id
                setOnCheckedChangeListener { _, _ -> binding.tvError.isVisible = false }
            }
            binding.chipsMaterias.addView(chip)
        }
        actualizar()
    }

    /**
     * Estados de la carga de materias:
     * sin carrera · cargando · error (tocar para reintentar) · sin materias · lista lista para elegir.
     */
    private fun actualizar() {
        val cargando = viewModel.cargandoMaterias.value == true
        val error = viewModel.errorMaterias.value
        val sinCarrera = carreraId == SIN_CARRERA
        val hayMaterias = !viewModel.materias.value.isNullOrEmpty()
        val enviando = viewModel.estado.value is EstadoPostulacion.Enviando

        binding.progress.isVisible = cargando || enviando
        binding.btnEnviar.isEnabled = hayMaterias && !cargando && !enviando

        val aviso: String? = when {
            sinCarrera -> getString(R.string.postulacion_sin_carrera)
            cargando -> getString(R.string.postulacion_cargando_materias)
            error != null -> getString(R.string.postulacion_error_materias, error)
            !hayMaterias -> getString(R.string.postulacion_sin_materias)
            else -> null
        }
        binding.tvAvisoMaterias.isVisible = aviso != null
        binding.tvAvisoMaterias.text = aviso
        val reintentable = !sinCarrera && !cargando && error != null
        binding.tvAvisoMaterias.isClickable = reintentable
        binding.tvAvisoMaterias.setOnClickListener(
            if (reintentable) View.OnClickListener { viewModel.cargarMaterias(carreraId, forzar = true) } else null,
        )
    }

    private fun materiasElegidas(): List<Long> = (0 until binding.chipsMaterias.childCount)
        .mapNotNull { binding.chipsMaterias.getChildAt(it) as? Chip }
        .filter { it.isChecked }
        .mapNotNull { it.tag as? Long }

    private fun enviar() {
        val elegidas = materiasElegidas()
        val bio = binding.etBio.text?.toString().orEmpty().trim()
        binding.tvError.isVisible = false
        binding.tilBio.error = null

        if (elegidas.isEmpty()) {
            mostrarError(getString(R.string.postulacion_err_materias))
            return
        }
        if (bio.length > BIO_MAX) {
            binding.tilBio.error = getString(R.string.postulacion_err_bio_larga, BIO_MAX)
            return
        }
        viewModel.enviar(elegidas, bio.ifEmpty { null })
    }

    private fun manejar(estado: EstadoPostulacion) {
        actualizar()
        when (estado) {
            EstadoPostulacion.Inactivo, EstadoPostulacion.Enviando -> Unit
            EstadoPostulacion.Exito -> {
                viewModel.consumir()
                Toast.makeText(requireContext(), R.string.postulacion_exito, Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
            is EstadoPostulacion.Error -> {
                viewModel.consumir()
                binding.tilBio.error = estado.errores["bio"]?.firstOrNull()
                mostrarError(estado.errores["materia_ids"]?.firstOrNull() ?: estado.mensaje)
            }
        }
    }

    private fun mostrarError(texto: String) {
        binding.tvError.text = texto
        binding.tvError.isVisible = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_CARRERA_ID = "carrera_id"
        const val SIN_CARRERA = -1L
        private const val BIO_MAX = 300
    }
}

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
import com.example.tutoriamovil2.databinding.ItemMateriaCheckBinding
import com.example.tutoriamovil2.red.dto.MateriaDto

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
        binding.tvTituloMaterias.text = requireArguments().getString(ARG_CARRERA_NOMBRE)
            ?.let { getString(R.string.postulacion_carrera, it) }
            ?: getString(R.string.postulacion_carrera_generica)
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

    /** Una tarjeta por materia de la carrera; se pueden elegir varias. */
    private fun pintarMaterias(materias: List<MateriaDto>) {
        binding.listaMaterias.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        materias.forEach { materia ->
            val item = ItemMateriaCheckBinding.inflate(inflater, binding.listaMaterias, false)
            item.tvNombre.text = materia.nombre.orEmpty()
            marcar(item, viewModel.estaElegida(materia.id))
            item.root.setOnClickListener {
                marcar(item, viewModel.alternar(materia.id))
                binding.tvError.isVisible = false
                actualizarContador()
            }
            binding.listaMaterias.addView(item.root)
        }
        actualizarContador()
        actualizar()
    }

    private fun marcar(item: ItemMateriaCheckBinding, elegida: Boolean) {
        val ctx = requireContext()
        item.root.isChecked = elegida
        item.check.isChecked = elegida
        item.root.strokeColor = ContextCompat.getColor(ctx, if (elegida) R.color.primary else R.color.line)
        item.root.strokeWidth = resources.getDimensionPixelSize(if (elegida) R.dimen.borde_elegido else R.dimen.borde_normal)
        item.root.setCardBackgroundColor(ContextCompat.getColor(ctx, if (elegida) R.color.primary_container else R.color.white))
        item.tvNombre.paint.isFakeBoldText = elegida
        item.tvNombre.invalidate()
    }

    private fun actualizarContador() {
        val total = viewModel.elegidas().size
        val hayMaterias = !viewModel.materias.value.isNullOrEmpty()
        binding.tvContador.isVisible = hayMaterias
        binding.tvContador.text = if (total == 0) getString(R.string.postulacion_ninguna_elegida)
        else resources.getQuantityString(R.plurals.postulacion_elegidas, total, total)
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

    private fun enviar() {
        val elegidas = viewModel.elegidas()
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
                // Laravel nombra los errores "materias" o "materias.0", "materias.1"...
                val errorMaterias = estado.errores.entries
                    .firstOrNull { it.key == "materias" || it.key.startsWith("materias.") }
                    ?.value?.firstOrNull()
                mostrarError(errorMaterias ?: estado.mensaje)
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
        const val ARG_CARRERA_NOMBRE = "carrera_nombre"
        const val SIN_CARRERA = -1L
        private const val BIO_MAX = 300
    }
}

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
import com.example.tutoriamovil2.databinding.FragmentCalificarTutoriaBinding
import com.example.tutoriamovil2.red.CargadorFotos
import com.example.tutoriamovil2.red.dto.TutoriaDto
import com.google.gson.Gson

/**
 * El estudiante evalúa una tutoría realizada: estrellas (públicas) y comentario opcional (solo Bienestar).
 * Recibe la tutoría como JSON en [ARG_TUTORIA], igual que el detalle (`NavController.irACalificar`).
 */
class CalificarTutoria : Fragment() {

    private var _binding: FragmentCalificarTutoriaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalificarTutoriaViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalificarTutoriaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tutoria = runCatching {
            Gson().fromJson(requireArguments().getString(ARG_TUTORIA), TutoriaDto::class.java)
        }.getOrNull()
        if (tutoria == null) {
            Toast.makeText(requireContext(), R.string.calificar_no_disponible, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvTutor.text = tutoria.tutor?.name?.ifBlank { null } ?: getString(R.string.detalle_tutor)
        binding.tvDetalle.text = listOfNotNull(tutoria.materia?.nombre, fechaCorta(requireContext(), tutoria.fecha).ifBlank { null })
            .joinToString(" · ")
        CargadorFotos.mostrar(viewLifecycleOwner.lifecycleScope, binding.ivFoto, tutoria.tutor?.fotoUrl)

        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            val puntaje = rating.toInt()
            binding.tvPuntaje.isVisible = puntaje > 0
            binding.tvPuntaje.text = getString(R.string.calificar_puntaje, puntaje)
            binding.tvError.isVisible = false
        }
        binding.tvPuntaje.isVisible = false

        binding.btnEnviar.setOnClickListener { enviar(tutoria.id) }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            val enviando = estado is EstadoCalificacion.Enviando
            binding.progress.isVisible = enviando
            binding.btnEnviar.isEnabled = !enviando
            binding.ratingBar.isEnabled = !enviando

            when (estado) {
                EstadoCalificacion.Inactivo, EstadoCalificacion.Enviando -> Unit
                EstadoCalificacion.Exito -> terminar(R.string.calificar_exito)
                EstadoCalificacion.YaCalificada -> terminar(R.string.calificar_ya_calificada)
                is EstadoCalificacion.Error -> {
                    viewModel.consumir()
                    binding.tilComentario.error = estado.errores["comentario"]?.firstOrNull()
                    mostrarError(estado.errores["puntaje"]?.firstOrNull() ?: estado.mensaje)
                }
            }
        }
    }

    private fun enviar(tutoriaId: Long) {
        val puntaje = binding.ratingBar.rating.toInt()
        val comentario = binding.etComentario.text?.toString().orEmpty().trim()
        binding.tilComentario.error = null
        binding.tvError.isVisible = false

        if (puntaje < 1) {
            mostrarError(getString(R.string.calificar_err_puntaje))
            return
        }
        if (comentario.length > COMENTARIO_MAX) {
            binding.tilComentario.error = getString(R.string.calificar_err_comentario_largo, COMENTARIO_MAX)
            return
        }
        viewModel.calificar(tutoriaId, puntaje, comentario.ifEmpty { null })
    }

    private fun mostrarError(texto: String) {
        binding.tvError.text = texto
        binding.tvError.isVisible = true
    }

    /** Vuelve a la lista; al reanudarse recarga y la tutoría ya no ofrece el botón Calificar. */
    private fun terminar(@androidx.annotation.StringRes mensaje: Int) {
        viewModel.consumir()
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val COMENTARIO_MAX = 500
    }
}

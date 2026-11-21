package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentCancelarTutoriaBinding

/** Pide el motivo y cancela la tutoría recibida en `ARG_TUTORIA_ID`. */
class CancelarTutoria : Fragment() {

    private var _binding: FragmentCancelarTutoriaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CancelarTutoriaViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCancelarTutoriaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tutoriaId = requireArguments().getLong(ARG_TUTORIA_ID)

        binding.btnVolverCancelacion.setOnClickListener { findNavController().popBackStack() }
        binding.btnConfirmarCancelacion.setOnClickListener {
            val motivo = binding.etMotivoCancelacion.text?.toString().orEmpty().trim()
            if (motivo.isEmpty()) {
                binding.etMotivoCancelacion.error = "Escribí el motivo de la cancelación."
                return@setOnClickListener
            }
            viewModel.cancelar(tutoriaId, motivo)
        }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            val enviando = estado is EstadoCancelacion.Enviando
            binding.btnConfirmarCancelacion.isEnabled = !enviando
            binding.btnVolverCancelacion.isEnabled = !enviando
            when (estado) {
                EstadoCancelacion.Exito -> {
                    viewModel.consumir()
                    Toast.makeText(requireContext(), "Tutoría cancelada.", Toast.LENGTH_SHORT).show()
                    val nav = findNavController()
                    if (!nav.popBackStack(R.id.misTutoriasFragment, false)) nav.popBackStack()
                }
                is EstadoCancelacion.Error -> {
                    viewModel.consumir()
                    Toast.makeText(requireContext(), estado.mensaje, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_TUTORIA_ID = "tutoria_id"
    }
}

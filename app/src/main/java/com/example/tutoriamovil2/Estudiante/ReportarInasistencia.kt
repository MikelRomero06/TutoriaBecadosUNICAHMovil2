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
import com.example.tutoriamovil2.databinding.FragmentReportarInasistenciaBinding

/**
 * Pide el motivo y reporta que la otra parte no llegó a la tutoría.
 * La usan el estudiante (reporta al tutor) y el tutor (reporta al estudiante); `ARG_ES_TUTOR` cambia los textos.
 */
class ReportarInasistencia : Fragment() {

    private var _binding: FragmentReportarInasistenciaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportarInasistenciaViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportarInasistenciaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val tutoriaId = args.getLong(ARG_TUTORIA_ID)
        val esTutor = args.getBoolean(ARG_ES_TUTOR, false)

        binding.tvAyudaReporte.setText(
            if (esTutor) R.string.reportar_ayuda_tutor else R.string.reportar_ayuda_estudiante,
        )

        binding.btnCancelarReporte.setOnClickListener { findNavController().popBackStack() }
        binding.btnEnviarReporte.setOnClickListener {
            val motivo = binding.etMotivoInasistencia.text?.toString().orEmpty().trim()
            if (motivo.isEmpty()) {
                binding.tilMotivoInasistencia.error = getString(R.string.reportar_err_motivo)
                return@setOnClickListener
            }
            binding.tilMotivoInasistencia.error = null
            viewModel.reportar(tutoriaId, motivo)
        }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            val enviando = estado is EstadoReporte.Enviando
            binding.btnEnviarReporte.isEnabled = !enviando
            binding.btnCancelarReporte.isEnabled = !enviando
            when (estado) {
                EstadoReporte.Exito -> {
                    viewModel.consumir()
                    Toast.makeText(requireContext(), R.string.reportar_exito, Toast.LENGTH_SHORT).show()
                    // Vuelve a la lista; al reanudarse recarga y la tutoría ya sale en el historial.
                    findNavController().popBackStack()
                }
                is EstadoReporte.Error -> {
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
        const val ARG_ES_TUTOR = "es_tutor"
    }
}

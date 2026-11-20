package com.example.tutoriamovil2.Login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentRegistroCorreoBinding

class RegistroCorreoFragment : Fragment() {

    private var _binding: FragmentRegistroCorreoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegistroViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegistroCorreoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnEnviarCodigo.setOnClickListener {
            binding.tvError.visibility = View.GONE
            viewModel.enviarCodigo(binding.etUsuarioCorreo.text?.toString().orEmpty())
        }

        viewModel.correoEstado.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is PasoRegistro.Inicial -> Unit
                is PasoRegistro.Cargando -> mostrarCargando(true)
                is PasoRegistro.Error -> {
                    mostrarCargando(false)
                    binding.tvError.text = estado.mensaje
                    binding.tvError.visibility = View.VISIBLE
                }
                is PasoRegistro.Ok -> {
                    mostrarCargando(false)
                    viewModel.limpiarCorreo()
                    findNavController().navigate(R.id.action_registroCorreo_a_registroCodigo)
                }
                else -> Unit
            }
        }
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progress.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnEnviarCodigo.isEnabled = !cargando
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

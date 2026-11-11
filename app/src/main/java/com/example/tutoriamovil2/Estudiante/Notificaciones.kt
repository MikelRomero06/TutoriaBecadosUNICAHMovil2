package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutoriamovil2.databinding.FragmentNotificacionesBinding

/** Citaciones de Bienestar (con "Enterado") y notificaciones del usuario. */
class Notificaciones : Fragment() {

    private var _binding: FragmentNotificacionesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificacionesViewModel by viewModels()
    private lateinit var adapter: AvisoInicioAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNotificacionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnLeerTodas.setOnClickListener {
            binding.btnLeerTodas.isEnabled = false
            viewModel.leerTodas()
        }

        adapter = AvisoInicioAdapter { id -> viewModel.marcarEnterado(id) }
        binding.rvAvisos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAvisos.adapter = adapter

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            binding.progress.isVisible = estado is EstadoAvisos.Cargando
            binding.rvAvisos.isVisible = estado is EstadoAvisos.Exito
            binding.grupoVacio.isVisible = estado is EstadoAvisos.Vacio || estado is EstadoAvisos.Error
            binding.btnLeerTodas.isVisible = estado is EstadoAvisos.Exito && estado.sinLeer > 0
            binding.btnLeerTodas.isEnabled = true
            when (estado) {
                is EstadoAvisos.Exito -> adapter.submitList(estado.avisos)
                is EstadoAvisos.Vacio -> {
                    adapter.submitList(emptyList())
                    binding.tvEstado.text = "No tenés notificaciones por ahora."
                }
                is EstadoAvisos.Error -> binding.tvEstado.text = estado.mensaje
                EstadoAvisos.Cargando -> Unit
            }
        }

        viewModel.aviso.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje != null) {
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                viewModel.avisoMostrado()
            }
        }

        viewModel.cargar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

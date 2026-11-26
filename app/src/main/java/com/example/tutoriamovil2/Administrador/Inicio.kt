package com.example.tutoriamovil2.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentInicioBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Inicio del administrador: contadores pendientes y tutorías de hoy. */
class Inicio : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminInicioViewModel by viewModels()
    private val adapter = TutoriaHoyAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hoy = SimpleDateFormat("EEEE d MMM yyyy", Locale("es", "HN")).format(Date())
            .replace(".", "").replaceFirstChar { it.uppercase() }
        binding.tvFecha.text = getString(R.string.admin_resumen_fecha, hoy)

        binding.rvTutoriasHoy.adapter = adapter
        binding.btnReintentar.setOnClickListener { viewModel.cargar() }

        // Cada contador lleva a la pestaña donde se resuelve.
        binding.cardAprobaciones.setOnClickListener { irA(R.id.adminAprobaciones) }
        binding.cardNoRealizadas.setOnClickListener { irA(R.id.adminNoRealizadas) }
        binding.cardEventos.setOnClickListener { irA(R.id.adminBitacora) }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            binding.progress.isVisible = estado is EstadoAdminInicio.Cargando
            binding.scrollInicio.isVisible = estado is EstadoAdminInicio.Exito
            binding.grupoError.isVisible = estado is EstadoAdminInicio.Error

            when (estado) {
                EstadoAdminInicio.Cargando -> Unit
                is EstadoAdminInicio.Error -> binding.tvError.text = estado.mensaje
                is EstadoAdminInicio.Exito -> {
                    val r = estado.resumen
                    binding.tvAprobaciones.text = r.aprobacionesPendientes.toString()
                    binding.tvNoRealizadas.text = r.noRealizadasSinRevisar.toString()
                    binding.tvEventos.text = r.eventosSinRevisar.toString()
                    binding.tvHoyTotal.text = maxOf(r.tutoriasHoy, estado.tutorias.size).toString()
                    adapter.submitList(estado.tutorias)
                    binding.rvTutoriasHoy.isVisible = estado.tutorias.isNotEmpty()
                    binding.tvHoyVacio.isVisible = estado.tutorias.isEmpty()
                }
            }
        }
    }

    /** Al volver de aprobar algo, los contadores se actualizan. */
    override fun onResume() {
        super.onResume()
        viewModel.cargar()
    }

    private fun irA(destino: Int) {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavAdministrador)?.selectedItemId = destino
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

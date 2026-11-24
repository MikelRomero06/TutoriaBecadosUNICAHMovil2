package com.example.tutoriamovil2.Tutor

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.Estudiante.EstadoMisTutorias
import com.example.tutoriamovil2.Estudiante.MisTutoriasViewModel
import com.example.tutoriamovil2.Estudiante.irAReportar
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.base.BaseFragment
import com.example.tutoriamovil2.databinding.FragmentTutorAgendaBinding
import com.example.tutoriamovil2.red.CargadorFotos

/** Agenda del tutor con próximas e historial; desde aquí reporta si el estudiante no llegó. */
class TutorAgendaFragment : BaseFragment<FragmentTutorAgendaBinding>(FragmentTutorAgendaBinding::inflate) {

    private val viewModel: MisTutoriasViewModel by viewModels()
    private lateinit var adapter: AgendaAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.usarAgendaTutor()

        adapter = AgendaAdapter(
            onReportar = { findNavController().irAReportar(it.id, esTutor = true) },
        ) { imagen, url ->
            CargadorFotos.mostrar(viewLifecycleOwner.lifecycleScope, imagen, url)
        }
        binding.rvAgenda.adapter = adapter
        binding.rvAgenda.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val manager = rv.layoutManager as? LinearLayoutManager ?: return
                if (dy > 0 && manager.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    viewModel.cargarMas()
                }
            }
        })

        binding.chipHistorial.isChecked = viewModel.tipo == MisTutoriasViewModel.HISTORIAL
        binding.grupoTipo.setOnCheckedStateChangeListener { _, ids ->
            val tipo = if (ids.firstOrNull() == R.id.chipHistorial) {
                MisTutoriasViewModel.HISTORIAL
            } else {
                MisTutoriasViewModel.PROXIMAS
            }
            binding.rvAgenda.scrollToPosition(0)
            viewModel.elegirTipo(tipo)
        }
        binding.btnAccion.setOnClickListener { viewModel.cargar() }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            val esHistorial = viewModel.tipo == MisTutoriasViewModel.HISTORIAL
            binding.progress.isVisible = estado is EstadoMisTutorias.Cargando
            binding.rvAgenda.isVisible = estado is EstadoMisTutorias.Exito
            binding.grupoVacio.isVisible = estado is EstadoMisTutorias.Vacio || estado is EstadoMisTutorias.Error
            binding.progressMas.isVisible = estado is EstadoMisTutorias.Exito && estado.cargandoMas
            binding.tvResumen.isVisible = estado is EstadoMisTutorias.Exito

            when (estado) {
                is EstadoMisTutorias.Exito -> {
                    adapter.submitList(estado.tutorias)
                    binding.tvResumen.text = resumen(estado.total, esHistorial)
                }
                is EstadoMisTutorias.Vacio -> {
                    adapter.submitList(emptyList())
                    binding.ivVacio.setImageResource(R.drawable.magnuslibros)
                    binding.tvEstado.setText(
                        if (esHistorial) R.string.agenda_vacia_historial else R.string.agenda_vacia_proximas,
                    )
                    binding.btnAccion.isVisible = false
                }
                is EstadoMisTutorias.Error -> {
                    binding.ivVacio.setImageResource(R.drawable.magnuspensando1)
                    binding.tvEstado.text = estado.mensaje
                    binding.btnAccion.setText(R.string.btn_reintentar_accion)
                    binding.btnAccion.isVisible = true
                }
                EstadoMisTutorias.Cargando -> Unit
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargar()
    }

    private fun resumen(total: Int, historial: Boolean): String = resources.getQuantityString(
        if (historial) R.plurals.agenda_resumen_historial else R.plurals.agenda_resumen_proximas,
        total,
        total,
    )
}

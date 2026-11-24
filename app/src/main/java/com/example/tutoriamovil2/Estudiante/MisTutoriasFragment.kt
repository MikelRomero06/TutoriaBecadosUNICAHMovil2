package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentMisTutoriasBinding
import com.example.tutoriamovil2.red.CargadorFotos
import com.google.android.material.bottomnavigation.BottomNavigationView

/** Tutorías del estudiante: próximas (confirmadas) e historial. */
class MisTutoriasFragment : Fragment() {

    private var _binding: FragmentMisTutoriasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MisTutoriasViewModel by viewModels()
    private lateinit var adapter: TutoriaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMisTutoriasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TutoriaAdapter(
            onClick = { findNavController().navigate(R.id.estudianteTutoriaSolicitada, it.comoArgumentos()) },
            onCancelar = { findNavController().irACancelar(it.id) },
            onReportar = { findNavController().irAReportar(it.id, esTutor = false) },
        ) { imagen, url ->
            CargadorFotos.mostrar(viewLifecycleOwner.lifecycleScope, imagen, url)
        }
        binding.rvTutorias.adapter = adapter
        binding.rvTutorias.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
            binding.rvTutorias.scrollToPosition(0)
            viewModel.elegirTipo(tipo)
        }

        binding.btnAccion.setOnClickListener {
            if (viewModel.estado.value is EstadoMisTutorias.Error) {
                viewModel.cargar()
            } else {
                requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavEstudiante)
                    ?.selectedItemId = R.id.explorarFragment
            }
        }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            val esHistorial = viewModel.tipo == MisTutoriasViewModel.HISTORIAL
            binding.progress.isVisible = estado is EstadoMisTutorias.Cargando
            binding.rvTutorias.isVisible = estado is EstadoMisTutorias.Exito
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
                        if (esHistorial) R.string.mis_tutorias_vacio_historial else R.string.mis_tutorias_vacio_proximas,
                    )
                    binding.btnAccion.setText(R.string.agenda_buscar_tutor)
                    binding.btnAccion.isVisible = !esHistorial
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
        if (historial) R.plurals.agenda_resumen_historial else R.plurals.mis_tutorias_resumen_confirmadas,
        total,
        total,
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

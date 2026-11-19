package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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

        adapter = TutoriaAdapter { imagen, url ->
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
                    binding.tvEstado.text = if (esHistorial) {
                        "Todavía no tenés tutorías en el historial."
                    } else {
                        "No tenés tutorías próximas. Buscá un tutor y reservá un horario."
                    }
                    binding.btnAccion.text = "Buscar tutor"
                    binding.btnAccion.isVisible = !esHistorial
                }
                is EstadoMisTutorias.Error -> {
                    binding.ivVacio.setImageResource(R.drawable.magnuspensando1)
                    binding.tvEstado.text = estado.mensaje
                    binding.btnAccion.text = "Reintentar"
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

    private fun resumen(total: Int, historial: Boolean): String = when {
        historial && total == 1 -> "1 tutoría en el historial"
        historial -> "$total tutorías en el historial"
        total == 1 -> "1 tutoría confirmada"
        else -> "$total tutorías confirmadas"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentExplorarBinding

class ExplorarFragment : Fragment() {

    private var _binding: FragmentExplorarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExplorarViewModel by viewModels()
    private lateinit var tutorAdapter: TutorAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentExplorarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerView()
        configurarNavegacionInferior()
        configurarBuscadorYFiltros()
        observarEstado()

        viewModel.cargarTutorias()
    }

    private fun configurarRecyclerView() {
        tutorAdapter = TutorAdapter { tutoria ->
            val bundle = Bundle().apply {
                putLong("tutoria_id", tutoria.id)
            }
            findNavController().navigate(R.id.estudiantePerfilTutor, bundle)
        }

        binding.rvTutores.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tutorAdapter
        }
    }

    private fun configurarNavegacionInferior() {
        binding.btnNavInicio.setOnClickListener {
            findNavController().navigate(R.id.inicioEstudianteFragment)
        }
        binding.btnNavTutorias.setOnClickListener {
            findNavController().navigate(R.id.misTutoriasFragment)
        }
        binding.btnNavPerfil.setOnClickListener {
            findNavController().navigate(R.id.perfilEstudianteFragment)
        }
    }

    private fun configurarBuscadorYFiltros() {
        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.buscar(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipPresencial) -> viewModel.filtrarModalidad("PRESENCIAL")
                checkedIds.contains(R.id.chipVirtual) -> viewModel.filtrarModalidad("VIRTUAL")
                else -> viewModel.filtrarModalidad(null)
            }
        }
    }

    private fun observarEstado() {
        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is EstadoExplorar.Cargando -> {
                    binding.progress.isVisible = true
                    binding.rvTutores.isVisible = false
                    binding.tvEstado.isVisible = false
                }
                is EstadoExplorar.Exito -> {
                    binding.progress.isVisible = false
                    binding.rvTutores.isVisible = true
                    binding.tvEstado.isVisible = false
                    tutorAdapter.submitList(estado.tutorias)
                    binding.tvResumen.text = "${estado.tutorias.size} tutorías disponibles"
                }
                is EstadoExplorar.Vacio -> {
                    binding.progress.isVisible = false
                    binding.rvTutores.isVisible = false
                    binding.tvEstado.isVisible = true
                    binding.tvEstado.text = "No se encontraron tutorías disponibles."
                    binding.tvResumen.text = "0 tutorías disponibles"
                }
                is EstadoExplorar.Error -> {
                    binding.progress.isVisible = false
                    binding.rvTutores.isVisible = false
                    binding.tvEstado.isVisible = true
                    binding.tvEstado.text = estado.mensaje
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

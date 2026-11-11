package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.data.mensajeError
import com.example.tutoriamovil2.databinding.FragmentExplorarBinding
import com.example.tutoriamovil2.red.CargadorFotos
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.MateriaDto
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ExplorarFragment : Fragment() {

    private var _binding: FragmentExplorarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExplorarViewModel by viewModels()
    private lateinit var tutorAdapter: TutorAdapter
    private var filtros: FiltrosVisibles? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentExplorarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarLista()
        configurarCabecera()
        configurarBuscador()
        configurarChips()
        observar()

        viewModel.iniciar()
    }

    private fun configurarLista() {
        tutorAdapter = TutorAdapter(
            mostrarFoto = { vista, url -> CargadorFotos.mostrar(viewLifecycleOwner.lifecycleScope, vista, url) },
            alTocar = { tutor ->
                findNavController().navigate(
                    R.id.estudiantePerfilTutor,
                    Bundle().apply { putLong(EstudiantePerfilTutor.ARG_TUTOR_ID, tutor.id) },
                )
            },
        )

        val lm = LinearLayoutManager(requireContext())
        binding.rvTutores.layoutManager = lm
        binding.rvTutores.adapter = tutorAdapter
        binding.rvTutores.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                // Cerca del final pide la página siguiente.
                if (dy > 0 && lm.findLastVisibleItemPosition() >= tutorAdapter.itemCount - 3) {
                    viewModel.cargarMas()
                }
            }
        })
        binding.btnReintentar.setOnClickListener { viewModel.reintentar() }
    }

    private fun configurarCabecera() {
        binding.btnNotif.setOnClickListener { findNavController().navigate(R.id.notificaciones) }
    }

    private fun configurarBuscador() {
        binding.etBuscar.doAfterTextChanged { viewModel.buscar(it?.toString().orEmpty()) }
    }

    private fun configurarChips() {
        binding.grupoCarrera.setOnCheckedStateChangeListener { _, ids ->
            viewModel.usarMiCarrera(ids.contains(R.id.chipCarrera))
        }

        binding.grupoModalidad.setOnCheckedStateChangeListener { _, ids ->
            viewModel.filtrarModalidad(
                when {
                    ids.contains(R.id.chipPresencial) -> "PRESENCIAL"
                    ids.contains(R.id.chipVirtual) -> "VIRTUAL"
                    else -> null
                },
            )
        }

        binding.chipMateria.setOnClickListener {
            // El chip solo se marca cuando hay una materia elegida.
            binding.chipMateria.isChecked = filtros?.materia != null
            elegirMateria()
        }
        binding.chipMateria.setOnCloseIconClickListener { viewModel.elegirMateria(null) }
    }

    private fun elegirMateria() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.chipMateria.isEnabled = false
            val res = viewModel.materiasParaFiltro()
            if (_binding == null) return@launch
            binding.chipMateria.isEnabled = true
            when {
                res is Resultado.Exito && res.dato.isNotEmpty() -> mostrarDialogoMaterias(res.dato)
                res is Resultado.Exito -> aviso("No hay materias cargadas para esta carrera.")
                else -> aviso(res.mensajeError())
            }
        }
    }

    private fun mostrarDialogoMaterias(materias: List<MateriaDto>) {
        val nombres = (listOf("Todas las materias") + materias.map { it.nombre.orEmpty() }).toTypedArray()
        val actual = materias.indexOfFirst { it.id == filtros?.materia?.id } + 1
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtrar por materia")
            .setSingleChoiceItems(nombres, actual) { dialogo, posicion ->
                viewModel.elegirMateria(if (posicion == 0) null else materias[posicion - 1])
                dialogo.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observar() {
        viewModel.filtros.observe(viewLifecycleOwner) { f ->
            filtros = f
            binding.chipCarrera.isVisible = f.miCarrera != null
            val idCarrera = if (f.soloMiCarrera) R.id.chipCarrera else R.id.chipTodas
            if (binding.grupoCarrera.checkedChipId != idCarrera) binding.grupoCarrera.check(idCarrera)

            binding.chipMateria.text = f.materia?.nombre ?: "Materia"
            binding.chipMateria.isChecked = f.materia != null
            binding.chipMateria.isCloseIconVisible = f.materia != null
        }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            binding.progress.isVisible = estado is EstadoExplorar.Cargando
            binding.rvTutores.isVisible = estado is EstadoExplorar.Exito
            binding.grupoVacio.isVisible = estado is EstadoExplorar.Vacio || estado is EstadoExplorar.Error
            binding.progressMas.isVisible = estado is EstadoExplorar.Exito && estado.cargandoMas

            when (estado) {
                is EstadoExplorar.Cargando -> binding.tvResumen.text = ""
                is EstadoExplorar.Exito -> {
                    tutorAdapter.submitList(estado.tutores)
                    binding.tvResumen.text = resumen(estado.total)
                }
                is EstadoExplorar.Vacio -> {
                    tutorAdapter.submitList(emptyList())
                    binding.tvResumen.text = resumen(0)
                    binding.ivVacio.setImageResource(R.drawable.magnuspensando1)
                    binding.tvEstado.text = "No encontramos tutores con esos filtros.\nProbá con otra materia o con todas las carreras."
                    binding.btnReintentar.isVisible = false
                }
                is EstadoExplorar.Error -> {
                    binding.ivVacio.setImageResource(R.drawable.magnuspensando1)
                    binding.tvEstado.text = estado.mensaje
                    binding.btnReintentar.isVisible = true
                }
            }
        }
    }

    private fun resumen(cantidad: Int): String {
        val donde = filtros?.let { if (it.soloMiCarrera) it.miCarrera?.nombre else "Todas las carreras" }
        val cuantos = if (cantidad == 1) "1 tutor" else "$cantidad tutores"
        return listOfNotNull(donde, cuantos).joinToString(" · ")
    }

    private fun aviso(texto: String) {
        Toast.makeText(requireContext(), texto, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

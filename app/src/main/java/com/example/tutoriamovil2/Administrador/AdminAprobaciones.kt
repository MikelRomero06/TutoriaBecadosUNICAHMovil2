package com.example.tutoriamovil2.Administrador

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentAdminAprobacionesBinding
import com.example.tutoriamovil2.red.CargadorFotos

/** Aprobaciones pendientes del administrador, con dos pestañas: cambios de foto y postulaciones a tutor. */
class AdminAprobaciones : Fragment() {

    private var _binding: FragmentAdminAprobacionesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminAprobacionesViewModel by viewModels()

    private lateinit var fotoAdapter: FotoAprobacionAdapter
    private lateinit var postulacionAdapter: PostulacionAprobacionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminAprobacionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mostrarFoto: (android.widget.ImageView, String?) -> Unit = { imagen, url ->
            CargadorFotos.mostrar(viewLifecycleOwner.lifecycleScope, imagen, url)
        }
        fotoAdapter = FotoAprobacionAdapter(
            onAprobar = { viewModel.aprobarFoto(it.id) },
            onRechazar = { viewModel.rechazarFoto(it.id) },
            mostrarFoto = mostrarFoto,
        )
        postulacionAdapter = PostulacionAprobacionAdapter(
            onAprobar = { viewModel.aprobarPostulacion(it.id) },
            onRechazar = { postulacion, motivo -> viewModel.rechazarPostulacion(postulacion.id, motivo) },
            mostrarFoto = mostrarFoto,
        )

        binding.chipPostulaciones.isChecked = viewModel.tab.value == TabAprobaciones.POSTULACIONES
        binding.grupoTab.setOnCheckedStateChangeListener { _, ids ->
            binding.rvAprobaciones.scrollToPosition(0)
            viewModel.elegirTab(
                if (ids.firstOrNull() == R.id.chipPostulaciones) TabAprobaciones.POSTULACIONES else TabAprobaciones.FOTOS,
            )
        }
        binding.btnAccion.setOnClickListener { viewModel.recargarActual() }

        viewModel.tab.observe(viewLifecycleOwner) { pintar() }
        viewModel.fotos.observe(viewLifecycleOwner) { if (viewModel.tab.value == TabAprobaciones.FOTOS) pintar() }
        viewModel.postulaciones.observe(viewLifecycleOwner) {
            if (viewModel.tab.value == TabAprobaciones.POSTULACIONES) pintar()
        }
        viewModel.procesando.observe(viewLifecycleOwner) { enProceso ->
            fotoAdapter.bloqueado = enProceso
            postulacionAdapter.bloqueado = enProceso
        }
        viewModel.mensaje.observe(viewLifecycleOwner) { texto ->
            if (texto != null) {
                Toast.makeText(requireContext(), texto, Toast.LENGTH_SHORT).show()
                viewModel.consumirMensaje()
            }
        }
    }

    /** Al volver a la pestaña se refrescan ambas listas (p. ej. llegó una postulación nueva). */
    override fun onResume() {
        super.onResume()
        viewModel.cargarTodo()
    }

    private fun pintar() {
        when (viewModel.tab.value ?: TabAprobaciones.FOTOS) {
            TabAprobaciones.FOTOS -> {
                usarAdapter(fotoAdapter)
                aplicar(viewModel.fotos.value, { fotoAdapter.submitList(it) }, R.string.admin_fotos_vacio)
            }
            TabAprobaciones.POSTULACIONES -> {
                usarAdapter(postulacionAdapter)
                aplicar(viewModel.postulaciones.value, { postulacionAdapter.submitList(it) }, R.string.admin_postulaciones_vacio)
            }
        }
    }

    private fun usarAdapter(adapter: RecyclerView.Adapter<*>) {
        if (binding.rvAprobaciones.adapter !== adapter) binding.rvAprobaciones.adapter = adapter
    }

    /** Estados de cada pestaña: cargando · lista · vacío · error con reintento. */
    private fun <T> aplicar(estado: EstadoLista<T>?, entregar: (List<T>) -> Unit, @StringRes vacio: Int) {
        if (estado == null) return
        binding.progress.isVisible = estado is EstadoLista.Cargando
        binding.rvAprobaciones.isVisible = estado is EstadoLista.Exito
        binding.tvResumen.isVisible = estado is EstadoLista.Exito
        binding.grupoVacio.isVisible = estado is EstadoLista.Vacio || estado is EstadoLista.Error

        when (estado) {
            EstadoLista.Cargando -> Unit
            is EstadoLista.Exito -> {
                entregar(estado.items)
                binding.tvResumen.text =
                    resources.getQuantityString(R.plurals.admin_resumen_pendientes, estado.items.size, estado.items.size)
            }
            EstadoLista.Vacio -> {
                entregar(emptyList())
                binding.ivVacio.setImageResource(R.drawable.magnuslibros)
                binding.tvEstado.setText(vacio)
                binding.btnAccion.isVisible = false
            }
            is EstadoLista.Error -> {
                binding.ivVacio.setImageResource(R.drawable.magnuspensando1)
                binding.tvEstado.text = estado.mensaje
                binding.btnAccion.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

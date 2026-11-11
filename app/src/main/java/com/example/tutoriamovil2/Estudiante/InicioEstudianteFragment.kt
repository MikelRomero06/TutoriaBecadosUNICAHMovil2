package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentInicioEstudianteBinding
import com.example.tutoriamovil2.red.CargadorFotos
import com.google.android.material.bottomnavigation.BottomNavigationView

class InicioEstudianteFragment : Fragment() {

    private var _binding: FragmentInicioEstudianteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InicioEstudianteViewModel by viewModels()
    private lateinit var citaciones: AvisoInicioAdapter
    private lateinit var tutores: TutorAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInicioEstudianteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        citaciones = AvisoInicioAdapter { id -> viewModel.marcarEnterado(id) }
        binding.rvCitaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCitaciones.adapter = citaciones

        tutores = TutorAdapter(
            mostrarFoto = { imagen, url -> CargadorFotos.mostrar(viewLifecycleOwner.lifecycleScope, imagen, url) },
            alTocar = { tutor ->
                findNavController().navigate(
                    R.id.estudiantePerfilTutor,
                    bundleOf(EstudiantePerfilTutor.ARG_TUTOR_ID to tutor.id),
                )
            },
        )
        binding.rvTutores.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTutores.adapter = tutores

        binding.btnNotif.setOnClickListener { findNavController().navigate(R.id.notificaciones) }
        binding.cardBuscar.setOnClickListener { irA(R.id.explorarFragment) }
        binding.btnBuscarTutor.setOnClickListener { irA(R.id.explorarFragment) }
        binding.btnVerTutores.setOnClickListener { irA(R.id.explorarFragment) }
        binding.btnVerTutorias.setOnClickListener { irA(R.id.misTutoriasFragment) }
        binding.btnReintentar.setOnClickListener { viewModel.cargar() }

        viewModel.estado.observe(viewLifecycleOwner) { estado ->
            binding.progress.isVisible = estado is EstadoInicio.Cargando
            binding.scroll.isVisible = estado is EstadoInicio.Listo
            binding.grupoVacio.isVisible = estado is EstadoInicio.Error
            when (estado) {
                is EstadoInicio.Listo -> mostrar(estado.datos)
                is EstadoInicio.Error -> binding.tvEstado.text = estado.mensaje
                EstadoInicio.Cargando -> Unit
            }
        }

        viewModel.aviso.observe(viewLifecycleOwner) { mensaje ->
            if (mensaje != null) {
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
                viewModel.avisoMostrado()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Al volver de Notificaciones o de reservar, el contador y la próxima tutoría cambian.
        viewModel.cargar()
    }

    private fun mostrar(datos: DatosInicio) {
        binding.tvSaludo.text = datos.nombre?.let { "Hola, $it" } ?: "Hola"
        binding.tvCarrera.text = datos.carrera.orEmpty()
        binding.tvCarrera.isVisible = !datos.carrera.isNullOrBlank()

        binding.tvSinLeer.isVisible = datos.sinLeer > 0
        binding.tvSinLeer.text = if (datos.sinLeer > 9) "9+" else datos.sinLeer.toString()

        binding.grupoCitaciones.isVisible = datos.citaciones.isNotEmpty()
        citaciones.submitList(datos.citaciones)

        val proxima = datos.proxima
        binding.proxima.root.isVisible = proxima != null
        binding.cardSinProxima.isVisible = proxima == null
        if (proxima != null) {
            binding.proxima.mostrarTutoria(proxima) { imagen, url ->
                CargadorFotos.mostrar(viewLifecycleOwner.lifecycleScope, imagen, url)
            }
        } else {
            binding.tvSinProxima.text = datos.proximaError ?: "No tenés tutorías próximas."
        }

        tutores.submitList(datos.tutores)
        binding.rvTutores.isVisible = datos.tutores.isNotEmpty()
        binding.tvSinTutores.isVisible = datos.tutores.isEmpty()
        binding.tvSinTutores.text = datos.tutoresError ?: "Todavía no hay tutores aprobados en tu carrera."
    }

    /** Cambia de pestaña en la barra inferior, igual que si la persona la tocara. */
    private fun irA(destino: Int) {
        requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavEstudiante)?.selectedItemId = destino
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

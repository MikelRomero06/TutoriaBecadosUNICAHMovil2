package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.FragmentTutoriaConfirmacionBinding
import com.example.tutoriamovil2.red.CargadorFotos

/** Pantalla final: resumen de la tutoría reservada. */
class TutoriaConfirmacion : Fragment() {

    private var _binding: FragmentTutoriaConfirmacionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTutoriaConfirmacionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val a = requireArguments()

        binding.tvTituloExito.text = "¡Solicitud enviada!"
        binding.tvSubtituloExito.text = "Tu solicitud fue registrada. Te avisaremos cuando el tutor responda."
        binding.tvNombreTutor.text = a.getString(ARG_TUTOR_NOMBRE)
        binding.tvMateriaTutor.text = a.getString(ARG_MATERIA)
        binding.tvFecha.text = fechaLarga(a.getString(ARG_FECHA)!!)
        binding.tvDetalleHoraModalidad.text =
            "${rangoHoras(a.getInt(ARG_HORA_INICIO), a.getInt(ARG_HORA_FIN))} · ${modalidadTexto(a.getString(ARG_MODALIDAD))}"
        CargadorFotos.mostrar(viewLifecycleOwner.lifecycleScope, binding.ivFotoTutor, a.getString(ARG_TUTOR_FOTO))

        // El layout reutiliza btnConfirmar2 como botón principal.
        binding.btnConfirmar2.text = "Ver en mis tutorías"
        binding.btnConfirmar2.setOnClickListener { irA(R.id.misTutoriasFragment) }
        binding.btnVolverInicio.setOnClickListener { irA(R.id.inicioEstudianteFragment) }
    }

    /** Navega a una pestaña principal vaciando la pila para que "atrás" no regrese al flujo. */
    private fun irA(destino: Int) {
        val opciones = NavOptions.Builder()
            .setPopUpTo(R.id.nav_estudiante, inclusive = false, saveState = false)
            .build()
        findNavController().navigate(destino, null, opciones)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_TUTOR_NOMBRE = "tutor_nombre"
        const val ARG_TUTOR_FOTO = "tutor_foto"
        const val ARG_MATERIA = "materia"
        const val ARG_FECHA = "fecha"
        const val ARG_HORA_INICIO = "hora_inicio"
        const val ARG_HORA_FIN = "hora_fin"
        const val ARG_MODALIDAD = "modalidad"
    }
}

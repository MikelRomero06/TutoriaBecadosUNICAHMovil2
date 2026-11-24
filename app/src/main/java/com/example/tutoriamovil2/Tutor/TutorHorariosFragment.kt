package com.example.tutoriamovil2.Tutor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.example.tutoriamovil2.Login.Login
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.base.BaseFragment
import com.example.tutoriamovil2.databinding.DialogAgregarHorarioBinding
import com.example.tutoriamovil2.databinding.FragmentTutorHorariosBinding
import com.example.tutoriamovil2.red.dto.DisponibilidadDto
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Pantalla donde el tutor publica las horas en que puede dar tutorías.
// Elige un día arriba; abajo ve las horas de ese día. Una hora libre se publica con un diálogo
// (modalidad y lugar) y una publicada se puede quitar. Los estudiantes reservan sobre estos horarios.
class TutorHorariosFragment : BaseFragment<FragmentTutorHorariosBinding>(FragmentTutorHorariosBinding::inflate) {

    private val viewModel: TutorHorariosViewModel by viewModels()

    private lateinit var diaAdapter: DiaAdapter
    private lateinit var horarioAdapter: HorarioAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        diaAdapter = DiaAdapter { dia -> viewModel.elegirDia(dia) }
        horarioAdapter = HorarioAdapter(
            alTocarLibre = { hora -> mostrarDialogoAgregar(diaActual(), hora) },
            alTocarPublicado = { bloque -> confirmarEliminar(bloque) },
        )
        binding.rvDias.adapter = diaAdapter
        binding.rvHoras.adapter = horarioAdapter
        binding.btnReintentar.setOnClickListener { viewModel.cargar() }

        viewModel.estado.observe(viewLifecycleOwner) { pintar() }
        viewModel.diaSeleccionado.observe(viewLifecycleOwner) { pintar() }

        viewModel.mensaje.observe(viewLifecycleOwner) { texto ->
            if (texto != null) {
                Toast.makeText(requireContext(), texto, Toast.LENGTH_SHORT).show()
                viewModel.mensajeMostrado()
            }
        }
        viewModel.sesionCerrada.observe(viewLifecycleOwner) { cerrada ->
            if (cerrada) volverAlLogin()
        }
    }

    private fun diaActual() = viewModel.diaSeleccionado.value ?: 1

    // Dibuja la pantalla según el estado y el día elegido.
    private fun pintar() {
        val estado = viewModel.estado.value ?: return
        val dia = diaActual()

        binding.progressCarga.isVisible = estado is EstadoHorarios.Cargando
        binding.grupoError.isVisible = estado is EstadoHorarios.Error
        binding.rvHoras.isVisible = estado is EstadoHorarios.Listo
        binding.tvResumen.isVisible = estado is EstadoHorarios.Listo
        binding.progressGuardando.isVisible = (estado as? EstadoHorarios.Listo)?.guardando == true

        when (estado) {
            is EstadoHorarios.Listo -> {
                val delDia = estado.bloques.filter { it.diaSemana == dia }
                diaAdapter.actualizar(dia, estado.bloques)
                horarioAdapter.actualizar(delDia, habilitado = !estado.guardando)
                val publicados = if (delDia.isEmpty()) {
                    getString(R.string.horarios_sin_publicar)
                } else {
                    resources.getQuantityString(R.plurals.horarios_publicados, delDia.size, delDia.size)
                }
                binding.tvResumen.text = getString(R.string.horarios_resumen, nombreDia(requireContext(), dia), publicados)
            }
            is EstadoHorarios.Error -> binding.tvError.text = estado.mensaje
            EstadoHorarios.Cargando -> diaAdapter.actualizar(dia, emptyList())
        }
    }

    private fun mostrarDialogoAgregar(dia: Int, hora: Int) {
        val vista = DialogAgregarHorarioBinding.inflate(layoutInflater)

        // El texto de ayuda cambia según la modalidad: aula para presencial, enlace para virtual.
        vista.grupoModalidad.addOnButtonCheckedListener { _, id, marcado ->
            if (!marcado) return@addOnButtonCheckedListener
            val virtual = id == R.id.btnVirtual
            vista.tilLugar.hint = getString(if (virtual) R.string.horarios_hint_enlace else R.string.horarios_hint_lugar)
            vista.tilLugar.helperText =
                getString(if (virtual) R.string.horarios_ayuda_enlace else R.string.horarios_ayuda_lugar)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.horarios_rango, nombreDia(requireContext(), dia), textoHora(hora), textoHora(hora + 1)))
            .setView(vista.root)
            .setNegativeButton(R.string.horarios_cancelar, null)
            .setPositiveButton(R.string.horarios_publicar) { _, _ ->
                val modalidad = if (vista.grupoModalidad.checkedButtonId == R.id.btnVirtual) "VIRTUAL" else "PRESENCIAL"
                viewModel.agregar(dia, hora, modalidad, vista.etLugar.text?.toString())
            }
            .show()
    }

    private fun confirmarEliminar(bloque: DisponibilidadDto) {
        val modalidad = getString(if (bloque.modalidad == "VIRTUAL") R.string.horarios_virtual else R.string.horarios_presencial)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.horarios_quitar_titulo)
            .setMessage(
                getString(R.string.horarios_rango, nombreDia(requireContext(), bloque.diaSemana), textoHora(bloque.horaInicio), textoHora(bloque.horaInicio + 1)) + "\n" +
                    modalidad + (bloque.lugarOEnlace?.let { " · $it" } ?: "") +
                    "\n\n" + getString(R.string.horarios_quitar_aviso)
            )
            .setNegativeButton(R.string.horarios_cancelar, null)
            .setPositiveButton(R.string.horarios_quitar) { _, _ -> viewModel.eliminar(bloque.diaSemana, bloque.horaInicio) }
            .show()
    }

    private fun volverAlLogin() {
        val intent = Intent(requireContext(), Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}

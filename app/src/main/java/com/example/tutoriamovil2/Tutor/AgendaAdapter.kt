package com.example.tutoriamovil2.Tutor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.Estudiante.estiloEstado
import com.example.tutoriamovil2.Estudiante.fechaCorta
import com.example.tutoriamovil2.Estudiante.horario
import com.example.tutoriamovil2.Estudiante.textoModalidad
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.ItemAgendaBinding
import com.example.tutoriamovil2.red.dto.TutoriaDto

class AgendaAdapter(
    private val mostrarFoto: (ImageView, String?) -> Unit,
) : ListAdapter<TutoriaDto, AgendaAdapter.AgendaViewHolder>(Diferencia) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendaViewHolder {
        val binding = ItemAgendaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AgendaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AgendaViewHolder, position: Int) {
        holder.binding.mostrarAgenda(getItem(position), mostrarFoto)
    }

    class AgendaViewHolder(val binding: ItemAgendaBinding) : RecyclerView.ViewHolder(binding.root)

    private object Diferencia : DiffUtil.ItemCallback<TutoriaDto>() {
        override fun areItemsTheSame(viejo: TutoriaDto, nuevo: TutoriaDto) = viejo.id == nuevo.id
        override fun areContentsTheSame(viejo: TutoriaDto, nuevo: TutoriaDto) = viejo == nuevo
    }
}

/** Llena `item_agenda` con una tutoría del tutor. Reportar y Cancelar siguen ocultos hasta que exista el endpoint. */
fun ItemAgendaBinding.mostrarAgenda(tutoria: TutoriaDto, mostrarFoto: (ImageView, String?) -> Unit) {
    val estudiante = tutoria.estudiante
    tvNombre.text = estudiante?.name?.ifBlank { null } ?: root.context.getString(R.string.agenda_estudiante)
    tvCarrera.text = estudiante?.nombreCarrera.orEmpty()
    tvCarrera.isVisible = tvCarrera.text.isNotBlank()

    val estilo = estiloEstado(root.context, tutoria.estado, vistaTutor = true)
    tvEstado.text = estilo.texto
    tvEstado.setBackgroundResource(estilo.fondo)
    tvEstado.setTextColor(ContextCompat.getColor(root.context, estilo.color))

    tvMateriaTema.text = listOfNotNull(tutoria.materia?.nombre, tutoria.tema?.ifBlank { null })
        .joinToString(" · ")
    tvFechaHora.text = listOf(
        fechaCorta(root.context, tutoria.fecha),
        horario(tutoria.horaInicio, tutoria.horaFin),
        listOf(textoModalidad(root.context, tutoria.modalidad), tutoria.lugarOEnlace.orEmpty())
            .filter { it.isNotBlank() }.joinToString(" · "),
    ).filter { it.isNotBlank() }.joinToString(" · ")
    tvNota.text = root.context.getString(
        R.string.agenda_nota,
        tutoria.descripcion?.ifBlank { null } ?: root.context.getString(R.string.agenda_sin_nota),
    )

    (btnReportar.parent as View).isVisible = false
    mostrarFoto(ivFoto, estudiante?.fotoUrl)
}

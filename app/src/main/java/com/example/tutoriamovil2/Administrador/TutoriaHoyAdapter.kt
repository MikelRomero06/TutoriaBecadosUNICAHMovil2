package com.example.tutoriamovil2.Administrador

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.Estudiante.estiloEstado
import com.example.tutoriamovil2.Estudiante.horaTexto
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.ItemAdminTutoriaHoyBinding
import com.example.tutoriamovil2.red.dto.TutoriaDto

/** Tutorías del día en el inicio del administrador: hora, tutor → estudiante, materia y estado. */
class TutoriaHoyAdapter : ListAdapter<TutoriaDto, TutoriaHoyAdapter.VH>(Diferencia) {

    class VH(val b: ItemAdminTutoriaHoyBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAdminTutoriaHoyBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tutoria = getItem(position)
        val ctx = holder.itemView.context
        val sinNombre = ctx.getString(R.string.admin_sin_nombre)

        holder.b.tvHora.text = tutoria.horaInicio?.let { horaTexto(it) }.orEmpty()
        holder.b.tvPersonas.text = ctx.getString(
            R.string.admin_tutor_a_estudiante,
            tutoria.tutor?.name?.ifBlank { null } ?: sinNombre,
            tutoria.estudiante?.name?.ifBlank { null } ?: sinNombre,
        )
        holder.b.tvMateria.text = tutoria.materia?.nombre.orEmpty()

        val estilo = estiloEstado(ctx, tutoria.estado)
        holder.b.tvEstado.text = estilo.texto
        holder.b.tvEstado.setBackgroundResource(estilo.fondo)
        holder.b.tvEstado.setTextColor(ContextCompat.getColor(ctx, estilo.color))
    }

    private object Diferencia : DiffUtil.ItemCallback<TutoriaDto>() {
        override fun areItemsTheSame(a: TutoriaDto, b: TutoriaDto) = a.id == b.id
        override fun areContentsTheSame(a: TutoriaDto, b: TutoriaDto) = a == b
    }
}

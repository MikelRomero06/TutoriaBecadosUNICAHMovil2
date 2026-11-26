package com.example.tutoriamovil2.Administrador

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.ItemAprobacionPostulacionBinding
import com.example.tutoriamovil2.red.dto.PostulacionDto

/** Postulaciones a tutor pendientes. Rechazar exige escribir el motivo. */
class PostulacionAprobacionAdapter(
    private val onAprobar: (PostulacionDto) -> Unit,
    private val onRechazar: (PostulacionDto, String) -> Unit,
    private val mostrarFoto: (ImageView, String?) -> Unit,
) : ListAdapter<PostulacionDto, PostulacionAprobacionAdapter.VH>(Diferencia) {

    /** Mientras se envía una acción se bloquean los botones de todas las tarjetas. */
    var bloqueado = false
        set(valor) {
            field = valor
            notifyItemRangeChanged(0, itemCount, PAYLOAD_BLOQUEO)
        }

    class VH(val b: ItemAprobacionPostulacionBinding) : RecyclerView.ViewHolder(b.root) {
        /** Para borrar el motivo escrito si la vista se reutiliza con otra postulación. */
        var idMostrado: Long = -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val holder = VH(ItemAprobacionPostulacionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.b.etMotivo.doAfterTextChanged { holder.b.tilMotivo.error = null }
        return holder
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_BLOQUEO)) {
            actualizarBotones(holder)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val postulacion = getItem(position)
        val ctx = holder.itemView.context
        val usuario = postulacion.usuario

        if (holder.idMostrado != postulacion.id) {
            holder.b.etMotivo.setText("")
            holder.b.tilMotivo.error = null
            holder.idMostrado = postulacion.id
        }

        val nombre = usuario?.name?.ifBlank { null } ?: ctx.getString(R.string.admin_sin_nombre)
        val carrera = usuario?.nombreCarrera?.ifBlank { null }
        holder.b.tvTitulo.setText(R.string.admin_postulacion_titulo)
        holder.b.tvSubtitulo.text =
            if (carrera == null) nombre else ctx.getString(R.string.admin_postulacion_subtitulo, nombre, carrera)

        val materias = postulacion.materias.orEmpty().mapNotNull { it.nombre?.ifBlank { null } }
        holder.b.tvMaterias.text =
            if (materias.isEmpty()) ctx.getString(R.string.admin_postulacion_sin_materias)
            else ctx.getString(R.string.admin_postulacion_materias, materias.joinToString(", "))

        mostrarFoto(holder.b.ivFoto, usuario?.fotoUrl)

        holder.b.btnAprobar.setOnClickListener { onAprobar(postulacion) }
        holder.b.btnRechazar.setOnClickListener {
            val motivo = holder.b.etMotivo.text?.toString().orEmpty().trim()
            if (motivo.isEmpty()) {
                holder.b.tilMotivo.error = ctx.getString(R.string.admin_err_motivo)
            } else {
                onRechazar(postulacion, motivo)
            }
        }
        actualizarBotones(holder)
    }

    private fun actualizarBotones(holder: VH) {
        holder.b.btnAprobar.isEnabled = !bloqueado
        holder.b.btnRechazar.isEnabled = !bloqueado
    }

    private object Diferencia : DiffUtil.ItemCallback<PostulacionDto>() {
        override fun areItemsTheSame(a: PostulacionDto, b: PostulacionDto) = a.id == b.id
        override fun areContentsTheSame(a: PostulacionDto, b: PostulacionDto) = a == b
    }

    private companion object {
        const val PAYLOAD_BLOQUEO = "bloqueo"
    }
}

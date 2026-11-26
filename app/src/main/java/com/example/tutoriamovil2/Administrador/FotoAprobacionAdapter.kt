package com.example.tutoriamovil2.Administrador

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.ItemAprobacionFotoBinding
import com.example.tutoriamovil2.red.dto.CambioFotoDto

/** Cambios de foto de perfil pendientes: foto anterior junto a la nueva, con Aprobar y Rechazar. */
class FotoAprobacionAdapter(
    private val onAprobar: (CambioFotoDto) -> Unit,
    private val onRechazar: (CambioFotoDto) -> Unit,
    private val mostrarFoto: (ImageView, String?) -> Unit,
) : ListAdapter<CambioFotoDto, FotoAprobacionAdapter.VH>(Diferencia) {

    /** Mientras se envía una acción se bloquean los botones de todas las tarjetas. */
    var bloqueado = false
        set(valor) {
            field = valor
            notifyItemRangeChanged(0, itemCount, PAYLOAD_BLOQUEO)
        }

    class VH(val b: ItemAprobacionFotoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAprobacionFotoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_BLOQUEO)) {
            actualizarBotones(holder)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cambio = getItem(position)
        val ctx = holder.itemView.context
        holder.b.tvTitulo.text = ctx.getString(
            R.string.admin_foto_titulo,
            cambio.usuario?.name?.ifBlank { null } ?: ctx.getString(R.string.admin_sin_nombre),
        )
        mostrarFoto(holder.b.ivAnterior, cambio.fotoAnteriorUrl)
        mostrarFoto(holder.b.ivNueva, cambio.fotoNuevaUrl)
        holder.b.btnAprobar.setOnClickListener { onAprobar(cambio) }
        holder.b.btnRechazar.setOnClickListener { onRechazar(cambio) }
        actualizarBotones(holder)
    }

    private fun actualizarBotones(holder: VH) {
        holder.b.btnAprobar.isEnabled = !bloqueado
        holder.b.btnRechazar.isEnabled = !bloqueado
    }

    private object Diferencia : DiffUtil.ItemCallback<CambioFotoDto>() {
        override fun areItemsTheSame(a: CambioFotoDto, b: CambioFotoDto) = a.id == b.id
        override fun areContentsTheSame(a: CambioFotoDto, b: CambioFotoDto) = a == b
    }

    private companion object {
        const val PAYLOAD_BLOQUEO = "bloqueo"
    }
}

package com.example.tutoriamovil2.Estudiante

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.ItemBloqueBinding

/** Bloques de `rvBloques`: libre (verde), ocupado (amarillo, no tocable) y elegido (azul). */
class BloqueAdapter(
    private val alElegir: (BloqueUi) -> Unit,
) : RecyclerView.Adapter<BloqueAdapter.VH>() {

    private var bloques: List<BloqueUi> = emptyList()
    private var elegidaClave: String? = null

    fun actualizar(nuevos: List<BloqueUi>, elegidaClave: String?) {
        bloques = nuevos
        this.elegidaClave = elegidaClave
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemBloqueBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemBloqueBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = bloques.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val bloque = bloques[position]
        val card = holder.b.root
        val ctx = card.context
        val elegido = bloque.clave == elegidaClave

        holder.b.tvHora.text = rangoHoras(bloque.horaInicio, bloque.horaFin)
        val (fondo, borde, estado) = when {
            !bloque.disponible -> Triple(R.color.warning_container, R.color.warning_text, R.string.leyenda_ocupado)
            elegido -> Triple(R.color.primary_container, R.color.primary, R.string.leyenda_elegido)
            else -> Triple(R.color.success_container, R.color.success, R.string.estado_bloque_disponible)
        }
        holder.b.tvModalidad.text = ctx.getString(
            R.string.formato_modalidad_estado, modalidadTexto(ctx, bloque.modalidad), ctx.getString(estado),
        )
        card.setCardBackgroundColor(ContextCompat.getColor(ctx, fondo))
        card.strokeColor = ContextCompat.getColor(ctx, borde)
        card.alpha = if (bloque.disponible) 1f else 0.7f
        card.isEnabled = bloque.disponible
        card.setOnClickListener { if (bloque.disponible) alElegir(bloque) }
    }
}

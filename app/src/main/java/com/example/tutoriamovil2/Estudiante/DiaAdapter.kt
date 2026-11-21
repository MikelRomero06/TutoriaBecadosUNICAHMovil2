package com.example.tutoriamovil2.Estudiante

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.databinding.ItemDiaBinding

/** Fechas (`yyyy-MM-dd`) de `rvFechas`. Una sola seleccionada. */
class DiaAdapter(
    private val alElegir: (String) -> Unit,
) : RecyclerView.Adapter<DiaAdapter.VH>() {

    private var fechas: List<String> = emptyList()
    private var elegida: String? = null

    fun actualizar(nuevas: List<String>, elegida: String?) {
        fechas = nuevas
        this.elegida = elegida
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemDiaBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemDiaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = fechas.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val fecha = fechas[position]
        holder.b.tvDia.text = diaCorto(fecha)
        holder.b.tvNumero.text = diaNumero(fecha)
        (holder.b.root).isChecked = fecha == elegida
        holder.b.root.setOnClickListener { alElegir(fecha) }
    }
}

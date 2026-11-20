package com.example.tutoriamovil2.Estudiante

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.ItemNotificacionBinding

class AvisoInicioAdapter(
    private val alMarcarEnterado: (Long) -> Unit,
) : ListAdapter<AvisoInicio, AvisoInicioAdapter.AvisoViewHolder>(Diferencia) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvisoViewHolder {
        val binding = ItemNotificacionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AvisoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AvisoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AvisoViewHolder(
        private val binding: ItemNotificacionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(aviso: AvisoInicio) {
            binding.tvTitulo.text = aviso.titulo
            binding.tvMensaje.text = aviso.mensaje
            binding.tvMensaje.isVisible = aviso.mensaje.isNotBlank()

            binding.dot.isVisible = aviso.pendiente
            binding.dot.setBackgroundResource(
                if (aviso.esCitacion) R.drawable.bg_dot else R.drawable.bg_dot_primary,
            )

            val conBoton = aviso.esCitacion && aviso.pendiente && aviso.citacionId != null
            binding.btnEnterado.isVisible = conBoton
            binding.btnEnterado.isEnabled = true
            binding.btnEnterado.setOnClickListener {
                aviso.citacionId?.let {
                    binding.btnEnterado.isEnabled = false
                    alMarcarEnterado(it)
                }
            }
        }
    }

    private object Diferencia : DiffUtil.ItemCallback<AvisoInicio>() {
        override fun areItemsTheSame(viejo: AvisoInicio, nuevo: AvisoInicio) = viejo.clave == nuevo.clave
        override fun areContentsTheSame(viejo: AvisoInicio, nuevo: AvisoInicio) = viejo == nuevo
    }
}

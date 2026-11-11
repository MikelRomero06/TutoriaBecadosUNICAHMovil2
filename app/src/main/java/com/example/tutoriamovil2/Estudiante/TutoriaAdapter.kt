package com.example.tutoriamovil2.Estudiante

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.databinding.ItemTutoriaBinding
import com.example.tutoriamovil2.red.dto.TutoriaDto

class TutoriaAdapter(
    private val mostrarFoto: (ImageView, String?) -> Unit,
) : ListAdapter<TutoriaDto, TutoriaAdapter.TutoriaViewHolder>(Diferencia) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutoriaViewHolder {
        val binding = ItemTutoriaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TutoriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TutoriaViewHolder, position: Int) {
        holder.binding.mostrarTutoria(getItem(position), mostrarFoto)
    }

    class TutoriaViewHolder(val binding: ItemTutoriaBinding) : RecyclerView.ViewHolder(binding.root)

    private object Diferencia : DiffUtil.ItemCallback<TutoriaDto>() {
        override fun areItemsTheSame(viejo: TutoriaDto, nuevo: TutoriaDto) = viejo.id == nuevo.id
        override fun areContentsTheSame(viejo: TutoriaDto, nuevo: TutoriaDto) = viejo == nuevo
    }
}

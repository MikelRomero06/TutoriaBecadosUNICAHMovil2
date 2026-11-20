package com.example.tutoriamovil2.Estudiante

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.databinding.ItemTutorBinding
import com.example.tutoriamovil2.red.dto.TutorDto
import java.util.Locale

class TutorAdapter(
    private val mostrarFoto: (ImageView, String?) -> Unit,
    private val alTocar: (TutorDto) -> Unit,
) : ListAdapter<TutorDto, TutorAdapter.TutorViewHolder>(Diferencia) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorViewHolder {
        val binding = ItemTutorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TutorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TutorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TutorViewHolder(
        private val binding: ItemTutorBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tutor: TutorDto) {
            binding.tvNombre.text = tutor.name?.ifBlank { null } ?: "Tutor"
            binding.tvCarrera.text = tutor.carrera?.nombre.orEmpty()
            binding.tvCarrera.isVisible = tutor.carrera != null

            val materias = tutor.materias.orEmpty().mapNotNull { it.nombre }
            binding.tvMaterias.text = materias.joinToString(" · ")
            binding.tvMaterias.isVisible = materias.isNotEmpty()

            val esNuevo = tutor.nuevo == true || (tutor.totalCalificaciones ?: 0) < 3
            binding.tvNuevo.isVisible = esNuevo
            binding.ratingGroup.isVisible = !esNuevo
            if (!esNuevo) {
                binding.tvRating.text = formatoCalificacion(tutor.promedio, tutor.totalCalificaciones)
            }

            mostrarFoto(binding.ivFoto, tutor.fotoUrl)
            binding.root.setOnClickListener { alTocar(tutor) }
        }
    }

    private object Diferencia : DiffUtil.ItemCallback<TutorDto>() {
        override fun areItemsTheSame(viejo: TutorDto, nuevo: TutorDto) = viejo.id == nuevo.id
        override fun areContentsTheSame(viejo: TutorDto, nuevo: TutorDto) = viejo == nuevo
    }
}

/** "4.5 (12)": promedio de estrellas sobre 5 y cantidad de calificaciones. */
fun formatoCalificacion(promedio: Double?, total: Int?): String =
    "${String.format(Locale.US, "%.1f", promedio ?: 0.0)} (${total ?: 0})"

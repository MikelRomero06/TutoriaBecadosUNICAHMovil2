package com.example.tutoriamovil2.Tutor

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.ItemDiaBinding
import com.example.tutoriamovil2.databinding.ItemHorarioFilaBinding
import com.example.tutoriamovil2.red.dto.DisponibilidadDto

// Nombres de los días tal como los numera el servidor (1 = lunes ... 6 = sábado).
val NOMBRES_DIAS = mapOf(1 to "Lunes", 2 to "Martes", 3 to "Miércoles", 4 to "Jueves", 5 to "Viernes", 6 to "Sábado")

fun textoHora(hora: Int) = "$hora:00"

// Fila de días de lunes a sábado (usa item_dia). Debajo del nombre muestra cuántos horarios tiene ese día.
class DiaAdapter(private val alElegir: (Int) -> Unit) : RecyclerView.Adapter<DiaAdapter.Vista>() {

    private val dias = TutorHorariosViewModel.DIAS.toList()
    private var seleccionado = 1
    private var conteo: Map<Int, Int> = emptyMap()

    class Vista(val binding: ItemDiaBinding) : RecyclerView.ViewHolder(binding.root)

    fun actualizar(diaSeleccionado: Int, bloques: List<DisponibilidadDto>) {
        seleccionado = diaSeleccionado
        conteo = bloques.groupingBy { it.diaSemana }.eachCount()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Vista(ItemDiaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = dias.size

    override fun onBindViewHolder(holder: Vista, position: Int) {
        val dia = dias[position]
        val elegido = dia == seleccionado
        val ctx = holder.itemView.context
        with(holder.binding) {
            tvDia.text = NOMBRES_DIAS.getValue(dia).take(3)
            tvNumero.text = (conteo[dia] ?: 0).toString()
            // El día elegido se pinta de azul para que se note cuál se está viendo.
            root.setCardBackgroundColor(ContextCompat.getColor(ctx, if (elegido) R.color.primary else R.color.white))
            tvDia.setTextColor(ContextCompat.getColor(ctx, if (elegido) R.color.white else R.color.grey))
            tvNumero.setTextColor(ContextCompat.getColor(ctx, if (elegido) R.color.white else R.color.ink))
            val total = conteo[dia] ?: 0
            root.contentDescription = "${NOMBRES_DIAS[dia]}, $total " + if (total == 1) "horario" else "horarios"
            root.setOnClickListener { alElegir(dia) }
        }
    }
}

// Lista de horas (7:00 a 20:00) del día elegido. Cada fila dice si la hora está libre o qué se publicó.
class HorarioAdapter(
    private val alTocarLibre: (hora: Int) -> Unit,
    private val alTocarPublicado: (DisponibilidadDto) -> Unit,
) : RecyclerView.Adapter<HorarioAdapter.Vista>() {

    private val horas = TutorHorariosViewModel.HORAS.toList()
    private var publicados: Map<Int, DisponibilidadDto> = emptyMap()
    private var habilitado = true

    class Vista(val binding: ItemHorarioFilaBinding) : RecyclerView.ViewHolder(binding.root)

    // habilitado = false mientras se guarda un cambio, para no mandar dos cambios a la vez.
    fun actualizar(bloquesDelDia: List<DisponibilidadDto>, habilitado: Boolean) {
        publicados = bloquesDelDia.associateBy { it.horaInicio }
        this.habilitado = habilitado
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Vista(ItemHorarioFilaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = horas.size

    override fun onBindViewHolder(holder: Vista, position: Int) {
        val hora = horas[position]
        val bloque = publicados[hora]
        val ctx = holder.itemView.context
        val color = { id: Int -> ContextCompat.getColor(ctx, id) }

        holder.binding.hora.root.text = textoHora(hora)

        with(holder.binding.celda) {
            // Un enlace largo no debe romper la fila: se corta con "…".
            tvModalidad.maxLines = 1
            tvModalidad.ellipsize = TextUtils.TruncateAt.END
            tvModalidad.setPadding(24, 0, 24, 0)
            when {
                bloque == null -> {
                    tvModalidad.text = "+ Libre"
                    tvModalidad.setTextColor(color(R.color.grey_light))
                    root.setCardBackgroundColor(color(R.color.white))
                }
                // Presencial en azul y virtual en amarillo, igual que en el resto de la app.
                bloque.modalidad == "VIRTUAL" -> {
                    tvModalidad.text = "Virtual" + (bloque.lugarOEnlace?.let { " · $it" } ?: "")
                    tvModalidad.setTextColor(color(R.color.warning_text))
                    root.setCardBackgroundColor(color(R.color.accent_container))
                }
                else -> {
                    tvModalidad.text = "Presencial" + (bloque.lugarOEnlace?.let { " · $it" } ?: "")
                    tvModalidad.setTextColor(color(R.color.primary))
                    root.setCardBackgroundColor(color(R.color.primary_container))
                }
            }
            root.isEnabled = habilitado
            root.alpha = if (habilitado) 1f else 0.6f
            root.setOnClickListener {
                if (bloque == null) alTocarLibre(hora) else alTocarPublicado(bloque)
            }
        }
    }
}

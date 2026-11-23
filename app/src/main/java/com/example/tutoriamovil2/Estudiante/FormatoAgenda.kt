package com.example.tutoriamovil2.Estudiante

import android.content.Context
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.red.dto.DisponibilidadDto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val esHN = Locale("es", "HN")

private fun parsear(fecha: String) = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(fecha)

fun horaTexto(hora: Int) = String.format(Locale.US, "%02d:00", hora)

fun rangoHoras(inicio: Int, fin: Int) = "${horaTexto(inicio)} – ${horaTexto(fin)}"

/** "Vie" */
fun diaCorto(fecha: String): String =
    SimpleDateFormat("EEE", esHN).format(parsear(fecha)!!).trimEnd('.').replaceFirstChar { it.uppercase() }

/** "6" */
fun diaNumero(fecha: String): String = SimpleDateFormat("d", esHN).format(parsear(fecha)!!)

/** "Viernes 6 nov" */
fun fechaLarga(fecha: String): String =
    SimpleDateFormat("EEEE d MMM", esHN).format(parsear(fecha)!!).replace(".", "").replaceFirstChar { it.uppercase() }

/** El servidor manda "PRESENCIAL" / "VIRTUAL". */
fun modalidadTexto(ctx: Context, modalidad: String?): String = when (modalidad?.uppercase()) {
    "VIRTUAL" -> ctx.getString(R.string.modalidad_virtual)
    else -> ctx.getString(R.string.modalidad_presencial)
}

/** "Presencial · Aula 3". Sin lugar, solo la modalidad. */
fun modalidadYLugar(ctx: Context, modalidad: String?, lugar: String?): String =
    if (lugar.isNullOrBlank()) modalidadTexto(ctx, modalidad)
    else ctx.getString(R.string.formato_modalidad_lugar, modalidadTexto(ctx, modalidad), lugar)

/** "Viernes 6 nov · 14:00 – 15:00" */
fun fechaYHora(ctx: Context, fecha: String, inicio: Int, fin: Int): String =
    ctx.getString(R.string.formato_fecha_hora, fechaLarga(fecha), rangoHoras(inicio, fin))

/** Bloque concreto (una fecha y una hora) que el estudiante puede elegir. */
data class BloqueUi(
    val fecha: String,
    val horaInicio: Int,
    val horaFin: Int,
    val modalidad: String?,
    val lugarOEnlace: String?,
    val disponible: Boolean = true,
) {
    val clave get() = "$fecha#$horaInicio"
}

/**
 * El tutor publica un horario semanal (dia_semana 1=lunes..6=sábado, bloques de 1 h).
 * Lo convertimos en bloques con fecha para los próximos [dias] días; hoy solo cuentan las horas que aún no empezaron.
 */
fun expandirDisponibilidad(
    semanal: List<DisponibilidadDto>,
    dias: Int = 14,
    ahora: Calendar = Calendar.getInstance(),
): List<BloqueUi> {
    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dia = ahora.clone() as Calendar
    val salida = mutableListOf<BloqueUi>()
    for (i in 0 until dias) {
        val iso = (dia.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1 // domingo = 7, sin tutorías
        semanal.filter { it.diaSemana == iso }.sortedBy { it.horaInicio }.forEach { d ->
            val yaPaso = i == 0 && d.horaInicio <= ahora.get(Calendar.HOUR_OF_DAY)
            if (!yaPaso) {
                salida += BloqueUi(formato.format(dia.time), d.horaInicio, d.horaInicio + 1, d.modalidad, d.lugarOEnlace)
            }
        }
        dia.add(Calendar.DAY_OF_MONTH, 1)
    }
    return salida
}

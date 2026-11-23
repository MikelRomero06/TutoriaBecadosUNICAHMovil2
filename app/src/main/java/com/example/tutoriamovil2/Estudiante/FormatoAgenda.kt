package com.example.tutoriamovil2.Estudiante

import android.content.Context
import com.example.tutoriamovil2.R
import java.text.SimpleDateFormat
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

package com.example.tutoriamovil2.Estudiante

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.databinding.ItemTutoriaBinding
import com.example.tutoriamovil2.red.dto.TutoriaDto
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val ESPANOL = Locale("es", "HN")

/** Etiqueta y colores de la insignia de estado de una tutoría. */
data class EstiloEstado(val texto: String, @DrawableRes val fondo: Int, @ColorRes val color: Int)

fun estiloEstado(estado: String?, vistaTutor: Boolean = false): EstiloEstado = when (estado) {
    "CONFIRMADA" -> EstiloEstado("Confirmada", R.drawable.bg_pill_blue, R.color.primary)
    "COMPLETADA" -> EstiloEstado("Completada", R.drawable.bg_pill_green, R.color.success)
    "SIN_CONFIRMAR" -> EstiloEstado("Sin confirmar", R.drawable.bg_pill_yellow, R.color.warning_text)
    "NO_ASISTIO_TUTOR" -> EstiloEstado(if (vistaTutor) "No asististe" else "No asistió el tutor", R.drawable.bg_pill_red, R.color.error)
    "NO_ASISTIO_ESTUDIANTE" -> EstiloEstado(if (vistaTutor) "No asistió el estudiante" else "No asististe", R.drawable.bg_pill_red, R.color.error)
    "CANCELADA_ESTUDIANTE" -> EstiloEstado(if (vistaTutor) "Cancelada por el estudiante" else "Cancelada por vos", R.drawable.bg_pill_red, R.color.error)
    "CANCELADA_TUTOR" -> EstiloEstado(if (vistaTutor) "Cancelada por vos" else "Cancelada por el tutor", R.drawable.bg_pill_red, R.color.error)
    "CANCELADA_ADMIN" -> EstiloEstado("Cancelada", R.drawable.bg_pill_red, R.color.error)
    else -> EstiloEstado(estado?.lowercase()?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "—",
        R.drawable.bg_pill_white, R.color.grey)
}

/** "Hoy", "Mañana" o "Vie 6 nov", a partir de una fecha "yyyy-MM-dd". */
fun fechaCorta(fecha: String?): String {
    if (fecha.isNullOrBlank()) return ""
    val dia = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(fecha) }
        .getOrNull() ?: return fecha
    val objetivo = Calendar.getInstance().apply { time = dia }
    val hoy = Calendar.getInstance()
    val diferencia = diasEntre(hoy, objetivo)
    return when (diferencia) {
        0 -> "Hoy"
        1 -> "Mañana"
        -1 -> "Ayer"
        else -> SimpleDateFormat("EEE d MMM", ESPANOL).format(dia)
            .replace(".", "")
            .replaceFirstChar { it.uppercase() }
    }
}

private fun diasEntre(desde: Calendar, hasta: Calendar): Int {
    fun inicio(c: Calendar) = (c.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return Math.round((inicio(hasta) - inicio(desde)) / 86_400_000.0).toInt()
}

fun horario(inicio: Int?, fin: Int?): String {
    if (inicio == null) return ""
    val hasta = fin ?: (inicio + 1)
    return String.format(Locale.US, "%02d:00 – %02d:00", inicio, hasta)
}

fun textoModalidad(modalidad: String?): String = when (modalidad) {
    "PRESENCIAL" -> "Presencial"
    "VIRTUAL" -> "Virtual"
    else -> modalidad.orEmpty()
}

const val ARG_TUTORIA = "tutoria_json"

/** Argumentos para abrir el detalle (`estudianteTutoriaSolicitada`) de una tutoría. */
fun TutoriaDto.comoArgumentos() = Bundle().apply { putString(ARG_TUTORIA, Gson().toJson(this@comoArgumentos)) }

/** Solo se cancela lo que aún no ocurrió ni fue cancelado. */
fun puedeCancelar(estado: String?) = estado == "CONFIRMADA" || estado == "SIN_CONFIRMAR"

fun NavController.irACancelar(tutoriaId: Long) =
    navigate(R.id.cancelarTutoria, Bundle().apply { putLong(CancelarTutoria.ARG_TUTORIA_ID, tutoriaId) })

/**
 * Llena una tarjeta `item_tutoria`. `onCancelar` muestra el botón Cancelar (solo si el estado
 * lo permite); confirmar y reportar siguen ocultos hasta que el servidor publique esos endpoints.
 */
fun ItemTutoriaBinding.mostrarTutoria(
    tutoria: TutoriaDto,
    onClick: ((TutoriaDto) -> Unit)? = null,
    onCancelar: ((TutoriaDto) -> Unit)? = null,
    mostrarFoto: (ImageView, String?) -> Unit,
) {
    val contexto = root.context
    tvNombre.text = tutoria.tutor?.name?.ifBlank { null } ?: "Tutor"

    val materia = tutoria.materia?.nombre
    val cuando = listOf(fechaCorta(tutoria.fecha), horario(tutoria.horaInicio, tutoria.horaFin))
        .filter { it.isNotBlank() }
        .joinToString(" ")
    tvDetalle.text = listOfNotNull(materia, cuando.ifBlank { null }).joinToString(" · ")

    val estilo = estiloEstado(tutoria.estado)
    tvEstado.text = estilo.texto
    tvEstado.setBackgroundResource(estilo.fondo)
    tvEstado.setTextColor(ContextCompat.getColor(contexto, estilo.color))

    val partes = listOfNotNull(
        textoModalidad(tutoria.modalidad).ifBlank { null },
        tutoria.lugarOEnlace?.ifBlank { null },
    )
    val tema = tutoria.tema?.ifBlank { null }
    tvModalidad.text = buildString {
        if (tema != null) append(tema)
        if (tema != null && partes.isNotEmpty()) append("\n")
        append(partes.joinToString(" · "))
    }
    tvModalidad.isVisible = tvModalidad.text.isNotBlank()

    val cancelable = onCancelar != null && puedeCancelar(tutoria.estado)
    btnConfirmar.isVisible = false
    btnReportar.isVisible = false
    btnCancelar.isVisible = cancelable
    btnCancelar.setOnClickListener { onCancelar?.invoke(tutoria) }
    filaAcciones.isVisible = cancelable
    root.setOnClickListener(onClick?.let { abrir -> View.OnClickListener { abrir(tutoria) } })

    mostrarFoto(ivFoto, tutoria.tutor?.fotoUrl)
}

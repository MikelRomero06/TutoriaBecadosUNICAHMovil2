package com.example.tutoriamovil2.Estudiante

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import java.util.TimeZone

private val ESPANOL = Locale("es", "HN")

/** Etiqueta y colores de la insignia de estado de una tutoría. */
data class EstiloEstado(val texto: String, @DrawableRes val fondo: Int, @ColorRes val color: Int)

fun estiloEstado(ctx: Context, estado: String?, vistaTutor: Boolean = false): EstiloEstado {
    fun rojo(@StringRes texto: Int) = EstiloEstado(ctx.getString(texto), R.drawable.bg_pill_red, R.color.error)

    return when (estado) {
        "CONFIRMADA" -> EstiloEstado(ctx.getString(R.string.estado_confirmada), R.drawable.bg_pill_blue, R.color.primary)
        "COMPLETADA" -> EstiloEstado(ctx.getString(R.string.estado_completada), R.drawable.bg_pill_green, R.color.success)
        "SIN_CONFIRMAR" -> EstiloEstado(ctx.getString(R.string.estado_sin_confirmar), R.drawable.bg_pill_yellow, R.color.warning_text)
        "NO_ASISTIO_TUTOR" -> rojo(if (vistaTutor) R.string.estado_no_asististe else R.string.estado_no_asistio_tutor)
        "NO_ASISTIO_ESTUDIANTE" -> rojo(if (vistaTutor) R.string.estado_no_asistio_estudiante else R.string.estado_no_asististe)
        "CANCELADA_ESTUDIANTE" -> rojo(if (vistaTutor) R.string.estado_cancelada_estudiante else R.string.estado_cancelada_por_vos)
        "CANCELADA_TUTOR" -> rojo(if (vistaTutor) R.string.estado_cancelada_por_vos else R.string.estado_cancelada_tutor)
        "CANCELADA_ADMIN" -> rojo(R.string.estado_cancelada)
        else -> EstiloEstado(estado?.lowercase()?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "—",
            R.drawable.bg_pill_white, R.color.grey)
    }
}

/** Igual que [fechaCorta], con "Hoy", "Mañana" y "Ayer" tomados de strings.xml. */
fun fechaCorta(ctx: Context, fecha: String?): String = fechaCorta(
    fecha,
    ctx.getString(R.string.fecha_hoy),
    ctx.getString(R.string.fecha_manana),
    ctx.getString(R.string.fecha_ayer),
)

/** "Hoy", "Mañana" o "Vie 6 nov", a partir de una fecha "yyyy-MM-dd". */
fun fechaCorta(fecha: String?, hoyTexto: String = "Hoy", mananaTexto: String = "Mañana", ayerTexto: String = "Ayer"): String {
    if (fecha.isNullOrBlank()) return ""
    val dia = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(fecha) }
        .getOrNull() ?: return fecha
    val objetivo = Calendar.getInstance().apply { time = dia }
    val hoy = Calendar.getInstance()
    val diferencia = diasEntre(hoy, objetivo)
    return when (diferencia) {
        0 -> hoyTexto
        1 -> mananaTexto
        -1 -> ayerTexto
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

fun textoModalidad(ctx: Context, modalidad: String?): String = when (modalidad) {
    "PRESENCIAL" -> ctx.getString(R.string.modalidad_presencial)
    "VIRTUAL" -> ctx.getString(R.string.modalidad_virtual)
    else -> modalidad.orEmpty()
}

const val ARG_TUTORIA = "tutoria_json"

/** Argumentos para abrir el detalle (`estudianteTutoriaSolicitada`) de una tutoría. */
fun TutoriaDto.comoArgumentos() = Bundle().apply { putString(ARG_TUTORIA, Gson().toJson(this@comoArgumentos)) }

/** Igual que en el servidor: solo se cancela una tutoría que sigue confirmada. */
fun puedeCancelar(estado: String?) = estado == "CONFIRMADA"

fun NavController.irACancelar(tutoriaId: Long) =
    navigate(R.id.cancelarTutoria, Bundle().apply { putLong(CancelarTutoria.ARG_TUTORIA_ID, tutoriaId) })

// Las horas de las tutorías vienen en hora de Honduras, igual que en el servidor (config/app.php).
private val ZONA_SERVIDOR: TimeZone = TimeZone.getTimeZone("America/Tegucigalpa")

// Plazo del servidor (parámetro horas_para_sin_confirmar). Si admin lo cambia, el servidor manda igual.
private const val HORAS_PARA_REPORTAR = 48

/** Momento (en milisegundos) en que empieza la tutoría, o null si faltan datos. */
private fun inicioEnMillis(tutoria: TutoriaDto): Long? {
    val hora = tutoria.horaInicio ?: return null
    val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false; timeZone = ZONA_SERVIDOR }
    val dia = tutoria.fecha?.let { runCatching { formato.parse(it) }.getOrNull() } ?: return null
    return Calendar.getInstance(ZONA_SERVIDOR).apply {
        time = dia
        set(Calendar.HOUR_OF_DAY, hora)
    }.timeInMillis
}

/** True si la tutoría ya empezó; desde ese momento ya no se cancela, se reporta. */
fun yaEmpezo(tutoria: TutoriaDto, ahora: Long = System.currentTimeMillis()): Boolean =
    inicioEnMillis(tutoria)?.let { ahora >= it } ?: false

/**
 * Misma ventana que el servidor: desde que empieza la tutoría hasta 48 h después de que termina
 * (dura 1 h). Solo sirve para mostrar el botón; si el plazo cambió, el servidor responde con el error.
 */
fun puedeReportar(tutoria: TutoriaDto, ahora: Long = System.currentTimeMillis()): Boolean {
    if (tutoria.estado != "CONFIRMADA") return false
    val inicio = inicioEnMillis(tutoria) ?: return false
    val cierre = inicio + (1 + HORAS_PARA_REPORTAR) * 3_600_000L
    return ahora in inicio..cierre
}

/** Realizada y todavía sin evaluación del estudiante: es cuando aparece el botón Calificar. */
fun puedeCalificar(tutoria: TutoriaDto) = tutoria.estado == "COMPLETADA" && tutoria.calificada != true

fun NavController.irACalificar(tutoria: TutoriaDto) = navigate(R.id.calificarTutoria, tutoria.comoArgumentos())

/** Abre la pantalla de reporte; `esTutor` cambia los textos (a quién se reporta). */
fun NavController.irAReportar(tutoriaId: Long, esTutor: Boolean) =
    navigate(R.id.reportarInasistencia, Bundle().apply {
        putLong(ReportarInasistencia.ARG_TUTORIA_ID, tutoriaId)
        putBoolean(ReportarInasistencia.ARG_ES_TUTOR, esTutor)
    })

/**
 * Llena una tarjeta `item_tutoria`. Antes de que empiece se puede cancelar; una vez empezada
 * aparece Reportar inasistencia (dentro del plazo); en las realizadas sin evaluar, Calificar.
 * Confirmar asistencia es solo del tutor.
 */
fun ItemTutoriaBinding.mostrarTutoria(
    tutoria: TutoriaDto,
    onClick: ((TutoriaDto) -> Unit)? = null,
    onCancelar: ((TutoriaDto) -> Unit)? = null,
    onReportar: ((TutoriaDto) -> Unit)? = null,
    onCalificar: ((TutoriaDto) -> Unit)? = null,
    mostrarFoto: (ImageView, String?) -> Unit,
) {
    val contexto = root.context
    tvNombre.text = tutoria.tutor?.name?.ifBlank { null } ?: contexto.getString(R.string.detalle_tutor)

    val materia = tutoria.materia?.nombre
    val cuando = listOf(fechaCorta(contexto, tutoria.fecha), horario(tutoria.horaInicio, tutoria.horaFin))
        .filter { it.isNotBlank() }
        .joinToString(" ")
    tvDetalle.text = listOfNotNull(materia, cuando.ifBlank { null }).joinToString(" · ")

    val estilo = estiloEstado(contexto, tutoria.estado)
    tvEstado.text = estilo.texto
    tvEstado.setBackgroundResource(estilo.fondo)
    tvEstado.setTextColor(ContextCompat.getColor(contexto, estilo.color))

    val partes = listOfNotNull(
        textoModalidad(contexto, tutoria.modalidad).ifBlank { null },
        tutoria.lugarOEnlace?.ifBlank { null },
    )
    val tema = tutoria.tema?.ifBlank { null }
    tvModalidad.text = buildString {
        if (tema != null) append(tema)
        if (tema != null && partes.isNotEmpty()) append("\n")
        append(partes.joinToString(" · "))
    }
    tvModalidad.isVisible = tvModalidad.text.isNotBlank()

    val cancelable = onCancelar != null && puedeCancelar(tutoria.estado) && !yaEmpezo(tutoria)
    val reportable = onReportar != null && puedeReportar(tutoria)
    btnConfirmar.isVisible = false
    btnReportar.isVisible = reportable
    btnReportar.setOnClickListener { onReportar?.invoke(tutoria) }
    btnCancelar.isVisible = cancelable
    btnCancelar.setOnClickListener { onCancelar?.invoke(tutoria) }
    val calificable = onCalificar != null && puedeCalificar(tutoria)
    btnCalificar.isVisible = calificable
    btnCalificar.setOnClickListener { onCalificar?.invoke(tutoria) }
    filaAcciones.isVisible = cancelable || reportable || calificable
    root.setOnClickListener(onClick?.let { abrir -> View.OnClickListener { abrir(tutoria) } })

    mostrarFoto(ivFoto, tutoria.tutor?.fotoUrl)
}

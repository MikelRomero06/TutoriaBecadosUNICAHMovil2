package com.example.tutoriamovil2.Estudiante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.AuthRepositorio
import com.example.tutoriamovil2.data.FiltroTutores
import com.example.tutoriamovil2.data.InicioRepositorio
import com.example.tutoriamovil2.data.TutoriasRepositorio
import com.example.tutoriamovil2.data.mensajeError
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.TutorDto
import com.example.tutoriamovil2.red.dto.TutoriaDto
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/** Cada sección del inicio se carga por separado: si una falla, las demás se muestran igual. */
data class DatosInicio(
    val nombre: String?,
    val carrera: String?,
    val citaciones: List<AvisoInicio>,
    val sinLeer: Int,
    val proxima: TutoriaDto?,
    val proximaError: String?,
    val tutores: List<TutorDto>,
    val tutoresError: String?,
)

sealed class EstadoInicio {
    data object Cargando : EstadoInicio()
    data class Listo(val datos: DatosInicio) : EstadoInicio()
    data class Error(val mensaje: String) : EstadoInicio()
}

class InicioEstudianteViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = AuthRepositorio(ApiCliente.auth(app))
    private val avisos = InicioRepositorio(ApiCliente.notificaciones(app))
    private val tutorias = TutoriasRepositorio(ApiCliente.tutorias(app))

    private val _estado = MutableLiveData<EstadoInicio>(EstadoInicio.Cargando)
    val estado: LiveData<EstadoInicio> = _estado

    private val _aviso = MutableLiveData<String?>()
    val aviso: LiveData<String?> = _aviso

    fun cargar() {
        if (_estado.value !is EstadoInicio.Listo) _estado.value = EstadoInicio.Cargando
        viewModelScope.launch {
            val yo = async { auth.yo() }
            val citaciones = async { avisos.citaciones() }
            val notificaciones = async { avisos.notificaciones() }
            val proximas = async { tutorias.misTutorias("proximas", 1) }
            // Sin carrera en el filtro: el servidor usa la del usuario.
            val tutores = async { tutorias.obtenerTutores(FiltroTutores(), 1, deMiCarrera = true) }

            val resYo = yo.await()
            val resCitaciones = citaciones.await()
            val resNotificaciones = notificaciones.await()
            val resProximas = proximas.await()
            val resTutores = tutores.await()

            val todas = listOf(resYo, resCitaciones, resNotificaciones, resProximas, resTutores)
            if (todas.none { it is Resultado.Exito }) {
                _estado.value = EstadoInicio.Error(resYo.mensajeError())
                return@launch
            }

            val usuario = (resYo as? Resultado.Exito)?.dato?.usuario
            _estado.value = EstadoInicio.Listo(
                DatosInicio(
                    nombre = usuario?.name?.trim()?.substringBefore(' ')?.ifBlank { null },
                    carrera = usuario?.carrera?.nombre,
                    citaciones = (resCitaciones as? Resultado.Exito)?.dato.orEmpty()
                        .filter { !it.enterado }
                        .map(::avisoDesdeCitacion),
                    sinLeer = (resNotificaciones as? Resultado.Exito)?.dato.orEmpty().count { !it.leida },
                    proxima = (resProximas as? Resultado.Exito)?.dato?.data?.firstOrNull(),
                    proximaError = errorSeccion(resProximas),
                    tutores = (resTutores as? Resultado.Exito)?.dato?.data.orEmpty().take(3),
                    tutoresError = errorSeccion(resTutores),
                ),
            )
        }
    }

    fun marcarEnterado(citacionId: Long) {
        viewModelScope.launch {
            when (val res = avisos.marcarEnterado(citacionId)) {
                is Resultado.Exito -> cargar()
                else -> _aviso.value = res.mensajeError()
            }
        }
    }

    fun avisoMostrado() {
        _aviso.value = null
    }

    /** Un 404 es un endpoint que todavía no existe: se trata como sección vacía, no como error. */
    private fun errorSeccion(res: Resultado<*>): String? = when (res) {
        is Resultado.Exito -> null
        is Resultado.ErrorApi -> if (res.codigo == 404) null else res.mensaje
        else -> res.mensajeError()
    }
}

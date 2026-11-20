package com.example.tutoriamovil2.Login

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.RegistroRepositorio
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.SesionStore
import com.example.tutoriamovil2.red.dto.CarreraDto
import com.example.tutoriamovil2.red.dto.CompletarRegistroPeticion
import kotlinx.coroutines.launch

/**
 * ViewModel compartido por el grafo de login: lleva el correo y el token de registro
 * entre las tres pantallas (correo → código → completar perfil).
 */
class RegistroViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = RegistroRepositorio(ApiCliente.registro(app))
    private val dispositivo = Build.MODEL ?: "Android"

    /** Correo completo (`usuario@unicah.edu`) que se está registrando. */
    var correo: String = ""
        private set

    private var registroToken: String = ""

    var carreras: List<CarreraDto> = emptyList()
        private set

    /** Segundos a esperar antes de poder reenviar el código (lo informa el servidor). */
    var reenviarEnSegundos: Int = 0
        private set

    /** Queda en true cuando la cuenta ya se creó, para que un reintento solo suba la foto. */
    var cuentaCreada: Boolean = false
        private set

    private val _correoEstado = MutableLiveData<PasoRegistro>(PasoRegistro.Inicial)
    val correoEstado: LiveData<PasoRegistro> = _correoEstado

    private val _codigoEstado = MutableLiveData<PasoRegistro>(PasoRegistro.Inicial)
    val codigoEstado: LiveData<PasoRegistro> = _codigoEstado

    private val _carrerasEstado = MutableLiveData<PasoRegistro>(PasoRegistro.Inicial)
    val carrerasEstado: LiveData<PasoRegistro> = _carrerasEstado

    private val _completarEstado = MutableLiveData<PasoRegistro>(PasoRegistro.Inicial)
    val completarEstado: LiveData<PasoRegistro> = _completarEstado

    // ---------- Paso 1: enviar código ----------

    fun enviarCodigo(usuario: String) {
        val limpio = usuario.trim().lowercase()
        if (limpio.isEmpty()) {
            _correoEstado.value = PasoRegistro.Error("Escribí tu usuario de correo.")
            return
        }
        correo = "$limpio@unicah.edu"
        _correoEstado.value = PasoRegistro.Cargando
        viewModelScope.launch {
            when (val r = repo.enviarCodigo(correo)) {
                is Resultado.Exito -> {
                    reenviarEnSegundos = r.dato.reenviarEnSegundos ?: 60
                    _correoEstado.value = PasoRegistro.Ok
                }
                else -> _correoEstado.value = PasoRegistro.Error(mensajeDe(r))
            }
        }
    }

    fun limpiarCorreo() {
        _correoEstado.value = PasoRegistro.Inicial
    }

    // ---------- Paso 2: verificar / reenviar código ----------

    fun verificarCodigo(codigo: String) {
        if (codigo.length != 6) {
            _codigoEstado.value = PasoRegistro.Error("Escribí los 6 dígitos del código.")
            return
        }
        _codigoEstado.value = PasoRegistro.Cargando
        viewModelScope.launch {
            when (val r = repo.verificarCodigo(correo, codigo)) {
                is Resultado.Exito -> {
                    registroToken = r.dato.registroToken
                    _codigoEstado.value = PasoRegistro.Ok
                }
                else -> _codigoEstado.value = PasoRegistro.Error(mensajeDe(r))
            }
        }
    }

    fun reenviarCodigo() {
        _codigoEstado.value = PasoRegistro.Cargando
        viewModelScope.launch {
            when (val r = repo.enviarCodigo(correo)) {
                is Resultado.Exito -> {
                    reenviarEnSegundos = r.dato.reenviarEnSegundos ?: 60
                    _codigoEstado.value = PasoRegistro.Reenviado
                }
                else -> _codigoEstado.value = PasoRegistro.Error(mensajeDe(r))
            }
        }
    }

    fun limpiarCodigo() {
        _codigoEstado.value = PasoRegistro.Inicial
    }

    // ---------- Paso 3: carreras + completar + foto ----------

    fun cargarCarreras() {
        if (carreras.isNotEmpty()) {
            _carrerasEstado.value = PasoRegistro.Ok
            return
        }
        _carrerasEstado.value = PasoRegistro.Cargando
        viewModelScope.launch {
            when (val r = repo.carreras()) {
                is Resultado.Exito -> {
                    carreras = r.dato.data
                    _carrerasEstado.value = PasoRegistro.Ok
                }
                else -> _carrerasEstado.value = PasoRegistro.Error(mensajeDe(r))
            }
        }
    }

    fun crearCuenta(
        nombre: String,
        carreraId: Long,
        telefono: String,
        password: String,
        confirmar: String,
        fotoBytes: ByteArray?,
        fotoNombre: String,
        fotoMime: String,
    ) {
        val nombreLimpio = nombre.trim()
        val telefonoDigitos = telefono.filter { it.isDigit() }
        when {
            nombreLimpio.isEmpty() -> return error("El nombre es obligatorio.")
            carreraId <= 0L -> return error("Elegí tu carrera.")
            telefonoDigitos.length != 8 -> return error("El teléfono debe tener 8 dígitos.")
            password.isEmpty() -> return error("La contraseña es obligatoria.")
            password != confirmar -> return error("Las contraseñas no coinciden.")
            fotoBytes == null && !cuentaCreada -> return error("La foto es obligatoria.")
            fotoBytes == null -> return error("Elegí una foto para terminar.")
        }

        _completarEstado.value = PasoRegistro.Cargando
        viewModelScope.launch {
            if (!cuentaCreada) {
                val r = repo.completar(
                    CompletarRegistroPeticion(
                        registroToken = registroToken,
                        name = nombreLimpio,
                        carreraId = carreraId,
                        telefono = telefonoDigitos,
                        password = password,
                        passwordConfirmation = confirmar,
                        deviceName = dispositivo,
                    ),
                )
                if (r is Resultado.Exito) {
                    SesionStore.guardarToken(r.dato.token)
                    cuentaCreada = true
                } else {
                    _completarEstado.value = PasoRegistro.Error(mensajeDe(r))
                    return@launch
                }
            }

            when (repo.subirFoto(fotoBytes!!, fotoNombre, fotoMime)) {
                is Resultado.Exito -> _completarEstado.value = PasoRegistro.Ok
                else -> _completarEstado.value = PasoRegistro.Error(
                    "Tu cuenta se creó, pero la foto no se pudo subir. Probá de nuevo.",
                )
            }
        }
    }

    fun limpiarCompletar() {
        _completarEstado.value = PasoRegistro.Inicial
    }

    /**
     * Reinicia el estado del registro. Como ahora el ViewModel vive mientras viva la
     * Activity de login (activityViewModels), hay que limpiarlo al terminar para que
     * un registro nuevo empiece desde cero.
     */
    fun reiniciar() {
        correo = ""
        registroToken = ""
        carreras = emptyList()
        reenviarEnSegundos = 0
        cuentaCreada = false
        _correoEstado.value = PasoRegistro.Inicial
        _codigoEstado.value = PasoRegistro.Inicial
        _carrerasEstado.value = PasoRegistro.Inicial
        _completarEstado.value = PasoRegistro.Inicial
    }

    private fun error(mensaje: String) {
        _completarEstado.value = PasoRegistro.Error(mensaje)
    }

    private fun mensajeDe(resultado: Resultado<*>): String = when (resultado) {
        is Resultado.ErrorValidacion ->
            resultado.errores.values.firstOrNull()?.firstOrNull() ?: resultado.mensaje
        is Resultado.NoAutorizado -> "No autorizado. Pedí un código nuevo."
        is Resultado.ErrorApi -> resultado.mensaje
        is Resultado.ErrorRed -> resultado.mensaje
        is Resultado.Exito -> ""
    }
}

sealed class PasoRegistro {
    data object Inicial : PasoRegistro()
    data object Cargando : PasoRegistro()
    data object Ok : PasoRegistro()
    data object Reenviado : PasoRegistro()
    data class Error(val mensaje: String) : PasoRegistro()
}

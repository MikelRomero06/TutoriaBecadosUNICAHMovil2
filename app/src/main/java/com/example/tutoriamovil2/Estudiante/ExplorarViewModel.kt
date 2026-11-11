package com.example.tutoriamovil2.Estudiante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.AuthRepositorio
import com.example.tutoriamovil2.data.FiltroTutores
import com.example.tutoriamovil2.data.TutoriasRepositorio
import com.example.tutoriamovil2.data.mensajeError
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.CarreraDto
import com.example.tutoriamovil2.red.dto.MateriaDto
import com.example.tutoriamovil2.red.dto.TutorDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class EstadoExplorar {
    data object Cargando : EstadoExplorar()
    data class Exito(val tutores: List<TutorDto>, val total: Int, val cargandoMas: Boolean) : EstadoExplorar()
    data object Vacio : EstadoExplorar()
    data class Error(val mensaje: String) : EstadoExplorar()
}

/** Lo que muestran los chips: si filtra por la carrera propia y la materia elegida. */
data class FiltrosVisibles(
    val miCarrera: CarreraDto?,
    val soloMiCarrera: Boolean,
    val materia: MateriaDto?,
    val modalidad: String?,
)

class ExplorarViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TutoriasRepositorio(ApiCliente.tutorias(app))
    private val auth = AuthRepositorio(ApiCliente.auth(app))

    private val _estado = MutableLiveData<EstadoExplorar>(EstadoExplorar.Cargando)
    val estado: LiveData<EstadoExplorar> = _estado

    private val _filtros = MutableLiveData(FiltrosVisibles(null, true, null, null))
    val filtros: LiveData<FiltrosVisibles> = _filtros

    private var miCarrera: CarreraDto? = null
    private var soloMiCarrera = true
    private var materia: MateriaDto? = null
    private var modalidad: String? = null
    private var busqueda = ""

    private val tutores = mutableListOf<TutorDto>()
    private var pagina = 1
    private var ultimaPagina = 1
    private var total = 0
    private var consulta: Job? = null
    private var iniciado = false

    /** Primera carga: averigua la carrera del usuario y lista los tutores de esa carrera. */
    fun iniciar() {
        if (iniciado) return
        iniciado = true
        _estado.value = EstadoExplorar.Cargando
        viewModelScope.launch {
            val yo = auth.yo()
            if (yo is Resultado.Exito) {
                miCarrera = yo.dato.usuario.carrera
                    ?: yo.dato.usuario.carreraId?.let { CarreraDto(it, "Mi carrera", null) }
            }
            // Sin carrera conocida (por ejemplo, una cuenta admin) se muestran todas.
            if (miCarrera == null) soloMiCarrera = false
            publicarFiltros()
            consultar(espera = 0L)
        }
    }

    fun reintentar() = consultar(espera = 0L)

    fun buscar(texto: String) {
        val limpio = texto.trim()
        if (limpio == busqueda) return
        busqueda = limpio
        // Espera a que la persona deje de escribir antes de consultar.
        consultar(espera = 350L)
    }

    fun usarMiCarrera(soloLaMia: Boolean) {
        val valor = soloLaMia && miCarrera != null
        if (valor == soloMiCarrera) return
        soloMiCarrera = valor
        // Las materias dependen de la carrera: al cambiarla se quita la materia elegida.
        materia = null
        publicarFiltros()
        consultar(espera = 0L)
    }

    fun elegirMateria(nueva: MateriaDto?) {
        if (nueva?.id == materia?.id) return
        materia = nueva
        publicarFiltros()
        consultar(espera = 0L)
    }

    fun filtrarModalidad(nueva: String?) {
        if (nueva == modalidad) return
        modalidad = nueva
        publicarFiltros()
        consultar(espera = 0L)
    }

    /** Materias para el filtro: las de la carrera propia, o todas si se ven todas las carreras. */
    suspend fun materiasParaFiltro(): Resultado<List<MateriaDto>> =
        repo.obtenerMaterias(if (soloMiCarrera) miCarrera?.id else null)

    fun cargarMas() {
        val actual = _estado.value
        if (actual !is EstadoExplorar.Exito || actual.cargandoMas || pagina >= ultimaPagina) return
        if (consulta?.isActive == true) return

        _estado.value = actual.copy(cargandoMas = true)
        consulta = viewModelScope.launch {
            when (val res = repo.obtenerTutores(filtroActual(), pagina + 1)) {
                is Resultado.Exito -> {
                    pagina = res.dato.meta?.paginaActual ?: (pagina + 1)
                    ultimaPagina = res.dato.meta?.ultimaPagina ?: pagina
                    tutores.addAll(res.dato.data.filter { nuevo -> tutores.none { it.id == nuevo.id } })
                    publicarLista()
                }
                // Si falla la página siguiente se conserva lo ya cargado.
                else -> _estado.value = actual.copy(cargandoMas = false)
            }
        }
    }

    private fun consultar(espera: Long) {
        consulta?.cancel()
        consulta = viewModelScope.launch {
            if (espera > 0) delay(espera)
            _estado.value = EstadoExplorar.Cargando
            when (val res = repo.obtenerTutores(filtroActual(), 1)) {
                is Resultado.Exito -> {
                    tutores.clear()
                    tutores.addAll(res.dato.data)
                    pagina = res.dato.meta?.paginaActual ?: 1
                    ultimaPagina = res.dato.meta?.ultimaPagina ?: 1
                    total = res.dato.meta?.total ?: res.dato.data.size
                    publicarLista()
                }
                // Mientras el servidor no tenga publicado el listado, se ve vacío en lugar de un error.
                is Resultado.ErrorApi -> _estado.value =
                    if (res.codigo == 404) EstadoExplorar.Vacio else EstadoExplorar.Error(res.mensaje)
                else -> _estado.value = EstadoExplorar.Error(res.mensajeError())
            }
        }
    }

    private fun publicarLista() {
        _estado.value = if (tutores.isEmpty()) {
            EstadoExplorar.Vacio
        } else {
            EstadoExplorar.Exito(tutores.toList(), maxOf(total, tutores.size), cargandoMas = false)
        }
    }

    private fun publicarFiltros() {
        _filtros.value = FiltrosVisibles(miCarrera, soloMiCarrera, materia, modalidad)
    }

    private fun filtroActual() = FiltroTutores(
        carreraId = if (soloMiCarrera) miCarrera?.id else null,
        materiaId = materia?.id,
        modalidad = modalidad,
        buscar = busqueda,
    )
}

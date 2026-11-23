package com.example.tutoriamovil2.red

/** Resultado de una llamada a la API, con los casos de error que distingue el contrato. */
sealed class Resultado<out T> {
    data class Exito<T>(val dato: T) : Resultado<T>()

    /** 422: validación, con el mensaje y los errores por campo. */
    data class ErrorValidacion(
        val mensaje: String,
        val errores: Map<String, List<String>>,
    ) : Resultado<Nothing>()

    /** 401: sin sesión o token vencido. */
    data object NoAutorizado : Resultado<Nothing>()

    /** 403, 404, 409, 429, 5xx u otro error del servidor con mensaje. */
    data class ErrorApi(val codigo: Int, val mensaje: String) : Resultado<Nothing>() {
        /** 409: el recurso cambió (p. ej. el bloque ya lo reservó otra persona). */
        val esConflicto: Boolean get() = codigo == 409

        /** 404: el recurso ya no existe. */
        val esNoEncontrado: Boolean get() = codigo == 404
    }

    /** No se pudo contactar al servidor. */
    data class ErrorRed(val mensaje: String) : Resultado<Nothing>()
}

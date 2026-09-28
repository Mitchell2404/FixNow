package com.fixnow.app.core.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Ejecuta [block] y devuelve un [Result] en vez de lanzar excepciones.
 * Si algo falla, el mensaje del error se traduce con [errorMapper].
 * Las cancelaciones de corrutinas se relanzan (no se deben "tragar").
 */
suspend inline fun <T> safeCall(
    errorMapper: (Throwable) -> String = { it.message ?: "Ocurrió un error inesperado" },
    block: suspend () -> T
): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(Exception(errorMapper(e), e))
    }

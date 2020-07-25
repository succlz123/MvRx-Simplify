package org.succlz123.mvrx.result

import java.util.*

sealed class Result<out T>(val complete: Boolean, val shouldLoad: Boolean, private val value: T?) {

    /**
     * Returns the value or null.
     *
     * Success always have a value. Loading and Fail can also return a value which is useful for
     * pagination or progressive data loading.
     *
     * Can be invoked as an operator like: `yourProp()`
     */
    open operator fun invoke(): T? = value
}

object Uninitialized : Result<Nothing>(complete = false, shouldLoad = true, value = null),
    Incomplete

data class Loading<out T>(private val value: T? = null) :
    Result<T>(complete = false, shouldLoad = false, value = value),
    Incomplete

data class Success<out T>(private val value: T) :
    Result<T>(complete = true, shouldLoad = false, value = value) {

    override operator fun invoke(): T = value

    /**
     * Optional information about the value.
     * This is intended to support tooling (eg logging).
     * It allows data about the original Observable to be kept and accessed later. For example,
     * you could map a network request to just the data you need in the value, but your base layers could
     * keep metadata about the request, like timing, for logging.
     */
    var metadata: Any? = null
}

data class FailDataNone(val errorMessage: String? = null) :
    Result<Nothing>(complete = true, shouldLoad = true, value = null),
    Fail

data class FailDataError<out T>(val errorMessage: String? = null, private val value: T? = null) :
    Result<T>(complete = true, shouldLoad = true, value = value),
    Fail

data class FailException<out T>(val error: Throwable, private val value: T? = null) :
    Result<T>(complete = true, shouldLoad = true, value = value),
    Fail {
    override fun equals(other: Any?): Boolean {
        if (other !is FailException<*>) {
            return false
        }
        val otherError = other.error
        return error::class == otherError::class &&
                error.message == otherError.message &&
                error.stackTrace.firstOrNull() == otherError.stackTrace.firstOrNull()
    }

    override fun hashCode(): Int = Arrays.hashCode(arrayOf(error::class, error.message, error.stackTrace[0]))
}

/**
 * Helper interface for using Async in a when clause for handling both Uninitialized and Loading.
 *
 * With this, you can do:
 * when (data) {
 *     is Incomplete -> Unit
 *     is Success    -> Unit
 *     is Fail       -> Unit
 * }
 */
interface Incomplete

interface Fail

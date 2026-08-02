package com.gondroid.core.domain.util

sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : com.gondroid.core.domain.util.Error>(val error: E) : Result<Nothing, E>
}

inline fun <T, E : Error, R> Result<T, E>.map(map: (T) -> R): Result<R, E> {
    return when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(map(data))
    }
}

/**
 *  This function converts any Result<T, E> into a Result<Unit, E>,
 *  meaning a result without data.
 */

fun <T, E : Error> Result<T, E>.asEmptyDataResult(): EmptyResult<E> {
    return map { }
}

/**
 *  What is typealias?
 *  It’s a type alias: it allows you to create a more readable name for a complex type.
 *
 * Here, it’s used to represent a Result without meaningful data—just success or error.
 *
 * typealias EmptyResult<E> = Result<Unit, E>
 * Why use it?
 *  Because in many cases, like a login(), you don’t need to return any data—
 *  you just need to know whether it was successful or not.
 */
typealias EmptyResult<E> = Result<Unit, E>
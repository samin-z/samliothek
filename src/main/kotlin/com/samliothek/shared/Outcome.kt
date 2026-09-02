package com.samliothek.shared

/**
 * Typed success/failure without exceptions. Domain errors are part of the signature.
 */
sealed interface Outcome<out E, out A> {
    data class Success<A>(
        val value: A,
    ) : Outcome<Nothing, A>

    data class Failure<E>(
        val error: E,
    ) : Outcome<E, Nothing>
}

inline fun <E, A, B> Outcome<E, A>.map(f: (A) -> B): Outcome<E, B> =
    when (this) {
        is Outcome.Success -> Outcome.Success(f(value))
        is Outcome.Failure -> this
    }

inline fun <E, A, B> Outcome<E, A>.flatMap(f: (A) -> Outcome<E, B>): Outcome<E, B> =
    when (this) {
        is Outcome.Success -> f(value)
        is Outcome.Failure -> this
    }

inline fun <E, A, B> Outcome<E, A>.fold(
    onFailure: (E) -> B,
    onSuccess: (A) -> B,
): B =
    when (this) {
        is Outcome.Success -> onSuccess(value)
        is Outcome.Failure -> onFailure(error)
    }

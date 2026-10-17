package com.samliothek.support

import com.samliothek.shared.Isbn
import com.samliothek.shared.Outcome
import com.samliothek.shared.fold

fun requireIsbn(raw: String): Isbn =
    Isbn.of(raw).fold(
        onFailure = { error("Invalid test ISBN '$raw': ${it.message}") },
        onSuccess = { it },
    )

fun <E, A> Outcome<E, A>.requireSuccess(message: String = "Expected Outcome.Success"): A =
    fold(
        onFailure = { error("$message but was Failure($it)") },
        onSuccess = { it },
    )

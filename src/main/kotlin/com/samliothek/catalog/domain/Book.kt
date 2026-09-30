package com.samliothek.catalog.domain

import com.samliothek.shared.Isbn
import com.samliothek.shared.ids.BookId

data class Book(
    val id: BookId,
    val isbn: Isbn,
    val title: String,
    val author: String,
    val publishedYear: Int,
)

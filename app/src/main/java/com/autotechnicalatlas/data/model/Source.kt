package com.autotechnicalatlas.data.model

data class TechnicalSource(
    val id: String,
    val manufacturer: String,
    val document: String,
    val schemeNumber: String? = null,
    val year: Int? = null,
    val configuration: String? = null,
    val verified: Boolean = false,
    val note: String = ""
)

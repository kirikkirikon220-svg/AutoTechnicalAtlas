package com.autotechnicalatlas.data.model

data class TechnicalSource(
    val id: String,
    val manufacturer: String,
    val document: String,
    val schemeNumber: String?,
    val year: Int?,
    val configuration: String?,
    val verified: Boolean
)

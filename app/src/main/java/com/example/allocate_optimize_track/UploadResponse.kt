package com.example.allocate_optimize_track

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys


@Serializable
data class UploadResponse(
    @SerialName("storagePath") val storagePath: String,
    @SerialName("fullUrl") val fullUrl: String? = null // Optional: if function returns full URL
)

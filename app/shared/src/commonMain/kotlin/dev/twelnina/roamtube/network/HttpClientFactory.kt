package dev.twelnina.roamtube.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun createHttpClient(): HttpClient = HttpClient {
    expectSuccess = true

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}
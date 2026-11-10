package dev.twelnina.roamtube.repository

import dev.twelnina.roamtube.api.HomeResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class DefaultHomeRepository(
    private val client: HttpClient,
    baseUrl: String
) : HomeRepository {
    private val homeUrl = "${baseUrl.trimEnd('/')}/api/v1/home"

    override suspend fun fetchHome(region: String): HomeResponse =
        client.get(homeUrl) {
            parameter("region", region)
        }.body<HomeResponse>()
}
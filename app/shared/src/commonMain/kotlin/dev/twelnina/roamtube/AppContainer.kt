package dev.twelnina.roamtube

import dev.twelnina.roamtube.network.createHttpClient
import dev.twelnina.roamtube.repository.DefaultHomeRepository
import dev.twelnina.roamtube.repository.HomeRepository

class AppContainer(baseUrl: String) {
    private val httpClient = createHttpClient()

    internal val homeRepository: HomeRepository =
        DefaultHomeRepository(
            client = httpClient,
            baseUrl = baseUrl
        )
}
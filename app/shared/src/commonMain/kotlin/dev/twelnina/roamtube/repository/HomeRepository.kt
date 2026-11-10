package dev.twelnina.roamtube.repository

import dev.twelnina.roamtube.api.HomeResponse

interface HomeRepository {
    suspend fun fetchHome(region: String): HomeResponse
}
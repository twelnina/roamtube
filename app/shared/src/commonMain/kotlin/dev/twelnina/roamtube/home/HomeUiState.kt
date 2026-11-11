package dev.twelnina.roamtube.home

import dev.twelnina.roamtube.api.HomeResponse

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val response: HomeResponse
    ) : HomeUiState

    data class Empty(
        val response: HomeResponse
    ) : HomeUiState

    data object Error : HomeUiState
}
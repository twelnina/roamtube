package dev.twelnina.roamtube.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.twelnina.roamtube.repository.HomeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: HomeRepository,
    private val region: String
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        fetchHome()
    }

    fun retry() {
        fetchHome()
    }

    private fun fetchHome() {
        if (fetchJob?.isActive == true) return

        fetchJob = viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            try {
                val response = repository.fetchHome(region)

                _uiState.value = if (response.videos.isEmpty()) {
                    HomeUiState.Empty(response)
                } else {
                    HomeUiState.Success(response)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.value = HomeUiState.Error
            }
        }
    }
}

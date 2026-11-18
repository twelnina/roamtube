package dev.twelnina.roamtube.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.twelnina.roamtube.repository.HomeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    fun retry(region: String) {
        fetchHome(region)
    }

    fun fetchHome(region: String) {
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
            } catch (e: Exception) {
                println(e.stackTraceToString())
                _uiState.value = HomeUiState.Error
            }
        }
    }

    companion object {
        fun factory(repository: HomeRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    HomeViewModel(repository)
                }
            }
    }
}

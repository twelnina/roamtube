package dev.twelnina.roamtube.home

import dev.twelnina.roamtube.api.HomeResponse
import dev.twelnina.roamtube.api.VideoSummary
import dev.twelnina.roamtube.repository.HomeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialFetchWithNoVideosShowsEmpty() = runTest {
        val expectedResponse = HomeResponse(
            requestedRegion = "JP",
            contentRegion = "JP",
            fallbackReason = null,
            videos = emptyList()
        )
        var receivedRegion: String? = null

        val repository = object : HomeRepository {
            override suspend fun fetchHome(region: String): HomeResponse {
                receivedRegion = region
                return expectedResponse
            }
        }
        val viewModel = HomeViewModel(repository, "JP")

        assertEquals(HomeUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals("JP", receivedRegion)
        assertEquals(HomeUiState.Empty(expectedResponse), viewModel.uiState.value)
    }

    @Test
    fun initialFetchWithVideosShowsSuccessAndPreservesRegionFallback() = runTest {
        val expectedResponse = HomeResponse(
            requestedRegion = "JP",
            contentRegion = "US",
            fallbackReason = "test fallback",
            videos = listOf(
                VideoSummary(
                    videoId = "video-1",
                    title = "Test video",
                    thumbnailUrl = "https://example.com/thumbnail.jpg",
                    channelId = null,
                    channelName = "Test channel",
                    durationSeconds = 125L
                )
            )
        )
        val repository = object : HomeRepository {
            override suspend fun fetchHome(region: String): HomeResponse {
                return expectedResponse
            }
        }
        val viewModel = HomeViewModel(repository, region = "JP")

        assertEquals(HomeUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(
            HomeUiState.Success(expectedResponse),
            viewModel.uiState.value
        )
    }

    @Test
    fun initialFetchFailureShowsError() = runTest {
        val repository = object : HomeRepository {
            override suspend fun fetchHome(region: String): HomeResponse {
                throw IllegalStateException("Test failure")
            }
        }
        val viewModel = HomeViewModel(repository, "JP")

        assertEquals(HomeUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(HomeUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun retryAfterFailureShowsLoadingThenEmpty() = runTest {
        val expectedResponse = HomeResponse(
            requestedRegion = "JP",
            contentRegion = "JP",
            fallbackReason = null,
            videos = emptyList()
        )
        var fetchCount = 0
        val retryResponse = CompletableDeferred<HomeResponse>()

        val repository = object : HomeRepository {
            override suspend fun fetchHome(region: String): HomeResponse {
                fetchCount++
                if (fetchCount == 1) {
                    throw IllegalStateException("Test failure")
                }
                return retryResponse.await()
            }
        }
        val viewModel = HomeViewModel(repository, "JP")

        advanceUntilIdle()

        assertEquals(1, fetchCount)
        assertEquals(HomeUiState.Error, viewModel.uiState.value)

        viewModel.retry()
        runCurrent()

        assertEquals(2, fetchCount)
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)

        retryResponse.complete(expectedResponse)
        advanceUntilIdle()

        assertEquals(2, fetchCount)
        assertEquals(HomeUiState.Empty(expectedResponse), viewModel.uiState.value)
    }

    @Test
    fun retryWhileFetchingDoesNotStartAnotherFetch() = runTest {
        val expectedResponse = HomeResponse(
            requestedRegion = "JP",
            contentRegion = "JP",
            fallbackReason = null,
            videos = emptyList()
        )
        val response = CompletableDeferred<HomeResponse>()
        var fetchCount = 0
        val repository = object : HomeRepository {
            override suspend fun fetchHome(region: String): HomeResponse {
                fetchCount++
                return response.await()
            }
        }
        val viewModel = HomeViewModel(repository, "JP")
        runCurrent()

        assertEquals(1, fetchCount)

        repeat(3) { viewModel.retry() }
        runCurrent()

        assertEquals(1, fetchCount)
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)

        response.complete(expectedResponse)
        advanceUntilIdle()

        assertEquals(1, fetchCount)
        assertEquals(HomeUiState.Empty(expectedResponse), viewModel.uiState.value)
    }

    @Test
    fun initialFetchCancellationDoesNotShowError() = runTest {
        val repository = object : HomeRepository {
            override suspend fun fetchHome(region: String): HomeResponse {
                throw CancellationException("Test cancellation")
            }
        }
        val viewModel = HomeViewModel(repository, "JP")

        advanceUntilIdle()

        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    }
}

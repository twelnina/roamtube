package dev.twelnina.roamtube.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.twelnina.roamtube.api.HomeResponse
import dev.twelnina.roamtube.api.VideoSummary

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    region: String,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, region) {
        viewModel.fetchHome(region)
    }

    HomeScreen(
        uiState = uiState,
        onRetry = { viewModel.retry(region) },
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold() {
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val response = when (uiState) {
                is HomeUiState.Success -> uiState.response
                is HomeUiState.Empty -> uiState.response
                else -> null
            }

            if (response != null) {
                Text("Music charts: ${response.contentRegion}")

                if (response.contentRegion != response.requestedRegion) {
                    Text(
                        "Charts for ${response.requestedRegion} are unavailable. " +
                                "Showing charts for ${response.contentRegion} instead."
                    )
                }
            }

            when (uiState) {
                HomeUiState.Loading -> HomeLoadingContent(modifier = Modifier.fillMaxSize())
                is HomeUiState.Success -> HomeSuccessContent(
                    response = uiState.response,
                    modifier = Modifier.weight(1f)
                )

                is HomeUiState.Empty -> HomeEmptyContent()
                HomeUiState.Error -> HomeErrorContent(
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}

@Composable
private fun HomeSuccessContent(
    response: HomeResponse,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(response.videos) { video ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = video.channelName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatDuration(video.durationSeconds),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyContent() {
    Text("No videos found")
}

@Composable
private fun HomeErrorContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Unable to load music charts")
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

private fun formatDuration(durationSeconds: Long): String {
    val totalSeconds = durationSeconds.coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')

    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:$seconds"
    } else {
        "$minutes:$seconds"
    }
}

private val previewHomeResponse = HomeResponse(
    requestedRegion = "JP",
    contentRegion = "JP",
    fallbackReason = null,
    videos = listOf(
        VideoSummary(
            videoId = "preview-1",
            title = "A new song for your morning playlist",
            thumbnailUrl = "",
            channelId = null,
            channelName = "Morning Music",
            durationSeconds = 125L
        ),
        VideoSummary(
            videoId = "preview-2",
            title = "Live concert — full performance",
            thumbnailUrl = "",
            channelId = null,
            channelName = "Live Sessions",
            durationSeconds = 3665L
        )
    )
)

@Composable
private fun HomeScreenPreviewContent(uiState: HomeUiState) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            HomeScreen(uiState = uiState, onRetry = {})
        }
    }
}

@Preview(name = "Success")
@Composable
private fun HomeScreenSuccessPreview() {
    HomeScreenPreviewContent(HomeUiState.Success(previewHomeResponse))
}

@Preview(name = "Region fallback")
@Composable
private fun HomeScreenFallbackPreview() {
    HomeScreenPreviewContent(
        HomeUiState.Success(
            previewHomeResponse.copy(
                contentRegion = "US",
                fallbackReason = "Charts unavailable for the requested region"
            )
        )
    )
}

@Preview(name = "Loading")
@Composable
private fun HomeScreenLoadingPreview() {
    HomeScreenPreviewContent(HomeUiState.Loading)
}

@Preview(name = "Empty")
@Composable
private fun HomeScreenEmptyPreview() {
    HomeScreenPreviewContent(
        HomeUiState.Empty(previewHomeResponse.copy(videos = emptyList()))
    )
}

@Preview(name = "Error")
@Composable
private fun HomeScreenErrorPreview() {
    HomeScreenPreviewContent(HomeUiState.Error)
}

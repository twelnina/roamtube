package dev.twelnina.roamtube

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import dev.twelnina.roamtube.home.HomeEntry
import dev.twelnina.roamtube.home.HomeRoute

@Composable
fun App(appContainer: AppContainer) {
    MaterialTheme {
        Surface {
            HomeEntry(appContainer = appContainer)
        }
    }
}
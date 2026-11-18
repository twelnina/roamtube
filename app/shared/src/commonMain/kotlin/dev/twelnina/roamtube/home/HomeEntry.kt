package dev.twelnina.roamtube.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.twelnina.roamtube.AppContainer
import dev.twelnina.roamtube.locale.getDeviceRegion

@Composable
fun HomeEntry(appContainer: AppContainer) {
    val factory = remember(appContainer) {
        HomeViewModel.factory(appContainer.homeRepository)
    }
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val region = remember { getDeviceRegion() }

    HomeRoute(
        viewModel = homeViewModel,
        region = region
    )
}
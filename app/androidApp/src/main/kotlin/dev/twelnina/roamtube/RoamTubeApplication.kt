package dev.twelnina.roamtube

import android.app.Application

class RoamTubeApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(baseUrl = BuildConfig.API_BASE_URL)
    }
}
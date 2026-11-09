package dev.twelnina.roamtube

import dev.twelnina.roamtube.extractor.JvmDownloader
import dev.twelnina.roamtube.extractor.fetchMusicCharts
import dev.twelnina.roamtube.routes.homeRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.schabi.newpipe.extractor.NewPipe

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    NewPipe.init(JvmDownloader())

    install(ContentNegotiation) {
        json()
    }

    routing {
        route("api/v1") {
            homeRoutes(::fetchMusicCharts)
        }
    }
}

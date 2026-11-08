package dev.twelnina.roamtube

import dev.twelnina.roamtube.api.HomeResponse
import dev.twelnina.roamtube.extractor.JvmDownloader
import dev.twelnina.roamtube.extractor.MusicChartExtractor
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.exceptions.UnsupportedContentInCountryException
import java.util.Locale

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    NewPipe.init(JvmDownloader())

    install(ContentNegotiation) {
        json()
    }

    val musicChartExtractor = MusicChartExtractor()
    val validRegions = Locale.getISOCountries().toSet()

    routing {
        get("/") {
            call.respondText("Hello, Ktor!")
        }

        get("/api/v1/home") {
            val region = call.request.queryParameters["region"]
                ?.uppercase(Locale.ROOT)

            if (region == null || region !in validRegions) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "region must be an ISO 3166-1 alpha-2 country code")
                )
                return@get
            }

            var contentRegion = region

            val videos = try {
                musicChartExtractor.fetch(contentRegion)
            } catch (_: UnsupportedContentInCountryException) {
                contentRegion = "US"
                musicChartExtractor.fetch(contentRegion)
            }

            call.respond(
                HomeResponse(
                    requestedRegion = region,
                    contentRegion = contentRegion,
                    fallbackReason = if (contentRegion != region) {
                        "UNSUPPORTED_REGION"
                    } else {
                        null
                    },
                    videos = videos
                )
            )
        }
    }
}
package dev.twelnina.roamtube.routes

import dev.twelnina.roamtube.api.HomeResponse
import dev.twelnina.roamtube.api.VideoSummary
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.io.IOException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.UnsupportedContentInCountryException
import java.util.Locale

internal fun Route.homeRoutes(
    fetchMusicCharts: suspend (String) -> List<VideoSummary>
) {
    val validRegions = Locale.getISOCountries().toSet()

    get("/home") {
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
            try {
                fetchMusicCharts(contentRegion)
            } catch (_: UnsupportedContentInCountryException) {
                contentRegion = "US"
                fetchMusicCharts(contentRegion)
            }
        } catch (e: IOException) {
            call.application.log.error("Music charts request failed", e)
            call.respond(
                HttpStatusCode.BadGateway,
                mapOf("error" to "Failed to retrieve music charts")
            )
            return@get
        } catch (e: ExtractionException) {
            call.application.log.error("Music charts extraction failed", e)
            call.respond(
                HttpStatusCode.BadGateway,
                mapOf("error" to "Failed to extract music charts")
            )
            return@get
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
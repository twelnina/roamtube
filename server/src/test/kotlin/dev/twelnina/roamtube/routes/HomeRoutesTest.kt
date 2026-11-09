package dev.twelnina.roamtube.routes

import dev.twelnina.roamtube.api.HomeResponse
import dev.twelnina.roamtube.api.VideoSummary
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.UnsupportedContentInCountryException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeRoutesTest {
    @Test
    fun supportedRegionReturnsMusicCharts() = testApplication {
        val videos = listOf(
            VideoSummary(
                videoId = "dQw4w9WgXcQ",
                title = "Test song",
                thumbnailUrl = "https://example.com/thumbnail.jpg",
                channelId = "UCabcdefghijklmnopqrstuv",
                channelName = "Test artist",
                durationSeconds = 210L
            )
        )
        val requestedRegions = mutableListOf<String>()

        application {
            configureHomeRoutes { region ->
                requestedRegions += region
                videos
            }
        }

        val response = client.get("/api/v1/home?region=JP")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            ContentType.Application.Json,
            response.contentType()?.withoutParameters()
        )
        assertEquals(
            HomeResponse(
                requestedRegion = "JP",
                contentRegion = "JP",
                fallbackReason = null,
                videos = videos
            ),
            Json.decodeFromString<HomeResponse>(response.bodyAsText())
        )
        assertEquals(listOf("JP"), requestedRegions)
    }

    @Test
    fun invalidRegionReturnsBadRequestWithoutFetching() = testApplication {
        var fetchCount = 0

        application {
            configureHomeRoutes {
                fetchCount++
                emptyList()
            }
        }

        val invalidRequests = listOf(
            "/api/v1/home",
            "/api/v1/home?region=",
            "/api/v1/home?region=JPN",
            "/api/v1/home?region=ZZ"
        )

        invalidRequests.forEach { url ->
            val response = client.get(url)

            assertEquals(
                HttpStatusCode.BadRequest,
                response.status,
                "url=$url"
            )
            assertEquals(
                Json.parseToJsonElement(
                    """{"error":"region must be an ISO 3166-1 alpha-2 country code"}"""
                ),
                Json.parseToJsonElement(response.bodyAsText()),
                "url=$url"
            )
        }

        assertEquals(0, fetchCount)
    }

    @Test
    fun unsupportedRegionFallsBackToUs() = testApplication {
        val requestedRegions = mutableListOf<String>()
        val videos = listOf(testVideo())

        application {
            configureHomeRoutes { region ->
                requestedRegions += region
                if (region == "AQ") {
                    throw UnsupportedContentInCountryException("Unsupported region")
                }
                videos
            }
        }

        val response = client.get("/api/v1/home?region=AQ")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            HomeResponse("AQ", "US", "UNSUPPORTED_REGION", videos),
            Json.decodeFromString<HomeResponse>(response.bodyAsText())
        )
        assertEquals(listOf("AQ", "US"), requestedRegions)
    }

    @Test
    fun networkFailureReturnsBadGatewayWithoutFallback() {
        assertUpstreamFailure(
            failure = IOException("Connection failed"),
            expectedError = "Failed to retrieve music charts"
        )
    }

    @Test
    fun extractionFailureReturnsBadGatewayWithoutFallback() {
        assertUpstreamFailure(
            failure = ParsingException("Invalid upstream response"),
            expectedError = "Failed to extract music charts"
        )
    }

    @Test
    fun networkFailureAfterFallbackReturnsBadGateway() {
        assertUpstreamFailure(
            failure = IOException("Connection failed"),
            expectedError = "Failed to retrieve music charts",
            region = "AQ"
        )
    }

    @Test
    fun extractionFailureAfterFallbackReturnsBadGateway() {
        assertUpstreamFailure(
            failure = ParsingException("Invalid upstream response"),
            expectedError = "Failed to extract music charts",
            region = "AQ"
        )
    }

    @Test
    fun nullableFieldsAreExplicitlySerialized() = testApplication {
        val videos = listOf(testVideo().copy(channelId = null))

        application {
            configureHomeRoutes { videos }
        }

        val response = client.get("/api/v1/home?region=JP")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(JsonNull, body["fallbackReason"])
        assertEquals(
            JsonNull,
            body.getValue("videos").jsonArray.single().jsonObject["channelId"]
        )
    }

    @Test
    fun lowercaseRegionIsNormalized() = testApplication {
        val requestedRegions = mutableListOf<String>()
        val videos = listOf(testVideo())

        application {
            configureHomeRoutes { region ->
                requestedRegions += region
                videos
            }
        }

        val response = client.get("/api/v1/home?region=jp")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            HomeResponse("JP", "JP", null, videos),
            Json.decodeFromString<HomeResponse>(response.bodyAsText())
        )
        assertEquals(listOf("JP"), requestedRegions)
    }

    @Test
    fun supportedUsRegionDoesNotFallBack() = testApplication {
        val requestedRegions = mutableListOf<String>()
        val videos = listOf(testVideo())

        application {
            configureHomeRoutes { region ->
                requestedRegions += region
                videos
            }
        }

        val response = client.get("/api/v1/home?region=US")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            HomeResponse("US", "US", null, videos),
            Json.decodeFromString<HomeResponse>(response.bodyAsText())
        )
        assertEquals(listOf("US"), requestedRegions)
    }

    @Test
    fun emptyChartsReturnEmptyVideoList() = testApplication {
        application {
            configureHomeRoutes { emptyList() }
        }

        val response = client.get("/api/v1/home?region=JP")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            HomeResponse("JP", "JP", null, emptyList()),
            Json.decodeFromString<HomeResponse>(response.bodyAsText())
        )
    }

    private fun assertUpstreamFailure(
        failure: Exception,
        expectedError: String,
        region: String = "JP"
    ) = testApplication {
        val requestedRegions = mutableListOf<String>()

        application {
            configureHomeRoutes { requestedRegion ->
                requestedRegions += requestedRegion
                if (requestedRegion == "AQ") {
                    throw UnsupportedContentInCountryException("Unsupported region")
                }
                throw failure
            }
        }

        val response = client.get("/api/v1/home?region=$region")

        assertEquals(HttpStatusCode.BadGateway, response.status)
        assertEquals(
            mapOf("error" to expectedError),
            Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        )
        assertEquals(
            if (region == "AQ") listOf("AQ", "US") else listOf(region),
            requestedRegions
        )
    }

    private fun testVideo() = VideoSummary(
        videoId = "dQw4w9WgXcQ",
        title = "Test song",
        thumbnailUrl = "https://example.com/thumbnail.jpg",
        channelId = "UCabcdefghijklmnopqrstuv",
        channelName = "Test artist",
        durationSeconds = 210L
    )

    private fun Application.configureHomeRoutes(
        fetchMusicCharts: suspend (String) -> List<VideoSummary>
    ) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            route("/api/v1") {
                homeRoutes(fetchMusicCharts)
            }
        }
    }
}

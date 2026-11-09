package dev.twelnina.roamtube.extractor

import dev.twelnina.roamtube.api.VideoSummary
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.UnsupportedContentInCountryException
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MusicChartExtractorTest {
    @Test
    fun unsupportedRegionThrowsWithoutNetworkRequest() = runTest {
        val downloader = object : Downloader() {
            override fun execute(request: Request): Response {
                error("No network request should be made for unsupported region")
            }
        }
        NewPipe.init(downloader)

        assertFailsWith<UnsupportedContentInCountryException> {
            fetchMusicCharts("AQ")
        }
    }

    @Test
    fun streamInfoItemConvertsToVideoSummary() {
        val item = createItem()

        val actual = item.toVideoSummary()

        val expected = VideoSummary(
            videoId = "dQw4w9WgXcQ",
            title = "Test song",
            thumbnailUrl = "https://example.com/thumbnail.jpg",
            channelId = "UCabcdefghijklmnopqrstuv",
            channelName = "Test artist",
            durationSeconds = 210L
        )

        assertEquals(expected, actual)
    }

    @Test
    fun widestThumbnailIsSelected() {
        val item = createItem {
            thumbnails = listOf(
                Image(
                    "https://example.com/small.jpg",
                    90,
                    120,
                    Image.ResolutionLevel.LOW
                ),
                Image(
                    "https://example.com/large.jpg",
                    360,
                    640,
                    Image.ResolutionLevel.MEDIUM
                ),
                Image(
                    "https://example.com/medium.jpg",
                    180,
                    320,
                    Image.ResolutionLevel.MEDIUM
                )
            )
        }

        assertEquals(
            "https://example.com/large.jpg",
            item.toVideoSummary().thumbnailUrl
        )
    }

    @Test
    fun missingThumbnailThrowsParsingException() {
        val item = createItem {
            thumbnails = emptyList()
        }

        assertFailsWith<ParsingException> {
            item.toVideoSummary()
        }
    }

    @Test
    fun missingUploaderUrlProducesNullChannelId() {
        listOf(null, "", "   ").forEach { uploaderUrl ->
            val item = createItem {
                this.uploaderUrl = uploaderUrl
            }

            assertNull(
                item.toVideoSummary().channelId,
                "uploaderUrl=<$uploaderUrl>"
            )
        }
    }

    @Test
    fun handleUploaderUrlProducesNullChannelId() {
        val item = createItem {
            uploaderUrl = "https://www.youtube.com/@artist"
        }

        assertNull(item.toVideoSummary().channelId)
    }

    @Test
    fun malformedUploaderUrlProducesNullChannelId() {
        val item = createItem {
            uploaderUrl = "not a valid URL"
        }

        assertNull(item.toVideoSummary().channelId)
    }

    @Test
    fun invalidChannelIdProducesNullChannelId() {
        val invalidIds = listOf(
            "UC" + "a".repeat(21),
            "UC" + "a".repeat(23),
            "XX" + "a".repeat(22),
            "UC" + "a".repeat(21) + "!"
        )

        invalidIds.forEach { channelId ->
            val item = createItem {
                uploaderUrl = "https://www.youtube.com/channel/$channelId"
            }

            assertNull(
                item.toVideoSummary().channelId,
                "channelId=<$channelId>"
            )
        }
    }

    @Test
    fun channelIdWithHyphenAndUnderscoreIsPreserved() {
        val channelId = "UC" + "a".repeat(20) + "-_"

        val item = createItem {
            uploaderUrl = "https://www.youtube.com/channel/$channelId"
        }

        assertEquals(channelId, item.toVideoSummary().channelId)
    }

    @Test
    fun malformedVideoUrlThrowsParsingException() {
        val item = createItem(url = "not a valid URL")

        assertFailsWith<ParsingException> {
            item.toVideoSummary()
        }
    }

    private fun createItem(
        url: String = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        configure: StreamInfoItem.() -> Unit = {}
    ): StreamInfoItem =
        StreamInfoItem(
            ServiceList.YouTube.serviceId,
            url,
            "Test song",
            StreamType.VIDEO_STREAM
        ).apply {
            uploaderUrl = "https://www.youtube.com/channel/UCabcdefghijklmnopqrstuv"
            uploaderName = "Test artist"
            duration = 210L
            thumbnails = listOf(
                Image(
                    "https://example.com/thumbnail.jpg",
                    360,
                    640,
                    Image.ResolutionLevel.MEDIUM
                )
            )
        }.apply(configure)
}

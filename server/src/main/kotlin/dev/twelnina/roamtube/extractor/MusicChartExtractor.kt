package dev.twelnina.roamtube.extractor

import dev.twelnina.roamtube.api.VideoSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class MusicChartExtractor {
    suspend fun fetch(region: String): List<VideoSummary> =
        withContext(Dispatchers.IO) {
            val extractor = ServiceList.YouTube.kioskList
                .getExtractorById("trending_music", null)

            extractor.forceContentCountry(ContentCountry(region))
            extractor.fetchPage()

            extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .map { it.toVideoSummary() }
        }

    private fun StreamInfoItem.toVideoSummary(): VideoSummary {
        val thumbnail = thumbnails.maxByOrNull { it.width }
            ?: throw ParsingException("Missing thumbnail: $url")

        return VideoSummary(
            videoId = ServiceList.YouTube.streamLHFactory.getId(url),
            title = name,
            thumbnailUrl = thumbnail.url,
            channelId = extractChannelId(uploaderUrl),
            channelName = uploaderName,
            durationSeconds = duration
        )
    }

    private fun extractChannelId(url: String?): String? {
        if (url.isNullOrBlank()) return null

        val id = try {
            ServiceList.YouTube.channelLHFactory.getId(url)
        } catch (_: ParsingException) {
            return null
        }

        if (!id.startsWith("channel/")) return null

        return id.removePrefix("channel/")
            .takeIf { it.matches(Regex("UC[A-Za-z0-9_-]{22}")) }
    }
}
package dev.twelnina.roamtube.api

import kotlinx.serialization.Serializable

@Serializable
data class HomeResponse(
    val requestedRegion: String,
    val contentRegion: String,
    val fallbackReason: String?,
    val videos: List<VideoSummary>
)

@Serializable
data class VideoSummary(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val channelId: String?,
    val channelName: String,
    val durationSeconds: Long
)
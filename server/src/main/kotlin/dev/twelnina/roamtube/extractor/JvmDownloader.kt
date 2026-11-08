package dev.twelnina.roamtube.extractor

import kotlinx.io.IOException
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class JvmDownloader : Downloader() {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    override fun execute(request: Request): Response {
        val body = request.dataToSend()
            ?.let { HttpRequest.BodyPublishers.ofByteArray(it) }
            ?: HttpRequest.BodyPublishers.noBody()

        val builder = HttpRequest.newBuilder()
            .uri(URI.create(request.url()))
            .timeout(Duration.ofSeconds(30))
            .method(request.httpMethod(), body)

        request.headers().forEach { (name, values) ->
            values.forEach { value ->
                builder.header(name, value)
            }
        }

        val response = try {
            client.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Http request interrupted", e)
        }

        return Response(
            response.statusCode(),
            "",
            response.headers().map(),
            response.body(),
            response.uri().toString()
        )
    }
}

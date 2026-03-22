package com.xayah.databackup.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.Closeable

private const val BASE_URL = "https://api.github.com/repos/XayahSuSuSu/Android-DataBackup/"

@Serializable
data class GitHubAsset(
    @SerialName("id")
    val id: Long = 0L,
    @SerialName("name")
    val name: String = "",
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("size")
    val size: Long = 0L,
    @SerialName("download_count")
    val downloadCount: Long = 0L,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = "",
)

@Serializable
data class GitHubRelease(
    @SerialName("id")
    val id: Long = 0L,
    @SerialName("tag_name")
    val tagName: String = "",
    @SerialName("name")
    val name: String? = null,
    @SerialName("body")
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String = "",
    @SerialName("published_at")
    val publishedAt: String? = null,
    @SerialName("draft")
    val draft: Boolean = false,
    @SerialName("prerelease")
    val prerelease: Boolean = false,
    @SerialName("assets")
    val assets: List<GitHubAsset> = emptyList(),
)

enum class GitHubApiErrorKind {
    RATE_LIMITED,
    HTTP,
    NETWORK,
}

class GitHubApiException(
    val kind: GitHubApiErrorKind,
    override val message: String,
    val statusCode: Int? = null,
    val rateLimitResetAtMillis: Long? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

class GitHubReleaseRepository : Closeable {
    private val client = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
        defaultRequest {
            url(BASE_URL)
        }
    }

    override fun close() = client.close()

    suspend fun getLatestRelease(): GitHubRelease = request("releases/latest")

    private suspend inline fun <reified T> request(path: String): T {
        return try {
            val response = client.get(path)
            if (response.status.isSuccess()) {
                response.body<T>()
            } else {
                throw response.toApiException()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GitHubApiException) {
            throw e
        } catch (e: Throwable) {
            throw e.normalizeToApiException()
        }
    }

    private suspend fun HttpResponse.toApiException(): GitHubApiException {
        val bodyText = runCatching { bodyAsText() }.getOrDefault("")
        val apiMessage = extractApiMessage(bodyText)
        val rateLimitResetAtMillis = headers["X-RateLimit-Reset"]?.toLongOrNull()?.times(1000)
        val isRateLimited =
            (status == HttpStatusCode.Forbidden || status == HttpStatusCode.TooManyRequests) &&
            (headers["X-RateLimit-Remaining"] == "0" || apiMessage.contains("rate limit", ignoreCase = true))

        return if (isRateLimited) {
            GitHubApiException(
                kind = GitHubApiErrorKind.RATE_LIMITED,
                message = apiMessage.ifBlank { "GitHub API rate limit exceeded." },
                statusCode = status.value,
                rateLimitResetAtMillis = rateLimitResetAtMillis,
            )
        } else {
            GitHubApiException(
                kind = GitHubApiErrorKind.HTTP,
                message = apiMessage.ifBlank { "HTTP ${status.value} ${status.description}" },
                statusCode = status.value,
            )
        }
    }

    private fun Throwable.normalizeToApiException(): GitHubApiException {
        if (this is GitHubApiException) return this
        return GitHubApiException(
            kind = GitHubApiErrorKind.NETWORK,
            message = message?.takeIf { it.isNotBlank() } ?: "Network request failed.",
            cause = this
        )
    }

    private fun extractApiMessage(body: String): String {
        if (body.isBlank()) return ""
        val parsed = runCatching {
            Json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.content
        }.getOrNull()
        return parsed ?: body.trim()
    }
}

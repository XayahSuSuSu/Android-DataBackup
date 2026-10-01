package com.xayah.databackup.data

import com.xayah.databackup.entity.GitHubAsset
import com.xayah.databackup.entity.GitHubRelease
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.Closeable

private const val BASE_URL = "https://api.github.com/repos/XayahSuSuSu/Android-DataBackup/"

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
    private val mClient = HttpClient {
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

    override fun close() = mClient.close()

    suspend fun getLatestRelease(): GitHubRelease = request("releases/latest")

    private suspend inline fun <reified T> request(path: String): T {
        return runCatching {
            val response = mClient.get(path)
            if (response.status.isSuccess()) {
                response.body<T>()
            } else {
                throw response.toApiException()
            }
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            throw error.normalizeToApiException()
        }
    }

    private suspend fun HttpResponse.toApiException(): GitHubApiException {
        val bodyText = runCatching { bodyAsText() }.getOrElse { error ->
            if (error is CancellationException) throw error
            ""
        }
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

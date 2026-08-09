package com.xayah.databackup.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.io.IOException

private const val TRANSLATORS_URL = "https://DataBackupOfficial.github.io/api/translators.json"

internal class TranslatorHttpException(
    val statusCode: Int,
) : IOException("HTTP $statusCode")

@Serializable
data class Translator(
    val username: String = "",
    @SerialName("full_name")
    val fullName: String = "",
    @SerialName("date_joined")
    val dateJoined: String = "",
    @SerialName("change_count")
    val changeCount: Int = 0,
    val languages: Map<String, Int> = emptyMap(),
    val name: String? = null,
    val avatar: String? = null,
    val link: String? = null,
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: fullName.takeIf { it.isNotBlank() } ?: username
}

private val Translator.identity: String
    get() = link?.trim()?.trimEnd('/')?.takeIf(String::isNotEmpty)?.lowercase() ?: "username:${username.trim().lowercase()}"

private fun Translator.merge(other: Translator): Translator = copy(
    changeCount = maxOf(changeCount, other.changeCount),
    languages = buildMap {
        putAll(languages)
        other.languages.forEach { (language, contributions) ->
            put(language, maxOf(get(language) ?: 0, contributions))
        }
    },
    name = name?.takeIf(String::isNotBlank) ?: other.name,
    avatar = avatar?.takeIf(String::isNotBlank) ?: other.avatar,
    link = link?.takeIf(String::isNotBlank) ?: other.link,
)

private fun Iterable<Translator>.deduplicatedByIdentity(): List<Translator> =
    groupBy(Translator::identity).values.map { translators -> translators.reduce(Translator::merge) }

class TranslatorRepository : Closeable {
    private val mJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val mHttpClient = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }

    override fun close() = mHttpClient.close()

    suspend fun getTranslators(): List<Translator> {
        val response = mHttpClient.get(TRANSLATORS_URL)
        if (!response.status.isSuccess()) {
            throw TranslatorHttpException(response.status.value)
        }
        return mJson.decodeFromString<List<Translator>>(response.bodyAsText()).deduplicatedByIdentity()
    }
}

package com.example.opencodeandroidclient.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.opencodeandroidclient.data.ApiFactory
import com.example.opencodeandroidclient.data.ChatMessage
import com.example.opencodeandroidclient.data.MessageRequest
import com.example.opencodeandroidclient.data.MessageRole
import com.example.opencodeandroidclient.data.OpenCodeApi
import com.example.opencodeandroidclient.data.PartRequest
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URI
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("opencode_client", Context.MODE_PRIVATE)

    private val _serverUrl = MutableStateFlow(
        prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
    )
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var api: OpenCodeApi? = null

    init {
        rebuildApi()
    }

    fun setServerUrl(value: String) {
        val normalized = value.trim()
        _serverUrl.value = normalized
        prefs.edit().putString(KEY_URL, normalized).apply()
        _sessionId.value = null
        _error.value = null
        _messages.value = emptyList()
        rebuildApi()
    }

    private fun rebuildApi() {
        api = try {
            ApiFactory.create(_serverUrl.value)
        } catch (e: Exception) {
            _error.value = e.message ?: "Invalid server URL"
            null
        }
    }

    /** Create a new OpenCode session on the configured server. */
    fun connect() {
        val client = api
        if (client == null) {
            _error.value = "Укажите корректный адрес сервера"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = client.createSession()
                if (!response.isSuccessful) {
                    _error.value = "Ошибка сервера: HTTP ${response.code()}"
                    return@launch
                }
                val id = response.body()?.id
                if (id.isNullOrBlank()) {
                    _error.value = "Сервер не вернул id сессии"
                    return@launch
                }
                _sessionId.value = id
                _messages.value = _messages.value + ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.SYSTEM,
                    text = "Сессия создана: $id",
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Не удалось подключиться к серверу"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Send a chat message to the active session. */
    fun sendMessage(text: String) {
        val body = text.trim()
        if (body.isBlank()) return
        val session = _sessionId.value
        if (session.isNullOrBlank()) {
            _error.value = "Сначала подключитесь к серверу"
            return
        }
        val client = api ?: return

        _messages.value = _messages.value + ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            text = body,
        )
        val loadingId = UUID.randomUUID().toString()
        _messages.value = _messages.value + ChatMessage(
            id = loadingId,
            role = MessageRole.ASSISTANT,
            text = "",
            isLoading = true,
        )

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = client.sendMessage(
                    session,
                    MessageRequest(parts = listOf(PartRequest(text = body))),
                )
                if (!response.isSuccessful) {
                    _error.value = "Ошибка отправки: HTTP ${response.code()}"
                    removeMessage(loadingId)
                    return@launch
                }
                val answer = extractText(response.body())
                _messages.value = _messages.value.map {
                    if (it.id == loadingId) {
                        it.copy(text = answer, isLoading = false)
                    } else {
                        it
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка сети"
                removeMessage(loadingId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Handle the raw value scanned from an `opencode pair` QR code. */
    fun onQrResult(raw: String) {
        val parsed = parsePairValue(raw)
        setServerUrl(parsed)
        connect()
    }

    fun dismissError() {
        _error.value = null
    }

    private fun removeMessage(id: String) {
        _messages.value = _messages.value.filterNot { it.id == id }
    }

    private fun extractText(body: JsonObject?): String {
        if (body == null) return "(пустой ответ)"

        body.getAsJsonPrimitive("text")?.let { return it.asString }
        body.getAsJsonPrimitive("content")?.let { return it.asString }

        val parts = body.getAsJsonArray("parts")
        if (parts != null) {
            val result = StringBuilder()
            for (element in parts) {
                if (!element.isJsonObject) continue
                val obj = element.asJsonObject
                val type = obj.getAsJsonPrimitive("type")?.asString
                if (type != null && type != "text") continue
                obj.getAsJsonPrimitive("text")?.let { result.append(it.asString).append('\n') }
            }
            if (result.isNotBlank()) return result.toString().trim()
        }
        return body.toString()
    }

    private fun parsePairValue(raw: String): String {
        var value = raw.trim()
        try {
            val json = JSONObject(value)
            val candidate = json.optString("url", json.optString("serverUrl", ""))
            if (candidate.isNotBlank()) value = candidate
        } catch (_: Exception) {
            // Not a JSON payload — treat as a plain URL.
        }

        try {
            val uri = URI(value)
            if (uri.scheme != null && uri.host != null) {
                val port = if (uri.port > 0) ":${uri.port}" else ""
                return "${uri.scheme}://${uri.host}$port/"
            }
        } catch (_: Exception) {
            // Not a valid URI — fall back to a host:port string.
        }

        return value
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .ifBlank { value }
    }

    companion object {
        private const val KEY_URL = "server_url"
        private const val DEFAULT_URL = "http://10.0.2.2:4096/"
    }
}

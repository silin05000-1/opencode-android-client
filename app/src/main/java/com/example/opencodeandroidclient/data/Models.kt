package com.example.opencodeandroidclient.data

import com.google.gson.annotations.SerializedName

/** Response of POST /session. */
data class SessionDto(
    @SerializedName("id") val id: String,
)

/** Request body for POST /session/{id}/message. */
data class MessageRequest(
    @SerializedName("parts") val parts: List<PartRequest>,
)

data class PartRequest(
    @SerializedName("type") val type: String = "text",
    @SerializedName("text") val text: String,
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
}

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val isLoading: Boolean = false,
)

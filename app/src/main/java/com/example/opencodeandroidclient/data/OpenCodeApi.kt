package com.example.opencodeandroidclient.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface OpenCodeApi {
    @POST("session")
    suspend fun createSession(): Response<SessionDto>

    @POST("session/{id}/message")
    suspend fun sendMessage(
        @Path("id") sessionId: String,
        @Body request: MessageRequest,
    ): Response<JsonObject>
}

object ApiFactory {
    /** Normalize a raw server address into a Retrofit base URL ending with '/'. */
    fun normalizeBaseUrl(input: String): String {
        var url = input.trim()
        require(url.isNotBlank()) { "Server URL is empty" }
        if (!url.contains("://")) {
            url = "http://$url"
        }
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        return url
    }

    fun create(baseUrl: String): OpenCodeApi {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val gson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(OpenCodeApi::class.java)
    }
}

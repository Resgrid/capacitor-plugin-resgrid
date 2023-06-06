package com.resgrid.plugins.resgrid.api

import com.google.gson.GsonBuilder
import com.resgrid.plugins.resgrid.models.CanConnectToVoiceSessionResult
import com.resgrid.plugins.resgrid.models.ConfigData
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit

interface VoiceApi {
    @GET("api/v4/Voice/CanConnectToVoiceSession")
    suspend fun getCanConnectToVoiceSession(@Query("token") token: String): CanConnectToVoiceSessionResult
}

class VoiceApiService(val configData: ConfigData) {

    private val api: VoiceApi by lazy {
        createVoiceApi()
    }

    suspend fun getCanConnectToVoiceSession(): CanConnectToVoiceSessionResult {
        return api.getCanConnectToVoiceSession(configData.canConnectToVoiceApiToken!!)
    }

    private fun createVoiceApi(): VoiceApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(configData.apiUrl!!)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(VoiceApi::class.java)
    }
}

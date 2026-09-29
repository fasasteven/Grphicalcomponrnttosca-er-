package com.example.data.network

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

data class ApiLoginRequest(
    val identifier: String,
    val passcode: String,
    val role: String,
    val deviceId: String? = null
)

data class ApiRegisterRequest(
    val fullName: String,
    val identifier: String,
    val faculty: String,
    val passcode: String,
    val role: String,
    val deviceId: String? = null
)

data class ApiAttendanceVerifyRequest(
    val sessionId: Long,
    val studentMatric: String,
    val studentLatitude: Double,
    val studentLongitude: Double,
    val codeOrBarcode: String,
    val verificationMethod: String
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

interface GeoAttendApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: ApiRegisterRequest): ApiResponse<Map<String, Any>>

    @POST("api/auth/login")
    suspend fun login(@Body request: ApiLoginRequest): ApiResponse<Map<String, Any>>

    @GET("api/courses")
    suspend fun getCourses(): ApiResponse<List<Map<String, Any>>>

    @POST("api/sessions/start")
    suspend fun startSession(@Body sessionData: Map<String, Any>): ApiResponse<Map<String, Any>>

    @POST("api/attendance/verify")
    suspend fun verifyAttendance(@Body request: ApiAttendanceVerifyRequest): ApiResponse<Map<String, Any>>
}

object AttendanceApiClient {
    private var BASE_URL = "https://geoattend-api.example.com/"

    val service: GeoAttendApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeoAttendApiService::class.java)
    }

    fun updateBaseUrl(newUrl: String) {
        BASE_URL = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
    }
}

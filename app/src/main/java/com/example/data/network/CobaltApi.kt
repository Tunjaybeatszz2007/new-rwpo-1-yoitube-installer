package com.example.data.network

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface CobaltApi {
    @POST
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    suspend fun extractDownloadLink(
        @Url apiEndpoint: String,
        @Body request: CobaltRequest
    ): CobaltResponse
}

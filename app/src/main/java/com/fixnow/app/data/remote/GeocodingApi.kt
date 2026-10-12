package com.fixnow.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {
    @GET("reverse")
    suspend fun reverse(
        @Query("format") format: String = "jsonv2",
        @Query("lat") latitud: Double,
        @Query("lon") longitud: Double
    ): GeocodingResult
}

data class GeocodingResult(
    @SerializedName("display_name") val direccion: String = ""
)
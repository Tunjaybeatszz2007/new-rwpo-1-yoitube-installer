package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CobaltRequest(
    @Json(name = "url") val url: String,
    @Json(name = "videoQuality") val videoQuality: String? = "720",
    @Json(name = "audioFormat") val audioFormat: String? = "mp3",
    @Json(name = "isAudioOnly") val isAudioOnly: Boolean = false,
    @Json(name = "filenamePattern") val filenamePattern: String = "pretty"
)

@JsonClass(generateAdapter = true)
data class CobaltResponse(
    @Json(name = "status") val status: String,
    @Json(name = "url") val url: String? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "picker") val picker: List<CobaltPickerItem>? = null
)

@JsonClass(generateAdapter = true)
data class CobaltPickerItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "title") val title: String? = null
)

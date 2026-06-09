package com.example.util

import java.util.regex.Pattern

object YoutubeUtils {
    fun extractYoutubeVideoId(url: String): String? {
        val patterns = listOf(
            "v=([a-zA-Z0-9_-]{11})",
            "youtu\\.be/([a-zA-Z0-9_-]{11})",
            "youtube\\.com/shorts/([a-zA-Z0-9_-]{11})",
            "embed/([a-zA-Z0-9_-]{11})",
            "desktop/([a-zA-Z0-9_-]{11})"
        )
        for (patternString in patterns) {
            val pattern = Pattern.compile(patternString)
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    fun getYoutubeThumbnailUrl(url: String): String? {
        val id = extractYoutubeVideoId(url)
        return if (id != null) {
            "https://img.youtube.com/vi/$id/0.jpg"
        } else {
            null
        }
    }
}

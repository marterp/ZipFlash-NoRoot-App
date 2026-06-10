package com.zipflash.mrp.api

import com.google.gson.annotations.SerializedName

data class ModuleDto(
    val title: String,
    val linkText: String,
    val url: String,
    val description: String
)

data class UpdateResponse(
    @SerializedName("latest_version") val latestVersion: String,
    @SerializedName("apk_url") val apkUrl: String,
    val changelog: String
)

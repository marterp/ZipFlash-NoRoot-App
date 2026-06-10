package com.zipflash.mrp.api

import retrofit2.Call
import retrofit2.http.GET

interface ZipFlashApi {
    @GET("api/modules")
    suspend fun getModules(): List<ModuleDto>
}

interface ZipFlashUpdateApi {
    @GET("marternp/ZipFlash-NoRoot/refs/heads/main/update.json")
    fun checkUpdateSync(): Call<UpdateResponse>
}

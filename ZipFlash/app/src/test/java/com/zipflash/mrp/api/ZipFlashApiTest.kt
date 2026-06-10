package com.zipflash.mrp.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class ZipFlashApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ZipFlashApi
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        server = MockWebServer()
        server.start()

        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZipFlashApi::class.java)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun `getModules returns parsed modules`() = runTest(testDispatcher) {
        val json = """
            [
                {
                    "title": "Test Module",
                    "linkText": "Download",
                    "url": "https://example.com/module.zip",
                    "description": "A test module"
                },
                {
                    "title": "Module Two",
                    "linkText": "Get it",
                    "url": "https://example.com/module2.zip",
                    "description": "Second module"
                }
            ]
        """.trimIndent()

        server.enqueue(MockResponse().setBody(json).setResponseCode(200))

        val modules = api.getModules()

        assertEquals(2, modules.size)
        assertEquals("Test Module", modules[0].title)
        assertEquals("Download", modules[0].linkText)
        assertEquals("https://example.com/module.zip", modules[0].url)
        assertEquals("A test module", modules[0].description)
        assertEquals("Module Two", modules[1].title)
    }

    @Test
    fun `getModules handles empty response`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setBody("[]").setResponseCode(200))

        val modules = api.getModules()

        assertEquals(0, modules.size)
    }
}

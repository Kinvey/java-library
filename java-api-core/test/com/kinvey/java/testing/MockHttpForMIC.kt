package com.kinvey.java.testing

import com.google.api.client.http.HttpTransport
import com.google.api.client.http.LowLevelHttpRequest
import com.google.api.client.http.LowLevelHttpResponse
import com.google.api.client.json.GenericJson
import com.google.api.client.json.Json
import com.google.api.client.testing.http.MockLowLevelHttpRequest
import com.google.api.client.testing.http.MockLowLevelHttpResponse
import com.google.gson.Gson
import java.io.IOException

class MockHttpForMIC : HttpTransport() {

    @Throws(IOException::class)
    public override fun buildRequest(method: String, url: String): LowLevelHttpRequest {
        return object : MockLowLevelHttpRequest() {
            @Throws(IOException::class)
            override fun execute(): LowLevelHttpResponse? {
                if (url.contains("oauth/token")) {
                    return oauthToken()
                } else if (url.contains("oauth/auth")) {
                    return oauthAuth()
                } else if (url.contains("tempURL")) {
                    return tempURL()
                } else if (url.contains("/login")) {
                    return userLogin()
                }
                return null
            }
        }
    }

    private fun oauthToken(): LowLevelHttpResponse {
        val response = MockLowLevelHttpResponse()
        val content = GenericJson()
        content["access_token"] = "myAccess"
        content["refresh_token"] = "myRefresh"
        content["token_type"] = "bearer"
        content["expires_in"] = "100"
        response.contentType = Json.MEDIA_TYPE
        response.setContent(Gson().toJson(content))
        response.statusCode = 200
        return response
    }

    private fun oauthAuth(): MockLowLevelHttpResponse? {
        return null
    }

    private fun tempURL(): MockLowLevelHttpResponse? {
        return null
    }

    private fun userLogin(): MockLowLevelHttpResponse {
        val response = MockLowLevelHttpResponse()
        val content = GenericJson()
        content["_id"] = "123"
        response.contentType = Json.MEDIA_TYPE
        response.setContent(Gson().toJson(content))
        response.statusCode = 200
        return response
    }
}
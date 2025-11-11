package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName

data class PushTokenRegisterRequest(
    @SerializedName("token")
    val token: String,
    @SerializedName("platform")
    val platform: String = "android",
)

data class PushTokenUnregisterRequest(
    @SerializedName("token")
    val token: String,
)


package org.linphone.bcsws

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("access_token")
    val access_token: String,
    @SerializedName("token_type")
    val token_type: String,
    @SerializedName("expires_in")
    val expires_in: Long
)

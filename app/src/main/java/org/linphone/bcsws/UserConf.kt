package org.linphone.bcsws

import com.google.gson.annotations.SerializedName

data class Misc(
    @SerializedName("Buddies")
    val Buddies: List<Buddy>
)

data class Buddy(
    @SerializedName("Name")
    val Name: String,

    @SerializedName("Members")
    val Members: List<Member>
)

data class Member(
    @SerializedName("DisplayName")
    val DisplayName: String,

    @SerializedName("Uri")
    val Uri: String,

    @SerializedName("Buddy")
    val Buddy: Boolean
)

data class UserConf(
    @SerializedName("Id")
    val Id: String,

    @SerializedName("Misc")
    val Misc: Misc

)

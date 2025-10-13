package org.linphone.bcsws

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CallReportResponse(
    @SerializedName("Count")
    val Count: Int,
    @SerializedName("Items")
    val Items: List<CallReportItem>
) : Serializable

data class CallReportItem(
    @SerializedName("Id")
    val Id: String = "", // L'ID può essere vuoto in caso di POST
    @SerializedName("Direction")
    val Direction: String,
    @SerializedName("Duration")
    val Duration: Int,
    @SerializedName("IsConnected")
    val IsConnected: Boolean,
    @SerializedName("Timestamp")
    val Timestamp: String,
    @SerializedName("TermReason")
    val TermReason: String,
    @SerializedName("TermSipReason")
    val TermSipReason: Int,
    @SerializedName("Useragent")
    val Useragent: String,
    @SerializedName("RemoteParty")
    val RemoteParty: RemoteParty
) : Serializable

data class RemoteParty(
    @SerializedName("Uri")
    val Uri: String,
    @SerializedName("DisplayName")
    val DisplayName: String
) : Serializable

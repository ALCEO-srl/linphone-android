package org.linphone.bcsws

import com.google.gson.annotations.SerializedName
import java.io.Serializable
data class DirectoryResponse(
    @SerializedName("Count")
    val Count: Int,
    @SerializedName("Items")
    val Items: List<DirectoryItem>
)

data class DirectoryItem(
    @SerializedName("Id")
    val Id: String,
    @SerializedName("Uri")
    val Uri: String,
    @SerializedName("Name")
    val Name: String,
    @SerializedName("Surname")
    val Surname: String,
    @SerializedName("DisplayName")
    val DisplayName: String,
    @SerializedName("Profession")
    val Profession: String,
    @SerializedName("Company")
    val Company: String,
    @SerializedName("EmailAddress")
    val EmailAddress: String,
    @SerializedName("MobilePhone")
    val MobilePhone: String,
    @SerializedName("LandlinePhone")
    val LandlinePhone: String,
    @SerializedName("FaxPhone")
    val FaxPhone: String,
    @SerializedName("Title")
    val Title: String,
    @SerializedName("Address")
    val Address: String,
    @SerializedName("Branch")
    val Branch: String,
    @SerializedName("Office")
    val Office: String,
    @SerializedName("Manager")
    val Manager: String,
    @SerializedName("Assistant")
    val Assistant: String,
    @SerializedName("Attr1")
    val Attr1: String,
    @SerializedName("Attr2")
    val Attr2: String,
    @SerializedName("Attr3")
    val Attr3: String,
    @SerializedName("Attr4")
    val Attr4: String,
    @SerializedName("Attr5")
    val Attr5: String,
    @SerializedName("Attr6")
    val Attr6: String,
    @SerializedName("Attr7")
    val Attr7: String,
    @SerializedName("Attr8")
    val Attr8: String,
    @SerializedName("Attr9")
    val Attr9: String,
    @SerializedName("Attr10")
    val Attr10: String,
    @SerializedName("AvatarImageUrl")
    val AvatarImageUrl: String
) : Serializable

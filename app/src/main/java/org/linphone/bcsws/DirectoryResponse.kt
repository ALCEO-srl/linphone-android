package org.linphone.bcsws

data class DirectoryResponse(
    val Count: Int,
    val Items: List<DirectoryItem>
)

data class DirectoryItem(
    val Id: String,
    val Uri: String,
    val Name: String,
    val Surname: String,
    val DisplayName: String,
    val Profession: String,
    val Company: String,
    val EmailAddress: String,
    val MobilePhone: String,
    val LandlinePhone: String,
    val FaxPhone: String,
    val Title: String,
    val Address: String,
    val Branch: String,
    val Office: String,
    val Manager: String,
    val Assistant: String,
    val Attr1: String,
    val Attr2: String,
    val Attr3: String,
    val Attr4: String,
    val Attr5: String,
    val Attr6: String,
    val Attr7: String,
    val Attr8: String,
    val Attr9: String,
    val Attr10: String,
    val AvatarImageUrl: String
)

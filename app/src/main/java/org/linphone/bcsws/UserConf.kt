package org.linphone.bcsws

import com.google.gson.annotations.SerializedName

data class PresRules(
    @SerializedName("Blocked")
    val Blocked: List<String>,

    @SerializedName("Pending")
    val Pending: List<String>,

    @SerializedName("Allowed")
    val Allowed: List<String>
)

data class Misc(
    @SerializedName("LastUploadError")
    val LastUploadError: Int,

    @SerializedName("BeepOnIMMessage")
    val BeepOnIMMessage: String,

    @SerializedName("LogUploadStatus")
    val LogUploadStatus: Int,

    @SerializedName("EnanchedBusyBehaviour")
    val EnanchedBusyBehaviour: Int,

    @SerializedName("AuthenticationPassword")
    val AuthenticationPassword: String,

    @SerializedName("AutoAnswer")
    val AutoAnswer: Boolean,

    @SerializedName("DisplayNameOnly")
    val DisplayNameOnly: String,

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

data class RoutingProfile(
    @SerializedName("Note")
    val Note: String,

    @SerializedName("VoiceMail")
    val VoiceMail: Boolean,

    @SerializedName("DND")
    val DND: Boolean,

    @SerializedName("AltPhoneURIChoice")
    val AltPhoneURIChoice: String,

    @SerializedName("UCFTo")
    val UCFTo: String,

    @SerializedName("UCFToEnabled")
    val UCFToEnabled: Boolean,

    @SerializedName("AltPhone")
    val AltPhone: String
)

data class Main(
    @SerializedName("DisplayName")
    val DisplayName: String,

    @SerializedName("Name")
    val Name: String,

    @SerializedName("AccountType")
    val AccountType: String,

    @SerializedName("Surname")
    val Surname: String
)

data class DataHome(
    @SerializedName("Phone")
    val Phone: String
)

data class Competences(
    @SerializedName("NotAssignedCompetences")
    val NotAssignedCompetences: List<Competence>,

    @SerializedName("AssignedReadOnlyCompetences")
    val AssignedReadOnlyCompetences: List<Competence>,

    @SerializedName("AssignedCompetences")
    val AssignedCompetences: List<Competence>
)

data class Competence(
    @SerializedName("Id")
    val Id: String,

    @SerializedName("Rank")
    val Rank: String
)

data class DataOffice(
    @SerializedName("Assistant")
    val Assistant: String,

    @SerializedName("OfficeManager")
    val OfficeManager: String,

    @SerializedName("Field2")
    val Field2: String,

    @SerializedName("Phone")
    val Phone: String,

    @SerializedName("Field6")
    val Field6: String,

    @SerializedName("Field10")
    val Field10: String,

    @SerializedName("Field4")
    val Field4: String,

    @SerializedName("Field1")
    val Field1: String,

    @SerializedName("Fax")
    val Fax: String,

    @SerializedName("Mobile")
    val Mobile: String,

    @SerializedName("Field5")
    val Field5: String,

    @SerializedName("HomePage")
    val HomePage: String,

    @SerializedName("Field8")
    val Field8: String,

    @SerializedName("Mail")
    val Mail: String,

    @SerializedName("Field9")
    val Field9: String,

    @SerializedName("Company")
    val Company: String,

    @SerializedName("Office")
    val Office: String,

    @SerializedName("Department")
    val Department: String,

    @SerializedName("Icon")
    val Icon: String,

    @SerializedName("Field7")
    val Field7: String,

    @SerializedName("Position")
    val Position: String,

    @SerializedName("Field3")
    val Field3: String,

    @SerializedName("Address")
    val Address: String
)

data class UserConf(
    @SerializedName("Id")
    val Id: String,

    @SerializedName("PresRules")
    val PresRules: PresRules,

    @SerializedName("Misc")
    val Misc: Misc,

    @SerializedName("RoutingProfile")
    val RoutingProfile: RoutingProfile,

    @SerializedName("Main")
    val Main: Main,

    @SerializedName("DataHome")
    val DataHome: DataHome,

    @SerializedName("Competences")
    val Competences: Competences,

    @SerializedName("DataOffice")
    val DataOffice: DataOffice
)

package org.linphone.bcsws

data class PresRules(
    val Blocked: List<String>,
    val Pending: List<String>,
    val Allowed: List<String>
)

data class Misc(
    val LastUploadError: Int,
    val BeepOnIMMessage: String,
    val LogUploadStatus: Int,
    val EnanchedBusyBehaviour: Int,
    val AuthenticationPassword: String,
    val AutoAnswer: Boolean,
    val DisplayNameOnly: String,
    val Buddies: List<Buddy>
)

data class Buddy(
    val Name: String,
    val Members: List<Member>
)

data class Member(
    val DisplayName: String,
    val Uri: String,
    val Buddy: Boolean
)

data class RoutingProfile(
    val Note: String,
    val VoiceMail: Boolean,
    val DND: Boolean,
    val AltPhoneURIChoice: String,
    val UCFTo: String,
    val UCFToEnabled: Boolean,
    val AltPhone: String
)

data class Main(
    val DisplayName: String,
    val Name: String,
    val AccountType: String,
    val Surname: String
)

data class DataHome(
    val Phone: String
)

data class Competences(
    val NotAssignedCompetences: List<Competence>,
    val AssignedReadOnlyCompetences: List<Competence>,
    val AssignedCompetences: List<Competence>
)

data class Competence(
    val Id: String,
    val Rank: String
)

data class DataOffice(
    val Assistant: String,
    val OfficeManager: String,
    val Field2: String,
    val Phone: String,
    val Field6: String,
    val Field10: String,
    val Field4: String,
    val Field1: String,
    val Fax: String,
    val Mobile: String,
    val Field5: String,
    val HomePage: String,
    val Field8: String,
    val Mail: String,
    val Field9: String,
    val Company: String,
    val Office: String,
    val Department: String,
    val Icon: String,
    val Field7: String,
    val Position: String,
    val Field3: String,
    val Address: String
)

data class UserConf(
    val Id: String,
    val PresRules: PresRules,
    val Misc: Misc,
    val RoutingProfile: RoutingProfile,
    val Main: Main,
    val DataHome: DataHome,
    val Competences: Competences,
    val DataOffice: DataOffice
)

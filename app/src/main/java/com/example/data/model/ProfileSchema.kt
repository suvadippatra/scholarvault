package com.scholarvault.data.model

data class UserProfile(
    val id: String = "CURRENT_USER",
    val profilePicUri: String? = null,
    val digitalSignUri: String? = null,
    val socialLinksJson: String = "[]",
    val customFieldsJson: String = "[]",
    
    // Core Identity
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val motherTongue: String = "",
    val maritalStatus: String = "",
    val caste: String = "",
    val casteCertificateNumber: String = "",
    val religion: String = "",

    // Contact Details
    val mobileNumber: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val presentAddress: String = "",
    val permanentAddress: String = "",
    val isPermanentSameAsPresent: Boolean = false,

    // Guardianship
    val fatherName: String = "",
    val fatherQualification: String = "",
    val fatherOccupation: String = "",
    val fatherNumber: String = "",
    val fatherMail: String = "",
    val motherName: String = "",
    val motherQualification: String = "",
    val motherOccupation: String = "",
    val motherNumber: String = "",
    val motherMail: String = "",
    val guardianName: String = "",
    val guardianRelationship: String = "",
    val guardianNumber: String = "",
    val familyIncome: String = "",
    val emergencyContact: String = "",

    // Digital & Research
    val professionalSummary: String = ""
)

data class ProfileExperience(
    val id: String,
    val profileId: String = "CURRENT_USER",
    val role: String,
    val duration: String,
    val location: String
)

data class ProfileWork(
    val id: String,
    val profileId: String = "CURRENT_USER",
    val title: String,
    val date: String,
    val isWebLink: Boolean
)

data class ProfileDocumentLink(
    val id: String,
    val fieldKey: String, // e.g., "casteCertificateNumber", "familyIncome"
    val walletCardId: String? = null,
    val attachmentId: String? = null,
    val displayName: String = ""
)

data class UserProfileWithDetails(
    val profile: UserProfile,
    val experiences: List<ProfileExperience>,
    val works: List<ProfileWork>,
    val documents: List<ProfileDocumentLink> = emptyList()
)

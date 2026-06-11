package com.scholarvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
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

@Entity(
    tableName = "profile_experiences",
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class ProfileExperience(
    @PrimaryKey
    val id: String,
    val profileId: String = "CURRENT_USER",
    val role: String,
    val duration: String,
    val location: String
)

@Entity(
    tableName = "profile_works",
    foreignKeys = [
        ForeignKey(
            entity = UserProfile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class ProfileWork(
    @PrimaryKey
    val id: String,
    val profileId: String = "CURRENT_USER",
    val title: String,
    val date: String,
    val isWebLink: Boolean
)

data class UserProfileWithDetails(
    @Embedded val profile: UserProfile,
    @Relation(
        parentColumn = "id",
        entityColumn = "profileId"
    )
    val experiences: List<ProfileExperience>,
    @Relation(
        parentColumn = "id",
        entityColumn = "profileId"
    )
    val works: List<ProfileWork>
)

package com.scholarvault.data.dao

import androidx.room.*
import com.scholarvault.data.model.ProfileExperience
import com.scholarvault.data.model.ProfileWork
import com.scholarvault.data.model.UserProfile
import com.scholarvault.data.model.UserProfileWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Transaction
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun getUserProfileWithDetails(id: String = "CURRENT_USER"): Flow<UserProfileWithDetails?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfileOrIgnore(profile: UserProfile): Long

    @Update
    suspend fun updateProfile(profile: UserProfile)

    @Transaction
    suspend fun insertProfile(profile: UserProfile) {
        val id = insertProfileOrIgnore(profile)
        if (id == -1L) {
            updateProfile(profile)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperience(experience: ProfileExperience)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWork(work: ProfileWork)

    @Delete
    suspend fun deleteExperience(experience: ProfileExperience)

    @Delete
    suspend fun deleteWork(work: ProfileWork)
}

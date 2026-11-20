package com.billing.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.billing.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY username ASC")
    fun getActiveUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role AND isActive = 1 ORDER BY username ASC")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: String)

    @Query("SELECT COUNT(*) FROM users")
    fun getUserCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCountDirect(): Int

    @Query("UPDATE users SET failedAttempts = :attempts, lockedUntil = :lockedUntil WHERE id = :id")
    suspend fun updateFailedAttempts(id: String, attempts: Int, lockedUntil: Long?)

    @Query("UPDATE users SET failedAttempts = 0, lockedUntil = NULL WHERE id = :id")
    suspend fun resetFailedAttempts(id: String)

    @Query("UPDATE users SET pinHash = :pinHash, pinSalt = :pinSalt, mustSetPin = :mustSetPin WHERE id = :id")
    suspend fun updatePin(id: String, pinHash: String, pinSalt: String, mustSetPin: Boolean = false)

    @Query("UPDATE users SET isActive = :isActive WHERE id = :id")
    suspend fun setUserActiveStatus(id: String, isActive: Boolean)
}

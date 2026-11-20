package com.billing.app.domain.repository

import com.billing.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val currentUser: StateFlow<UserEntity?>
    fun getAllActiveUsers(): Flow<List<UserEntity>>
    fun getAllUsers(): Flow<List<UserEntity>>
    suspend fun getUserById(userId: String): UserEntity?
    suspend fun login(userId: String, pin: String): Result<UserEntity>
    fun logout()
    suspend fun createDefaultAdminIfNeeded()
    suspend fun verifyManagerPin(pin: String): Boolean
    suspend fun setUserPin(userId: String, newPin: String)
    suspend fun createUser(username: String, displayName: String, pin: String, role: String): Result<UserEntity>
    suspend fun deleteUser(userId: String)
    suspend fun setUserActiveStatus(userId: String, isActive: Boolean)
}

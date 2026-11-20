package com.billing.app.data.repository

import com.billing.app.core.util.SecurityUtil
import com.billing.app.data.local.dao.UserDao
import com.billing.app.data.local.entity.UserEntity
import com.billing.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class UserRepositoryImpl(
    private val userDao: UserDao
) : UserRepository {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    override val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    override fun getAllActiveUsers(): Flow<List<UserEntity>> {
        return userDao.getActiveUsers()
    }

    override fun getAllUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }

    override suspend fun getUserById(userId: String): UserEntity? {
        return userDao.getUserById(userId)
    }

    override suspend fun login(userId: String, pin: String): Result<UserEntity> {
        val user = userDao.getUserById(userId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        if (!user.isActive) {
            return Result.failure(IllegalStateException("User account is inactive"))
        }

        if (user.mustSetPin || user.pinHash.isBlank()) {
            return Result.failure(IllegalStateException("PIN not set. Please set up PIN first."))
        }

        val now = System.currentTimeMillis()
        if (user.lockedUntil != null && user.lockedUntil > now) {
            val remainingSeconds = kotlin.math.ceil((user.lockedUntil - now) / 1000.0).toLong().coerceAtLeast(1L)
            return Result.failure(com.billing.app.domain.model.AppError.AccountLocked(remainingSeconds))
        }

        val isPinValid = SecurityUtil.verifyPin(pin, user.pinSalt, user.pinHash)
        return if (isPinValid) {
            val loggedInUser = user.copy(
                failedAttempts = 0,
                lockedUntil = null
            )
            userDao.updateUser(loggedInUser)
            _currentUser.value = loggedInUser
            Result.success(loggedInUser)
        } else {
            val newFailedAttempts = user.failedAttempts + 1
            val isLocked = newFailedAttempts >= 5
            val lockedUntil = if (isLocked) now + 30_000L else null
            val attemptsToStore = if (isLocked) 0 else newFailedAttempts
            val updatedUser = user.copy(
                failedAttempts = attemptsToStore,
                lockedUntil = lockedUntil
            )
            userDao.updateUser(updatedUser)

            if (isLocked) {
                Result.failure(com.billing.app.domain.model.AppError.AccountLocked(30L))
            } else {
                Result.failure(com.billing.app.domain.model.AppError.IncorrectPin(5 - newFailedAttempts))
            }
        }
    }

    override fun logout() {
        _currentUser.value = null
    }

    override suspend fun createDefaultAdminIfNeeded() {
        val count = userDao.getUserCountDirect()
        if (count == 0) {
            val existingAdmin = userDao.getUserByUsername("admin")
            if (existingAdmin == null) {
                val salt = SecurityUtil.generateSalt()
                val hash = SecurityUtil.hashPin("1234", salt)
                val defaultAdmin = UserEntity(
                    username = "admin",
                    displayName = "Store Owner",
                    pinHash = hash,
                    pinSalt = salt,
                    role = "ADMIN",
                    isActive = true,
                    failedAttempts = 0,
                    lockedUntil = null,
                    mustSetPin = false
                )
                userDao.insertUser(defaultAdmin)
            }
        }
    }

    override suspend fun verifyManagerPin(pin: String): Boolean {
        if (pin.isBlank()) return false
        val activeUsers = userDao.getActiveUsers().first()
        val now = System.currentTimeMillis()
        val managers = activeUsers.filter { user ->
            (user.role.equals("ADMIN", ignoreCase = true) || user.role.equals("MANAGER", ignoreCase = true)) &&
                !user.mustSetPin &&
                user.pinHash.isNotBlank() &&
                (user.lockedUntil == null || user.lockedUntil <= now)
        }
        return managers.any { user ->
            SecurityUtil.verifyPin(pin, user.pinSalt, user.pinHash)
        }
    }

    override suspend fun setUserPin(userId: String, newPin: String) {
        val user = userDao.getUserById(userId) ?: return
        val salt = SecurityUtil.generateSalt()
        val hash = SecurityUtil.hashPin(newPin, salt)
        val updatedUser = user.copy(
            pinSalt = salt,
            pinHash = hash,
            mustSetPin = false,
            failedAttempts = 0,
            lockedUntil = null
        )
        userDao.updateUser(updatedUser)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updatedUser
        }
    }

    override suspend fun createUser(
        username: String,
        displayName: String,
        pin: String,
        role: String
    ): Result<UserEntity> {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isBlank()) {
            return Result.failure(IllegalArgumentException("Username cannot be blank"))
        }

        val existing = userDao.getUserByUsername(trimmedUsername)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Username '$trimmedUsername' already exists"))
        }

        val (salt, hash, mustSet) = if (pin.isNotBlank()) {
            val s = SecurityUtil.generateSalt()
            val h = SecurityUtil.hashPin(pin, s)
            Triple(s, h, false)
        } else {
            Triple("", "", true)
        }

        val newUser = UserEntity(
            username = trimmedUsername,
            displayName = displayName.trim().ifBlank { trimmedUsername },
            pinSalt = salt,
            pinHash = hash,
            role = role.trim().uppercase().ifBlank { "CASHIER" },
            isActive = true,
            failedAttempts = 0,
            lockedUntil = null,
            mustSetPin = mustSet
        )
        userDao.insertUser(newUser)
        return Result.success(newUser)
    }

    override suspend fun deleteUser(userId: String) {
        userDao.deleteUser(userId)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = null
        }
    }

    override suspend fun setUserActiveStatus(userId: String, isActive: Boolean) {
        userDao.setUserActiveStatus(userId, isActive)
        if (!isActive && _currentUser.value?.id == userId) {
            _currentUser.value = null
        }
    }
}

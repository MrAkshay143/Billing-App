package com.billing.app.data.repository

import com.billing.app.core.util.SecurityUtil
import com.billing.app.data.local.dao.UserDao
import com.billing.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRepositoryTest {

    private lateinit var fakeUserDao: FakeUserDao
    private lateinit var userRepository: UserRepositoryImpl

    @Before
    fun setup() {
        fakeUserDao = FakeUserDao()
        userRepository = UserRepositoryImpl(fakeUserDao)
    }

    @Test
    fun testCreateDefaultAdminIfNeeded() = runBlocking {
        assertEquals(0, fakeUserDao.getUserCountDirect())

        userRepository.createDefaultAdminIfNeeded()

        assertEquals(1, fakeUserDao.getUserCountDirect())
        val admin = fakeUserDao.getUserByUsername("admin")
        assertNotNull(admin)
        assertEquals("admin", admin!!.username)
        assertEquals("Store Owner", admin.displayName)
        assertEquals("ADMIN", admin.role)
        assertTrue(admin.isActive)
        assertFalse(admin.mustSetPin)
        assertTrue(SecurityUtil.verifyPin("1234", admin.pinSalt, admin.pinHash))

        // Calling again should not duplicate admin
        userRepository.createDefaultAdminIfNeeded()
        assertEquals(1, fakeUserDao.getUserCountDirect())
    }

    @Test
    fun testLoginSuccess() = runBlocking {
        userRepository.createDefaultAdminIfNeeded()
        val admin = fakeUserDao.getUserByUsername("admin")!!

        val result = userRepository.login(admin.id, "1234")
        assertTrue(result.isSuccess)
        assertEquals(admin.id, result.getOrNull()?.id)
        assertEquals(admin.id, userRepository.currentUser.value?.id)
    }

    @Test
    fun testLoginWrongPinIncrementsAttempts() = runBlocking {
        userRepository.createDefaultAdminIfNeeded()
        val admin = fakeUserDao.getUserByUsername("admin")!!

        val result = userRepository.login(admin.id, "9999")
        assertTrue(result.isFailure)

        val updated = fakeUserDao.getUserById(admin.id)!!
        assertEquals(1, updated.failedAttempts)
        assertNull(updated.lockedUntil)
        assertNull(userRepository.currentUser.value)
    }

    @Test
    fun testLockoutAfterFiveFailedAttempts() = runBlocking {
        userRepository.createDefaultAdminIfNeeded()
        val admin = fakeUserDao.getUserByUsername("admin")!!

        // 4 failed attempts
        for (i in 1..4) {
            val res = userRepository.login(admin.id, "0000")
            assertTrue(res.isFailure)
        }
        var updated = fakeUserDao.getUserById(admin.id)!!
        assertEquals(4, updated.failedAttempts)
        assertNull(updated.lockedUntil)

        // 5th failed attempt triggers 30-sec lockout
        val fifthRes = userRepository.login(admin.id, "0000")
        assertTrue(fifthRes.isFailure)
        assertTrue(fifthRes.exceptionOrNull()?.message?.contains("locked for 30 seconds") == true)

        updated = fakeUserDao.getUserById(admin.id)!!
        assertEquals(0, updated.failedAttempts) // Reset attempts after lock
        assertNotNull(updated.lockedUntil)
        assertTrue(updated.lockedUntil!! > System.currentTimeMillis())

        // Immediate subsequent login with correct PIN must still fail due to lock
        val lockedRes = userRepository.login(admin.id, "1234")
        assertTrue(lockedRes.isFailure)
        assertTrue(lockedRes.exceptionOrNull()?.message?.contains("locked") == true)
    }

    @Test
    fun testZeroTrustMustSetPinBlocksLogin() = runBlocking {
        val salt = SecurityUtil.generateSalt()
        val user = UserEntity(
            username = "cashier1",
            displayName = "Cashier One",
            pinHash = SecurityUtil.hashPin("1111", salt),
            pinSalt = salt,
            role = "CASHIER",
            mustSetPin = true
        )
        fakeUserDao.insertUser(user)

        val result = userRepository.login(user.id, "1111")
        assertTrue(result.isFailure)
        assertEquals("PIN not set. Please set up PIN first.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testVerifyManagerPin() = runBlocking {
        userRepository.createDefaultAdminIfNeeded() // Admin PIN "1234"

        // Cashier with PIN "5555"
        val cashierRes = userRepository.createUser("cashier1", "Cashier", "5555", "CASHIER")
        assertTrue(cashierRes.isSuccess)

        // Manager with PIN "7777"
        val managerRes = userRepository.createUser("mgr1", "Manager", "7777", "MANAGER")
        assertTrue(managerRes.isSuccess)

        // Test ADMIN PIN
        assertTrue(userRepository.verifyManagerPin("1234"))

        // Test MANAGER PIN
        assertTrue(userRepository.verifyManagerPin("7777"))

        // Cashier PIN should NOT work as manager PIN
        assertFalse(userRepository.verifyManagerPin("5555"))

        // Wrong PIN should return false
        assertFalse(userRepository.verifyManagerPin("0000"))
    }

    @Test
    fun testSetUserPin() = runBlocking {
        userRepository.createDefaultAdminIfNeeded()
        val admin = fakeUserDao.getUserByUsername("admin")!!

        userRepository.setUserPin(admin.id, "9876")

        val updated = fakeUserDao.getUserById(admin.id)!!
        assertFalse(updated.mustSetPin)
        assertEquals(0, updated.failedAttempts)
        assertNull(updated.lockedUntil)
        assertTrue(SecurityUtil.verifyPin("9876", updated.pinSalt, updated.pinHash))

        // Can login with new PIN
        val loginRes = userRepository.login(admin.id, "9876")
        assertTrue(loginRes.isSuccess)
    }

    @Test
    fun testLogout() = runBlocking {
        userRepository.createDefaultAdminIfNeeded()
        val admin = fakeUserDao.getUserByUsername("admin")!!

        userRepository.login(admin.id, "1234")
        assertNotNull(userRepository.currentUser.value)

        userRepository.logout()
        assertNull(userRepository.currentUser.value)
    }

    @Test
    fun testCreateUserDuplicateUsernameFails() = runBlocking {
        val res1 = userRepository.createUser("staff1", "Staff", "1111", "CASHIER")
        assertTrue(res1.isSuccess)

        val res2 = userRepository.createUser("staff1", "Staff Duplicate", "2222", "CASHIER")
        assertTrue(res2.isFailure)
    }

    // Helper fake DAO
    private class FakeUserDao : UserDao {
        val users = mutableListOf<UserEntity>()
        private val usersFlow = MutableStateFlow<List<UserEntity>>(emptyList())

        private fun updateFlow() {
            usersFlow.value = users.sortedBy { it.username }.toList()
        }

        override fun getAllUsers(): Flow<List<UserEntity>> = usersFlow

        override fun getActiveUsers(): Flow<List<UserEntity>> =
            usersFlow.map { list -> list.filter { it.isActive } }

        override suspend fun getUserById(id: String): UserEntity? =
            users.find { it.id == id }

        override suspend fun getUserByUsername(username: String): UserEntity? =
            users.find { it.username.equals(username, ignoreCase = true) }

        override fun getUsersByRole(role: String): Flow<List<UserEntity>> =
            usersFlow.map { list -> list.filter { it.role.equals(role, ignoreCase = true) && it.isActive } }

        override suspend fun upsertUser(user: UserEntity) {
            val index = users.indexOfFirst { it.id == user.id }
            if (index >= 0) users[index] = user else users.add(user)
            updateFlow()
        }

        override suspend fun upsertAll(users: List<UserEntity>) {
            users.forEach { upsertUser(it) }
        }

        override suspend fun insertUser(user: UserEntity) {
            if (users.any { it.username.equals(user.username, ignoreCase = true) }) {
                throw IllegalArgumentException("Username already exists")
            }
            users.add(user)
            updateFlow()
        }

        override suspend fun updateUser(user: UserEntity) {
            val index = users.indexOfFirst { it.id == user.id }
            if (index >= 0) {
                users[index] = user
                updateFlow()
            }
        }

        override suspend fun deleteUser(id: String) {
            users.removeAll { it.id == id }
            updateFlow()
        }

        override fun getUserCount(): Flow<Int> =
            usersFlow.map { it.size }

        override suspend fun getUserCountDirect(): Int = users.size

        override suspend fun updateFailedAttempts(id: String, attempts: Int, lockedUntil: Long?) {
            val index = users.indexOfFirst { it.id == id }
            if (index >= 0) {
                users[index] = users[index].copy(failedAttempts = attempts, lockedUntil = lockedUntil)
                updateFlow()
            }
        }

        override suspend fun resetFailedAttempts(id: String) {
            updateFailedAttempts(id, 0, null)
        }

        override suspend fun updatePin(id: String, pinHash: String, pinSalt: String, mustSetPin: Boolean) {
            val index = users.indexOfFirst { it.id == id }
            if (index >= 0) {
                users[index] = users[index].copy(pinHash = pinHash, pinSalt = pinSalt, mustSetPin = mustSetPin)
                updateFlow()
            }
        }

        override suspend fun setUserActiveStatus(id: String, isActive: Boolean) {
            val index = users.indexOfFirst { it.id == id }
            if (index >= 0) {
                users[index] = users[index].copy(isActive = isActive)
                updateFlow()
            }
        }
    }
}

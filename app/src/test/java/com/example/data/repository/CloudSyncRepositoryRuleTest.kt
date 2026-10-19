package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndGetUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(
                displayName = "Barun Kr Paul",
                email = ALICE_EMAIL,
                mobileNumber = "+91 98765 43210",
                investorCategory = "RETAIL",
                syncedDematCount = 4,
                syncedApplicationCount = 2
            )
        }
        assertTrue(saveResult.isSuccess)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile(uid)
        }
        assertTrue(profileResult.isSuccess)
        val profile = profileResult.getOrNull()
        assertNotNull(profile)
        assertEquals("Barun Kr Paul", profile?.displayName)
        assertEquals("+91 98765 43210", profile?.mobileNumber)
    }

    @Test
    fun addAndObserveWatchlist_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore)

        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addWatchlistItem(
                ipoSymbol = "SWIGGY",
                companyName = "Swiggy Ltd",
                targetLots = 4,
                notes = "Apply 1 lot per family Demat account"
            ).getOrThrow()
        }

        val emittedItems = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWatchlist(uid).first { list -> list.any { it.id == itemId } }
        }
        assertTrue(emittedItems.any { it.id == itemId })
    }

    @Test
    fun getUserProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudSyncRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveUserProfile(
                displayName = "Alice Investor",
                email = ALICE_EMAIL,
                mobileNumber = "+91 98765 00001"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudSyncRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getUserProfile(aliceUid)
        }
        assertTrue(crossResult.isFailure)
        val ex = crossResult.exceptionOrNull() as? FirebaseFirestoreException
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun observeWatchlist_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = CloudSyncRepository(firestore)

        var thrownException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeWatchlist(aliceUid).first()
            }
        } catch (t: Throwable) {
            thrownException = t
        }
        val firestoreEx = generateSequence(thrownException) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}

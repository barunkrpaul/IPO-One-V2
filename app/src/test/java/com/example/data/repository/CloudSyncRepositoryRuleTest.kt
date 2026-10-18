package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertThrows
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
        assertThat(saveResult.isSuccess).isTrue()

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getUserProfile(uid)
        }
        assertThat(profileResult.isSuccess).isTrue()
        val profile = profileResult.getOrNull()
        assertThat(profile).isNotNull()
        assertThat(profile?.displayName).isEqualTo("Barun Kr Paul")
        assertThat(profile?.mobileNumber).isEqualTo("+91 98765 43210")
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
        assertThat(emittedItems.map { it.id }).contains(itemId)
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
        assertThat(crossResult.isFailure).isTrue()
        val ex = crossResult.exceptionOrNull() as? FirebaseFirestoreException
        assertThat(ex?.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
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
        assertThat(firestoreEx).isNotNull()
        assertThat(firestoreEx?.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}

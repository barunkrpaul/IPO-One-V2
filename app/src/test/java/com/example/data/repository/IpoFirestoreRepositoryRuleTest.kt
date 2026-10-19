package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class IpoFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addAndObservePanProfile_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice@ipoone.test")
        val repo = IpoFirestoreRepository(firestore)

        val addResult = withTimeout(5000L) {
            repo.addPanProfile(
                holderName = "Alice Investor",
                panNumber = "ABCDE1234F",
                dematId = "1208160012345678",
                upiId = "alice@okicici",
                category = "Retail"
            )
        }
        assertTrue(addResult.isSuccess)
        val panId = addResult.getOrThrow()

        val observed = withTimeout(3000L) {
            repo.observePanProfiles(aliceUid).first { list -> list.any { it.id == panId } }
        }
        assertTrue(observed.any { it.id == panId && it.panNumber == "ABCDE1234F" })
    }

    @Test
    fun observePanProfiles_crossUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice2@ipoone.test")
        val aliceRepo = IpoFirestoreRepository(firestore)
        withTimeout(5000L) {
            aliceRepo.addPanProfile(
                holderName = "Alice Investor",
                panNumber = "ABCDE1234F",
                dematId = "1208160012345678",
                upiId = "alice@okicici",
                category = "Retail"
            )
        }

        signInTestUser("bob@ipoone.test")
        val bobRepo = IpoFirestoreRepository(firestore)
        try {
            withTimeout(3000L) {
                bobRepo.observePanProfiles(aliceUid).first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: Throwable) {
            val ffe = (e as? FirebaseFirestoreException)
                ?: (e.cause as? FirebaseFirestoreException)
                ?: (e.cause?.cause as? FirebaseFirestoreException)
            assertTrue("Expected FirebaseFirestoreException but got $e", ffe != null)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
        }
    }

    @Test
    fun observePanProfiles_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = IpoFirestoreRepository(firestore)
        try {
            withTimeout(3000L) {
                repo.observePanProfiles("any_user_123").first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for unauthenticated caller")
        } catch (e: Throwable) {
            val ffe = (e as? FirebaseFirestoreException)
                ?: (e.cause as? FirebaseFirestoreException)
                ?: (e.cause?.cause as? FirebaseFirestoreException)
            assertTrue("Expected FirebaseFirestoreException but got $e", ffe != null)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
        }
    }
}

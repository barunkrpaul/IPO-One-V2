package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.remote.OperationType
import com.example.data.remote.UserProfile
import com.example.data.remote.WatchlistItem
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun saveUserProfile(
        displayName: String,
        email: String,
        mobileNumber: String,
        investorCategory: String = "RETAIL",
        syncedDematCount: Int = 0,
        syncedApplicationCount: Int = 0
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updatePayload = mapOf(
                    "displayName" to displayName.trim().ifBlank { "IPO Investor" }.take(100),
                    "email" to email.trim().take(150),
                    "mobileNumber" to mobileNumber.trim().take(20),
                    "investorCategory" to investorCategory,
                    "syncedDematCount" to syncedDematCount.toLong(),
                    "syncedApplicationCount" to syncedApplicationCount.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.trim().ifBlank { "IPO Investor" }.take(100),
                    "email" to email.trim().take(150),
                    "mobileNumber" to mobileNumber.trim().take(20),
                    "investorCategory" to investorCategory,
                    "syncedDematCount" to syncedDematCount.toLong(),
                    "syncedApplicationCount" to syncedApplicationCount.toLong(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(targetUserId: String = requireUserId()): Result<UserProfile?> {
        val docRef = db.collection("users").document(targetUserId)
        return try {
            val snap = docRef.get().await()
            val profile = if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    suspend fun addWatchlistItem(
        ipoSymbol: String,
        companyName: String,
        targetLots: Int,
        notes: String
    ): Result<String> {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("ipo_watchlists")
        val docRef = collectionRef.document()
        return try {
            val payload = mapOf(
                "userId" to uid,
                "ipoSymbol" to ipoSymbol.trim().take(30),
                "companyName" to companyName.trim().take(120),
                "targetLots" to targetLots.coerceIn(1, 100).toLong(),
                "notes" to notes.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeWatchlist(userId: String): Flow<List<WatchlistItem>> {
        val collectionRef = db.collection("users").document(userId).collection("ipo_watchlists")
        return collectionRef
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(WatchlistItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, collectionRef.path)
                throw error
            }
    }

    suspend fun deleteWatchlistItem(watchlistId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("ipo_watchlists").document(watchlistId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}

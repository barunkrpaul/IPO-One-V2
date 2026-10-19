package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.IpoApplication
import com.example.data.model.IpoExtras
import com.example.data.model.PanProfile
import com.example.data.model.UserProfile
import com.example.data.model.WatchlistItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class IpoFirestoreRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before writing to Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun saveUserProfile(
        displayName: String,
        email: String?,
        phoneNumber: String?,
        authProvider: String,
        biometricEnabled: Boolean,
        priceAlertsEnabled: Boolean = true
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.take(100).ifBlank { "IPO Investor" },
                    "email" to email?.take(150),
                    "phoneNumber" to phoneNumber?.take(25),
                    "authProvider" to authProvider.take(40),
                    "biometricEnabled" to biometricEnabled,
                    "priceAlertsEnabled" to priceAlertsEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updatePayload = mapOf(
                    "displayName" to displayName.take(100).ifBlank { "IPO Investor" },
                    "email" to email?.take(150),
                    "phoneNumber" to phoneNumber?.take(25),
                    "authProvider" to authProvider.take(40),
                    "biometricEnabled" to biometricEnabled,
                    "priceAlertsEnabled" to priceAlertsEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(updatePayload, SetOptions.merge()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateBiometricPreference(enabled: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val user = auth.currentUser
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to (user?.displayName?.take(100) ?: "IPO Investor"),
                    "email" to user?.email?.take(150),
                    "biometricEnabled" to enabled,
                    "priceAlertsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "biometricEnabled" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observePanProfiles(userId: String): Flow<List<PanProfile>> {
        val colRef = db.collection("users").document(userId).collection("pan_profiles")
        return colRef.whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PanProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun addPanProfile(
        holderName: String,
        panNumber: String,
        dematId: String,
        upiId: String,
        category: String
    ): Result<String> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("pan_profiles")
        val docRef = colRef.document()
        return try {
            val payload = mapOf(
                "userId" to uid,
                "holderName" to holderName.trim().take(80),
                "panNumber" to panNumber.trim().uppercase().take(10),
                "dematId" to dematId.trim().take(32),
                "upiId" to upiId.trim().take(80),
                "category" to if (category in listOf("Retail", "sHNI", "bHNI")) category else "Retail",
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

    suspend fun updatePanProfile(
        panId: String,
        holderName: String,
        panNumber: String,
        dematId: String,
        upiId: String,
        category: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("pan_profiles").document(panId)
        return try {
            val validCat = if (category in listOf("Retail", "sHNI", "bHNI")) category else "Retail"
            docRef.update(
                mapOf(
                    "holderName" to holderName.trim().take(80),
                    "panNumber" to panNumber.trim().uppercase().padEnd(10, 'X').take(10),
                    "dematId" to dematId.trim().ifBlank { "1208160000000001" }.take(32),
                    "upiId" to upiId.trim().ifBlank { "investor@upi" }.take(80),
                    "category" to validCat,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePanProfile(panId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("pan_profiles").document(panId)
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

    fun observeApplications(userId: String): Flow<List<IpoApplication>> {
        val colRef = db.collection("users").document(userId).collection("applications")
        return colRef.whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    IpoApplication::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun submitIpoApplication(
        ipoId: String,
        ipoName: String,
        panHolderName: String,
        panNumber: String,
        category: String,
        lotsApplied: Int,
        sharesApplied: Int,
        bidPrice: Int,
        totalAmount: Int,
        upiId: String,
        status: String = "Applied"
    ): Result<String> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("applications")
        val docRef = colRef.document()
        return try {
            val payload = mapOf(
                "userId" to uid,
                "ipoId" to ipoId.take(64),
                "ipoName" to ipoName.take(120),
                "panHolderName" to panHolderName.trim().take(80),
                "panNumber" to panNumber.trim().uppercase().take(10),
                "category" to if (category in listOf("Retail", "sHNI", "bHNI")) category else "Retail",
                "lotsApplied" to lotsApplied.coerceIn(1, 1000).toLong(),
                "sharesApplied" to sharesApplied.coerceIn(1, 1000000).toLong(),
                "bidPrice" to bidPrice.coerceIn(1, 100000).toLong(),
                "totalAmount" to totalAmount.coerceIn(1, 100000000).toLong(),
                "upiId" to upiId.trim().take(80),
                "status" to if (status in listOf("Mandate Pending", "Applied", "Allotted", "Not Allotted")) status else "Applied",
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

    suspend fun updateApplicationStatus(appId: String, newStatus: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("applications").document(appId)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun requestNewMandate(appId: String, newUpiId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("applications").document(appId)
        return try {
            docRef.update(
                mapOf(
                    "upiId" to newUpiId.trim().ifBlank { "investor@upi" }.take(80),
                    "status" to "Mandate Pending",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteApplication(appId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("applications").document(appId)
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

    fun observeWatchlist(userId: String): Flow<List<WatchlistItem>> {
        val colRef = db.collection("users").document(userId).collection("watchlist")
        return colRef.whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WatchlistItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    /**
     * Hand-maintained IPO data that InvestorGain does not provide, keyed by the
     * InvestorGain IPO id. Readable without sign-in so guests get it too.
     */
    fun observeIpoExtras(): Flow<Map<String, IpoExtras>> {
        val colRef = db.collection("ipo_extras")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.documents.associate { doc ->
                    // Timestamps become java.util.Date so the model stays free of Firebase types
                    val data = doc.data.mapValues { (_, value) ->
                        if (value is Timestamp) value.toDate() else value
                    }
                    doc.id to IpoExtras.fromMap(data)
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun toggleWatchlist(ipoId: String, ipoName: String, currentlyWatched: Boolean): Result<Unit> {
        val uid = requireUserId()
        val safeId = ipoId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val docRef = db.collection("users").document(uid).collection("watchlist").document(safeId)
        return try {
            if (currentlyWatched) {
                docRef.delete().await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "ipoId" to safeId,
                    "ipoName" to ipoName.take(120),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}

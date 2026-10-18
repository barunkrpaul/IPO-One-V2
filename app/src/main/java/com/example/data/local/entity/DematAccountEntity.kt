package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "demat_accounts",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyMemberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["familyMemberId"])]
)
data class DematAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val familyMemberId: Long,
    val brokerName: String, // Zerodha, Groww, Angel One, Upstox, ICICI Direct, HDFC Sky, Kotak, etc.
    val depository: String, // CDSL, NSDL
    val dpId: String, // 8-digit DP ID (e.g. 12081600)
    val clientId: String, // Client ID / BOID (8 digits)
    val accountAlias: String, // e.g., "Rahul Zerodha", "Dad AngelOne"
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val maskedClientId: String
        get() = if (clientId.length >= 4) "••••${clientId.takeLast(4)}" else "••••"

    val maskedDpId: String
        get() = if (dpId.length >= 4) "${dpId.take(2)}••${dpId.takeLast(2)}" else dpId
}

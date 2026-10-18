package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "upi_accounts",
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
data class UpiAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val familyMemberId: Long,
    val vpa: String, // e.g., user@okhdfcbank, name@upi
    val bankName: String, // HDFC Bank, SBI, ICICI Bank, Axis Bank, etc.
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

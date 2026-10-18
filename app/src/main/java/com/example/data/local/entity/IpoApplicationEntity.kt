package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ipo_applications",
    foreignKeys = [
        ForeignKey(
            entity = IpoEntity::class,
            parentColumns = ["id"],
            childColumns = ["ipoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DematAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["dematAccountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyMemberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ipoId"]),
        Index(value = ["dematAccountId"]),
        Index(value = ["familyMemberId"])
    ]
)
data class IpoApplicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ipoId: Long,
    val dematAccountId: Long,
    val familyMemberId: Long,
    val upiVpa: String, // Any selected or custom UPI ID
    val upiAccountId: Long? = null,
    val category: String, // Retail, sHNI, bHNI, Shareholder, Employee
    val lotCount: Int,
    val bidPrice: Double,
    val isCutoff: Boolean = true,
    val totalAmount: Double,
    val applicationNumber: String,
    val mandateStatus: String, // Pending, Approved, Expired, Rejected
    val allotmentStatus: String, // Awaiting, Allotted, NotAllotted, Refunded
    val sharesAllotted: Int = 0,
    val refundAmount: Double = 0.0,
    val mandateExpiry: String? = null,
    val checklistBrokerSubmitted: Boolean = true,
    val checklistMandateAccepted: Boolean = false,
    val checklistAllotmentChecked: Boolean = false,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

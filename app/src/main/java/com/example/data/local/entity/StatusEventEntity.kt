package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "status_events",
    foreignKeys = [
        ForeignKey(
            entity = IpoApplicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["applicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["applicationId"])]
)
data class StatusEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val applicationId: Long,
    val eventType: String, // SUBMITTED, MANDATE_APPROVED, MANDATE_STATUS, ALLOTMENT_STATUS, ALLOTTED
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTimestamp: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            return "${sdf.format(Date(timestamp))} IST"
        }

    val sourceLabel: String
        get() = when {
            eventType.contains("ALLOT", ignoreCase = true) -> "Source: Official Registrar Lookup (Manual Record)"
            eventType.contains("MANDATE", ignoreCase = true) -> "Source: UPI Mandate App (Manual Confirmation)"
            else -> "Source: Manual Personal Tracker Entry"
        }
}

package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val relationship: String, // Self, Spouse, Father, Mother, Child, etc.
    val pan: String, // e.g., ABCDE1234F
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val maskedPan: String
        get() = if (pan.length >= 10) {
            "${pan.take(3)}****${pan.takeLast(2)}"
        } else {
            pan
        }
}

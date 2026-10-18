package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.IpoApplicationEntity
import com.example.data.local.entity.StatusEventEntity
import com.example.model.ApplicationWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {
    @Transaction
    @Query("SELECT * FROM ipo_applications ORDER BY createdAt DESC")
    fun getAllApplicationsWithDetails(): Flow<List<ApplicationWithDetails>>

    @Transaction
    @Query("SELECT * FROM ipo_applications WHERE ipoId = :ipoId ORDER BY createdAt DESC")
    fun getApplicationsForIpo(ipoId: Long): Flow<List<ApplicationWithDetails>>

    @Transaction
    @Query("SELECT * FROM ipo_applications WHERE allotmentStatus = :allotmentStatus ORDER BY createdAt DESC")
    fun getApplicationsByAllotmentStatus(allotmentStatus: String): Flow<List<ApplicationWithDetails>>

    @Transaction
    @Query("SELECT * FROM ipo_applications WHERE mandateStatus = :mandateStatus ORDER BY createdAt DESC")
    fun getApplicationsByMandateStatus(mandateStatus: String): Flow<List<ApplicationWithDetails>>

    @Transaction
    @Query("SELECT * FROM ipo_applications WHERE id = :id LIMIT 1")
    fun getApplicationById(id: Long): Flow<ApplicationWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: IpoApplicationEntity): Long

    @Update
    suspend fun updateApplication(application: IpoApplicationEntity)

    @Delete
    suspend fun deleteApplication(application: IpoApplicationEntity)

    // Status Events
    @Query("SELECT * FROM status_events ORDER BY timestamp DESC")
    fun getAllStatusEvents(): Flow<List<StatusEventEntity>>

    @Query("SELECT * FROM status_events WHERE applicationId = :applicationId ORDER BY timestamp DESC")
    fun getStatusEventsForApplication(applicationId: Long): Flow<List<StatusEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatusEvent(event: StatusEventEntity): Long

    @Query("SELECT COUNT(*) FROM ipo_applications")
    suspend fun getApplicationCount(): Int

    @Query("DELETE FROM ipo_applications")
    suspend fun deleteAllApplications()

    @Query("DELETE FROM status_events")
    suspend fun deleteAllStatusEvents()
}

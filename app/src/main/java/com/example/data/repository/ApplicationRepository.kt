package com.example.data.repository

import com.example.data.local.dao.ApplicationDao
import com.example.data.local.entity.IpoApplicationEntity
import com.example.data.local.entity.StatusEventEntity
import com.example.model.ApplicationWithDetails
import kotlinx.coroutines.flow.Flow

class ApplicationRepository(private val applicationDao: ApplicationDao) {
    val allApplications: Flow<List<ApplicationWithDetails>> = applicationDao.getAllApplicationsWithDetails()
    val allStatusEvents: Flow<List<StatusEventEntity>> = applicationDao.getAllStatusEvents()

    fun getApplicationsForIpo(ipoId: Long): Flow<List<ApplicationWithDetails>> =
        applicationDao.getApplicationsForIpo(ipoId)

    fun getApplicationsByAllotmentStatus(status: String): Flow<List<ApplicationWithDetails>> =
        applicationDao.getApplicationsByAllotmentStatus(status)

    fun getApplicationsByMandateStatus(status: String): Flow<List<ApplicationWithDetails>> =
        applicationDao.getApplicationsByMandateStatus(status)

    fun getApplicationById(id: Long): Flow<ApplicationWithDetails?> =
        applicationDao.getApplicationById(id)

    suspend fun insertApplication(application: IpoApplicationEntity): Long =
        applicationDao.insertApplication(application)

    suspend fun updateApplication(application: IpoApplicationEntity) =
        applicationDao.updateApplication(application)

    suspend fun deleteApplication(application: IpoApplicationEntity) =
        applicationDao.deleteApplication(application)

    fun getStatusEvents(applicationId: Long): Flow<List<StatusEventEntity>> =
        applicationDao.getStatusEventsForApplication(applicationId)

    suspend fun addStatusEvent(event: StatusEventEntity): Long =
        applicationDao.insertStatusEvent(event)
}

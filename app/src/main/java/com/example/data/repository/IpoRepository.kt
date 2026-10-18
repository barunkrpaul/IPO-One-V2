package com.example.data.repository

import com.example.data.local.dao.IpoDao
import com.example.data.local.entity.IpoEntity
import kotlinx.coroutines.flow.Flow

class IpoRepository(private val ipoDao: IpoDao) {
    val allIpos: Flow<List<IpoEntity>> = ipoDao.getAllIpos()

    fun getIposByStatus(status: String): Flow<List<IpoEntity>> = ipoDao.getIposByStatus(status)

    fun getIposBySeries(series: String): Flow<List<IpoEntity>> = ipoDao.getIposBySeries(series)

    fun getIpoById(id: Long): Flow<IpoEntity?> = ipoDao.getIpoById(id)

    suspend fun getIpoByIdSync(id: Long): IpoEntity? = ipoDao.getIpoByIdSync(id)

    suspend fun insertIpo(ipo: IpoEntity): Long = ipoDao.insertIpo(ipo)

    suspend fun updateIpo(ipo: IpoEntity) = ipoDao.updateIpo(ipo)

    suspend fun deleteIpo(ipo: IpoEntity) = ipoDao.deleteIpo(ipo)

    suspend fun deployLiveIpoData(): Int = com.example.data.local.SampleDataSeeder.deployLiveIpoData(ipoDao)
}

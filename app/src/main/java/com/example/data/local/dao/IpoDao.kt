package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.IpoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IpoDao {
    @Query("SELECT * FROM ipos ORDER BY openDate DESC")
    fun getAllIpos(): Flow<List<IpoEntity>>

    @Query("SELECT * FROM ipos WHERE status = :status ORDER BY openDate DESC")
    fun getIposByStatus(status: String): Flow<List<IpoEntity>>

    @Query("SELECT * FROM ipos WHERE series = :series ORDER BY openDate DESC")
    fun getIposBySeries(series: String): Flow<List<IpoEntity>>

    @Query("SELECT * FROM ipos WHERE id = :id LIMIT 1")
    fun getIpoById(id: Long): Flow<IpoEntity?>

    @Query("SELECT * FROM ipos WHERE id = :id LIMIT 1")
    suspend fun getIpoByIdSync(id: Long): IpoEntity?

    @Query("SELECT * FROM ipos WHERE symbol = :symbol LIMIT 1")
    suspend fun getIpoBySymbol(symbol: String): IpoEntity?

    @Query("SELECT COUNT(*) FROM ipos")
    suspend fun getIpoCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIpo(ipo: IpoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ipos: List<IpoEntity>)

    @Update
    suspend fun updateIpo(ipo: IpoEntity)

    @Delete
    suspend fun deleteIpo(ipo: IpoEntity)

    @Query("DELETE FROM ipos")
    suspend fun deleteAllIpos()
}

package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.DematAccountEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.UpiAccountEntity
import com.example.model.DematWithMember
import com.example.model.FamilyMemberWithAccounts
import kotlinx.coroutines.flow.Flow

@Dao
interface DematDao {
    // Family Members
    @Query("SELECT * FROM family_members ORDER BY isPrimary DESC, fullName ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMemberEntity>>

    @Transaction
    @Query("SELECT * FROM family_members ORDER BY isPrimary DESC, fullName ASC")
    fun getFamilyMembersWithAccounts(): Flow<List<FamilyMemberWithAccounts>>

    @Query("SELECT * FROM family_members WHERE id = :id LIMIT 1")
    suspend fun getFamilyMemberById(id: Long): FamilyMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMemberEntity): Long

    @Update
    suspend fun updateFamilyMember(member: FamilyMemberEntity)

    @Delete
    suspend fun deleteFamilyMember(member: FamilyMemberEntity)

    // Demat Accounts
    @Transaction
    @Query("SELECT * FROM demat_accounts ORDER BY isDefault DESC, accountAlias ASC")
    fun getAllDematWithMember(): Flow<List<DematWithMember>>

    @Query("SELECT * FROM demat_accounts WHERE familyMemberId = :memberId")
    fun getDematAccountsForMember(memberId: Long): Flow<List<DematAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDematAccount(account: DematAccountEntity): Long

    @Update
    suspend fun updateDematAccount(account: DematAccountEntity)

    @Delete
    suspend fun deleteDematAccount(account: DematAccountEntity)

    // UPI Accounts
    @Query("SELECT * FROM upi_accounts ORDER BY isDefault DESC, vpa ASC")
    fun getAllUpiAccounts(): Flow<List<UpiAccountEntity>>

    @Query("SELECT * FROM upi_accounts WHERE familyMemberId = :memberId")
    fun getUpiAccountsForMember(memberId: Long): Flow<List<UpiAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpiAccount(account: UpiAccountEntity): Long

    @Update
    suspend fun updateUpiAccount(account: UpiAccountEntity)

    @Delete
    suspend fun deleteUpiAccount(account: UpiAccountEntity)

    @Query("SELECT COUNT(*) FROM family_members")
    suspend fun getMemberCount(): Int

    @Query("DELETE FROM family_members")
    suspend fun deleteAllFamilyMembers()

    @Query("DELETE FROM demat_accounts")
    suspend fun deleteAllDematAccounts()

    @Query("DELETE FROM upi_accounts")
    suspend fun deleteAllUpiAccounts()
}

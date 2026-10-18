package com.example.data.repository

import com.example.data.local.dao.DematDao
import com.example.data.local.entity.DematAccountEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.UpiAccountEntity
import com.example.model.DematWithMember
import com.example.model.FamilyMemberWithAccounts
import kotlinx.coroutines.flow.Flow

class DematRepository(private val dematDao: DematDao) {
    val familyMembers: Flow<List<FamilyMemberEntity>> = dematDao.getAllFamilyMembers()
    val familyMembersWithAccounts: Flow<List<FamilyMemberWithAccounts>> = dematDao.getFamilyMembersWithAccounts()
    val dematAccountsWithMember: Flow<List<DematWithMember>> = dematDao.getAllDematWithMember()
    val upiAccounts: Flow<List<UpiAccountEntity>> = dematDao.getAllUpiAccounts()

    fun getDematAccountsForMember(memberId: Long): Flow<List<DematAccountEntity>> =
        dematDao.getDematAccountsForMember(memberId)

    fun getUpiAccountsForMember(memberId: Long): Flow<List<UpiAccountEntity>> =
        dematDao.getUpiAccountsForMember(memberId)

    suspend fun getMemberById(id: Long): FamilyMemberEntity? = dematDao.getFamilyMemberById(id)

    suspend fun insertFamilyMember(member: FamilyMemberEntity): Long = dematDao.insertFamilyMember(member)

    suspend fun updateFamilyMember(member: FamilyMemberEntity) = dematDao.updateFamilyMember(member)

    suspend fun deleteFamilyMember(member: FamilyMemberEntity) = dematDao.deleteFamilyMember(member)

    suspend fun insertDematAccount(account: DematAccountEntity): Long = dematDao.insertDematAccount(account)

    suspend fun updateDematAccount(account: DematAccountEntity) = dematDao.updateDematAccount(account)

    suspend fun deleteDematAccount(account: DematAccountEntity) = dematDao.deleteDematAccount(account)

    suspend fun insertUpiAccount(account: UpiAccountEntity): Long = dematDao.insertUpiAccount(account)

    suspend fun updateUpiAccount(account: UpiAccountEntity) = dematDao.updateUpiAccount(account)

    suspend fun deleteUpiAccount(account: UpiAccountEntity) = dematDao.deleteUpiAccount(account)
}

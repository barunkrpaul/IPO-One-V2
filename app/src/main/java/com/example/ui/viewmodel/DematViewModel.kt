package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.DematAccountEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.UpiAccountEntity
import com.example.data.repository.DematRepository
import com.example.model.DematWithMember
import com.example.model.FamilyMemberWithAccounts
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DematViewModel(private val dematRepository: DematRepository) : ViewModel() {

    val familyMembers: StateFlow<List<FamilyMemberEntity>> = dematRepository.familyMembers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val familyMembersWithAccounts: StateFlow<List<FamilyMemberWithAccounts>> =
        dematRepository.familyMembersWithAccounts
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val dematAccountsWithMember: StateFlow<List<DematWithMember>> =
        dematRepository.dematAccountsWithMember
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val allUpiAccounts: StateFlow<List<UpiAccountEntity>> =
        dematRepository.upiAccounts
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun addFamilyMember(fullName: String, relationship: String, pan: String, isPrimary: Boolean = false) {
        viewModelScope.launch {
            val member = FamilyMemberEntity(
                fullName = fullName.trim(),
                relationship = relationship.trim(),
                pan = pan.trim().uppercase(),
                isPrimary = isPrimary
            )
            dematRepository.insertFamilyMember(member)
        }
    }

    fun deleteFamilyMember(member: FamilyMemberEntity) {
        viewModelScope.launch {
            dematRepository.deleteFamilyMember(member)
        }
    }

    fun addDematAccount(
        familyMemberId: Long,
        brokerName: String,
        depository: String,
        dpId: String,
        clientId: String,
        accountAlias: String,
        isDefault: Boolean = false
    ) {
        viewModelScope.launch {
            val account = DematAccountEntity(
                familyMemberId = familyMemberId,
                brokerName = brokerName.trim(),
                depository = depository.trim(),
                dpId = dpId.trim(),
                clientId = clientId.trim(),
                accountAlias = accountAlias.ifBlank { "$brokerName ($clientId)" },
                isDefault = isDefault
            )
            dematRepository.insertDematAccount(account)
        }
    }

    fun deleteDematAccount(account: DematAccountEntity) {
        viewModelScope.launch {
            dematRepository.deleteDematAccount(account)
        }
    }

    fun addUpiAccount(
        familyMemberId: Long,
        vpa: String,
        bankName: String,
        isDefault: Boolean = false
    ) {
        viewModelScope.launch {
            val upi = UpiAccountEntity(
                familyMemberId = familyMemberId,
                vpa = vpa.trim().lowercase(),
                bankName = bankName.trim(),
                isDefault = isDefault
            )
            dematRepository.insertUpiAccount(upi)
        }
    }

    fun deleteUpiAccount(account: UpiAccountEntity) {
        viewModelScope.launch {
            dematRepository.deleteUpiAccount(account)
        }
    }
}

class DematViewModelFactory(private val dematRepository: DematRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DematViewModel(dematRepository) as T
    }
}

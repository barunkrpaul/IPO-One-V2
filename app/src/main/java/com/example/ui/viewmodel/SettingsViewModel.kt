package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SampleDataSeeder
import com.example.data.local.dao.ApplicationDao
import com.example.data.local.dao.DematDao
import com.example.data.local.dao.IpoDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class PrivacyDashboardCounts(
    val membersCount: Int = 0,
    val dematCount: Int = 0,
    val upiCount: Int = 0,
    val applicationsCount: Int = 0
)

class SettingsViewModel(
    private val ipoDao: IpoDao,
    private val dematDao: DematDao,
    private val applicationDao: ApplicationDao
) : ViewModel() {

    private val _exportJson = MutableStateFlow<String?>(null)
    val exportJson: StateFlow<String?> = _exportJson

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage

    val privacyCounts: StateFlow<PrivacyDashboardCounts> = combine(
        dematDao.getAllFamilyMembers(),
        dematDao.getAllDematWithMember(),
        dematDao.getAllUpiAccounts(),
        applicationDao.getAllApplicationsWithDetails()
    ) { members, demats, upis, apps ->
        PrivacyDashboardCounts(
            membersCount = members.size,
            dematCount = demats.size,
            upiCount = upis.size,
            applicationsCount = apps.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PrivacyDashboardCounts()
    )

    fun generateDataExport() {
        viewModelScope.launch {
            try {
                val root = JSONObject()
                root.put("appName", "IPO One")
                root.put("version", "2.0.0")
                root.put("exportedAt", System.currentTimeMillis())

                // Members
                val members = dematDao.getAllFamilyMembers().first()
                val membersArray = JSONArray()
                members.forEach { m ->
                    val obj = JSONObject()
                    obj.put("id", m.id)
                    obj.put("fullName", m.fullName)
                    obj.put("relationship", m.relationship)
                    obj.put("pan", m.maskedPan)
                    membersArray.put(obj)
                }
                root.put("familyMembers", membersArray)

                // Demat Accounts
                val demats = dematDao.getAllDematWithMember().first()
                val dematsArray = JSONArray()
                demats.forEach { d ->
                    val obj = JSONObject()
                    obj.put("id", d.dematAccount.id)
                    obj.put("holder", d.familyMember.fullName)
                    obj.put("brokerName", d.dematAccount.brokerName)
                    obj.put("depository", d.dematAccount.depository)
                    obj.put("dpId", d.dematAccount.maskedDpId)
                    obj.put("clientId", d.dematAccount.maskedClientId)
                    obj.put("accountAlias", d.dematAccount.accountAlias)
                    dematsArray.put(obj)
                }
                root.put("dematAccounts", dematsArray)

                // Applications
                val apps = applicationDao.getAllApplicationsWithDetails().first()
                val appsArray = JSONArray()
                apps.forEach { a ->
                    val obj = JSONObject()
                    obj.put("id", a.application.id)
                    obj.put("ipo", a.ipo.companyName)
                    obj.put("symbol", a.ipo.symbol)
                    obj.put("applicant", a.familyMember.fullName)
                    obj.put("broker", a.dematAccount.brokerName)
                    obj.put("upiVpa", a.application.upiVpa)
                    obj.put("category", a.application.category)
                    obj.put("lots", a.application.lotCount)
                    obj.put("totalAmount", a.application.totalAmount)
                    obj.put("mandateStatus", a.application.mandateStatus)
                    obj.put("allotmentStatus", a.application.allotmentStatus)
                    obj.put("applicationNumber", a.application.applicationNumber)
                    appsArray.put(obj)
                }
                root.put("applications", appsArray)

                _exportJson.value = root.toString(2)
                _actionMessage.value = "IPO One JSON export ready (${apps.size} applications)"
            } catch (e: Exception) {
                _actionMessage.value = "Export error: ${e.message}"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun clearExportJson() {
        _exportJson.value = null
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            applicationDao.deleteAllStatusEvents()
            applicationDao.deleteAllApplications()
            dematDao.deleteAllUpiAccounts()
            dematDao.deleteAllDematAccounts()
            dematDao.deleteAllFamilyMembers()
            ipoDao.deleteAllIpos()

            SampleDataSeeder.seedIfEmpty(ipoDao, dematDao, applicationDao)
            _actionMessage.value = "IPO One database reset with realistic sample IPOs & accounts!"
        }
    }

    fun wipeAllUserData() {
        viewModelScope.launch {
            applicationDao.deleteAllStatusEvents()
            applicationDao.deleteAllApplications()
            dematDao.deleteAllUpiAccounts()
            dematDao.deleteAllDematAccounts()
            dematDao.deleteAllFamilyMembers()
            _actionMessage.value = "All personal Demats, UPI IDs and applications wiped!"
        }
    }
}

class SettingsViewModelFactory(
    private val ipoDao: IpoDao,
    private val dematDao: DematDao,
    private val applicationDao: ApplicationDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(ipoDao, dematDao, applicationDao) as T
    }
}

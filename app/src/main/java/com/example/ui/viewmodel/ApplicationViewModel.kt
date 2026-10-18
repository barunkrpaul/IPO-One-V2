package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.IpoApplicationEntity
import com.example.data.local.entity.StatusEventEntity
import com.example.data.repository.ApplicationRepository
import com.example.model.AllotmentStatus
import com.example.model.ApplicationWithDetails
import com.example.model.MandateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ApplicationFilter(val displayName: String) {
    ALL("All Bids"),
    PENDING_MANDATE("Mandate Pending"),
    APPROVED_MANDATE("Mandate Approved"),
    ALLOTTED("Allotted"),
    NOT_ALLOTTED("Not Allotted")
}

data class BatchDematSelection(
    val dematAccountId: Long,
    val familyMemberId: Long,
    val upiVpa: String,
    val upiAccountId: Long? = null
)

class ApplicationViewModel(private val applicationRepository: ApplicationRepository) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(ApplicationFilter.ALL)
    val selectedFilter: StateFlow<ApplicationFilter> = _selectedFilter

    val allApplications: StateFlow<List<ApplicationWithDetails>> = applicationRepository.allApplications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredApplications: StateFlow<List<ApplicationWithDetails>> = combine(
        applicationRepository.allApplications,
        _selectedFilter
    ) { apps, filter ->
        when (filter) {
            ApplicationFilter.ALL -> apps
            ApplicationFilter.PENDING_MANDATE -> apps.filter {
                it.application.mandateStatus == MandateStatus.PENDING.displayName
            }
            ApplicationFilter.APPROVED_MANDATE -> apps.filter {
                it.application.mandateStatus == MandateStatus.APPROVED.displayName
            }
            ApplicationFilter.ALLOTTED -> apps.filter {
                it.application.allotmentStatus == AllotmentStatus.ALLOTTED.displayName
            }
            ApplicationFilter.NOT_ALLOTTED -> apps.filter {
                it.application.allotmentStatus == AllotmentStatus.NOT_ALLOTTED.displayName
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: ApplicationFilter) {
        _selectedFilter.value = filter
    }

    fun createApplication(
        ipoId: Long,
        dematAccountId: Long,
        familyMemberId: Long,
        upiVpa: String,
        upiAccountId: Long?,
        category: String,
        lotCount: Int,
        bidPrice: Double,
        isCutoff: Boolean,
        totalAmount: Double,
        applicationNumber: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val app = IpoApplicationEntity(
                ipoId = ipoId,
                dematAccountId = dematAccountId,
                familyMemberId = familyMemberId,
                upiVpa = upiVpa.trim(),
                upiAccountId = upiAccountId,
                category = category,
                lotCount = lotCount,
                bidPrice = bidPrice,
                isCutoff = isCutoff,
                totalAmount = totalAmount,
                applicationNumber = applicationNumber.ifBlank {
                    (1000000000L + (Math.random() * 9000000000L).toLong()).toString()
                },
                mandateStatus = MandateStatus.PENDING.displayName,
                allotmentStatus = AllotmentStatus.AWAITING.displayName,
                checklistBrokerSubmitted = true,
                checklistMandateAccepted = false,
                checklistAllotmentChecked = false,
                notes = notes
            )
            val appId = applicationRepository.insertApplication(app)
            applicationRepository.addStatusEvent(
                StatusEventEntity(
                    applicationId = appId,
                    eventType = "SUBMITTED",
                    description = "Bid recorded for $lotCount lot(s). UPI mandate requested on $upiVpa."
                )
            )
        }
    }

    fun createBatchApplications(
        ipoId: Long,
        selections: List<BatchDematSelection>,
        category: String,
        lotCount: Int,
        bidPrice: Double,
        isCutoff: Boolean,
        totalAmountPerBid: Double,
        notes: String? = null
    ) {
        viewModelScope.launch {
            selections.forEachIndexed { idx, sel ->
                val generatedAppNo = (1000000000L + (Math.random() * 9000000000L).toLong() + idx).toString()
                val app = IpoApplicationEntity(
                    ipoId = ipoId,
                    dematAccountId = sel.dematAccountId,
                    familyMemberId = sel.familyMemberId,
                    upiVpa = sel.upiVpa.trim(),
                    upiAccountId = sel.upiAccountId,
                    category = category,
                    lotCount = lotCount,
                    bidPrice = bidPrice,
                    isCutoff = isCutoff,
                    totalAmount = totalAmountPerBid,
                    applicationNumber = generatedAppNo,
                    mandateStatus = MandateStatus.PENDING.displayName,
                    allotmentStatus = AllotmentStatus.AWAITING.displayName,
                    checklistBrokerSubmitted = true,
                    checklistMandateAccepted = false,
                    checklistAllotmentChecked = false,
                    notes = notes ?: "1-Click Multi-Demat Batch Application"
                )
                val appId = applicationRepository.insertApplication(app)
                applicationRepository.addStatusEvent(
                    StatusEventEntity(
                        applicationId = appId,
                        eventType = "BATCH_SUBMITTED",
                        description = "1-Click Multi-Demat Batch Bid recorded ($lotCount lot(s)) on UPI ${sel.upiVpa}. Approve mandate in UPI app."
                    )
                )
            }
        }
    }

    fun updateMandateStatus(application: IpoApplicationEntity, newStatus: MandateStatus) {
        viewModelScope.launch {
            val updated = application.copy(
                mandateStatus = newStatus.displayName,
                checklistMandateAccepted = newStatus == MandateStatus.APPROVED
            )
            applicationRepository.updateApplication(updated)
            applicationRepository.addStatusEvent(
                StatusEventEntity(
                    applicationId = application.id,
                    eventType = "MANDATE_STATUS",
                    description = "Mandate status updated to: ${newStatus.displayName}"
                )
            )
        }
    }

    fun updateAllotmentStatus(
        application: IpoApplicationEntity,
        newStatus: AllotmentStatus,
        sharesAllotted: Int = 0,
        refundAmount: Double = 0.0
    ) {
        viewModelScope.launch {
            val updated = application.copy(
                allotmentStatus = newStatus.displayName,
                sharesAllotted = if (newStatus == AllotmentStatus.ALLOTTED) sharesAllotted else 0,
                refundAmount = refundAmount,
                checklistAllotmentChecked = true
            )
            applicationRepository.updateApplication(updated)
            applicationRepository.addStatusEvent(
                StatusEventEntity(
                    applicationId = application.id,
                    eventType = "ALLOTMENT_STATUS",
                    description = "Allotment status updated: ${newStatus.displayName} (Shares: $sharesAllotted)"
                )
            )
        }
    }

    fun toggleChecklist(
        application: IpoApplicationEntity,
        brokerSubmitted: Boolean? = null,
        mandateAccepted: Boolean? = null,
        allotmentChecked: Boolean? = null
    ) {
        viewModelScope.launch {
            val updated = application.copy(
                checklistBrokerSubmitted = brokerSubmitted ?: application.checklistBrokerSubmitted,
                checklistMandateAccepted = mandateAccepted ?: application.checklistMandateAccepted,
                checklistAllotmentChecked = allotmentChecked ?: application.checklistAllotmentChecked
            )
            applicationRepository.updateApplication(updated)
        }
    }

    fun deleteApplication(application: IpoApplicationEntity) {
        viewModelScope.launch {
            applicationRepository.deleteApplication(application)
        }
    }
}

class ApplicationViewModelFactory(
    private val applicationRepository: ApplicationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ApplicationViewModel(applicationRepository) as T
    }
}

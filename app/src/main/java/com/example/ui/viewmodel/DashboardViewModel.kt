package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.StatusEventEntity
import com.example.data.repository.ApplicationRepository
import com.example.data.repository.DematRepository
import com.example.data.repository.IpoRepository
import com.example.model.AllotmentStatus
import com.example.model.ApplicationWithDetails
import com.example.model.IpoStatus
import com.example.model.MandateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalApplications: Int = 0,
    val totalBlockedCapital: Double = 0.0,
    val totalExpectedListingGain: Double = 0.0,
    val pendingMandatesCount: Int = 0,
    val allottedCount: Int = 0,
    val notAllottedCount: Int = 0,
    val activeBids: List<ApplicationWithDetails> = emptyList(),
    val urgentMandates: List<ApplicationWithDetails> = emptyList(),
    val openIposCount: Int = 0,
    val upcomingIposCount: Int = 0
)

class DashboardViewModel(
    private val ipoRepository: IpoRepository,
    private val dematRepository: DematRepository,
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun deployLiveIpos() {
        viewModelScope.launch {
            val count = ipoRepository.deployLiveIpoData()
            _syncMessage.value = "Deployed $count live IPOs (Open applications & upcoming issues with live GMP!)"
        }
    }

    val applications: StateFlow<List<ApplicationWithDetails>> = applicationRepository.allApplications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allIpos = ipoRepository.allIpos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentStatusEvents: StateFlow<List<StatusEventEntity>> = applicationRepository.allStatusEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<DashboardUiState> = combine(
        applicationRepository.allApplications,
        ipoRepository.allIpos
    ) { apps, ipos ->
        val totalBlocked = apps
            .filter { it.application.allotmentStatus == AllotmentStatus.AWAITING.displayName }
            .sumOf { it.application.totalAmount }

        val totalExpectedGain = apps
            .filter {
                it.application.allotmentStatus == AllotmentStatus.AWAITING.displayName ||
                        it.application.allotmentStatus == AllotmentStatus.ALLOTTED.displayName
            }
            .sumOf { it.application.lotCount * it.ipo.estimatedProfitPerLot }

        val pendingMandates = apps.filter {
            it.application.mandateStatus == MandateStatus.PENDING.displayName
        }

        val allotted = apps.count {
            it.application.allotmentStatus == AllotmentStatus.ALLOTTED.displayName
        }

        val notAllotted = apps.count {
            it.application.allotmentStatus == AllotmentStatus.NOT_ALLOTTED.displayName
        }

        val active = apps.filter {
            it.application.allotmentStatus == AllotmentStatus.AWAITING.displayName
        }

        val openCount = ipos.count { it.status == IpoStatus.OPEN.displayName }
        val upcomingCount = ipos.count { it.status == IpoStatus.UPCOMING.displayName }

        DashboardUiState(
            totalApplications = apps.size,
            totalBlockedCapital = totalBlocked,
            totalExpectedListingGain = totalExpectedGain,
            pendingMandatesCount = pendingMandates.size,
            allottedCount = allotted,
            notAllottedCount = notAllotted,
            activeBids = active,
            urgentMandates = pendingMandates,
            openIposCount = openCount,
            upcomingIposCount = upcomingCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun quickApproveMandate(application: ApplicationWithDetails) {
        viewModelScope.launch {
            val updated = application.application.copy(
                mandateStatus = MandateStatus.APPROVED.displayName,
                checklistMandateAccepted = true
            )
            applicationRepository.updateApplication(updated)
            applicationRepository.addStatusEvent(
                StatusEventEntity(
                    applicationId = application.application.id,
                    eventType = "MANDATE_APPROVED",
                    description = "UPI Mandate confirmed for ${application.ipo.symbol} (${application.application.upiVpa})."
                )
            )
        }
    }
}

class DashboardViewModelFactory(
    private val ipoRepository: IpoRepository,
    private val dematRepository: DematRepository,
    private val applicationRepository: ApplicationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(ipoRepository, dematRepository, applicationRepository) as T
    }
}

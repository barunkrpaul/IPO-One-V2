package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.IpoEntity
import com.example.data.repository.IpoRepository
import com.example.model.IpoSeries
import com.example.model.IpoStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.round

data class IpoFilterState(
    val searchQuery: String = "",
    val selectedSeries: String = "ALL", // ALL, Mainboard, SME
    val selectedStatus: String = "ALL" // ALL, Open Now, Upcoming, Closed / Awaiting Allotment
)

class IpoViewModel(private val ipoRepository: IpoRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(IpoFilterState())
    val filterState: StateFlow<IpoFilterState> = _filterState

    private val _selectedIpo = MutableStateFlow<IpoEntity?>(null)
    val selectedIpo: StateFlow<IpoEntity?> = _selectedIpo

    private val _gmpSyncMessage = MutableStateFlow<String?>(null)
    val gmpSyncMessage: StateFlow<String?> = _gmpSyncMessage

    val allIpos: StateFlow<List<IpoEntity>> = ipoRepository.allIpos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredIpos: StateFlow<List<IpoEntity>> = combine(
        ipoRepository.allIpos,
        _filterState
    ) { ipos, filter ->
        ipos.filter { ipo ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                    ipo.companyName.contains(filter.searchQuery, ignoreCase = true) ||
                    ipo.symbol.contains(filter.searchQuery, ignoreCase = true)

            val matchesSeries = when (filter.selectedSeries) {
                "ALL" -> true
                IpoSeries.MAINBOARD.displayName -> ipo.series == IpoSeries.MAINBOARD.displayName
                IpoSeries.SME.displayName -> ipo.series == IpoSeries.SME.displayName
                else -> true
            }

            val matchesStatus = when (filter.selectedStatus) {
                "ALL" -> true
                IpoStatus.OPEN.displayName -> ipo.status == IpoStatus.OPEN.displayName
                IpoStatus.UPCOMING.displayName -> ipo.status == IpoStatus.UPCOMING.displayName
                "CLOSED" -> ipo.status == IpoStatus.CLOSED.displayName ||
                        ipo.status == IpoStatus.ALLOTTED.displayName ||
                        ipo.status == IpoStatus.LISTED.displayName
                else -> true
            }

            matchesQuery && matchesSeries && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun setSelectedSeries(series: String) {
        _filterState.value = _filterState.value.copy(selectedSeries = series)
    }

    fun setSelectedStatus(status: String) {
        _filterState.value = _filterState.value.copy(selectedStatus = status)
    }

    fun selectIpo(ipo: IpoEntity?) {
        _selectedIpo.value = ipo
    }

    fun toggleFavorite(ipo: IpoEntity) {
        viewModelScope.launch {
            ipoRepository.updateIpo(ipo.copy(isFavorite = !ipo.isFavorite))
        }
    }

    fun updateGmp(ipo: IpoEntity, newGmpInr: Double) {
        viewModelScope.launch {
            val pct = if (ipo.maxPrice > 0) {
                round((newGmpInr / ipo.maxPrice) * 1000.0) / 10.0
            } else 0.0
            val updated = ipo.copy(
                gmpInr = newGmpInr,
                gmpPercent = pct,
                updatedAt = System.currentTimeMillis()
            )
            ipoRepository.updateIpo(updated)
            if (_selectedIpo.value?.id == ipo.id) {
                _selectedIpo.value = updated
            }
            _gmpSyncMessage.value = "GMP updated for ${ipo.symbol}: +₹${newGmpInr.toInt()} (${pct}%)"
        }
    }

    fun updateSubscriptionRates(
        ipo: IpoEntity,
        qibSub: Double,
        niiSub: Double,
        retailSub: Double,
        totalSub: Double
    ) {
        viewModelScope.launch {
            val updated = ipo.copy(
                qibSub = qibSub,
                niiSub = niiSub,
                retailSub = retailSub,
                totalSub = totalSub,
                updatedAt = System.currentTimeMillis()
            )
            ipoRepository.updateIpo(updated)
            if (_selectedIpo.value?.id == ipo.id) {
                _selectedIpo.value = updated
            }
            _gmpSyncMessage.value = "Subscription updated for ${ipo.symbol}: Total ${totalSub}x (QIB ${qibSub}x, NII ${niiSub}x, Retail ${retailSub}x)"
        }
    }

    fun refreshAllLiveGmpTimestamps() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val list = ipoRepository.allIpos.first()
            list.forEach { ipo ->
                ipoRepository.updateIpo(ipo.copy(updatedAt = now))
            }
            _gmpSyncMessage.value = "Live GMP & Subscription timestamps synced across ${list.size} IPOs"
        }
    }

    fun deployLiveIpoFeed() {
        viewModelScope.launch {
            val count = ipoRepository.deployLiveIpoData()
            _gmpSyncMessage.value = "Successfully deployed $count live IPOs (Open Now & Upcoming with real-time GMP)"
        }
    }

    fun clearGmpSyncMessage() {
        _gmpSyncMessage.value = null
    }

    fun addCustomIpo(
        companyName: String,
        symbol: String,
        series: String,
        minPrice: Double,
        maxPrice: Double,
        lotSize: Int,
        issueSizeCr: Double,
        openDate: String,
        closeDate: String,
        allotmentDate: String,
        refundDate: String,
        listingDate: String,
        registrarName: String,
        status: String,
        gmpInr: Double,
        qibSub: Double = 0.0,
        niiSub: Double = 0.0,
        retailSub: Double = 0.0,
        totalSub: Double = 0.0
    ) {
        viewModelScope.launch {
            val pct = if (maxPrice > 0) {
                round((gmpInr / maxPrice) * 1000.0) / 10.0
            } else 0.0
            val registrarUrl = when {
                registrarName.contains("Link Intime", ignoreCase = true) ->
                    "https://linkintime.co.in/initial_offer/public-issues.html"
                registrarName.contains("Bigshare", ignoreCase = true) ->
                    "https://www.bigshareonline.com/ipo_Allotment.html"
                else -> "https://kosmic.kfintech.com/ipostatus/"
            }
            val newIpo = IpoEntity(
                companyName = companyName.trim(),
                symbol = symbol.trim().uppercase(),
                series = series,
                minPrice = minPrice,
                maxPrice = maxPrice,
                lotSize = lotSize,
                issueSizeCr = issueSizeCr,
                openDate = openDate,
                closeDate = closeDate,
                allotmentDate = allotmentDate,
                refundDate = refundDate,
                listingDate = listingDate,
                registrarName = registrarName,
                registrarUrl = registrarUrl,
                status = status,
                gmpInr = gmpInr,
                gmpPercent = pct,
                qibSub = qibSub,
                niiSub = niiSub,
                retailSub = retailSub,
                totalSub = totalSub,
                updatedAt = System.currentTimeMillis()
            )
            ipoRepository.insertIpo(newIpo)
        }
    }
}

class IpoViewModelFactory(private val ipoRepository: IpoRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return IpoViewModel(ipoRepository) as T
    }
}

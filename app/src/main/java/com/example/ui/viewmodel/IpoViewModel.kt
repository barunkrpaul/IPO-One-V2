package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.IpoApplication
import com.example.data.model.IpoBoardFilter
import com.example.data.model.IpoStatusFilter
import com.example.data.model.LiveIpoItem
import com.example.data.model.PanProfile
import com.example.data.model.UserProfile
import com.example.data.model.WatchlistItem
import com.example.data.remote.InvestorGainClient
import com.example.data.repository.IpoFirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class IpoSortOption(val label: String) {
    DEFAULT("Market Order"),
    HIGHEST_GMP_PCT("Highest GMP %"),
    HIGHEST_PROFIT("Est. Profit / Lot"),
    HIGHEST_SUB("Subscription (x)")
}

class IpoViewModel(
    private val repository: IpoFirestoreRepository,
    private val investorGainClient: InvestorGainClient,
    private val currentUserId: String
) : ViewModel() {

    private val _liveIposState = MutableStateFlow<UiState<List<LiveIpoItem>>>(UiState.Loading)
    val liveIposState: StateFlow<UiState<List<LiveIpoItem>>> = _liveIposState.asStateFlow()

    private val _statusFilter = MutableStateFlow(IpoStatusFilter.ALL)
    val statusFilter: StateFlow<IpoStatusFilter> = _statusFilter.asStateFlow()

    private val _boardFilter = MutableStateFlow(IpoBoardFilter.ALL)
    val boardFilter: StateFlow<IpoBoardFilter> = _boardFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(IpoSortOption.DEFAULT)
    val sortOption: StateFlow<IpoSortOption> = _sortOption.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _onlyWatchlist = MutableStateFlow(false)
    val onlyWatchlist: StateFlow<Boolean> = _onlyWatchlist.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing user profile", e)
            emit(UiState.Error(e.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _guestPanProfiles = MutableStateFlow<List<PanProfile>>(emptyList())
    private val _guestApplications = MutableStateFlow<List<IpoApplication>>(emptyList())
    private val _guestWatchlist = MutableStateFlow<List<WatchlistItem>>(emptyList())

    val panProfilesState: StateFlow<UiState<List<PanProfile>>> = if (currentUserId == "guest") {
        _guestPanProfiles.map { UiState.Success(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
    } else {
        repository.observePanProfiles(currentUserId)
            .map<List<PanProfile>, UiState<List<PanProfile>>> { UiState.Success(it) }
            .catch { e ->
                Log.w(TAG, "Error observing PAN profiles", e)
                emit(UiState.Error(e.message ?: "Failed to load PAN Vault"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    }

    val applicationsState: StateFlow<UiState<List<IpoApplication>>> = if (currentUserId == "guest") {
        _guestApplications.map { UiState.Success(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
    } else {
        repository.observeApplications(currentUserId)
            .map<List<IpoApplication>, UiState<List<IpoApplication>>> { UiState.Success(it) }
            .catch { e ->
                Log.w(TAG, "Error observing applications", e)
                emit(UiState.Error(e.message ?: "Failed to load IPO applications"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    }

    val watchlistState: StateFlow<UiState<List<WatchlistItem>>> = if (currentUserId == "guest") {
        _guestWatchlist.map { UiState.Success(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
    } else {
        repository.observeWatchlist(currentUserId)
            .map<List<WatchlistItem>, UiState<List<WatchlistItem>>> { UiState.Success(it) }
            .catch { e ->
                Log.w(TAG, "Error observing watchlist", e)
                emit(UiState.Error(e.message ?: "Failed to load watchlist"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    }

    val filteredIposState: StateFlow<UiState<List<LiveIpoItem>>> = combine(
        _liveIposState,
        _statusFilter,
        _boardFilter,
        _sortOption,
        combine(_searchQuery, _onlyWatchlist, watchlistState) { q, onlyWatch, wState ->
            val watchedIds = (wState as? UiState.Success)?.data?.map { it.ipoId }?.toSet() ?: emptySet()
            Triple(q, onlyWatch, watchedIds)
        }
    ) { liveState, status, board, sort, (query, onlyWatch, watchedIds) ->
        when (liveState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> liveState
            is UiState.Success -> {
                val filtered = liveState.data.filter { item ->
                    val matchesStatus = when (status) {
                        IpoStatusFilter.ALL -> true
                        IpoStatusFilter.OPEN -> item.statusCode == "O" || item.statusCode == "CT"
                        IpoStatusFilter.UPCOMING -> item.statusCode == "U"
                        IpoStatusFilter.CLOSED -> item.statusCode == "C"
                        IpoStatusFilter.LISTED -> item.statusCode.startsWith("L")
                    }
                    val matchesBoard = when (board) {
                        IpoBoardFilter.ALL -> true
                        IpoBoardFilter.MAINBOARD -> item.category.equals("Mainboard", ignoreCase = true)
                        IpoBoardFilter.SME -> item.category.equals("SME", ignoreCase = true)
                    }
                    val matchesSearch = query.isBlank() ||
                        item.name.contains(query, ignoreCase = true) ||
                        item.sector.contains(query, ignoreCase = true)
                    val matchesWatch = !onlyWatch || watchedIds.contains(item.id)
                    matchesStatus && matchesBoard && matchesSearch && matchesWatch
                }

                val sorted = when (sort) {
                    IpoSortOption.DEFAULT -> filtered
                    IpoSortOption.HIGHEST_GMP_PCT -> filtered.sortedByDescending { it.gmpPercent }
                    IpoSortOption.HIGHEST_PROFIT -> filtered.sortedByDescending { it.estProfitPerLotInr }
                    IpoSortOption.HIGHEST_SUB -> filtered.sortedByDescending {
                        it.totalSubscription.replace("x", "", ignoreCase = true).toDoubleOrNull() ?: 0.0
                    }
                }
                UiState.Success(sorted)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    init {
        refreshLiveInvestorGainData()
    }

    fun refreshLiveInvestorGainData() {
        viewModelScope.launch {
            _liveIposState.value = UiState.Loading
            val result = investorGainClient.fetchLiveIpos()
            result.fold(
                onSuccess = { list ->
                    _liveIposState.value = UiState.Success(list)
                },
                onFailure = { err ->
                    Log.e(TAG, "Failed to fetch live InvestorGain IPOs", err)
                    _liveIposState.value = UiState.Error(
                        err.localizedMessage ?: "Failed to load live IPO market data"
                    )
                }
            )
        }
    }

    fun setStatusFilter(filter: IpoStatusFilter) {
        _statusFilter.value = filter
    }

    fun setBoardFilter(filter: IpoBoardFilter) {
        _boardFilter.value = filter
    }

    fun setSortOption(option: IpoSortOption) {
        _sortOption.value = option
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleOnlyWatchlist() {
        _onlyWatchlist.value = !_onlyWatchlist.value
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun ensureUserProfileInitialized(
        displayName: String,
        email: String?,
        phoneNumber: String?,
        authProvider: String
    ) {
        viewModelScope.launch {
            val currentProfile = (userProfileState.value as? UiState.Success)?.data
            val biometricEnabled = currentProfile?.biometricEnabled ?: false
            val finalPhone = phoneNumber ?: currentProfile?.phoneNumber
            val finalName = displayName.ifBlank { currentProfile?.displayName ?: "IPO Investor" }
            repository.saveUserProfile(
                displayName = finalName,
                email = email ?: currentProfile?.email,
                phoneNumber = finalPhone,
                authProvider = authProvider,
                biometricEnabled = biometricEnabled
            )
        }
    }

    fun updateBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            val res = repository.updateBiometricPreference(enabled)
            if (res.isSuccess) {
                _statusBanner.value = if (enabled) {
                    "Biometric App Lock enabled for your financial vault"
                } else {
                    "Biometric App Lock disabled"
                }
            } else {
                _statusBanner.value = "Failed to update biometric setting"
            }
        }
    }

    fun updateMobilePhone(phoneNumber: String, displayName: String, email: String?) {
        viewModelScope.launch {
            val currentProfile = (userProfileState.value as? UiState.Success)?.data
            val res = repository.saveUserProfile(
                displayName = displayName.ifBlank { currentProfile?.displayName ?: "IPO Investor" },
                email = email ?: currentProfile?.email,
                phoneNumber = phoneNumber,
                authProvider = "Mobile+Google",
                biometricEnabled = currentProfile?.biometricEnabled ?: false,
                priceAlertsEnabled = currentProfile?.priceAlertsEnabled ?: true
            )
            if (res.isSuccess) {
                _statusBanner.value = "Mobile number $phoneNumber linked to your IPO One profile!"
            }
        }
    }

    fun addPanProfile(
        holderName: String,
        panNumber: String,
        dematId: String,
        upiId: String,
        category: String
    ) {
        viewModelScope.launch {
            if (currentUserId == "guest") {
                val profile = PanProfile(
                    id = "pan_${System.currentTimeMillis()}",
                    userId = "guest",
                    holderName = holderName,
                    panNumber = panNumber.uppercase(),
                    dematId = dematId,
                    upiId = upiId,
                    category = category
                )
                _guestPanProfiles.value = _guestPanProfiles.value + profile
                _statusBanner.value = "Added $holderName (${panNumber.uppercase()}) to Family PAN Vault"
            } else {
                val res = repository.addPanProfile(holderName, panNumber, dematId, upiId, category)
                if (res.isSuccess) {
                    _statusBanner.value = "Added $holderName (${panNumber.uppercase()}) to Encrypted PAN Vault"
                } else {
                    _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save PAN profile"
                }
            }
        }
    }

    fun deletePanProfile(panId: String) {
        viewModelScope.launch {
            if (currentUserId == "guest") {
                _guestPanProfiles.value = _guestPanProfiles.value.filter { it.id != panId }
            } else {
                repository.deletePanProfile(panId)
            }
        }
    }

    fun submitIpoApplication(
        ipo: LiveIpoItem,
        panHolderName: String,
        panNumber: String,
        category: String,
        lotsApplied: Int,
        upiId: String,
        initialStatus: String = "Mandate Pending"
    ) {
        viewModelScope.launch {
            val shares = (if (ipo.lotSize > 0) ipo.lotSize else 1) * lotsApplied
            val price = if (ipo.issuePrice > 0) ipo.issuePrice else 100
            val total = shares * price
            if (currentUserId == "guest") {
                val app = IpoApplication(
                    id = "app_${System.currentTimeMillis()}",
                    userId = "guest",
                    ipoId = ipo.id,
                    ipoName = ipo.name,
                    panHolderName = panHolderName,
                    panNumber = panNumber.uppercase(),
                    category = category,
                    lotsApplied = lotsApplied.toLong(),
                    sharesApplied = shares.toLong(),
                    bidPrice = price.toLong(),
                    totalAmount = total.toLong(),
                    upiId = upiId,
                    status = initialStatus
                )
                _guestApplications.value = _guestApplications.value + app
                _statusBanner.value = "Logged ${ipo.name} application ($lotsApplied lot(s)) for $panHolderName"
            } else {
                val res = repository.submitIpoApplication(
                    ipoId = ipo.id,
                    ipoName = ipo.name,
                    panHolderName = panHolderName,
                    panNumber = panNumber,
                    category = category,
                    lotsApplied = lotsApplied,
                    sharesApplied = shares,
                    bidPrice = price,
                    totalAmount = total,
                    upiId = upiId,
                    status = initialStatus
                )
                if (res.isSuccess) {
                    _statusBanner.value = "Logged ${ipo.name} bid ($lotsApplied lot(s)) for $panHolderName!"
                } else {
                    _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to log IPO bid"
                }
            }
        }
    }

    fun cycleApplicationStatus(app: IpoApplication) {
        val next = when (app.status) {
            "Mandate Pending" -> "Applied"
            "Applied" -> "Allotted"
            "Allotted" -> "Not Allotted"
            "Not Allotted" -> "Mandate Pending"
            else -> "Mandate Pending"
        }
        setApplicationStatus(app, next)
    }

    fun setApplicationStatus(app: IpoApplication, newStatus: String) {
        viewModelScope.launch {
            if (currentUserId == "guest") {
                _guestApplications.value = _guestApplications.value.map {
                    if (it.id == app.id) it.copy(status = newStatus) else it
                }
                _statusBanner.value = "Updated ${app.ipoName} status to: $newStatus"
            } else {
                repository.updateApplicationStatus(app.id, newStatus)
                _statusBanner.value = "Updated ${app.ipoName} status to: $newStatus"
            }
        }
    }

    fun deleteApplication(appId: String) {
        viewModelScope.launch {
            if (currentUserId == "guest") {
                _guestApplications.value = _guestApplications.value.filter { it.id != appId }
            } else {
                repository.deleteApplication(appId)
            }
        }
    }

    fun toggleWatchlist(ipo: LiveIpoItem, isWatched: Boolean) {
        viewModelScope.launch {
            if (currentUserId == "guest") {
                if (isWatched) {
                    _guestWatchlist.value = _guestWatchlist.value + WatchlistItem(
                        id = "watch_${ipo.id}",
                        userId = "guest",
                        ipoId = ipo.id,
                        ipoName = ipo.name
                    )
                } else {
                    _guestWatchlist.value = _guestWatchlist.value.filter { it.ipoId != ipo.id }
                }
            } else {
                repository.toggleWatchlist(ipo.id, ipo.name, isWatched)
            }
        }
    }

    companion object {
        private const val TAG = "IpoViewModel"
    }
}

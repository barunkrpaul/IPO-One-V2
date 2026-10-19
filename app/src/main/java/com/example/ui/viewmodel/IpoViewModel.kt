package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.IpoApplication
import com.example.data.model.AllotmentOutcome
import com.example.data.model.AllotmentResult
import com.example.data.model.AllotmentTarget
import com.example.data.model.CalendarEvent
import com.example.data.model.IpoBoardFilter
import com.example.data.model.IpoExtras
import com.example.data.local.IpoPageDetailsCache
import com.example.data.model.IntermediaryDirectory
import com.example.data.model.IpoLifecycleStage
import com.example.data.model.IpoPageDetails
import com.example.data.model.IpoRef
import com.example.data.model.buildIntermediaryDirectory
import com.example.data.model.listingLocalDate
import com.example.data.model.mergedOver
import com.example.data.model.ListedIpoRecord
import com.example.data.model.ListingPerformance
import com.example.data.model.listingPerformance
import com.example.data.model.listingYears
import com.example.data.model.sortListedHistory
import com.example.data.model.IpoStatusFilter
import com.example.data.model.LiveIpoItem
import com.example.data.model.PanProfile
import com.example.data.model.UserProfile
import com.example.data.model.WatchlistItem
import com.example.data.model.calendarEvents
import com.example.data.model.lifecycleStage
import com.example.data.model.withExtras
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/** What the dashboard's Listed section shows from the listed-IPO history. */
data class ListedHistoryView(
    val isLoading: Boolean = true,
    val error: String? = null,
    val records: List<ListedIpoRecord> = emptyList(),
    val totalMatching: Int = 0,
    val years: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val monthPerformance: ListingPerformance? = null
) {
    val canLoadMore: Boolean get() = records.size < totalMatching
}

enum class IpoSortOption(val label: String) {
    DEFAULT("Market Order"),
    HIGHEST_GMP_PCT("Highest GMP %"),
    HIGHEST_PROFIT("Est. Profit / Lot"),
    HIGHEST_SUB("Subscription (x)")
}

/** Progress of downloading IPO pages for the intermediary directory. */
data class PageDetailsSyncState(
    val isRunning: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val failed: Int = 0
)

class IpoViewModel(
    private val repository: IpoFirestoreRepository,
    private val investorGainClient: InvestorGainClient,
    private val currentUserId: String,
    private val pageDetailsCache: IpoPageDetailsCache? = null
) : ViewModel() {

    private val _liveIposState = MutableStateFlow<UiState<List<LiveIpoItem>>>(UiState.Loading)

    // ipo_extras/{ipoId} overlay; an unreachable or empty collection simply leaves the feed as is
    private val firestoreExtras: StateFlow<Map<String, IpoExtras>> = repository.observeIpoExtras()
        .catch { e ->
            Log.w(TAG, "ipo_extras unavailable, using InvestorGain data only", e)
            emit(emptyMap())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Details read from each IPO's InvestorGain page, keyed by IPO id. */
    private val _pageDetails = MutableStateFlow<Map<String, IpoPageDetails>>(emptyMap())
    private val pageFetchMutex = Mutex()
    private var bulkSyncJob: Job? = null

    private val _pageDetailsSync = MutableStateFlow(PageDetailsSyncState())
    val pageDetailsSync: StateFlow<PageDetailsSyncState> = _pageDetailsSync.asStateFlow()

    /** Page details overlaid with ipo_extras; hand-maintained Firestore values win. */
    val ipoExtrasState: StateFlow<Map<String, IpoExtras>> = combine(
        firestoreExtras,
        _pageDetails
    ) { firestore, pages ->
        (firestore.keys + pages.keys).associateWith { id ->
            (firestore[id] ?: IpoExtras()).mergedOver(pages[id]?.toExtras())
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _listedHistory = MutableStateFlow<UiState<List<ListedIpoRecord>>>(UiState.Loading)

    /** Listed-IPO history with ipo_extras corporate-action adjustments, newest first. */
    val listedHistoryState: StateFlow<UiState<List<ListedIpoRecord>>> = combine(
        _listedHistory,
        ipoExtrasState
    ) { state, extras ->
        if (state is UiState.Success) {
            UiState.Success(sortListedHistory(state.data.map { it.withExtras(extras[it.id]) }))
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val liveIposState: StateFlow<UiState<List<LiveIpoItem>>> = combine(
        _liveIposState,
        ipoExtrasState,
        listedHistoryState
    ) { state, extras, history ->
        if (state !is UiState.Success) return@combine state
        // Current market price for recently listed IPOs comes from the history report
        val ltpById = (history as? UiState.Success)?.data
            ?.associate { it.id to it.currentPrice }
            .orEmpty()
        UiState.Success(
            state.data.map { item ->
                val merged = item.withExtras(extras[item.id])
                if (merged.currentPrice == null && ltpById[item.id] != null) {
                    merged.copy(currentPrice = ltpById[item.id])
                } else {
                    merged
                }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _listedYearFilter = MutableStateFlow<Int?>(null)
    private val _listedVisibleCount = MutableStateFlow(LISTED_PAGE_SIZE)

    /** All dated IPO milestones (including estimated ones) for the calendar and today's strip. */
    val calendarEventsState: StateFlow<List<CalendarEvent>> = combine(
        liveIposState,
        ipoExtrasState
    ) { state, extras ->
        (state as? UiState.Success)?.data
            ?.let { calendarEvents(it, extras, LocalDate.now()) }
            .orEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _lastLiveRefreshMillis = MutableStateFlow<Long?>(null)
    val lastLiveRefreshMillis: StateFlow<Long?> = _lastLiveRefreshMillis.asStateFlow()

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

    private val _hidePanNumbers = MutableStateFlow(false)
    val hidePanNumbers: StateFlow<Boolean> = _hidePanNumbers.asStateFlow()

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
        _guestPanProfiles.map { UiState.Success(it.sortedBy { p -> p.holderName.lowercase() }) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
    } else {
        repository.observePanProfiles(currentUserId)
            .map<List<PanProfile>, UiState<List<PanProfile>>> { list ->
                // The vault is always shown in alphabetical order of holder name
                UiState.Success(list.sortedBy { it.holderName.lowercase() })
            }
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
        liveIposState,
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
                val today = LocalDate.now()
                val filtered = liveState.data.filter { item ->
                    val stage = item.lifecycleStage(today)
                    val matchesStatus = when (status) {
                        IpoStatusFilter.ALL -> true
                        IpoStatusFilter.OPEN -> stage == IpoLifecycleStage.OPEN
                        IpoStatusFilter.UPCOMING -> stage == IpoLifecycleStage.UPCOMING
                        IpoStatusFilter.CLOSED -> stage == IpoLifecycleStage.CLOSED
                        IpoStatusFilter.ALLOTTED -> stage == IpoLifecycleStage.ALLOTTED
                        IpoStatusFilter.LISTED -> stage == IpoLifecycleStage.LISTED
                        IpoStatusFilter.CANCELLED -> stage == IpoLifecycleStage.CANCELLED
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

    val listedHistoryView: StateFlow<ListedHistoryView> = combine(
        listedHistoryState,
        combine(_boardFilter, _statusFilter, _searchQuery) { board, status, query -> Triple(board, status, query) },
        combine(_onlyWatchlist, watchlistState) { onlyWatch, wState ->
            onlyWatch to ((wState as? UiState.Success)?.data?.map { it.ipoId }?.toSet() ?: emptySet())
        },
        combine(_listedYearFilter, _listedVisibleCount) { year, count -> year to count },
        _liveIposState
    ) { historyState, (board, status, query), (onlyWatch, watchedIds), (year, visibleCount), liveState ->
        when (historyState) {
            is UiState.Loading -> ListedHistoryView(isLoading = true, selectedYear = year)
            is UiState.Error -> ListedHistoryView(isLoading = false, error = historyState.message, selectedYear = year)
            is UiState.Success -> {
                // Listings still in the live feed are already shown as full cards
                val liveIds = (liveState as? UiState.Success)?.data?.map { it.id }?.toSet().orEmpty()
                val boardFiltered = historyState.data.filter { record ->
                    when (board) {
                        IpoBoardFilter.ALL -> true
                        IpoBoardFilter.MAINBOARD -> record.category == "Mainboard"
                        IpoBoardFilter.SME -> record.category == "SME"
                    }
                }
                val showHistory = status == IpoStatusFilter.ALL || status == IpoStatusFilter.LISTED
                val matching = if (!showHistory) emptyList() else boardFiltered.filter { record ->
                    record.id !in liveIds &&
                        (year == null || record.listingDate?.year == year) &&
                        (query.isBlank() ||
                            record.name.contains(query, ignoreCase = true) ||
                            record.symbol.contains(query, ignoreCase = true)) &&
                        (!onlyWatch || record.id in watchedIds)
                }
                ListedHistoryView(
                    isLoading = false,
                    records = matching.take(visibleCount),
                    totalMatching = matching.size,
                    years = listingYears(historyState.data),
                    selectedYear = year,
                    monthPerformance = listingPerformance(boardFiltered, YearMonth.now())
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ListedHistoryView())

    /** Lead manager, registrar and market maker directories across all known IPOs. */
    val intermediaryDirectoryState: StateFlow<IntermediaryDirectory> = combine(
        liveIposState,
        listedHistoryState,
        ipoExtrasState
    ) { live, history, extras ->
        buildIntermediaryDirectory(ipoRefs(live, history), extras)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        IntermediaryDirectory(emptyMap(), 0, 0)
    )

    init {
        viewModelScope.launch {
            pageDetailsCache?.load()?.let { cached ->
                _pageDetails.value = cached + _pageDetails.value
            }
        }
        refreshLiveInvestorGainData()
    }

    private fun ipoRefs(
        live: UiState<List<LiveIpoItem>>,
        history: UiState<List<ListedIpoRecord>>
    ): List<IpoRef> {
        val historyRefs = (history as? UiState.Success)?.data.orEmpty().map {
            IpoRef(it.id, it.name, it.category, it.listingGainPct, it.listingDate, it.url)
        }
        val today = LocalDate.now()
        val liveRefs = (live as? UiState.Success)?.data.orEmpty().map { item ->
            val gain = item.listingPrice?.takeIf { item.issuePrice > 0 }
                ?.let { (it - item.issuePrice) / item.issuePrice * 100.0 }
            IpoRef(item.id, item.name, item.category, gain, item.listingLocalDate(today), item.investorGainUrl)
        }
        // History rows carry the authoritative listing gain, so they go first
        return (historyRefs + liveRefs).distinctBy { it.id }
    }

    private suspend fun fetchPageDetails(id: String, url: String): Boolean {
        if (url.isBlank()) return false
        val result = pageFetchMutex.withLock { investorGainClient.fetchIpoPageDetails(url) }
        return result.fold(
            onSuccess = { details ->
                _pageDetails.value = _pageDetails.value + (id to details)
                true
            },
            onFailure = { err ->
                Log.w(TAG, "Could not read IPO page $url", err)
                false
            }
        )
    }

    private fun persistPageDetails() {
        val cache = pageDetailsCache ?: return
        viewModelScope.launch { cache.save(_pageDetails.value) }
    }

    /**
     * Reads the IPO page if it is not cached yet, or again when [refresh] is set
     * (open IPOs, whose subscription changes during the day).
     */
    fun ensurePageDetails(ipo: LiveIpoItem, refresh: Boolean = false) {
        if (!refresh && _pageDetails.value.containsKey(ipo.id)) return
        viewModelScope.launch {
            if (fetchPageDetails(ipo.id, ipo.investorGainUrl)) persistPageDetails()
        }
    }

    /** After each feed refresh: read pages of new IPOs and re-read open ones. */
    private fun refreshLivePageDetails(items: List<LiveIpoItem>) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val targets = items.filter {
                !_pageDetails.value.containsKey(it.id) || it.lifecycleStage(today) == IpoLifecycleStage.OPEN
            }
            var changed = false
            for (item in targets) {
                if (fetchPageDetails(item.id, item.investorGainUrl)) changed = true
                delay(PAGE_FETCH_GAP_MS)
            }
            if (changed) persistPageDetails()
        }
    }

    /** Downloads the pages of every known IPO that has no details yet. */
    fun syncAllPageDetails() {
        if (bulkSyncJob?.isActive == true) return
        bulkSyncJob = viewModelScope.launch {
            val missing = ipoRefs(liveIposState.value, listedHistoryState.value)
                .filter { it.url.isNotBlank() && !_pageDetails.value.containsKey(it.id) }
            _pageDetailsSync.value = PageDetailsSyncState(isRunning = true, total = missing.size)
            var failed = 0
            try {
                missing.forEachIndexed { index, ref ->
                    if (!fetchPageDetails(ref.id, ref.url)) failed++
                    _pageDetailsSync.value = PageDetailsSyncState(true, index + 1, missing.size, failed)
                    // Save as we go so an interrupted sync keeps its progress
                    if ((index + 1) % 10 == 0) pageDetailsCache?.save(_pageDetails.value)
                    delay(PAGE_FETCH_GAP_MS)
                }
            } finally {
                withContext(NonCancellable) { pageDetailsCache?.save(_pageDetails.value) }
                _pageDetailsSync.value = _pageDetailsSync.value.copy(isRunning = false)
            }
        }
    }

    fun cancelPageDetailsSync() {
        bulkSyncJob?.cancel()
    }

    private var autoRefreshJob: Job? = null

    /** Refreshes the live feed every [minutes] while the app is open; 0 turns it off. */
    fun setAutoRefreshMinutes(minutes: Int) {
        autoRefreshJob?.cancel()
        if (minutes <= 0) return
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(minutes * 60_000L)
                refreshLiveInvestorGainData()
            }
        }
    }

    /** Drops downloaded IPO page details, then reads the live IPOs' pages again. */
    fun clearCache(onCleared: () -> Unit = {}) {
        viewModelScope.launch {
            bulkSyncJob?.cancel()
            pageDetailsCache?.clear()
            _pageDetails.value = emptyMap()
            _pageDetailsSync.value = PageDetailsSyncState()
            (_liveIposState.value as? UiState.Success)?.data?.let { refreshLivePageDetails(it) }
            onCleared()
        }
    }

    fun setListedYearFilter(year: Int?) {
        _listedYearFilter.value = year
        _listedVisibleCount.value = LISTED_PAGE_SIZE
    }

    fun loadMoreListed() {
        _listedVisibleCount.value += LISTED_PAGE_SIZE
    }

    private fun refreshListedHistory() {
        viewModelScope.launch {
            if (_listedHistory.value !is UiState.Success) _listedHistory.value = UiState.Loading
            investorGainClient.fetchListedHistory().fold(
                onSuccess = { _listedHistory.value = UiState.Success(it) },
                onFailure = { err ->
                    Log.w(TAG, "Failed to fetch listed IPO history", err)
                    // Keep showing the last good copy if there is one
                    if (_listedHistory.value !is UiState.Success) {
                        _listedHistory.value = UiState.Error(
                            err.localizedMessage ?: "Failed to load listed IPO history"
                        )
                    }
                }
            )
        }
    }

    fun refreshLiveInvestorGainData() {
        refreshListedHistory()
        viewModelScope.launch {
            _liveIposState.value = UiState.Loading
            val result = investorGainClient.fetchLiveIpos()
            result.fold(
                onSuccess = { list ->
                    _liveIposState.value = UiState.Success(list)
                    _lastLiveRefreshMillis.value = System.currentTimeMillis()
                    refreshLivePageDetails(list)
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

    fun toggleHidePanNumbers() {
        _hidePanNumbers.value = !_hidePanNumbers.value
        _statusBanner.value = if (_hidePanNumbers.value) {
            "Privacy Mode ON: PAN numbers are now masked (ABCDE****F)"
        } else {
            "Privacy Mode OFF: Full PAN numbers visible"
        }
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

    fun updatePanProfile(
        panId: String,
        holderName: String,
        panNumber: String,
        dematId: String,
        upiId: String,
        category: String
    ) {
        viewModelScope.launch {
            val safePan = panNumber.trim().uppercase().padEnd(10, 'X').take(10)
            if (currentUserId == "guest") {
                _guestPanProfiles.value = _guestPanProfiles.value.map {
                    if (it.id == panId) {
                        it.copy(
                            holderName = holderName.trim(),
                            panNumber = safePan,
                            dematId = dematId.trim().ifBlank { "1208160000000001" },
                            upiId = upiId.trim(),
                            category = category
                        )
                    } else it
                }
                _statusBanner.value = "Updated PAN profile for $holderName ($safePan)"
            } else {
                val res = repository.updatePanProfile(panId, holderName, safePan, dematId, upiId, category)
                if (res.isSuccess) {
                    _statusBanner.value = "Updated PAN profile for $holderName ($safePan)"
                } else {
                    _statusBanner.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update PAN profile"
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
                _statusBanner.value = "Updated ${app.ipoName} (${app.panNumber}) status to: $newStatus"
            } else {
                repository.updateApplicationStatus(app.id, newStatus)
                _statusBanner.value = "Updated ${app.ipoName} (${app.panNumber}) status to: $newStatus"
            }
        }
    }

    fun requestNewMandate(app: IpoApplication, newUpiId: String) {
        viewModelScope.launch {
            val cleanUpi = newUpiId.trim().ifBlank { app.upiId.ifBlank { "investor@upi" } }
            if (currentUserId == "guest") {
                _guestApplications.value = _guestApplications.value.map {
                    if (it.id == app.id) it.copy(upiId = cleanUpi, status = "Mandate Pending") else it
                }
                _statusBanner.value = "New UPI Mandate requested on $cleanUpi for ${app.panHolderName} (${app.ipoName})"
            } else {
                val res = repository.requestNewMandate(app.id, cleanUpi)
                if (res.isSuccess) {
                    _statusBanner.value = "New UPI Mandate requested on $cleanUpi for ${app.panHolderName} (${app.ipoName})"
                } else {
                    _statusBanner.value = "Failed to request new UPI mandate"
                }
            }
        }
    }

    fun applyIpoForAllSavedPans(
        ipo: LiveIpoItem,
        panProfiles: List<PanProfile>,
        lotsPerPan: Int = 1,
        isPreApply: Boolean = false
    ) {
        viewModelScope.launch {
            val existingApps = (applicationsState.value as? UiState.Success)?.data ?: emptyList()
            val lotShares = if (ipo.lotSize > 0) ipo.lotSize else 15
            val safeLots = lotsPerPan.coerceIn(1, 25)
            val shares = lotShares * safeLots
            val price = if (ipo.issuePrice > 0) ipo.issuePrice else 100
            val total = shares * price
            val targetStatus = if (isPreApply || ipo.statusCode == "U") "Mandate Pending" else "Applied"

            val profilesToUse = if (panProfiles.isNotEmpty()) {
                panProfiles
            } else {
                listOf(
                    PanProfile(
                        id = "default_pan",
                        userId = currentUserId,
                        holderName = "Primary Investor",
                        panNumber = "ABCDE1234F",
                        dematId = "1208160000000001",
                        upiId = "investor@upi",
                        category = "Retail"
                    )
                )
            }

            var addedCount = 0
            for (pan in profilesToUse) {
                val alreadyExists = existingApps.any {
                    (it.ipoId == ipo.id || it.ipoName.equals(ipo.name, ignoreCase = true)) &&
                        it.panNumber.equals(pan.panNumber, ignoreCase = true)
                }
                if (!alreadyExists) {
                    addedCount++
                    if (currentUserId == "guest") {
                        val app = IpoApplication(
                            id = "app_${ipo.id}_${pan.panNumber}_${System.currentTimeMillis()}",
                            userId = "guest",
                            ipoId = ipo.id,
                            ipoName = ipo.name,
                            panHolderName = pan.holderName.ifBlank { "Investor" },
                            panNumber = pan.panNumber.uppercase().padEnd(10, 'X').take(10),
                            category = pan.category.ifBlank { "Retail" },
                            lotsApplied = safeLots.toLong(),
                            sharesApplied = shares.toLong(),
                            bidPrice = price.toLong(),
                            totalAmount = total.toLong(),
                            upiId = pan.upiId.ifBlank { "investor@upi" },
                            status = targetStatus
                        )
                        _guestApplications.value = _guestApplications.value + app
                    } else {
                        repository.submitIpoApplication(
                            ipoId = ipo.id,
                            ipoName = ipo.name,
                            panHolderName = pan.holderName.ifBlank { "Investor" },
                            panNumber = pan.panNumber.uppercase().padEnd(10, 'X').take(10),
                            category = pan.category.ifBlank { "Retail" },
                            lotsApplied = safeLots,
                            sharesApplied = shares,
                            bidPrice = price,
                            totalAmount = total,
                            upiId = pan.upiId.ifBlank { "investor@upi" },
                            status = targetStatus
                        )
                    }
                }
            }
            _statusBanner.value = if (addedCount > 0) {
                if (isPreApply || ipo.statusCode == "U") {
                    "Pre-Applied ${ipo.name} for $addedCount PAN card(s) • Mandate queued!"
                } else {
                    "Added ${ipo.name} to Applied IPOs for $addedCount saved PAN card(s)"
                }
            } else {
                "${ipo.name} is ready for Multi-PAN Allotment Check"
            }
        }
    }

    fun saveBatchAllotmentResults(
        ipoId: String,
        ipoName: String,
        lotSize: Int,
        issuePrice: Int,
        results: Map<PanProfile, String>
    ) {
        viewModelScope.launch {
            val existingApps = (applicationsState.value as? UiState.Success)?.data ?: emptyList()
            val shares = (if (lotSize > 0) lotSize else 15).coerceIn(1, 1000000)
            val price = (if (issuePrice > 0) issuePrice else 100).coerceIn(1, 100000)
            val total = (shares * price).coerceIn(1, 100000000)

            for ((pan, resolvedStatus) in results) {
                val safePan = pan.panNumber.trim().uppercase().padEnd(10, 'X').take(10)
                val existing = existingApps.firstOrNull {
                    (it.ipoId == ipoId || it.ipoName.equals(ipoName, ignoreCase = true)) &&
                        it.panNumber.equals(safePan, ignoreCase = true)
                }
                if (existing != null) {
                    if (existing.status != resolvedStatus) {
                        if (currentUserId == "guest") {
                            _guestApplications.value = _guestApplications.value.map {
                                if (it.id == existing.id) it.copy(status = resolvedStatus) else it
                            }
                        } else {
                            repository.updateApplicationStatus(existing.id, resolvedStatus)
                        }
                    }
                } else {
                    if (currentUserId == "guest") {
                        val newApp = IpoApplication(
                            id = "app_${ipoId}_${safePan}_${System.currentTimeMillis()}",
                            userId = "guest",
                            ipoId = ipoId.ifBlank { "ipo_${ipoName.hashCode()}" },
                            ipoName = ipoName,
                            panHolderName = pan.holderName.ifBlank { "Investor" },
                            panNumber = safePan,
                            category = pan.category.ifBlank { "Retail" },
                            lotsApplied = 1L,
                            sharesApplied = shares.toLong(),
                            bidPrice = price.toLong(),
                            totalAmount = total.toLong(),
                            upiId = pan.upiId.ifBlank { "investor@upi" },
                            status = resolvedStatus
                        )
                        _guestApplications.value = _guestApplications.value + newApp
                    } else {
                        repository.submitIpoApplication(
                            ipoId = ipoId.ifBlank { "ipo_${ipoName.hashCode()}" },
                            ipoName = ipoName,
                            panHolderName = pan.holderName.ifBlank { "Investor" },
                            panNumber = safePan,
                            category = pan.category.ifBlank { "Retail" },
                            lotsApplied = 1,
                            sharesApplied = shares,
                            bidPrice = price,
                            totalAmount = total,
                            upiId = pan.upiId.ifBlank { "investor@upi" },
                            status = resolvedStatus
                        )
                    }
                }
            }
        }
    }

    /**
     * Records one registrar check. Updates the PAN's application for this IPO,
     * or creates one when a definite result arrives for a vault PAN that has no
     * logged application yet. "Could not verify" never creates a record.
     */
    fun saveAllotmentOutcome(
        ipoId: String,
        ipoName: String,
        lotSize: Int,
        issuePrice: Int,
        target: AllotmentTarget,
        outcome: AllotmentOutcome
    ) {
        viewModelScope.launch {
            val existingApps = (applicationsState.value as? UiState.Success)?.data ?: emptyList()
            val existing = existingApps.firstOrNull { it.id == target.applicationId }
                ?: existingApps.firstOrNull {
                    (it.ipoId == ipoId || it.ipoName.equals(ipoName, ignoreCase = true)) &&
                        it.panNumber.equals(target.panNumber, ignoreCase = true)
                }
            val newStatus = when (outcome.result) {
                AllotmentResult.ALLOTTED -> "Allotted"
                AllotmentResult.NOT_ALLOTTED -> "Not Allotted"
                AllotmentResult.UNVERIFIED -> null
            }
            if (currentUserId == "guest") {
                if (existing != null) {
                    _guestApplications.value = _guestApplications.value.map {
                        if (it.id == existing.id) it.copy(
                            allotmentResult = outcome.result.stored,
                            sharesAllotted = outcome.sharesAllotted,
                            checkedAt = com.google.firebase.Timestamp.now(),
                            status = newStatus ?: it.status
                        ) else it
                    }
                } else if (newStatus != null) {
                    val shares = (if (lotSize > 0) lotSize else 1).toLong()
                    val price = (if (issuePrice > 0) issuePrice else 1).toLong()
                    _guestApplications.value = _guestApplications.value + IpoApplication(
                        id = "app_${ipoId}_${target.panNumber}_${System.currentTimeMillis()}",
                        userId = "guest",
                        ipoId = ipoId,
                        ipoName = ipoName,
                        panHolderName = target.holderName.ifBlank { "Investor" },
                        panNumber = target.panNumber,
                        lotsApplied = 1L,
                        sharesApplied = shares,
                        bidPrice = price,
                        totalAmount = shares * price,
                        status = newStatus,
                        allotmentResult = outcome.result.stored,
                        sharesAllotted = outcome.sharesAllotted,
                        checkedAt = com.google.firebase.Timestamp.now()
                    )
                }
                return@launch
            }
            val appId = existing?.id ?: if (newStatus != null) {
                val shares = if (lotSize > 0) lotSize else 1
                val price = if (issuePrice > 0) issuePrice else 1
                repository.submitIpoApplication(
                    ipoId = ipoId.ifBlank { "ipo_${ipoName.hashCode()}" },
                    ipoName = ipoName,
                    panHolderName = target.holderName.ifBlank { "Investor" },
                    panNumber = target.panNumber,
                    category = "Retail",
                    lotsApplied = 1,
                    sharesApplied = shares,
                    bidPrice = price,
                    totalAmount = shares * price,
                    // Applications need a UPI; use the vault PAN's default when it has one
                    upiId = (panProfilesState.value as? UiState.Success)?.data
                        ?.firstOrNull { it.panNumber.equals(target.panNumber, ignoreCase = true) }
                        ?.upiId?.takeIf { it.length >= 3 } ?: "not-set@upi",
                    status = newStatus
                ).getOrNull()
            } else null
            if (appId != null) {
                val res = repository.updateAllotmentResult(appId, outcome.result.stored, outcome.sharesAllotted, newStatus)
                if (res.isFailure) {
                    _statusBanner.value = "Could not save the allotment result for ${target.holderName}"
                }
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
        private const val LISTED_PAGE_SIZE = 20
        private const val PAGE_FETCH_GAP_MS = 300L
    }
}

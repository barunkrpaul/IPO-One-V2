package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.LiveIpoItem
import com.example.data.remote.InvestorGainClient
import com.example.data.repository.IpoFirestoreRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AllotmentAndVaultScreen
import com.example.ui.screens.IpoDetailSheet
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.SecuritySettingsScreen
import com.example.ui.theme.IpoOneTheme
import com.example.ui.viewmodel.IpoViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.BiometricAuthHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IpoOneTheme {
                AppRootNavigation()
            }
        }
    }
}

@Composable
fun AppRootNavigation(auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var pendingMobilePhone by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingInvestorName by rememberSaveable { mutableStateOf<String?>(null) }
    var isGuestMode by rememberSaveable { mutableStateOf(false) }

    val user = currentUser
    if (user == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = { mobilePhone, investorName ->
                pendingMobilePhone = mobilePhone
                pendingInvestorName = investorName
                isGuestMode = false
            },
            onExploreAsGuest = {
                isGuestMode = true
            }
        )
    } else {
        MainAppContent(
            currentUser = user,
            isGuestMode = (user == null),
            onSignInRequested = {
                isGuestMode = false
            },
            initialMobilePhone = pendingMobilePhone,
            initialInvestorName = pendingInvestorName
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    currentUser: FirebaseUser?,
    isGuestMode: Boolean,
    onSignInRequested: () -> Unit,
    initialMobilePhone: String?,
    initialInvestorName: String?
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val userId = currentUser?.uid ?: "guest"

    val viewModel: IpoViewModel = viewModel(
        key = userId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                IpoViewModel(
                    repository = IpoFirestoreRepository(db),
                    investorGainClient = InvestorGainClient(),
                    currentUserId = userId
                )
            }
        }
    )

    val allIposState by viewModel.liveIposState.collectAsStateWithLifecycle()
    val filteredIposState by viewModel.filteredIposState.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val boardFilter by viewModel.boardFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val onlyWatchlist by viewModel.onlyWatchlist.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val panProfilesState by viewModel.panProfilesState.collectAsStateWithLifecycle()
    val applicationsState by viewModel.applicationsState.collectAsStateWithLifecycle()
    val watchlistState by viewModel.watchlistState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? UiState.Success)?.data
    val watchedIds = remember(watchlistState) {
        (watchlistState as? UiState.Success)?.data?.map { it.ipoId }?.toSet() ?: emptySet()
    }
    val panList = (panProfilesState as? UiState.Success)?.data ?: emptyList()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedIpo by remember { mutableStateOf<LiveIpoItem?>(null) }
    var isVaultLocked by rememberSaveable { mutableStateOf(false) }
    var lockInitializedForSession by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentUser?.uid, initialMobilePhone, initialInvestorName) {
        if (currentUser != null) {
            viewModel.ensureUserProfileInitialized(
                displayName = initialInvestorName ?: currentUser.displayName ?: "IPO Investor",
                email = currentUser.email,
                phoneNumber = initialMobilePhone,
                authProvider = if (initialMobilePhone != null) "Mobile+Google" else "Google"
            )
        }
    }

    LaunchedEffect(userProfile?.biometricEnabled) {
        if (currentUser != null && userProfile?.biometricEnabled == true && !lockInitializedForSession) {
            lockInitializedForSession = true
            isVaultLocked = true
        }
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    if (currentTab != 0) {
        BackHandler { currentTab = 0 }
    }

    if (isVaultLocked) {
        BiometricLockOverlay(
            onUnlockRequested = {
                if (activity != null) {
                    BiometricAuthHelper.authenticate(
                        activity = activity,
                        onSuccess = { isVaultLocked = false },
                        onError = { isVaultLocked = false }
                    )
                } else {
                    isVaultLocked = false
                }
            },
            onBypassUnlock = { isVaultLocked = false }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "IPO One",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    if (isGuestMode) {
                        Button(
                            onClick = onSignInRequested,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("top_sign_in_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign In")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Live IPOs") },
                    label = { Text("Live GMP") },
                    modifier = Modifier.testTag("nav_live_gmp")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Allotment & Vault") },
                    label = { Text("Allotment & Vault") },
                    modifier = Modifier.testTag("nav_allotment_vault")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Security Settings") },
                    label = { Text("Security") },
                    modifier = Modifier.testTag("nav_security_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> MainDashboardScreen(
                    allIposState = allIposState,
                    filteredIposState = filteredIposState,
                    statusFilter = statusFilter,
                    boardFilter = boardFilter,
                    sortOption = sortOption,
                    searchQuery = searchQuery,
                    onlyWatchlist = onlyWatchlist,
                    watchedIds = watchedIds,
                    onStatusFilterChange = viewModel::setStatusFilter,
                    onBoardFilterChange = viewModel::setBoardFilter,
                    onSortOptionChange = viewModel::setSortOption,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onToggleOnlyWatchlist = viewModel::toggleOnlyWatchlist,
                    onToggleWatchItem = viewModel::toggleWatchlist,
                    onRefresh = viewModel::refreshLiveInvestorGainData,
                    onSelectIpo = { selectedIpo = it }
                )

                1 -> Column(modifier = Modifier.fillMaxSize()) {
                    if (isGuestMode) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { onSignInRequested() }
                                .testTag("guest_sync_reminder_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Guest Mode: Vault active on-device. Sign In anytime to enable Cloud Sync.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "Sign In",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    AllotmentAndVaultScreen(
                        panProfilesState = panProfilesState,
                        applicationsState = applicationsState,
                        onAddPanProfile = viewModel::addPanProfile,
                        onDeletePanProfile = viewModel::deletePanProfile,
                        onCycleApplicationStatus = viewModel::cycleApplicationStatus,
                        onUpdateApplicationStatus = viewModel::setApplicationStatus,
                        onDeleteApplication = viewModel::deleteApplication
                    )
                }

                2 -> if (isGuestMode) {
                    GuestSecurityPlaceholder(onSignInRequested = onSignInRequested)
                } else {
                    SecuritySettingsScreen(
                        userProfile = userProfile,
                        userEmail = currentUser?.email,
                        userDisplayName = currentUser?.displayName,
                        onToggleBiometric = viewModel::updateBiometricLock,
                        onLockAppNow = { isVaultLocked = true },
                        onUpdateMobilePhone = { phone ->
                            viewModel.updateMobilePhone(
                                phoneNumber = phone,
                                displayName = currentUser?.displayName ?: "IPO Investor",
                                email = currentUser?.email
                            )
                        },
                        onSignOut = {
                            signOutUser(
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignInRequested,
                                scope = scope
                            )
                        }
                    )
                }
            }
        }
    }

    selectedIpo?.let { ipo ->
        IpoDetailSheet(
            ipo = ipo,
            isWatched = watchedIds.contains(ipo.id),
            panProfiles = panList,
            onToggleWatch = {
                if (isGuestMode) {
                    onSignInRequested()
                } else {
                    viewModel.toggleWatchlist(ipo, watchedIds.contains(ipo.id))
                }
            },
            onSubmitBid = { panHolderName, panNumber, category, lots, upiId ->
                if (isGuestMode) {
                    onSignInRequested()
                } else {
                    viewModel.submitIpoApplication(
                        ipo = ipo,
                        panHolderName = panHolderName,
                        panNumber = panNumber,
                        category = category,
                        lotsApplied = lots,
                        upiId = upiId
                    )
                }
            },
            onDismiss = { selectedIpo = null }
        )
    }
}

@Composable
private fun GuestVaultPlaceholder(onSignInRequested: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Encrypted Family PAN Vault",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign in with Google to securely store multiple family PANs, Demat IDs, and UPI handles, and track all your multi-lot IPO applications with cloud sync.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onSignInRequested,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("guest_pan_vault_sign_in"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google")
                }
            }
        }
    }
}

@Composable
private fun GuestSecurityPlaceholder(onSignInRequested: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Biometric Vault Security",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Protect your financial data with device fingerprint or face unlock. Sign in with Google to configure your hardware security preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onSignInRequested,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("guest_security_sign_in"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google")
                }
            }
        }
    }
}

@Composable
private fun BiometricLockOverlay(
    onUnlockRequested: () -> Unit,
    onBypassUnlock: () -> Unit
) {
    LaunchedEffect(Unit) {
        onUnlockRequested()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Unlock with Biometrics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "IPO One Vault Locked",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Authenticate with Fingerprint, Face Unlock, or Device PIN to access your sensitive PAN Vault and IPO Applications.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onUnlockRequested,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("biometric_unlock_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authenticate with BiometricPrompt", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.local.SampleDataSeeder
import com.example.data.local.entity.IpoEntity
import com.example.data.repository.ApplicationRepository
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.DematRepository
import com.example.data.repository.IpoRepository
import com.example.security.BiometricAuthHelper
import com.example.security.authStateFlow
import com.example.ui.components.IpoDetailDialog
import com.example.ui.components.IpoOneLogo
import com.example.ui.components.NewApplicationDialog
import com.example.ui.components.OnboardingDialog
import com.example.ui.components.ThemeSelectorDialog
import com.example.ui.screens.AllotmentHubScreen
import com.example.ui.screens.ApplicationTrackerScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ComplianceSettingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DematManagerScreen
import com.example.ui.screens.IpoListScreen
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NaradaIpoTheme
import com.example.ui.viewmodel.ApplicationViewModel
import com.example.ui.viewmodel.ApplicationViewModelFactory
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.DashboardViewModelFactory
import com.example.ui.viewmodel.DematViewModel
import com.example.ui.viewmodel.DematViewModelFactory
import com.example.ui.viewmodel.IpoViewModel
import com.example.ui.viewmodel.IpoViewModelFactory
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.SettingsViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

enum class AppDestination(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    IPOS("IPOs", Icons.AutoMirrored.Filled.TrendingUp),
    APPLICATIONS("Bids", Icons.AutoMirrored.Filled.Assignment),
    DEMAT("Accounts", Icons.Default.Groups),
    ALLOTMENT("Allotment", Icons.Default.AccountBalance),
    COMPLIANCE("Security", Icons.Default.Security)
}

class MainActivity : FragmentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var ipoRepository: IpoRepository
    private lateinit var dematRepository: DematRepository
    private lateinit var applicationRepository: ApplicationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getInstance(this)
        ipoRepository = IpoRepository(database.ipoDao())
        dematRepository = DematRepository(database.dematDao())
        applicationRepository = ApplicationRepository(database.applicationDao())

        // Seed initial sample data if database is empty & deploy live IPO feed
        lifecycleScope.launch {
            SampleDataSeeder.seedIfEmpty(
                database.ipoDao(),
                database.dematDao(),
                database.applicationDao()
            )
            SampleDataSeeder.deployLiveIpoData(database.ipoDao())
        }

        setContent {
            var selectedTheme by remember { mutableStateOf(AppThemeOption.OBSIDIAN_EMERALD) }

            NaradaIpoTheme(selectedTheme = selectedTheme) {
                MainAppContent(
                    database = database,
                    ipoRepository = ipoRepository,
                    dematRepository = dematRepository,
                    applicationRepository = applicationRepository,
                    currentTheme = selectedTheme,
                    onThemeChange = { selectedTheme = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    database: AppDatabase,
    ipoRepository: IpoRepository,
    dematRepository: DematRepository,
    applicationRepository: ApplicationRepository,
    currentTheme: AppThemeOption,
    onThemeChange: (AppThemeOption) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firebaseAuth = remember {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }
    val currentUser by if (firebaseAuth != null) {
        firebaseAuth.authStateFlow().collectAsStateWithLifecycle(initialValue = firebaseAuth.currentUser)
    } else {
        remember { mutableStateOf(null) }
    }

    var showAuthScreen by remember { mutableStateOf(false) }
    var investorName by remember { mutableStateOf("Barun Kr Paul") }
    var mobileNumber by remember { mutableStateOf("+91 98765 43210") }
    var investorCategory by remember { mutableStateOf("RETAIL") }

    // If user explicitly opened the Google / Mobile Sign-Up screen
    if (showAuthScreen) {
        AuthScreen(
            initialMobileNumber = mobileNumber,
            initialInvestorName = investorName,
            onAuthSuccess = { name, mobile, category ->
                if (name.isNotBlank()) investorName = name
                if (mobile.isNotBlank()) mobileNumber = mobile
                investorCategory = category
                showAuthScreen = false
                // Sync profile to Firestore only when authenticated with Google
                if (FirebaseAuth.getInstance().currentUser != null) {
                    coroutineScope.launch {
                        val repo = CloudSyncRepository(context)
                        repo.saveUserProfile(
                            displayName = investorName,
                            email = FirebaseAuth.getInstance().currentUser?.email ?: "",
                            mobileNumber = mobileNumber,
                            investorCategory = investorCategory
                        )
                    }
                }
            },
            onContinueLocalVaultOnly = { name, mobile, category ->
                investorName = name
                mobileNumber = mobile
                investorCategory = category
                showAuthScreen = false
                Toast.makeText(
                    context,
                    "Mobile Vault Registered ($mobileNumber)!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        return
    }

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(ipoRepository, dematRepository, applicationRepository)
    )
    val ipoViewModel: IpoViewModel = viewModel(
        factory = IpoViewModelFactory(ipoRepository)
    )
    val dematViewModel: DematViewModel = viewModel(
        factory = DematViewModelFactory(dematRepository)
    )
    val applicationViewModel: ApplicationViewModel = viewModel(
        factory = ApplicationViewModelFactory(applicationRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(database.ipoDao(), database.dematDao(), database.applicationDao())
    )

    var currentDestination by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var isVaultLocked by remember { mutableStateOf(false) }

    // Dialog states
    var selectedIpoForDetail by remember { mutableStateOf<IpoEntity?>(null) }
    var selectedIpoForNewApplication by remember { mutableStateOf<IpoEntity?>(null) }
    var showNewApplicationDialog by remember { mutableStateOf(false) }

    val allIpos by ipoViewModel.allIpos.collectAsStateWithLifecycle()
    val dematAccountsWithMember by dematViewModel.dematAccountsWithMember.collectAsStateWithLifecycle()
    val allUpiAccounts by dematViewModel.allUpiAccounts.collectAsStateWithLifecycle()

    if (isVaultLocked) {
        VaultLockScreen(
            onUnlock = { isVaultLocked = false }
        )
        return
    }

    // Back handling: if not on Dashboard, back returns to Dashboard
    BackHandler(enabled = currentDestination != AppDestination.DASHBOARD) {
        currentDestination = AppDestination.DASHBOARD
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IpoOneLogo(size = 30.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (currentDestination) {
                                AppDestination.DASHBOARD -> "IPO One"
                                AppDestination.IPOS -> "IPO Radar & Live GMP"
                                AppDestination.APPLICATIONS -> "My IPO Bids"
                                AppDestination.DEMAT -> "Demat & UPI Vault"
                                AppDestination.ALLOTMENT -> "Allotment Hub"
                                AppDestination.COMPLIANCE -> "Security & Account"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAuthScreen = true },
                        modifier = Modifier.testTag("topbar_account_signup_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Google & Mobile Sign-Up",
                            tint = if (currentUser != null) EmeraldSuccess else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.testTag("topbar_theme_palette_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Change Color Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { isVaultLocked = true },
                        modifier = Modifier.testTag("topbar_lock_vault_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault Session",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { currentDestination = AppDestination.COMPLIANCE },
                        modifier = Modifier.testTag("topbar_compliance_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy & Security",
                            tint = if (currentDestination == AppDestination.COMPLIANCE) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    IconButton(
                        onClick = { showOnboarding = true },
                        modifier = Modifier.testTag("topbar_help_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "About IPO One",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                listOf(
                    AppDestination.DASHBOARD,
                    AppDestination.IPOS,
                    AppDestination.APPLICATIONS,
                    AppDestination.DEMAT,
                    AppDestination.ALLOTMENT
                ).forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontSize = 11.sp,
                                fontWeight = if (currentDestination == destination) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { target ->
                when (target) {
                    AppDestination.DASHBOARD -> DashboardScreen(
                        dashboardViewModel = dashboardViewModel,
                        onNavigateToIpos = { currentDestination = AppDestination.IPOS },
                        onNavigateToApplications = { currentDestination = AppDestination.APPLICATIONS },
                        onNavigateToAllotment = { currentDestination = AppDestination.ALLOTMENT },
                        onOpenNewApplicationDialog = { ipo ->
                            selectedIpoForNewApplication = ipo
                            showNewApplicationDialog = true
                        },
                        onIpoClick = { ipo -> selectedIpoForDetail = ipo },
                        onNavigateToDemat = { currentDestination = AppDestination.DEMAT }
                    )
                    AppDestination.IPOS -> IpoListScreen(
                        ipoViewModel = ipoViewModel,
                        onIpoClick = { ipo -> selectedIpoForDetail = ipo },
                        onApplyClick = { ipo ->
                            selectedIpoForNewApplication = ipo
                            showNewApplicationDialog = true
                        }
                    )
                    AppDestination.APPLICATIONS -> ApplicationTrackerScreen(
                        applicationViewModel = applicationViewModel,
                        onOpenNewApplicationDialog = {
                            selectedIpoForNewApplication = null
                            showNewApplicationDialog = true
                        }
                    )
                    AppDestination.DEMAT -> DematManagerScreen(
                        dematViewModel = dematViewModel
                    )
                    AppDestination.ALLOTMENT -> AllotmentHubScreen(
                        ipoViewModel = ipoViewModel,
                        applicationViewModel = applicationViewModel,
                        dematViewModel = dematViewModel
                    )
                    AppDestination.COMPLIANCE -> ComplianceSettingsScreen(
                        settingsViewModel = settingsViewModel,
                        currentTheme = currentTheme,
                        onOpenThemeSelector = { showThemeDialog = true },
                        onLockAppNow = { isVaultLocked = true },
                        currentUser = currentUser,
                        investorName = investorName,
                        mobileNumber = mobileNumber,
                        investorCategory = investorCategory,
                        onUpdateInvestorProfile = { name, mobile, cat ->
                            investorName = name
                            mobileNumber = mobile
                            investorCategory = cat
                        },
                        onOpenAuthScreen = { showAuthScreen = true }
                    )
                }
            }
        }
    }

    // Theme Selector Dialog
    if (showThemeDialog) {
        ThemeSelectorDialog(
            currentTheme = currentTheme,
            onSelectTheme = {
                onThemeChange(it)
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // IPO Details Dialog
    selectedIpoForDetail?.let { ipo ->
        IpoDetailDialog(
            ipo = ipo,
            onDismiss = { selectedIpoForDetail = null },
            onApplyClick = { targetIpo, _, _ ->
                selectedIpoForNewApplication = targetIpo
                showNewApplicationDialog = true
            },
            onUpdateGmp = { targetIpo, newGmp ->
                ipoViewModel.updateGmp(targetIpo, newGmp)
                val pct = if (targetIpo.maxPrice > 0) {
                    kotlin.math.round((newGmp / targetIpo.maxPrice) * 1000.0) / 10.0
                } else 0.0
                selectedIpoForDetail = targetIpo.copy(
                    gmpInr = newGmp,
                    gmpPercent = pct,
                    updatedAt = System.currentTimeMillis()
                )
            },
            onUpdateSubscription = { targetIpo, qib, nii, retail, total ->
                ipoViewModel.updateSubscriptionRates(targetIpo, qib, nii, retail, total)
                selectedIpoForDetail = targetIpo.copy(
                    qibSub = qib,
                    niiSub = nii,
                    retailSub = retail,
                    totalSub = total,
                    subscriptionTimes = total,
                    updatedAt = System.currentTimeMillis()
                )
            }
        )
    }

    // New Application Dialog (Single & 1-Click All Family Demats Batch Apply)
    if (showNewApplicationDialog) {
        NewApplicationDialog(
            ipos = allIpos,
            dematAccounts = dematAccountsWithMember,
            upiAccounts = allUpiAccounts,
            initialIpo = selectedIpoForNewApplication,
            onDismiss = { showNewApplicationDialog = false },
            onSubmit = { ipoId, dematId, memberId, upiVpa, upiAccountId, category, lots, bidPrice, isCutoff, totalAmount, appNo, notes ->
                applicationViewModel.createApplication(
                    ipoId = ipoId,
                    dematAccountId = dematId,
                    familyMemberId = memberId,
                    upiVpa = upiVpa,
                    upiAccountId = upiAccountId,
                    category = category,
                    lotCount = lots,
                    bidPrice = bidPrice,
                    isCutoff = isCutoff,
                    totalAmount = totalAmount,
                    applicationNumber = appNo,
                    notes = notes
                )
                showNewApplicationDialog = false
                currentDestination = AppDestination.APPLICATIONS
            },
            onBatchSubmit = { ipoId, selections, category, lots, bidPrice, isCutoff, totalAmountPerBid, notes ->
                applicationViewModel.createBatchApplications(
                    ipoId = ipoId,
                    selections = selections,
                    category = category,
                    lotCount = lots,
                    bidPrice = bidPrice,
                    isCutoff = isCutoff,
                    totalAmountPerBid = totalAmountPerBid,
                    notes = notes
                )
                showNewApplicationDialog = false
                currentDestination = AppDestination.APPLICATIONS
            }
        )
    }

    // Onboarding Dialog
    if (showOnboarding) {
        OnboardingDialog(
            onDismiss = { showOnboarding = false }
        )
    }
}

@Composable
fun VaultLockScreen(
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vault_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Vault Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "IPO One Biometric Vault Locked",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your family PANs, Demat BOIDs, and IPO application history are protected by AndroidX BiometricPrompt & Android Keystore AES-256-GCM encryption.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                BiometricAuthHelper.authenticate(
                                    activity = activity,
                                    title = "Unlock IPO One Vault",
                                    subtitle = "Authenticate with Fingerprint, Face, or Device PIN",
                                    onSuccess = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        onUnlock()
                                    },
                                    onError = { err ->
                                        errorMessage = err
                                    }
                                )
                            } else {
                                onUnlock()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("unlock_vault_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlock with BiometricPrompt / PIN",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

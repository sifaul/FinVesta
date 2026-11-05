package com.example

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.data.security.BiometricAuthManager
import com.example.data.sync.BackupRestoreManager
import com.example.ui.screens.BiometricLockScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.ui.theme.AppThemeMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.ui.components.AddAssetDialog
import com.example.ui.components.AddReminderDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.ExportReportDialog
import com.example.ui.components.ShopeeFoodDriverDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CategoryManagementScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.IncomeExpenseScreen
import com.example.ui.screens.InvestmentScreen
import com.example.ui.screens.MonthlySpendingScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SyncSecurityScreen
import androidx.compose.ui.draw.clip
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinVestaViewModel
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import com.example.util.ReportExporter
import java.util.Calendar

enum class FinNavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Beranda", Icons.Filled.Home, Icons.Outlined.Home, "nav_dashboard"),
    TRANSACTIONS("Transaksi", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_transactions"),
    SPENDING("Spending", Icons.Filled.Payments, Icons.Outlined.Payments, "nav_spending"),
    GOALS("Tujuan", Icons.Filled.Flag, Icons.Outlined.Flag, "nav_goals"),
    CALCULATOR("Kalkulator", Icons.Filled.Calculate, Icons.Outlined.Calculate, "nav_calculator"),
    INVESTMENTS("Investasi", Icons.AutoMirrored.Filled.ShowChart, Icons.AutoMirrored.Outlined.ShowChart, "nav_investments"),
    CATEGORIES("Kategori", Icons.Filled.Palette, Icons.Outlined.Palette, "nav_categories"),
    ANALYTICS("Laporan", Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_reports"),
    REMINDERS("Tagihan", Icons.Filled.Notifications, Icons.Outlined.Notifications, "nav_reminders"),
    SYNC_SECURITY("E2EE & Sync", Icons.Filled.Security, Icons.Outlined.Security, "nav_sync_security")
}

class MainActivity : FragmentActivity() {

    // Scoped to FragmentActivity (ViewModelStoreOwner) using custom Factory
    private val viewModel: FinVestaViewModel by viewModels {
        com.example.ui.viewmodel.FinVestaViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            System.loadLibrary("sqlcipher")
        } catch (_: Throwable) {}
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)

        setContent {
            val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
            var isAppUnlocked by remember {
                mutableStateOf(!BiometricAuthManager.isBiometricEnabled(this@MainActivity))
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        if (!BiometricAuthManager.isPromptActive &&
                            BiometricAuthManager.isBiometricEnabled(this@MainActivity) &&
                            BiometricAuthManager.isAutoLockOnBackgroundEnabled(this@MainActivity)
                        ) {
                            isAppUnlocked = false
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MyApplicationTheme(appThemeMode = appThemeMode) {
                if (!isAppUnlocked && BiometricAuthManager.isBiometricEnabled(this@MainActivity)) {
                    BiometricLockScreen(
                        onUnlockSuccess = {
                            isAppUnlocked = true
                        }
                    )
                } else {
                    FinVestaMainApp(
                        viewModel = viewModel,
                        onLockApp = {
                            isAppUnlocked = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinVestaMainApp(
    viewModel: FinVestaViewModel,
    onLockApp: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(FinNavDestination.DASHBOARD) }
    var showTopMenu by remember { mutableStateOf(false) }

    // Dialog state
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var addTransactionInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var addTransactionInitialCategory by remember { mutableStateOf<String?>(null) }
    var showShopeeFoodDialog by remember { mutableStateOf(false) }
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportMonthTarget by remember { mutableStateOf("") }
    var showThemeDialog by remember { mutableStateOf(false) }

    // State collections
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val assets by viewModel.allAssets.collectAsStateWithLifecycle()
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val goals by viewModel.allGoals.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val budgetAlerts by viewModel.activeBudgetAlerts.collectAsStateWithLifecycle()
    val monthlySpendingLimit by viewModel.monthlySpendingLimit.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val deviceList by viewModel.deviceList.collectAsStateWithLifecycle()
    val latencyMs by viewModel.realtimeLatencyMs.collectAsStateWithLifecycle()
    val isE2eeActive by viewModel.isE2eeActive.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val connectedEmail by viewModel.connectedEmail.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
    val lastCloudBackupTime by viewModel.lastCloudBackupTime.collectAsStateWithLifecycle()
    val isAutoCloudSyncEnabled by viewModel.isAutoCloudSyncEnabled.collectAsStateWithLifecycle()
    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsStateWithLifecycle()

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(context, uri) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupFromUri(context, uri) { summary ->
                Toast.makeText(context, summary.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val anyDialogOpen = showAddTransactionDialog || showShopeeFoodDialog ||
            showAddAssetDialog || showAddReminderDialog || showExportDialog || showThemeDialog

    BackHandler(enabled = anyDialogOpen) {
        showAddTransactionDialog = false
        showShopeeFoodDialog = false
        showAddAssetDialog = false
        showAddReminderDialog = false
        showExportDialog = false
        showThemeDialog = false
    }

    BackHandler(enabled = !anyDialogOpen && currentDestination != FinNavDestination.DASHBOARD) {
        currentDestination = FinNavDestination.DASHBOARD
    }

    var lastBackPressTime by remember { mutableStateOf(0L) }
    BackHandler(enabled = !anyDialogOpen && currentDestination == FinNavDestination.DASHBOARD) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackPressTime = now
            Toast.makeText(context, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_finvesta_logo),
                                contentDescription = "Logo FinVesta",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FinVesta",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.2.sp
                            )
                            Text(
                                text = currentDestination.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { currentDestination = FinNavDestination.REMINDERS }) {
                        Box {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = "Tagihan",
                                tint = if (currentDestination == FinNavDestination.REMINDERS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (reminders.any { it.status == com.example.data.model.ReminderStatus.PENDING }) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(Rose500)
                                        )
                            }
                        }
                    }
                    IconButton(onClick = { currentDestination = FinNavDestination.SYNC_SECURITY }) {
                        Box {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = "E2EE & Sync",
                                tint = if (currentDestination == FinNavDestination.SYNC_SECURITY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isE2eeActive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            showThemeDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = when (appThemeMode) {
                                AppThemeMode.LIGHT -> Icons.Filled.LightMode
                                AppThemeMode.DARK -> Icons.Filled.DarkMode
                                AppThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                            },
                            contentDescription = "Ganti Tema (Terang/Gelap/Sistem)",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { showTopMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu Lainnya")
                    }

                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ekspor Laporan Bulanan") },
                            leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showTopMenu = false
                                exportMonthTarget = ""
                                showExportDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Kelola Kategori Kustom") },
                            leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showTopMenu = false
                                currentDestination = FinNavDestination.CATEGORIES
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Kunci Aplikasi (Biometrik)") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showTopMenu = false
                                onLockApp()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (currentDestination == FinNavDestination.DASHBOARD || currentDestination == FinNavDestination.TRANSACTIONS) {
                FloatingActionButton(
                    onClick = {
                        addTransactionInitialType = TransactionType.EXPENSE
                        addTransactionInitialCategory = null
                        showAddTransactionDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_add_transaction")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Transaksi",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                listOf(
                    FinNavDestination.DASHBOARD,
                    FinNavDestination.TRANSACTIONS,
                    FinNavDestination.INVESTMENTS,
                    FinNavDestination.ANALYTICS,
                    FinNavDestination.GOALS
                ).forEach { dest ->
                    val isSelected = currentDestination == dest
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = dest },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.title
                            )
                        },
                        label = {
                            Text(
                                text = dest.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag(dest.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    val isEnteringDetails = (initialState == FinNavDestination.DASHBOARD &&
                            (targetState == FinNavDestination.INVESTMENTS || targetState == FinNavDestination.TRANSACTIONS || targetState == FinNavDestination.SPENDING || targetState == FinNavDestination.GOALS))

                    if (isEnteringDetails) {
                        (fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                            slideInVertically(initialOffsetY = { it / 14 }, animationSpec = tween(280, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 0.98f, animationSpec = tween(180, easing = FastOutSlowInEasing))
                            )
                    } else if (initialState == FinNavDestination.INVESTMENTS && targetState == FinNavDestination.DASHBOARD) {
                        (fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.98f, animationSpec = tween(260, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                slideOutVertically(targetOffsetY = { it / 14 }, animationSpec = tween(200, easing = FastOutSlowInEasing))
                            )
                    } else {
                        (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.985f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
                            .togetherWith(fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)))
                    }
                },
                label = "ScreenTransition"
            ) { target ->
                when (target) {
                    FinNavDestination.DASHBOARD -> DashboardScreen(
                        transactions = transactions,
                        assets = assets,
                        reminders = reminders,
                        goals = goals,
                        categories = categories,
                        budgetAlerts = budgetAlerts,
                        monthlySpendingLimit = monthlySpendingLimit,
                        syncState = syncState,
                        isE2eeActive = isE2eeActive,
                        onOpenAddIncome = {
                            addTransactionInitialType = TransactionType.INCOME
                            addTransactionInitialCategory = null
                            showAddTransactionDialog = true
                        },
                        onOpenAddExpense = {
                            addTransactionInitialType = TransactionType.EXPENSE
                            addTransactionInitialCategory = null
                            showAddTransactionDialog = true
                        },
                        onOpenShopeeFoodDriver = {
                            showShopeeFoodDialog = true
                        },
                        onDismissAllBudgetAlerts = {
                            viewModel.dismissAllBudgetAlerts()
                        },
                        onOpenAddAsset = {
                            showAddAssetDialog = true
                        },
                        onOpenReminders = {
                            currentDestination = FinNavDestination.REMINDERS
                        },
                        onOpenSyncSecurity = {
                            currentDestination = FinNavDestination.SYNC_SECURITY
                        },
                        onOpenGoals = {
                            currentDestination = FinNavDestination.GOALS
                        },
                        onOpenCalculator = {
                            currentDestination = FinNavDestination.CALCULATOR
                        },
                        onOpenCategories = {
                            currentDestination = FinNavDestination.CATEGORIES
                        },
                        onOpenSpending = {
                            currentDestination = FinNavDestination.SPENDING
                        },
                        onUpdateMonthlySpendingLimit = {
                            viewModel.updateMonthlySpendingLimit(it)
                        },
                        onOpenInvestments = {
                            currentDestination = FinNavDestination.INVESTMENTS
                        },
                        onOpenTransactions = {
                            currentDestination = FinNavDestination.TRANSACTIONS
                        },
                        onOpenAnalytics = {
                            currentDestination = FinNavDestination.ANALYTICS
                        },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onLockApp = onLockApp
                    )

                    FinNavDestination.TRANSACTIONS -> IncomeExpenseScreen(
                        transactions = transactions,
                        categories = categories,
                        onAddTransaction = {
                            addTransactionInitialType = TransactionType.EXPENSE
                            showAddTransactionDialog = true
                        },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onMoveTransactionCategory = { txId, newCatName ->
                            viewModel.moveTransactionCategory(txId, newCatName)
                        },
                        onOpenCategoryManager = {
                            currentDestination = FinNavDestination.CATEGORIES
                        },
                        onOpenShopeeFoodDriver = {
                            showShopeeFoodDialog = true
                        },
                        onOpenMonthlySpending = {
                            currentDestination = FinNavDestination.SPENDING
                        }
                    )

                    FinNavDestination.SPENDING -> MonthlySpendingScreen(
                        transactions = transactions,
                        categories = categories,
                        monthlySpendingLimit = monthlySpendingLimit,
                        onUpdateMonthlySpendingLimit = { viewModel.updateMonthlySpendingLimit(it) },
                        onUpdateCategoryBudget = { catId, newBudget -> viewModel.updateCategoryBudget(catId, newBudget) },
                        onAddCategory = { name, budget, iconName, colorHex ->
                            viewModel.createSpendingPos(
                                name = name,
                                initialAllocation = budget,
                                iconName = iconName,
                                colorHex = colorHex
                            )
                        },
                        onAllocateSpendingFunds = { category, amountToAdd ->
                            viewModel.allocateFundsToSpendingPos(category, amountToAdd)
                        },
                        onReturnRemainingSpendingToCash = { category, remainingAmount ->
                            viewModel.returnRemainingSpendingToCash(category, remainingAmount)
                        },
                        onReturnAllRemainingSpendingToCash = {
                            viewModel.returnAllRemainingSpendingToCash(categories, transactions)
                        },
                        onUpdateCategory = { viewModel.updateCategory(it) },
                        onDeleteCategory = { viewModel.deleteCategory(it) },
                        onAddExpense = { prefilledCategory ->
                            addTransactionInitialType = TransactionType.EXPENSE
                            addTransactionInitialCategory = prefilledCategory
                            showAddTransactionDialog = true
                        },
                        onOpenCategoryManager = {
                            currentDestination = FinNavDestination.CATEGORIES
                        },
                        onBack = {
                            currentDestination = FinNavDestination.DASHBOARD
                        }
                    )

                    FinNavDestination.GOALS -> GoalsScreen(
                        goals = goals,
                        transactions = transactions,
                        onAddGoal = { name, targetAmount, targetDateMillis, category, iconName, colorHex, note, initialSaved ->
                            viewModel.addGoal(
                                name = name,
                                targetAmount = targetAmount,
                                targetDateEpochMillis = targetDateMillis,
                                category = category,
                                iconName = iconName,
                                colorHex = colorHex,
                                note = note,
                                initialSavedAmount = initialSaved
                            )
                        },
                        onDeleteGoal = { viewModel.deleteGoal(it) },
                        onAllocateFunds = { goal, amount -> viewModel.allocateFundsToGoal(goal, amount) },
                        onSpendGoalFunds = { goal, amount, title, note, dateMillis ->
                            viewModel.spendGoalFunds(goal, amount, title, note, dateMillis)
                        },
                        onDeleteGoalExpense = { goal, tx ->
                            viewModel.deleteTransaction(tx)
                        }
                    )

                    FinNavDestination.CALCULATOR -> CalculatorScreen()

                    FinNavDestination.CATEGORIES -> CategoryManagementScreen(
                        categories = categories,
                        transactions = transactions,
                        onAddCategory = { name, isExpense, iconName, colorHex, monthlyBudgetLimit ->
                            viewModel.addCategory(name, isExpense, iconName, colorHex, monthlyBudgetLimit)
                        },
                        onUpdateCategory = { viewModel.updateCategory(it) },
                        onDeleteCategory = { viewModel.deleteCategory(it) },
                        onDeleteCategoryWithMove = { cat, targetCat -> viewModel.deleteCategory(cat, targetCat) },
                        onMoveTransactions = { src, target -> viewModel.moveTransactionsBetweenCategories(src, target) },
                        onAutoCategorize = { proposals -> viewModel.autoCategorizeTransactions(proposals) },
                        onUpdateBudget = { catId, newBudget -> viewModel.updateCategoryBudget(catId, newBudget) },
                        onTriggerTestAlert = { cat -> viewModel.triggerBudgetAlertTest(context, cat) },
                        onRestoreDefaults = {
                            viewModel.restoreDefaultCategories { count ->
                                val message = if (count > 0) {
                                    "$count kategori default berhasil dimuat kembali!"
                                } else {
                                    "Semua kategori default sudah tersedia."
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    FinNavDestination.INVESTMENTS -> InvestmentScreen(
                        assets = assets,
                        onAddAsset = { showAddAssetDialog = true },
                        onUpdatePrice = { asset, price -> viewModel.updateAssetPrice(asset, price) },
                        onEditAsset = { viewModel.updateAsset(it) },
                        onDeleteAsset = { viewModel.deleteAsset(it) }
                    )

                    FinNavDestination.ANALYTICS -> AnalyticsScreen(
                        transactions = transactions,
                        assets = assets,
                        onOpenExportDialog = { monthName ->
                            exportMonthTarget = monthName
                            showExportDialog = true
                        }
                    )

                    FinNavDestination.REMINDERS -> RemindersScreen(
                        reminders = reminders,
                        onAddReminder = { showAddReminderDialog = true },
                        onToggleStatus = { viewModel.toggleReminderStatus(it) },
                        onDeleteReminder = { viewModel.deleteReminder(it) },
                        onSendNotification = { ctx, item -> viewModel.sendReminderNotification(ctx, item) }
                    )

                    FinNavDestination.SYNC_SECURITY -> SyncSecurityScreen(
                        syncState = syncState,
                        lastSyncTimestamp = lastSyncTimestamp,
                        deviceList = deviceList,
                        latencyMs = latencyMs,
                        localDeviceId = viewModel.localDeviceId,
                        pairingCode = viewModel.pairingCode,
                        isE2eeActive = isE2eeActive,
                        keyFingerprint = viewModel.keyFingerprint,
                        appThemeMode = appThemeMode,
                        connectedEmail = connectedEmail,
                        cloudSyncStatus = cloudSyncStatus,
                        lastCloudBackupTime = lastCloudBackupTime,
                        isAutoCloudSyncEnabled = isAutoCloudSyncEnabled,
                        lastBackupTime = lastBackupTime,
                        isAutoBackupEnabled = isAutoBackupEnabled,
                        onBackupToGoogleDrive = {
                            viewModel.shareToGoogleDrive(context) { driveIntent, errorMsg ->
                                if (driveIntent != null) {
                                    try {
                                        val chooser = Intent.createChooser(driveIntent, "Cadangkan ke Google Drive").apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(chooser)
                                    } catch (e: Exception) {
                                        try {
                                            createBackupLauncher.launch(BackupRestoreManager.getSuggestedBackupFileName())
                                        } catch (ex: Exception) {
                                            Toast.makeText(context, "Tidak dapat membuka Google Drive / Pengelola Berkas: ${ex.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    try {
                                        createBackupLauncher.launch(BackupRestoreManager.getSuggestedBackupFileName())
                                    } catch (ex: Exception) {
                                        Toast.makeText(context, "Gagal membuat cadangan: ${errorMsg ?: ex.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        onBackupViaStoragePicker = {
                            try {
                                createBackupLauncher.launch(BackupRestoreManager.getSuggestedBackupFileName())
                            } catch (e: Exception) {
                                Toast.makeText(context, "Pengelola berkas tidak tersedia di ponsel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        },
                        onRestoreFromGoogleDrive = {
                            try {
                                restoreBackupLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "Aplikasi pengelola berkas / Google Drive tidak ditemukan di perangkat.", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal membuka berkas: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRestoreFromLocalSnapshot = {
                            viewModel.restoreFromLocalSnapshot(context) { summary ->
                                Toast.makeText(context, summary.message, Toast.LENGTH_LONG).show()
                            }
                        },
                        onShareBackup = {
                            viewModel.shareBackup(context) { intent ->
                                if (intent != null) {
                                    try {
                                        val chooser = Intent.createChooser(intent, "Simpan Cadangan ke Google Drive / Kirim Berkas").apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(chooser)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal membuka menu berbagi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Gagal membuat berkas cadangan", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onToggleAutoBackup = { enabled ->
                            viewModel.setAutoBackupEnabled(context, enabled)
                        },
                        onConnectEmail = { email, ctx -> viewModel.connectEmail(email, ctx) },
                        onDisconnectEmail = { ctx -> viewModel.disconnectEmail(ctx) },
                        onBackupToCloudNow = { ctx -> viewModel.backupToCloudNow(ctx) },
                        onRestoreFromCloudNow = { ctx -> viewModel.restoreFromCloudNow(ctx) },
                        onToggleAutoCloudSync = { enabled -> viewModel.setAutoCloudSyncEnabled(enabled) },
                        onToggleE2ee = { viewModel.toggleE2ee() },
                        onSyncNow = { ctx -> viewModel.syncNow(ctx) },
                        onChangeThemeMode = { mode -> viewModel.setThemeMode(mode) },
                        onExportPdf = {
                            val currentMonth = DateUtils.formatMonthYear(System.currentTimeMillis())
                            try {
                                val file = viewModel.exportReport(context, isPdf = true, monthName = currentMonth)
                                val intent = ReportExporter.createShareIntent(context, file, "application/pdf")
                                val chooser = Intent.createChooser(intent, "Bagikan Laporan PDF FinVesta").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(chooser)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal ekspor PDF: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        onExportExcel = {
                            val currentMonth = DateUtils.formatMonthYear(System.currentTimeMillis())
                            try {
                                val file = viewModel.exportReport(context, isPdf = false, monthName = currentMonth)
                                val intent = ReportExporter.createShareIntent(context, file, "text/csv")
                                val chooser = Intent.createChooser(intent, "Buka / Bagikan Excel (CSV) FinVesta").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(chooser)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal ekspor Excel: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        onClearAllData = {
                            viewModel.clearAllUserData()
                        }
                    )
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            initialType = addTransactionInitialType,
            initialCategory = addTransactionInitialCategory,
            customCategories = categories,
            goals = goals,
            onOpenShopeeFoodDriver = {
                showShopeeFoodDialog = true
            },
            onDismiss = {
                showAddTransactionDialog = false
                addTransactionInitialCategory = null
            },
            onConfirm = { type, title, amount, category, wallet, note ->
                viewModel.addTransaction(
                    type = type,
                    title = title,
                    amount = amount,
                    categoryName = category,
                    wallet = wallet,
                    note = note
                )
                Toast.makeText(context, "Transaksi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                addTransactionInitialCategory = null
            }
        )
    }

    // Shopee Food Driver Specific Dialog (Matches Form Design)
    if (showShopeeFoodDialog) {
        ShopeeFoodDriverDialog(
            onDismiss = { showShopeeFoodDialog = false },
            onConfirm = { type, title, amount, category, wallet, note ->
                viewModel.addTransaction(
                    type = type,
                    title = title,
                    amount = amount,
                    categoryName = category,
                    wallet = wallet,
                    note = note
                )
                Toast.makeText(context, "Pendapatan Shopee Food berhasil disimpan!", Toast.LENGTH_SHORT).show()
                showShopeeFoodDialog = false
            }
        )
    }

    // Add Asset Dialog
    if (showAddAssetDialog) {
        AddAssetDialog(
            onDismiss = { showAddAssetDialog = false },
            onConfirm = { name, assetType, units, unitLabel, buyPrice, currentPrice, platform, note ->
                viewModel.addAsset(
                    name = name,
                    assetType = assetType,
                    units = units,
                    unitLabel = unitLabel,
                    avgBuyPrice = buyPrice,
                    currentPrice = currentPrice,
                    platform = platform,
                    note = note
                )
                Toast.makeText(context, "Aset investasi baru berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add Reminder Dialog
    if (showAddReminderDialog) {
        AddReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onConfirm = { title, amount, dueDate, frequency, paymentAccount, note ->
                viewModel.addReminder(
                    title = title,
                    amount = amount,
                    dueDateEpochMillis = dueDate,
                    frequency = frequency,
                    paymentAccount = paymentAccount,
                    note = note
                )
                Toast.makeText(context, "Pengingat pembayaran tersimpan!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Export Report Dialog
    if (showExportDialog) {
        ExportReportDialog(
            monthName = exportMonthTarget.ifEmpty { DateUtils.formatMonthYear(System.currentTimeMillis()) },
            onDismiss = { showExportDialog = false },
            onExport = { isPdf ->
                val monthName = exportMonthTarget.ifEmpty { DateUtils.formatMonthYear(System.currentTimeMillis()) }
                try {
                    val file = viewModel.exportReport(context, isPdf = isPdf, monthName = monthName)
                    val mimeType = if (isPdf) "application/pdf" else "text/csv"
                    val intent = ReportExporter.createShareIntent(context, file, mimeType)
                    val chooser = Intent.createChooser(intent, "Bagikan Laporan FinVesta ($monthName)").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal ekspor laporan: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Pilih Tema Tampilan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        ThemeOptionItem(AppThemeMode.SYSTEM, "Sistem (Otomatis)", "Mengikuti mode terang/gelap sistem HP Anda", Icons.Filled.BrightnessAuto),
                        ThemeOptionItem(AppThemeMode.LIGHT, "Mode Terang (Light)", "Latar belakang putih bersih, nyaman di siang hari", Icons.Filled.LightMode),
                        ThemeOptionItem(AppThemeMode.DARK, "Mode Gelap (Dark)", "Latar belakang gelap pekat, hemat daya & nyaman di malam hari", Icons.Filled.DarkMode)
                    ).forEach { (mode, title, desc, icon) ->
                        val isSelected = appThemeMode == mode
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.setAppThemeMode(mode)
                                    showThemeDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setAppThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// Kept for test compatibility (e.g. GreetingScreenshotTest)
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("FinVesta") }
}

private data class ThemeOptionItem(
    val mode: AppThemeMode,
    val title: String,
    val desc: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

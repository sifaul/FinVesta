package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Standard Navigation Views for FinVesta Base Scaffold.
 * Specifically includes Transactions, Investments, and Reports views as required.
 */
enum class FinVestaView(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Beranda", Icons.Filled.Home, Icons.Outlined.Home, "bottom_nav_dashboard"),
    TRANSACTIONS("Transaksi", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "bottom_nav_transactions"),
    INVESTMENTS("Investasi", Icons.AutoMirrored.Filled.ShowChart, Icons.AutoMirrored.Outlined.ShowChart, "bottom_nav_investments"),
    REPORTS("Laporan", Icons.Filled.Assessment, Icons.Outlined.Assessment, "bottom_nav_reports")
}

/**
 * Base Compose Scaffold for FinVesta that includes a BottomNavigationBar
 * for switching smoothly between Transactions, Investments, and Reports views.
 *
 * @param currentView The currently active navigation view.
 * @param onViewSelected Callback when a bottom navigation item is selected.
 * @param modifier Optional modifier for the scaffold.
 * @param topBar Optional TopAppBar composable slot.
 * @param floatingActionButton Optional FloatingActionButton composable slot.
 * @param availableViews The list of views displayed in the bottom navigation bar.
 *                       Defaults to [FinVestaView.TRANSACTIONS], [FinVestaView.INVESTMENTS], and [FinVestaView.REPORTS].
 * @param content The main screen content lambda receiving Scaffold [PaddingValues].
 */
@Composable
fun FinVestaBaseScaffold(
    currentView: FinVestaView,
    onViewSelected: (FinVestaView) -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    availableViews: List<FinVestaView> = listOf(
        FinVestaView.DASHBOARD,
        FinVestaView.TRANSACTIONS,
        FinVestaView.INVESTMENTS,
        FinVestaView.REPORTS
    ),
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        floatingActionButton = floatingActionButton,
        bottomBar = {
            FinVestaBottomNavigationBar(
                currentView = currentView,
                availableViews = availableViews,
                onViewSelected = onViewSelected
            )
        },
        content = content
    )
}

/**
 * Bottom Navigation Bar specifically crafted for FinVesta with Material Design 3 guidelines.
 */
@Composable
fun FinVestaBottomNavigationBar(
    currentView: FinVestaView,
    availableViews: List<FinVestaView>,
    onViewSelected: (FinVestaView) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("finvesta_bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        availableViews.forEach { view ->
            val isSelected = currentView == view
            NavigationBarItem(
                selected = isSelected,
                onClick = { onViewSelected(view) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) view.selectedIcon else view.unselectedIcon,
                        contentDescription = view.title
                    )
                },
                label = {
                    Text(
                        text = view.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag(view.testTag)
            )
        }
    }
}

/**
 * Animated Content Host helper to animate view switching inside [FinVestaBaseScaffold].
 */
@Composable
fun FinVestaAnimatedContent(
    currentView: FinVestaView,
    modifier: Modifier = Modifier,
    content: @Composable (FinVestaView) -> Unit
) {
    AnimatedContent(
        targetState = currentView,
        transitionSpec = {
            val direction = targetState.ordinal > initialState.ordinal
            if (direction) {
                (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it / 6 } +
                    fadeIn(animationSpec = tween(280)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { -it / 6 } +
                            fadeOut(animationSpec = tween(200))
                    )
            } else {
                (slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it / 6 } +
                    fadeIn(animationSpec = tween(280)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { it / 6 } +
                            fadeOut(animationSpec = tween(200))
                    )
            }
        },
        label = "FinVestaViewTransition",
        modifier = modifier.fillMaxSize()
    ) { targetView ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(targetView)
        }
    }
}

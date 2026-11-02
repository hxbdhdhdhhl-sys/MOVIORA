package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.MovioraRepository
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.i18n.LocalMovioraStrings
import com.example.ui.screens.*
import com.example.ui.theme.MovioraGold
import com.example.ui.theme.MovioraObsidianBg
import com.example.ui.theme.MovioraTheme
import com.example.ui.viewmodel.MovioraViewModel
import com.example.ui.viewmodel.OverlayScreen
import com.example.ui.viewmodel.PrimaryTab
import com.example.util.NetworkAndNotificationHelper

private data class NavDestinationItem(
    val tab: PrimaryTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NetworkAndNotificationHelper.ensureNotificationChannel(this)
        setContent {
            MovioraRootApp()
        }
    }
}

@Composable
fun MovioraRootApp() {
    MovioraAppShell(userId = MovioraRepository.DEFAULT_LOCAL_USER_ID)
}

@Composable
private fun MovioraAppShell(
    userId: String
) {
    val context = LocalContext.current
    val viewModel: MovioraViewModel = viewModel(
        key = "moviora_vm_$userId",
        factory = MovioraViewModel.factory(context, userId)
    )

    val clientSettings by viewModel.clientSettings.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val overlayStack by viewModel.overlayStack.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            viewModel.clearToast()
        }
    }

    BackHandler(enabled = overlayStack.isNotEmpty() || selectedTab != PrimaryTab.HOME) {
        viewModel.popOverlay()
    }

    MovioraTheme(
        themeMode = clientSettings.themeMode,
        languageCode = clientSettings.languageCode,
        isVipActive = subscription?.isCurrentlyActive == true
    ) {
        val strings = LocalMovioraStrings.current
        val topOverlay = overlayStack.lastOrNull()

        if (topOverlay is OverlayScreen.Player) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MovioraObsidianBg)
            ) {
                MovioraPlayerScreen(
                    mediaId = topOverlay.mediaId,
                    initialEpisodeId = topOverlay.episodeId,
                    viewModel = viewModel,
                    onClosePlayer = { viewModel.popOverlay() }
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                )
            }
            return@MovioraTheme
        }

        val destinations = remember(strings, isAdmin) {
            buildList {
                add(
                    NavDestinationItem(
                        tab = PrimaryTab.HOME,
                        label = strings.navHome,
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home,
                        testTag = "nav_tab_home"
                    )
                )
                add(
                    NavDestinationItem(
                        tab = PrimaryTab.LIBRARY,
                        label = strings.navLibrary,
                        selectedIcon = Icons.Filled.VideoLibrary,
                        unselectedIcon = Icons.Outlined.VideoLibrary,
                        testTag = "nav_tab_library"
                    )
                )
                add(
                    NavDestinationItem(
                        tab = PrimaryTab.ARCHIVE,
                        label = strings.navHistory,
                        selectedIcon = Icons.Filled.AccountBalance,
                        unselectedIcon = Icons.Outlined.AccountBalance,
                        testTag = "nav_tab_archive"
                    )
                )
                add(
                    NavDestinationItem(
                        tab = PrimaryTab.SAVED,
                        label = strings.navSaved,
                        selectedIcon = Icons.Filled.Bookmarks,
                        unselectedIcon = Icons.Outlined.Bookmarks,
                        testTag = "nav_tab_saved"
                    )
                )
                add(
                    NavDestinationItem(
                        tab = PrimaryTab.VIP,
                        label = strings.navVip,
                        selectedIcon = Icons.Filled.WorkspacePremium,
                        unselectedIcon = Icons.Outlined.WorkspacePremium,
                        testTag = "nav_tab_vip"
                    )
                )
                add(
                    NavDestinationItem(
                        tab = PrimaryTab.PROFILE,
                        label = strings.navProfile,
                        selectedIcon = Icons.Filled.Person,
                        unselectedIcon = Icons.Outlined.Person,
                        testTag = "nav_tab_profile"
                    )
                )
                if (isAdmin) {
                    add(
                        NavDestinationItem(
                            tab = PrimaryTab.ADMIN,
                            label = strings.navAdmin,
                            selectedIcon = Icons.Filled.AdminPanelSettings,
                            unselectedIcon = Icons.Outlined.AdminPanelSettings,
                            testTag = "nav_tab_admin"
                        )
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isWideScreen = maxWidth >= 640.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!isWideScreen && topOverlay == null) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                            tonalElevation = 6.dp,
                            modifier = Modifier.testTag("moviora_bottom_nav")
                        ) {
                            val compactItems = if (destinations.size > 6) {
                                destinations.filter { it.tab != PrimaryTab.ADMIN }
                            } else {
                                destinations
                            }
                            compactItems.forEach { dest ->
                                val isSelected = selectedTab == dest.tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(dest.tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    alwaysShowLabel = true,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            header = {
                                Text(
                                    text = "MOVIORA",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MovioraGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .testTag("moviora_nav_rail")
                        ) {
                            Spacer(Modifier.height(8.dp))
                            destinations.forEach { dest ->
                                val isSelected = topOverlay == null && selectedTab == dest.tab
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(dest.tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("${dest.testTag}_rail")
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        OfflineStatusBanner(isConnected = networkStatus.isConnected)

                        // Compact-width Admin quick bar for verified owner/admins
                        if (isAdmin && !isWideScreen && topOverlay == null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AdminPanelSettings,
                                            contentDescription = strings.navAdmin,
                                            tint = MovioraGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "صلاحية المدير مفعّلة",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.selectTab(
                                                if (selectedTab == PrimaryTab.ADMIN) PrimaryTab.HOME else PrimaryTab.ADMIN
                                            )
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("compact_admin_toggle_button")
                                    ) {
                                        Text(
                                            text = if (selectedTab == PrimaryTab.ADMIN) strings.navHome else strings.navAdmin,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (val overlay = topOverlay) {
                                is OverlayScreen.MediaDetail -> {
                                    MediaDetailScreen(
                                        mediaId = overlay.mediaId,
                                        viewModel = viewModel,
                                        onBack = { viewModel.popOverlay() }
                                    )
                                }
                                is OverlayScreen.PersonDetail -> {
                                    PersonDetailScreen(
                                        personId = overlay.personId,
                                        fallbackName = overlay.fallbackName,
                                        viewModel = viewModel,
                                        onBack = { viewModel.popOverlay() }
                                    )
                                }
                                is OverlayScreen.Player -> Unit
                                null -> {
                                    AnimatedContent(
                                        targetState = selectedTab,
                                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                                        label = "moviora_tab_transition"
                                    ) { tab ->
                                        when (tab) {
                                            PrimaryTab.HOME -> HomeScreen(viewModel = viewModel)
                                            PrimaryTab.LIBRARY -> DiscoverAndSearchScreen(viewModel = viewModel)
                                            PrimaryTab.ARCHIVE -> HistoryArchiveScreen(viewModel = viewModel)
                                            PrimaryTab.SAVED -> SavedAndDownloadsScreen(viewModel = viewModel)
                                            PrimaryTab.VIP -> VipSubscriptionScreen(viewModel = viewModel)
                                            PrimaryTab.PROFILE -> ProfileAndSettingsScreen(
                                                viewModel = viewModel
                                            )
                                            PrimaryTab.ADMIN -> AdminDashboardScreen(viewModel = viewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

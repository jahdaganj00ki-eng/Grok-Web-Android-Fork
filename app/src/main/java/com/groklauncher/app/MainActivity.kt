package com.groklauncher.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.groklauncher.app.about.AboutActivity
import com.groklauncher.app.core.CustomTabsLauncher
import com.groklauncher.app.core.NetworkState
import com.groklauncher.app.core.ShareUtils
import com.groklauncher.app.ui.theme.GrokLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            GrokLauncherTheme {
                MainScreen(
                    onOpen = { openGrok() },
                    onOpenInBrowser = { openInBrowser() },
                    onShare = { share() },
                    onAbout = { openAbout() },
                    onOpenWebView = { openWebView() },
                )
            }
        }
    }

    private fun grokUri(): Uri = Uri.parse(getString(R.string.grok_url))

    private fun openGrok() {
        CustomTabsLauncher.launch(this, grokUri())
    }

    private fun openInBrowser() {
        ShareUtils.openInBrowser(this, grokUri())
    }

    private fun share() {
        ShareUtils.shareText(this, getString(R.string.app_name), getString(R.string.grok_url))
    }

    private fun openAbout() {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    private fun openWebView() {
        startActivity(Intent(this, WebViewActivity::class.java))
    }
}

private enum class UiState { OPENING, READY, OFFLINE, ERROR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    onOpen: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onShare: () -> Unit,
    onAbout: () -> Unit,
    onOpenWebView: () -> Unit,
) {
    val context = LocalContext.current
    val canCustomTabs = remember { CustomTabsLauncher.canLaunchCustomTabs(context) }

    var menuOpen by remember { mutableStateOf(false) }
    var openedOnce by rememberSaveable { mutableStateOf(false) }
    var uiState by rememberSaveable { mutableStateOf(UiState.OPENING) }

    LaunchedEffect(Unit) {
        val online = NetworkState.isOnline(context)
        uiState = if (!online) UiState.OFFLINE else UiState.OPENING

        if (!online) return@LaunchedEffect
        if (!canCustomTabs) {
            uiState = UiState.READY
            return@LaunchedEffect
        }
        if (openedOnce) {
            uiState = UiState.READY
            return@LaunchedEffect
        }

        openedOnce = true
        try {
            onOpen()
            uiState = UiState.READY
        } catch (_: Throwable) {
            uiState = UiState.ERROR
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_open)) },
                            onClick = { menuOpen = false; onOpen() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_reload)) },
                            onClick = { menuOpen = false; onOpen() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_open_in_browser)) },
                            onClick = { menuOpen = false; onOpenInBrowser() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_share)) },
                            onClick = { menuOpen = false; onShare() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.menu_about)) },
                            onClick = { menuOpen = false; onAbout() },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Content(
            padding = padding,
            uiState = uiState,
            canCustomTabs = canCustomTabs,
            onRetry = {
                val online = NetworkState.isOnline(context)
                if (!online) {
                    uiState = UiState.OFFLINE
                } else {
                    uiState = UiState.OPENING
                    if (canCustomTabs) {
                        try {
                            onOpen()
                            uiState = UiState.READY
                        } catch (_: Throwable) {
                            uiState = UiState.ERROR
                        }
                    } else {
                        uiState = UiState.READY
                    }
                }
            },
            onOpenWebView = onOpenWebView,
            onOpenInBrowser = onOpenInBrowser,
        )
    }
}

@Composable
private fun Content(
    padding: PaddingValues,
    uiState: UiState,
    canCustomTabs: Boolean,
    onRetry: () -> Unit,
    onOpenWebView: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState) {
            UiState.OPENING -> {
                CircularProgressIndicator()
                Text(text = stringResource(id = R.string.state_opening), style = MaterialTheme.typography.titleMedium)
            }
            UiState.READY -> {
                Text(text = stringResource(id = R.string.state_ready), style = MaterialTheme.typography.titleMedium)
            }
            UiState.OFFLINE -> {
                Text(text = stringResource(id = R.string.state_offline_title), style = MaterialTheme.typography.titleLarge)
                Text(text = stringResource(id = R.string.state_offline_body))
            }
            UiState.ERROR -> {
                Text(text = stringResource(id = R.string.state_error_title), style = MaterialTheme.typography.titleLarge)
                Text(text = stringResource(id = R.string.state_error_body))
            }
        }

        Button(onClick = onRetry) {
            Text(text = stringResource(id = R.string.action_retry))
        }

        if (!canCustomTabs) {
            Button(onClick = onOpenWebView) {
                Text(text = stringResource(id = R.string.action_open_webview))
            }
        }

        Button(onClick = onOpenInBrowser) {
            Text(text = stringResource(id = R.string.menu_open_in_browser))
        }
    }
}

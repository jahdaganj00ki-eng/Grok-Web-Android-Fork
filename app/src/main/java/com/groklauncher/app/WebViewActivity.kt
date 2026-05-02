package com.groklauncher.app

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.viewinterop.AndroidView
import com.groklauncher.app.core.NetworkState
import com.groklauncher.app.core.ShareUtils
import com.groklauncher.app.ui.theme.GrokLauncherTheme

class WebViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrokLauncherTheme {
                WebViewScreen(onClose = { finish() })
            }
        }
    }
}

private enum class WebUiState { LOADING, READY, OFFLINE, ERROR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebViewScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val grokUri = remember { Uri.parse(context.getString(R.string.grok_url)) }

    var uiState by rememberSaveable { mutableStateOf(WebUiState.LOADING) }
    var reloadToken by rememberSaveable { mutableStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val online = NetworkState.isOnline(context)
    if (!online && uiState != WebUiState.OFFLINE) uiState = WebUiState.OFFLINE

    BackHandler(enabled = webViewRef?.canGoBack() == true) {
        webViewRef?.goBack()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = context.getString(R.string.app_name)) }) },
    ) { padding ->
        if (uiState == WebUiState.OFFLINE || uiState == WebUiState.ERROR) {
            ErrorOverlay(
                padding = padding,
                title = if (uiState == WebUiState.OFFLINE) stringResource(id = R.string.state_offline_title) else stringResource(id = R.string.state_error_title),
                body = if (uiState == WebUiState.OFFLINE) stringResource(id = R.string.state_offline_body) else stringResource(id = R.string.state_error_body),
                onRetry = {
                    if (NetworkState.isOnline(context)) {
                        uiState = WebUiState.LOADING
                        reloadToken = reloadToken + 1
                    } else {
                        uiState = WebUiState.OFFLINE
                    }
                },
                onOpenInBrowser = { ShareUtils.openInBrowser(context, grokUri) },
                onClose = onClose,
            )
        } else {
            WebViewContainer(
                padding = padding,
                url = grokUri.toString(),
                reloadToken = reloadToken,
                onWebViewCreated = { webViewRef = it },
                onLoading = { uiState = WebUiState.LOADING },
                onReady = { uiState = WebUiState.READY },
                onError = { uiState = if (NetworkState.isOnline(context)) WebUiState.ERROR else WebUiState.OFFLINE },
            )
        }
    }
}

@Composable
private fun WebViewContainer(
    padding: PaddingValues,
    url: String,
    reloadToken: Int,
    onWebViewCreated: (WebView) -> Unit,
    onLoading: () -> Unit,
    onReady: () -> Unit,
    onError: () -> Unit,
) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                configureWebView(this)
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        onLoading()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onReady()
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        onError()
                    }
                }
                onWebViewCreated(this)
                loadUrl(url)
            }
        },
        update = { webView ->
            val lastToken = webView.tag as? Int
            if (lastToken != reloadToken) {
                webView.tag = reloadToken
                if (reloadToken > 0) {
                    webView.reload()
                }
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
    )
}

@Composable
private fun ErrorOverlay(
    padding: PaddingValues,
    title: String,
    body: String,
    onRetry: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(text = body)
        Button(onClick = onRetry) { Text(text = stringResource(id = R.string.action_retry)) }
        Button(onClick = onOpenInBrowser) { Text(text = stringResource(id = R.string.menu_open_in_browser)) }
        Button(onClick = onClose) { Text(text = stringResource(id = R.string.action_close)) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(webView: WebView) {
    val settings: WebSettings = webView.settings
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.setSupportZoom(false)
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    webView.overScrollMode = WebView.OVER_SCROLL_NEVER
}

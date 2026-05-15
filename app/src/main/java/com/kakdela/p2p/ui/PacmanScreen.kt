package com.kakdela.p2p.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PacmanScreen(
    navController: NavHostController? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            // Уничтожаем WebView при выходе
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
            webView = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // WebView на весь экран
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false

                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()

                    loadUrl("file:///android_asset/pacman/index.html")
                    webView = this
                }
            },
            update = { }
        )

        // Кнопка выхода поверх WebView, но не перекрывает игру
        TextButton(
            onClick = {
                webView?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    removeAllViews()
                    destroy()
                }
                webView = null
                if (navController != null) {
                    navController.popBackStack()
                } else {
                    (context as? android.app.Activity)?.finish()
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .height(32.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF00FFFF)
            )
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Выйти",
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF00FFFF)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Выйти",
                color = Color(0xFF00FFFF),
                fontSize = 13.sp
            )
        }
    }
}

package com.kakdela.p2p.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.kakdela.p2p.network.CookieStore
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Продакшн-браузер с динамическим переключением Mobile/Desktop версий.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String, title: String, navController: NavHostController) {
    
    val decodedUrl = remember(url) {
        try {
            URLDecoder.decode(url, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            url
        }
    }

    // Определяем, является ли сайт Тик-Током
    val isTikTok = remember(decodedUrl) { 
        decodedUrl.contains("tiktok.com", ignoreCase = true) 
    }
    
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Константы User Agent
    val desktopAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
    val mobileAgent = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"

    BackHandler(enabled = true) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            navController.popBackStack()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    // Базовые настройки для работы современных сайтов (Ozon, WB)
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    
                    // Настройки адаптивности
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    
                    // Смешанный контент (нужно для некоторых старых сайтов)
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    // Динамическая настройка User Agent
                    userAgentString = if (isTikTok) desktopAgent else mobileAgent
                }

                // Работа с куками (важно для авторизации на Госуслугах и маркетплейсах)
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val targetUrl = request?.url.toString()

                        // 1. Обработка внешних приложений (звонки, почта, карты)
                        if (targetUrl.startsWith("tel:") ||
                            targetUrl.startsWith("mailto:") ||
                            targetUrl.startsWith("geo:") ||
                            targetUrl.startsWith("intent:") // Добавлено для системных интентов
                        ) {
                            try {
                                val intent = Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Если интент не парсится, пробуем просто открыть как URI
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                                } catch (e2: Exception) {
                                    Log.e("WebView", "Ошибка открытия: $targetUrl")
                                }
                            }
                            return true
                        }

                        // 2. Блокировка редиректов TikTok в Play Store, чтобы не вылетало из браузера
                        if (isTikTok && (targetUrl.contains("play.google.com") || targetUrl.contains("app-ad"))) {
                            return true
                        }

                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        // 3. Специфическая логика для InfinityFree (твоя защита)
                        if (url != null && url.contains("infinityfree.me")) {
                            CookieStore.updateFromWebView(context, url)
                            if (url.contains("api.php")) {
                                navController.popBackStack()
                            }
                        }

                        // 4. Инъекция скрипта для TikTok (подавление попыток открыть приложение)
                        if (isTikTok) {
                            view?.evaluateJavascript(
                                "(function() { " +
                                "window.open = function() { return null; }; " +
                                "document.querySelectorAll('.download-button').forEach(e => e.style.display = 'none');" +
                                "})();",
                                null
                            )
                        }
                    }
                }

                webChromeClient = WebChromeClient()
                
                loadUrl(decodedUrl)
                webViewInstance = this
            }
        },
        update = { webView ->
            // При обновлении Composable проверяем, не изменился ли тип ссылки
            val currentAgent = if (isTikTok) desktopAgent else mobileAgent
            if (webView.settings.userAgentString != currentAgent) {
                webView.settings.userAgentString = currentAgent
            }
        },
        onRelease = {
            it.stopLoading()
            it.removeAllViews()
            it.destroy()
        }
    )
}

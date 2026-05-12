package com.kakdela.p2p.ui.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class BrowserActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var urlEditText: EditText
    private lateinit var progressBar: ProgressBar

    // Навигация (теперь только снизу для удобства)
    private lateinit var bottomBack: TextView
    private lateinit var bottomForward: TextView
    private lateinit var bottomRefresh: TextView
    private lateinit var bottomHome: TextView

    // Overlay
    private lateinit var offlineOverlay: View
    private lateinit var networkIndicator: TextView

    // Неоновая палитра
    private val colorBg = Color.parseColor("#09090B") // Глубокий черный
    private val colorSurface = Color.parseColor("#18181B") // Темно-серый
    private val colorCyan = Color.parseColor("#00FFFF") // Яркий циан
    private val colorPink = Color.parseColor("#FF00FF") // Яркий маджента
    private val colorHint = Color.parseColor("#66FFFFFF")

    private val homeUrl = "https://www.google.com"

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private var lastUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Включаем Edge-to-Edge, чтобы приложение рисовало фон под системными панелями
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBg)
        }

        // 2. Обрабатываем Insets (отступы системных панелей Android)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Добавляем отступы, чтобы наш UI не залезал под камеру или нижнюю полоску навигации
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // ───── TOP BAR (Адресная строка) ─────
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(colorSurface)
        }

        urlEditText = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(12)
            }
            hint = "Введите URL или запрос..."
            setTextColor(Color.WHITE)
            setHintTextColor(colorHint)
            textSize = 16f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            // Неоновая обводка для поля ввода
            background = bg(Color.parseColor("#27272A"), 24f, 2, colorCyan)
            setPadding(dp(20), 0, dp(20), 0)

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    processUrlInput()
                    true
                } else false
            }
        }

        val go = neonButton("➜", colorPink) { processUrlInput() }

        topBar.addView(urlEditText)
        topBar.addView(go)

        // ───── NETWORK INDICATOR ─────
        networkIndicator = TextView(this).apply {
            textSize = 10f
            setPadding(dp(16), dp(2), dp(16), dp(2))
            setTextColor(colorCyan)
            setBackgroundColor(colorSurface)
            text = "● Online"
            setShadowLayer(8f, 0f, 0f, colorCyan) // Эффект неонового свечения
        }

        // ───── PROGRESS ─────
        progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3))
            max = 100
            progressTintList = ColorStateList.valueOf(colorPink)
            visibility = View.GONE
        }

        // ───── WEB + OVERLAY ─────
        val webContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        webView = WebView(this).apply {
            setBackgroundColor(colorBg)
            id = ViewCompat.generateViewId()
        }

        offlineOverlay = offlineOverlay()

        webContainer.addView(webView)
        webContainer.addView(offlineOverlay)

        // ───── BOTTOM NAV (Панель управления) ─────
        val bottomNav = navBar(60)
        // Делаем кнопки крупными для удобного нажатия на смартфонах
        bottomBack = bigNav("❮", colorCyan)
        bottomForward = bigNav("❯", colorCyan)
        bottomRefresh = bigNav("↻", colorPink)
        bottomHome = bigNav("⌂", colorCyan)
        addNav(bottomNav, bottomBack, bottomForward, bottomRefresh, bottomHome)

        // ───── ASSEMBLY ─────
        root.addView(topBar)
        root.addView(progressBar)
        root.addView(networkIndicator)
        root.addView(webContainer)
        root.addView(bottomNav)

        setContentView(root)

        initWebView()
        bindNav()
        registerNetworkCallback()

        loadUrl(homeUrl)
    }

    // ─────────────────────────────────────────────
    // OFFLINE OVERLAY
    // ─────────────────────────────────────────────
    private fun offlineOverlay(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F009090B")) // Полупрозрачный фон
            visibility = View.GONE

            val title = TextView(context).apply {
                text = "Как дела?"
                textSize = 36f
                setTextColor(colorPink)
                setShadowLayer(15f, 0f, 0f, colorPink) // Свечение заголовка
                setPadding(0, 0, 0, dp(8))
            }

            val sub = TextView(context).apply {
                text = "Нет подключения к интернету"
                textSize = 16f
                setTextColor(colorCyan)
                setShadowLayer(10f, 0f, 0f, colorCyan)
                setPadding(0, 0, 0, dp(32))
            }

            val retry = neonButton("Повторить", colorCyan) {
                if (isOnline()) {
                    hideOverlay()
                    lastUrl?.let { webView.loadUrl(it) }
                }
            }

            val back = neonButton("Вернуться", colorPink) {
                hideOverlay()
                if (webView.canGoBack()) webView.goBack()
                else loadUrl(homeUrl)
            }.apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(16)
            }

            addView(title)
            addView(sub)
            addView(retry)
            addView(back)
        }

    private fun showOverlay() {
        if (offlineOverlay.visibility != View.VISIBLE) {
            offlineOverlay.visibility = View.VISIBLE
            offlineOverlay.startAnimation(
                AlphaAnimation(0f, 1f).apply { duration = 300 }
            )
        }
    }

    private fun hideOverlay() {
        offlineOverlay.visibility = View.GONE
    }

    // ─────────────────────────────────────────────
    // WEBVIEW
    // ─────────────────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            if (Build.VERSION.SDK_INT >= 29) {
                forceDark = WebSettings.FORCE_DARK_ON
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(v: WebView?, url: String?, f: Bitmap?) {
                lastUrl = url
                urlEditText.setText(url) // Обновляем строку поиска
                progressBar.visibility = View.VISIBLE
                updateNav()
            }

            override fun onPageFinished(v: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                hideOverlay()
                updateNav()
            }

            override fun onReceivedError(
                v: WebView?,
                r: WebResourceRequest?,
                e: WebResourceError?
            ) {
                progressBar.visibility = View.GONE
                if (!isOnline()) showOverlay()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(v: WebView?, p: Int) {
                progressBar.progress = p
            }
        }
    }

    // ─────────────────────────────────────────────
    // NETWORK
    // ─────────────────────────────────────────────
    private fun registerNetworkCallback() {
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    networkIndicator.text = "● Online"
                    networkIndicator.setTextColor(colorCyan)
                    networkIndicator.setShadowLayer(8f, 0f, 0f, colorCyan)
                    hideOverlay()
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    networkIndicator.text = "● Offline"
                    networkIndicator.setTextColor(colorPink)
                    networkIndicator.setShadowLayer(8f, 0f, 0f, colorPink)
                    showOverlay()
                }
            }
        }

        connectivityManager.registerNetworkCallback(req, networkCallback)
    }

    private fun isOnline(): Boolean {
        val net = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ─────────────────────────────────────────────
    // NAV / UTILS
    // ─────────────────────────────────────────────
    private fun processUrlInput() {
        val input = urlEditText.text.toString().trim()
        if (input.isEmpty()) return
        hideKeyboard()
        urlEditText.clearFocus()

        if (!isOnline()) {
            showOverlay()
            return
        }

        loadUrl(
            if (Patterns.WEB_URL.matcher(input).matches() || input.contains("."))
                input
            else "https://www.google.com/search?q=$input"
        )
    }

    private fun loadUrl(url: String) {
        val u = if (url.startsWith("http")) url else "https://$url"
        webView.loadUrl(u)
    }

    private fun bindNav() {
        bottomBack.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        bottomForward.setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        bottomRefresh.setOnClickListener { webView.reload() }
        bottomHome.setOnClickListener { loadUrl(homeUrl) }
    }

    private fun updateNav() {
        val b = webView.canGoBack()
        val f = webView.canGoForward()
        bottomBack.alpha = if (b) 1f else 0.3f
        bottomForward.alpha = if (f) 1f else 0.3f
    }

    override fun onKeyDown(keyCode: Int, e: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, e)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
        webView.destroy()
    }

    private fun hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(urlEditText.windowToken, 0)
    }

    // ─────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────
    private fun navBar(h: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setBackgroundColor(colorSurface)
        layoutParams = LinearLayout.LayoutParams(-1, dp(h))
    }

    private fun addNav(p: LinearLayout, vararg v: View) {
        val sp = LinearLayout.LayoutParams(0, 1, 1f)
        v.forEachIndexed { i, b ->
            p.addView(b)
            if (i != v.lastIndex) p.addView(View(this).apply { layoutParams = sp })
        }
    }

    private fun bigNav(t: String, c: Int) =
        TextView(this).apply {
            text = t
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(c)
            setShadowLayer(15f, 0f, 0f, c) // Неоновое свечение иконок
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
            background = ripple(c)
        }

    private fun neonButton(text: String, c: Int, onClick: () -> Unit) =
        Button(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            setShadowLayer(8f, 0f, 0f, Color.WHITE) // Легкое свечение текста кнопки
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(48)
            )
            background = bg(Color.TRANSPARENT, 24f, 3, c) // Прозрачный фон с яркой рамкой
            setOnClickListener { onClick() }
            // Добавляем ripple эффект на кнопку
            foreground = ripple(c)
        }

    private fun ripple(c: Int) =
        StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_pressed),
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.argb(80, Color.red(c), Color.green(c), Color.blue(c)))
                }
            )
        }

    private fun bg(c: Int, r: Float, s: Int, sc: Int) =
        GradientDrawable().apply {
            cornerRadius = dp(r.toInt()).toFloat()
            setColor(c)
            if (s > 0) setStroke(s, sc)
        }

    private fun dp(v: Int) =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            v.toFloat(),
            resources.displayMetrics
        ).toInt()
}
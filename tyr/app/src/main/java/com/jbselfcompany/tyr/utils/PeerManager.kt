package com.jbselfcompany.tyr.utils

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * Автоматический менеджер Yggdrasil-пиров с проверкой доступности,
 * загрузкой из внешних источников и fallback-логикой.
 */
object PeerManager {

    private const val TAG = "PeerManager"
    private const val CONNECT_TIMEOUT_MS = 3000
    private const val PEER_CHECK_INTERVAL_MS = 60_000L
    private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 часа

    // Локальный стартовый набор (10 пиров)
    private val LOCAL_PEERS = listOf(
        "tls://yggno.de:18227",
        "tls://longseason.1200bps.xyz:13121",
        "tls://188.225.9.167:18227",
        "tls://62.113.203.46:18227",
        "tls://45.147.200.202:6010",
        "tls://ygg-msk-1.averyan.ru:8363",
        "quic://ygg-msk-1.averyan.ru:8364",
        "tls://yggdrasil.ecliptik.com:443",
        "tls://router.dev.ygg:443",
        "tls://ygg.peer.rocks:443"
    )

    // Внешние источники (региональные + глобальные)
    private val EXTERNAL_SOURCES = listOf(
        "https://raw.githubusercontent.com/yggdrasil-network/public-peers/master/europe/russia.md",
        "https://raw.githubusercontent.com/yggdrasil-network/public-peers/master/other/europe.md",
        "https://raw.githubusercontent.com/yggdrasil-network/public-peers/master/global.md"
    )

    // Trust-all SSL context для проверки пиров
    private val trustAllSslContext: SSLContext by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }
    }

    // Кэш рабочих пиров
    @Volatile
    private var cachedWorkingPeers: List<String>? = null

    @Volatile
    private var cacheTimestamp: Long = 0

    @Volatile
    var bestPeer: String? = null
        private set

    @Volatile
    var isChecking = false
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Callback для UI
    interface PeerCheckCallback {
        fun onStatusUpdate(message: String)
        fun onPeerFound(peer: String, latencyMs: Long)
        fun onAllPeersFailed()
    }

    /**
     * Найти рабочий пир (основной метод)
     */
    fun findWorkingPeer(callback: PeerCheckCallback) {
        if (isChecking) {
            TyrLogger.w(TAG, "Already checking peers")
            return
        }

        isChecking = true
        scope.launch {
            try {
                // 1. Проверить кэш
                callback.onStatusUpdate("Проверка кэшированных пиров...")
                if (tryCache(callback)) return@launch

                // 2. Проверить локальный список
                callback.onStatusUpdate("Проверка локальных пиров (0/10)...")
                if (tryLocalPeers(callback)) return@launch

                // 3. Загрузить из внешних источников
                callback.onStatusUpdate("Загрузка пиров из GitHub...")
                if (tryExternalSources(callback)) return@launch

                // 4. Всё не работает
                callback.onAllPeersFailed()
                TyrLogger.e(TAG, "All peers failed!")

            } catch (e: Exception) {
                TyrLogger.e(TAG, "Error finding peer", e)
                callback.onAllPeersFailed()
            } finally {
                isChecking = false
            }
        }
    }

    private suspend fun tryCache(callback: PeerCheckCallback): Boolean {
        val cached = cachedWorkingPeers
        if (cached != null && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS) {
            TyrLogger.i(TAG, "Trying cached peers (${cached.size})")
            callback.onStatusUpdate("Проверка ${cached.size} кэшированных пиров...")

            var index = 0
            for (peer in cached) {
                index++
                callback.onStatusUpdate("Проверка кэш-пира $index/${cached.size}: $peer")
                val latency = checkPeer(peer)
                if (latency > 0) {
                    bestPeer = peer
                    callback.onPeerFound(peer, latency)
                    return true
                }
            }
        }
        return false
    }

    private suspend fun tryLocalPeers(callback: PeerCheckCallback): Boolean {
        TyrLogger.i(TAG, "Trying local peers (${LOCAL_PEERS.size})")
        val workingPeers = mutableListOf<String>()

        var index = 0
        for (peer in LOCAL_PEERS) {
            index++
            callback.onStatusUpdate("Проверка пира $index/${LOCAL_PEERS.size}: $peer")
            TyrLogger.d(TAG, "Testing peer $index/${LOCAL_PEERS.size}: $peer")

            val latency = checkPeer(peer)
            if (latency > 0) {
                TyrLogger.i(TAG, "Peer available: $peer (${latency}ms)")
                workingPeers.add(peer)
                bestPeer = peer
                cachePeers(workingPeers)
                callback.onPeerFound(peer, latency)
                return true
            } else {
                TyrLogger.d(TAG, "Peer unavailable: $peer")
            }
        }

        // Сохраняем всех рабочих
        if (workingPeers.isNotEmpty()) {
            cachePeers(workingPeers)
            bestPeer = workingPeers.first()
            callback.onPeerFound(bestPeer!!, 0)
            return true
        }
        return false
    }

    private suspend fun tryExternalSources(callback: PeerCheckCallback): Boolean {
        for ((srcIndex, source) in EXTERNAL_SOURCES.withIndex()) {
            callback.onStatusUpdate("Загрузка из источника ${srcIndex + 1}/${EXTERNAL_SOURCES.size}...")
            TyrLogger.i(TAG, "Fetching peers from: $source")

            val peers = fetchPeersFromUrl(source)
            if (peers.isEmpty()) {
                TyrLogger.w(TAG, "No peers found in: $source")
                continue
            }

            TyrLogger.i(TAG, "Got ${peers.size} peers from $source")
            callback.onStatusUpdate("Проверка ${peers.size} пиров из источника ${srcIndex + 1}...")

            val workingPeers = mutableListOf<String>()
            var index = 0
            for (peer in peers) {
                index++
                if (index % 5 == 0) {
                    callback.onStatusUpdate("Проверено $index/${peers.size} пиров...")
                }

                val latency = checkPeer(peer)
                if (latency > 0) {
                    TyrLogger.i(TAG, "External peer available: $peer (${latency}ms)")
                    workingPeers.add(peer)
                    bestPeer = peer
                    cachePeers(workingPeers)
                    callback.onPeerFound(peer, latency)
                    return true
                }
            }

            if (workingPeers.isNotEmpty()) {
                cachePeers(workingPeers)
                bestPeer = workingPeers.first()
                callback.onPeerFound(bestPeer!!, 0)
                return true
            }
        }
        return false
    }

    /**
     * Проверить доступность пира (TCP connect)
     * @return latency in ms, or -1 if unavailable
     */
    private fun checkPeer(peerUri: String): Long {
        return try {
            val uri = parsePeerUri(peerUri) ?: return -1
            val startTime = System.currentTimeMillis()

            val socket = Socket()
            socket.connect(InetSocketAddress(uri.host, uri.port), CONNECT_TIMEOUT_MS)

            val latency = System.currentTimeMillis() - startTime

            // Для TLS проверяем рукопожатие с trust-all контекстом
            if (uri.scheme == "tls") {
                try {
                    val sslSocket = trustAllSslContext.socketFactory.createSocket(
                        socket, uri.host, uri.port, true
                    ) as javax.net.ssl.SSLSocket
                    sslSocket.startHandshake()
                    sslSocket.close()
                } catch (e: Exception) {
                    // TLS handshake failed, but TCP connected — peer may use QUIC or plain TCP
                    TyrLogger.d(TAG, "TLS handshake failed for $peerUri, but TCP OK: ${e.message}")
                }
            }

            try { socket.close() } catch (_: Exception) {}
            latency
        } catch (e: Exception) {
            TyrLogger.d(TAG, "Peer check failed for $peerUri: ${e.message}")
            -1
        }
    }

    /**
     * Распарсить URI пира
     */
    private fun parsePeerUri(uri: String): PeerUri? {
        return try {
            val withoutScheme = uri.substringAfter("://")
            val host = if (withoutScheme.startsWith("[")) {
                withoutScheme.substringAfter("[").substringBefore("]")
            } else {
                withoutScheme.substringBefore(":")
            }
            val port = uri.substringAfterLast(":").toIntOrNull() ?: return null
            val scheme = uri.substringBefore("://")
            PeerUri(scheme, host, port)
        } catch (e: Exception) {
            null
        }
    }

    private data class PeerUri(val scheme: String, val host: String, val port: Int)

    /**
     * Загрузить пиры из внешнего URL (GitHub raw)
     */
    private fun fetchPeersFromUrl(url: String): List<String> {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val content = reader.readText()
            reader.close()
            connection.disconnect()

            parsePeersFromMarkdown(content)
        } catch (e: Exception) {
            TyrLogger.e(TAG, "Error fetching peers from $url: ${e.message}")
            emptyList()
        }
    }

    /**
     * Извлечь пиры из Markdown (GitHub public-peers формат)
     */
    private fun parsePeersFromMarkdown(markdown: String): List<String> {
        val peers = mutableListOf<String>()
        val regex = Regex("`(tls|tcp|quic)://[^`]+`")
        val matches = regex.findAll(markdown)
        for (match in matches) {
            val peer = match.value.removeSurrounding("`")
            peers.add(peer)
        }
        return peers
    }

    /**
     * Сохранить рабочие пиры в кэш
     */
    private fun cachePeers(peers: List<String>) {
        cachedWorkingPeers = peers.toList()
        cacheTimestamp = System.currentTimeMillis()
        TyrLogger.i(TAG, "Cached ${peers.size} working peers")
    }

    /**
     * Запустить периодическую проверку активного пира
     */
    fun startPeriodicCheck(callback: PeerCheckCallback) {
        scope.launch {
            while (isActive) {
                delay(PEER_CHECK_INTERVAL_MS)
                bestPeer?.let { peer ->
                    TyrLogger.d(TAG, "Periodic check: $peer")
                    val latency = checkPeer(peer)
                    if (latency < 0) {
                        TyrLogger.w(TAG, "Active peer lost: $peer")
                        bestPeer = null
                        findWorkingPeer(callback)
                    }
                }
            }
        }
    }

    /**
     * Остановить все проверки
     */
    fun stop() {
        scope.cancel()
        isChecking = false
    }
}

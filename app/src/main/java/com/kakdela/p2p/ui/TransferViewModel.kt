package com.kakdela.p2p.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class TransferState {
    object Idle : TransferState()
    object StartingServer : TransferState()
    data class ServerReady(val url: String, val qrCode: Bitmap) : TransferState()
}

class TransferViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<TransferState>(TransferState.Idle)
    val uiState: StateFlow<TransferState> = _uiState

    fun onServerStarted(url: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val qrBitmap = generateQrCode(url)
            _uiState.value = TransferState.ServerReady(url, qrBitmap)
        }
    }

    fun setStartingStatus() {
        _uiState.value = TransferState.StartingServer
    }

    fun resetState() {
        _uiState.value = TransferState.Idle
    }

    private fun generateQrCode(text: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}

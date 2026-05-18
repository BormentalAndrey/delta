// app/src/main/java/com/kakdela/p2p/model/Color.kt
package com.kakdela.p2p.model

enum class Color {
    WHITE, BLACK;
    
    fun opposite(): Color = if (this == WHITE) BLACK else WHITE
}

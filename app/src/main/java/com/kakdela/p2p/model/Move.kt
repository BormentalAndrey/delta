// app/src/main/java/com/kakdela/p2p/model/Move.kt
package com.kakdela.p2p.model

data class Move(
    val from: Position,
    val to: Position,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val promotion: PieceType? = null,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false
)

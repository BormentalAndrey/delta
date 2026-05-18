// app/src/main/java/com/kakdela/p2p/model/Piece.kt
package com.kakdela.p2p.model

data class Piece(val type: PieceType, val color: Color) {
    val symbol: String
        get() = when (type) {
            PieceType.KING -> if (color == Color.WHITE) "♔" else "♚"
            PieceType.QUEEN -> if (color == Color.WHITE) "♕" else "♛"
            PieceType.ROOK -> if (color == Color.WHITE) "♖" else "♜"
            PieceType.BISHOP -> if (color == Color.WHITE) "♗" else "♝"
            PieceType.KNIGHT -> if (color == Color.WHITE) "♘" else "♞"
            PieceType.PAWN -> if (color == Color.WHITE) "♙" else "♟"
        }
}

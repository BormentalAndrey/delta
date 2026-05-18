// app/src/main/java/com/kakdela/p2p/model/ChessGame.kt
package com.kakdela.p2p.model

class ChessGame {
    private val board = Array(8) { Array<Piece?>(8) { null } }
    var currentPlayer: Color = Color.WHITE
        private set
    
    private val moveHistory = mutableListOf<Move>()
    private var enPassantTarget: Position? = null
    private var whiteKingSideCastling = true
    private var whiteQueenSideCastling = true
    private var blackKingSideCastling = true
    private var blackQueenSideCastling = true
    
    init {
        setupInitialPosition()
    }
    
    private fun setupInitialPosition() {
        // Расстановка фигур на начальные позиции
        // Белые фигуры
        board[0][0] = Piece(PieceType.ROOK, Color.WHITE)
        board[1][0] = Piece(PieceType.KNIGHT, Color.WHITE)
        board[2][0] = Piece(PieceType.BISHOP, Color.WHITE)
        board[3][0] = Piece(PieceType.QUEEN, Color.WHITE)
        board[4][0] = Piece(PieceType.KING, Color.WHITE)
        board[5][0] = Piece(PieceType.BISHOP, Color.WHITE)
        board[6][0] = Piece(PieceType.KNIGHT, Color.WHITE)
        board[7][0] = Piece(PieceType.ROOK, Color.WHITE)
        
        for (col in 0..7) {
            board[col][1] = Piece(PieceType.PAWN, Color.WHITE)
        }
        
        // Черные фигуры
        board[0][7] = Piece(PieceType.ROOK, Color.BLACK)
        board[1][7] = Piece(PieceType.KNIGHT, Color.BLACK)
        board[2][7] = Piece(PieceType.BISHOP, Color.BLACK)
        board[3][7] = Piece(PieceType.QUEEN, Color.BLACK)
        board[4][7] = Piece(PieceType.KING, Color.BLACK)
        board[5][7] = Piece(PieceType.BISHOP, Color.BLACK)
        board[6][7] = Piece(PieceType.KNIGHT, Color.BLACK)
        board[7][7] = Piece(PieceType.ROOK, Color.BLACK)
        
        for (col in 0..7) {
            board[col][6] = Piece(PieceType.PAWN, Color.BLACK)
        }
    }
    
    fun getPieceAt(position: Position): Piece? {
        return board[position.col][position.row]
    }
    
    fun makeMove(from: Position, to: Position): Boolean {
        val piece = getPieceAt(from) ?: return false
        
        if (piece.color != currentPlayer) return false
        
        val legalMoves = getLegalMoves(from)
        if (to !in legalMoves) return false
        
        // Создаем запись хода
        val capturedPiece = getPieceAt(to)
        val move = Move(from, to, piece, capturedPiece)
        
        // Выполняем ход на доске
        board[to.col][to.row] = piece
        board[from.col][from.row] = null
        
        // Обработка специальных ходов
        // Взятие на проходе
        if (piece.type == PieceType.PAWN && to == enPassantTarget) {
            val captureRow = if (piece.color == Color.WHITE) to.row - 1 else to.row + 1
            board[to.col][captureRow] = null
        }
        
        // Обновление en passant цели
        enPassantTarget = if (piece.type == PieceType.PAWN && 
            kotlin.math.abs(to.row - from.row) == 2) {
            Position(to.col, (from.row + to.row) / 2)
        } else {
            null
        }
        
        // Рокировка
        if (piece.type == PieceType.KING && kotlin.math.abs(to.col - from.col) == 2) {
            if (to.col == 6) { // Короткая рокировка
                board[5][to.row] = board[7][to.row]
                board[7][to.row] = null
            } else { // Длинная рокировка
                board[3][to.row] = board[0][to.row]
                board[0][to.row] = null
            }
        }
        
        // Обновление прав на рокировку
        if (piece.type == PieceType.KING) {
            if (piece.color == Color.WHITE) {
                whiteKingSideCastling = false
                whiteQueenSideCastling = false
            } else {
                blackKingSideCastling = false
                blackQueenSideCastling = false
            }
        }
        
        if (piece.type == PieceType.ROOK) {
            when {
                from.col == 0 && from.row == 0 -> whiteQueenSideCastling = false
                from.col == 7 && from.row == 0 -> whiteKingSideCastling = false
                from.col == 0 && from.row == 7 -> blackQueenSideCastling = false
                from.col == 7 && from.row == 7 -> blackKingSideCastling = false
            }
        }
        
        // Превращение пешки
        if (piece.type == PieceType.PAWN && (to.row == 7 || to.row == 0)) {
            // По умолчанию превращаем в ферзя
            board[to.col][to.row] = Piece(PieceType.QUEEN, piece.color)
        }
        
        moveHistory.add(move)
        currentPlayer = currentPlayer.opposite()
        return true
    }
    
    fun getLegalMoves(from: Position): List<Position> {
        val piece = getPieceAt(from) ?: return emptyList()
        val moves = mutableListOf<Position>()
        
        when (piece.type) {
            PieceType.PAWN -> getPawnMoves(from, piece.color, moves)
            PieceType.KNIGHT -> getKnightMoves(from, piece.color, moves)
            PieceType.BISHOP -> getBishopMoves(from, piece.color, moves)
            PieceType.ROOK -> getRookMoves(from, piece.color, moves)
            PieceType.QUEEN -> getQueenMoves(from, piece.color, moves)
            PieceType.KING -> getKingMoves(from, piece.color, moves)
        }
        
        // Фильтруем ходы, которые оставляют короля под шахом
        return moves.filter { move ->
            !wouldBeInCheck(from, move, piece.color)
        }
    }
    
    private fun getPawnMoves(from: Position, color: Color, moves: MutableList<Position>) {
        val direction = if (color == Color.WHITE) 1 else -1
        val startRow = if (color == Color.WHITE) 1 else 6
        
        // Ход вперед на 1 клетку
        val oneForward = Position(from.col, from.row + direction)
        if (isInBounds(oneForward) && getPieceAt(oneForward) == null) {
            moves.add(oneForward)
            
            // Ход вперед на 2 клетки со стартовой позиции
            if (from.row == startRow) {
                val twoForward = Position(from.col, from.row + 2 * direction)
                if (getPieceAt(twoForward) == null) {
                    moves.add(twoForward)
                }
            }
        }
        
        // Взятие по диагонали
        for (dx in -1..1 step 2) {
            val capturePos = Position(from.col + dx, from.row + direction)
            if (isInBounds(capturePos)) {
                val targetPiece = getPieceAt(capturePos)
                if (targetPiece != null && targetPiece.color != color) {
                    moves.add(capturePos)
                }
                // Взятие на проходе
                if (capturePos == enPassantTarget) {
                    moves.add(capturePos)
                }
            }
        }
    }
    
    private fun getKnightMoves(from: Position, color: Color, moves: MutableList<Position>) {
        val knightMoves = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        
        for ((dx, dy) in knightMoves) {
            val pos = Position(from.col + dx, from.row + dy)
            if (isInBounds(pos)) {
                val targetPiece = getPieceAt(pos)
                if (targetPiece == null || targetPiece.color != color) {
                    moves.add(pos)
                }
            }
        }
    }
    
    private fun getBishopMoves(from: Position, color: Color, moves: MutableList<Position>) {
        for ((dx, dy) in listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)) {
            var x = from.col + dx
            var y = from.row + dy
            while (x in 0..7 && y in 0..7) {
                val pos = Position(x, y)
                val targetPiece = getPieceAt(pos)
                if (targetPiece == null) {
                    moves.add(pos)
                } else {
                    if (targetPiece.color != color) {
                        moves.add(pos)
                    }
                    break
                }
                x += dx
                y += dy
            }
        }
    }
    
    private fun getRookMoves(from: Position, color: Color, moves: MutableList<Position>) {
        for ((dx, dy) in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
            var x = from.col + dx
            var y = from.row + dy
            while (x in 0..7 && y in 0..7) {
                val pos = Position(x, y)
                val targetPiece = getPieceAt(pos)
                if (targetPiece == null) {
                    moves.add(pos)
                } else {
                    if (targetPiece.color != color) {
                        moves.add(pos)
                    }
                    break
                }
                x += dx
                y += dy
            }
        }
    }
    
    private fun getQueenMoves(from: Position, color: Color, moves: MutableList<Position>) {
        getBishopMoves(from, color, moves)
        getRookMoves(from, color, moves)
    }
    
    private fun getKingMoves(from: Position, color: Color, moves: MutableList<Position>) {
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val pos = Position(from.col + dx, from.row + dy)
                if (isInBounds(pos)) {
                    val targetPiece = getPieceAt(pos)
                    if (targetPiece == null || targetPiece.color != color) {
                        moves.add(pos)
                    }
                }
            }
        }
        
        // Рокировка
        if (color == Color.WHITE) {
            if (whiteKingSideCastling && canCastle(4, 7, 0)) {
                moves.add(Position(6, 0))
            }
            if (whiteQueenSideCastling && canCastle(4, 0, 0)) {
                moves.add(Position(2, 0))
            }
        } else {
            if (blackKingSideCastling && canCastle(4, 7, 7)) {
                moves.add(Position(6, 7))
            }
            if (blackQueenSideCastling && canCastle(4, 0, 7)) {
                moves.add(Position(2, 7))
            }
        }
    }
    
    private fun canCastle(kingCol: Int, rookCol: Int, row: Int): Boolean {
        val step = if (rookCol > kingCol) 1 else -1
        val between = if (step == 1) kingCol + 1..rookCol - 1 else rookCol + 1..kingCol - 1
        
        // Проверяем, что между королем и ладьей пусто
        for (col in between) {
            if (getPieceAt(Position(col, row)) != null) return false
        }
        
        // Проверяем, что король не проходит через битое поле
        val color = getPieceAt(Position(kingCol, row))?.color ?: return false
        for (col in kingCol..kingCol + 2 * step step step) {
            if (isSquareAttacked(Position(col, row), color)) return false
        }
        
        return true
    }
    
    private fun isSquareAttacked(position: Position, defenderColor: Color): Boolean {
        val attackerColor = defenderColor.opposite()
        
        for (col in 0..7) {
            for (row in 0..7) {
                val piece = getPieceAt(Position(col, row))
                if (piece != null && piece.color == attackerColor) {
                    val from = Position(col, row)
                    when (piece.type) {
                        PieceType.PAWN -> {
                            val direction = if (attackerColor == Color.WHITE) 1 else -1
                            for (dx in -1..1 step 2) {
                                if (position == Position(from.col + dx, from.row + direction)) {
                                    return true
                                }
                            }
                        }
                        PieceType.KNIGHT -> {
                            val moves = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 
                                             1 to -2, 1 to 2, 2 to -1, 2 to 1)
                            if (moves.any { (dx, dy) -> Position(from.col + dx, from.row + dy) == position }) {
                                return true
                            }
                        }
                        PieceType.BISHOP -> {
                            if (isDiagonalAttack(from, position)) return true
                        }
                        PieceType.ROOK -> {
                            if (isStraightAttack(from, position)) return true
                        }
                        PieceType.QUEEN -> {
                            if (isDiagonalAttack(from, position) || isStraightAttack(from, position)) return true
                        }
                        PieceType.KING -> {
                            if (kotlin.math.abs(position.col - from.col) <= 1 && 
                                kotlin.math.abs(position.row - from.row) <= 1) {
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
    }
    
    private fun isDiagonalAttack(from: Position, to: Position): Boolean {
        if (kotlin.math.abs(to.col - from.col) != kotlin.math.abs(to.row - from.row)) return false
        val dx = if (to.col > from.col) 1 else -1
        val dy = if (to.row > from.row) 1 else -1
        var x = from.col + dx
        var y = from.row + dy
        while (x != to.col && y != to.row) {
            if (getPieceAt(Position(x, y)) != null) return false
            x += dx
            y += dy
        }
        return true
    }
    
    private fun isStraightAttack(from: Position, to: Position): Boolean {
        if (from.col != to.col && from.row != to.row) return false
        val dx = when {
            to.col > from.col -> 1
            to.col < from.col -> -1
            else -> 0
        }
        val dy = when {
            to.row > from.row -> 1
            to.row < from.row -> -1
            else -> 0
        }
        var x = from.col + dx
        var y = from.row + dy
        while (x != to.col || y != to.row) {
            if (getPieceAt(Position(x, y)) != null) return false
            x += dx
            y += dy
        }
        return true
    }
    
    private fun wouldBeInCheck(from: Position, to: Position, color: Color): Boolean {
        // Временно делаем ход и проверяем
        val piece = getPieceAt(from) ?: return true
        val capturedPiece = getPieceAt(to)
        
        board[to.col][to.row] = piece
        board[from.col][from.row] = null
        
        val inCheck = isCheck(color)
        
        // Возвращаем как было
        board[from.col][from.row] = piece
        board[to.col][to.row] = capturedPiece
        
        return inCheck
    }
    
    fun isCheck(color: Color? = null): Boolean {
        val checkColor = color ?: currentPlayer
        val kingPos = findKing(checkColor) ?: return false
        return isSquareAttacked(kingPos, checkColor)
    }
    
    fun isCheckmate(): Boolean {
        return isCheck() && getAllLegalMoves().isEmpty()
    }
    
    fun isStalemate(): Boolean {
        return !isCheck() && getAllLegalMoves().isEmpty()
    }
    
    fun isGameOver(): Boolean {
        return isCheckmate() || isStalemate()
    }
    
    private fun getAllLegalMoves(): List<Pair<Position, Position>> {
        val allMoves = mutableListOf<Pair<Position, Position>>()
        for (col in 0..7) {
            for (row in 0..7) {
                val from = Position(col, row)
                val piece = getPieceAt(from)
                if (piece?.color == currentPlayer) {
                    val legalMoves = getLegalMoves(from)
                    for (to in legalMoves) {
                        allMoves.add(from to to)
                    }
                }
            }
        }
        return allMoves
    }
    
    private fun findKing(color: Color): Position? {
        for (col in 0..7) {
            for (row in 0..7) {
                val piece = getPieceAt(Position(col, row))
                if (piece?.type == PieceType.KING && piece.color == color) {
                    return Position(col, row)
                }
            }
        }
        return null
    }
    
    fun undoMove(): Boolean {
        if (moveHistory.isEmpty()) return false
        
        val lastMove = moveHistory.removeAt(moveHistory.size - 1)
        
        // Возвращаем фигуру обратно
        board[lastMove.from.col][lastMove.from.row] = lastMove.piece
        board[lastMove.to.col][lastMove.to.row] = lastMove.capturedPiece
        
        currentPlayer = currentPlayer.opposite()
        return true
    }
    
    private fun isInBounds(position: Position): Boolean {
        return position.col in 0..7 && position.row in 0..7
    }
}

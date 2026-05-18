// app/src/main/java/com/kakdela/p2p/model/ChessGame.kt
package com.kakdela.p2p.model

class ChessGame {
    private val board = Array(8) { Array<Piece?>(8) { null } }
    
    var currentPlayer: Color = Color.WHITE
        private set
    
    private val _moveHistory = mutableListOf<Move>()
    val moveHistory: List<Move> get() = _moveHistory.toList()
    val moveCount: Int get() = _moveHistory.size
    val lastMove: Move? get() = _moveHistory.lastOrNull()
    
    private var enPassantTarget: Position? = null
    private var whiteKingSideCastling = true
    private var whiteQueenSideCastling = true
    private var blackKingSideCastling = true
    private var blackQueenSideCastling = true
    
    init {
        setupInitialPosition()
    }
    
    private fun setupInitialPosition() {
        // Белые фигуры (ряд 0)
        board[0][0] = Piece(PieceType.ROOK, Color.WHITE)
        board[1][0] = Piece(PieceType.KNIGHT, Color.WHITE)
        board[2][0] = Piece(PieceType.BISHOP, Color.WHITE)
        board[3][0] = Piece(PieceType.QUEEN, Color.WHITE)
        board[4][0] = Piece(PieceType.KING, Color.WHITE)
        board[5][0] = Piece(PieceType.BISHOP, Color.WHITE)
        board[6][0] = Piece(PieceType.KNIGHT, Color.WHITE)
        board[7][0] = Piece(PieceType.ROOK, Color.WHITE)
        
        // Белые пешки (ряд 1)
        for (col in 0..7) {
            board[col][1] = Piece(PieceType.PAWN, Color.WHITE)
        }
        
        // Черные фигуры (ряд 7)
        board[0][7] = Piece(PieceType.ROOK, Color.BLACK)
        board[1][7] = Piece(PieceType.KNIGHT, Color.BLACK)
        board[2][7] = Piece(PieceType.BISHOP, Color.BLACK)
        board[3][7] = Piece(PieceType.QUEEN, Color.BLACK)
        board[4][7] = Piece(PieceType.KING, Color.BLACK)
        board[5][7] = Piece(PieceType.BISHOP, Color.BLACK)
        board[6][7] = Piece(PieceType.KNIGHT, Color.BLACK)
        board[7][7] = Piece(PieceType.ROOK, Color.BLACK)
        
        // Черные пешки (ряд 6)
        for (col in 0..7) {
            board[col][6] = Piece(PieceType.PAWN, Color.BLACK)
        }
    }
    
    fun getPieceAt(position: Position): Piece? {
        if (!isInBounds(position)) return null
        return board[position.col][position.row]
    }
    
    fun getBoardState(): Array<Array<Piece?>> {
        return Array(8) { row ->
            Array(8) { col ->
                board[col][row]
            }
        }
    }
    
    fun makeMove(from: Position, to: Position): Boolean {
        val piece = getPieceAt(from) ?: return false
        
        if (piece.color != currentPlayer) return false
        
        val legalMoves = getLegalMoves(from)
        if (to !in legalMoves) return false
        
        val capturedPiece = getPieceAt(to)
        val move = Move(from, to, piece, capturedPiece)
        
        // Выполняем ход
        board[to.col][to.row] = piece
        board[from.col][from.row] = null
        
        // Взятие на проходе
        if (piece.type == PieceType.PAWN && to == enPassantTarget) {
            val captureRow = if (piece.color == Color.WHITE) to.row - 1 else to.row + 1
            if (captureRow in 0..7) {
                board[to.col][captureRow] = null
            }
        }
        
        // Обновление en passant цели
        enPassantTarget = if (piece.type == PieceType.PAWN && 
            kotlin.math.abs(to.row - from.row) == 2) {
            val midRow = (from.row + to.row) / 2
            if (midRow in 0..7) Position(to.col, midRow) else null
        } else {
            null
        }
        
        // Рокировка
        if (piece.type == PieceType.KING && kotlin.math.abs(to.col - from.col) == 2) {
            if (to.col == 6) {
                // Короткая рокировка
                board[5][to.row] = board[7][to.row]
                board[7][to.row] = null
            } else if (to.col == 2) {
                // Длинная рокировка
                board[3][to.row] = board[0][to.row]
                board[0][to.row] = null
            }
        }
        
        // Обновление прав на рокировку
        updateCastlingRights(piece, from)
        
        // Превращение пешки
        if (piece.type == PieceType.PAWN && (to.row == 7 || to.row == 0)) {
            board[to.col][to.row] = Piece(PieceType.QUEEN, piece.color)
        }
        
        _moveHistory.add(move)
        currentPlayer = currentPlayer.opposite()
        return true
    }
    
    private fun updateCastlingRights(piece: Piece, from: Position) {
        when (piece.type) {
            PieceType.KING -> {
                if (piece.color == Color.WHITE) {
                    whiteKingSideCastling = false
                    whiteQueenSideCastling = false
                } else {
                    blackKingSideCastling = false
                    blackQueenSideCastling = false
                }
            }
            PieceType.ROOK -> {
                when {
                    from.col == 0 && from.row == 0 -> whiteQueenSideCastling = false
                    from.col == 7 && from.row == 0 -> whiteKingSideCastling = false
                    from.col == 0 && from.row == 7 -> blackQueenSideCastling = false
                    from.col == 7 && from.row == 7 -> blackKingSideCastling = false
                }
            }
            else -> {}
        }
    }
    
    fun getLegalMoves(from: Position): List<Position> {
        val piece = getPieceAt(from) ?: return emptyList()
        val moves = mutableListOf<Position>()
        
        when (piece.type) {
            PieceType.PAWN -> addPawnMoves(from, piece.color, moves)
            PieceType.KNIGHT -> addKnightMoves(from, piece.color, moves)
            PieceType.BISHOP -> addBishopMoves(from, piece.color, moves)
            PieceType.ROOK -> addRookMoves(from, piece.color, moves)
            PieceType.QUEEN -> addQueenMoves(from, piece.color, moves)
            PieceType.KING -> addKingMoves(from, piece.color, moves)
        }
        
        return moves.filter { move -> 
            isInBounds(move) && !wouldBeInCheck(from, move, piece.color) 
        }
    }
    
    private fun addPawnMoves(from: Position, color: Color, moves: MutableList<Position>) {
        val direction = if (color == Color.WHITE) 1 else -1
        val startRow = if (color == Color.WHITE) 1 else 6
        
        // Ход вперед на 1 клетку
        val oneForwardRow = from.row + direction
        if (oneForwardRow in 0..7) {
            val oneForward = Position(from.col, oneForwardRow)
            if (getPieceAt(oneForward) == null) {
                moves.add(oneForward)
                
                // Ход вперед на 2 клетки со стартовой позиции
                if (from.row == startRow) {
                    val twoForwardRow = from.row + 2 * direction
                    if (twoForwardRow in 0..7) {
                        val twoForward = Position(from.col, twoForwardRow)
                        if (getPieceAt(twoForward) == null) {
                            moves.add(twoForward)
                        }
                    }
                }
            }
        }
        
        // Взятие по диагонали
        for (dx in listOf(-1, 1)) {
            val captureCol = from.col + dx
            val captureRow = from.row + direction
            
            if (captureCol in 0..7 && captureRow in 0..7) {
                val capturePos = Position(captureCol, captureRow)
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
    
    private fun addKnightMoves(from: Position, color: Color, moves: MutableList<Position>) {
        val knightMoves = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        
        for ((dx, dy) in knightMoves) {
            val newCol = from.col + dx
            val newRow = from.row + dy
            
            if (newCol in 0..7 && newRow in 0..7) {
                val pos = Position(newCol, newRow)
                val targetPiece = getPieceAt(pos)
                if (targetPiece == null || targetPiece.color != color) {
                    moves.add(pos)
                }
            }
        }
    }
    
    private fun addBishopMoves(from: Position, color: Color, moves: MutableList<Position>) {
        val directions = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        addSlidingMoves(from, color, directions, moves)
    }
    
    private fun addRookMoves(from: Position, color: Color, moves: MutableList<Position>) {
        val directions = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        addSlidingMoves(from, color, directions, moves)
    }
    
    private fun addQueenMoves(from: Position, color: Color, moves: MutableList<Position>) {
        addBishopMoves(from, color, moves)
        addRookMoves(from, color, moves)
    }
    
    private fun addSlidingMoves(
        from: Position, 
        color: Color, 
        directions: List<Pair<Int, Int>>, 
        moves: MutableList<Position>
    ) {
        for ((dx, dy) in directions) {
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
    
    private fun addKingMoves(from: Position, color: Color, moves: MutableList<Position>) {
        // Обычные ходы короля
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                
                val newCol = from.col + dx
                val newRow = from.row + dy
                
                if (newCol in 0..7 && newRow in 0..7) {
                    val pos = Position(newCol, newRow)
                    val targetPiece = getPieceAt(pos)
                    if (targetPiece == null || targetPiece.color != color) {
                        moves.add(pos)
                    }
                }
            }
        }
        
        // Рокировка
        if (color == Color.WHITE && from.row == 0 && from.col == 4) {
            if (whiteKingSideCastling && canCastle(4, 7, 0)) {
                moves.add(Position(6, 0))
            }
            if (whiteQueenSideCastling && canCastle(4, 0, 0)) {
                moves.add(Position(2, 0))
            }
        } else if (color == Color.BLACK && from.row == 7 && from.col == 4) {
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
        val between = if (step == 1) (kingCol + 1) until rookCol else (rookCol + 1) until kingCol
        
        // Проверяем, что между королем и ладьей пусто
        for (col in between) {
            if (getPieceAt(Position(col, row)) != null) return false
        }
        
        // Проверяем, что ладья на месте
        val rook = getPieceAt(Position(rookCol, row))
        if (rook?.type != PieceType.ROOK) return false
        
        // Проверяем, что король не проходит через битое поле
        val king = getPieceAt(Position(kingCol, row))
        val color = king?.color ?: return false
        
        var currentCol = kingCol
        for (i in 1..2) {
            currentCol += step
            if (isSquareAttacked(Position(currentCol, row), color)) return false
        }
        
        return true
    }
    
    private fun isSquareAttacked(position: Position, defenderColor: Color): Boolean {
        if (!isInBounds(position)) return false
        
        val attackerColor = defenderColor.opposite()
        
        for (col in 0..7) {
            for (row in 0..7) {
                val piece = board[col][row]
                if (piece != null && piece.color == attackerColor) {
                    val from = Position(col, row)
                    if (canPieceAttackSquare(piece.type, from, position, attackerColor)) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
    private fun canPieceAttackSquare(
        type: PieceType, 
        from: Position, 
        target: Position, 
        attackerColor: Color
    ): Boolean {
        return when (type) {
            PieceType.PAWN -> {
                val direction = if (attackerColor == Color.WHITE) 1 else -1
                kotlin.math.abs(target.col - from.col) == 1 && 
                target.row == from.row + direction
            }
            PieceType.KNIGHT -> {
                val dx = kotlin.math.abs(target.col - from.col)
                val dy = kotlin.math.abs(target.row - from.row)
                (dx == 2 && dy == 1) || (dx == 1 && dy == 2)
            }
            PieceType.BISHOP -> isDiagonalPathClear(from, target)
            PieceType.ROOK -> isStraightPathClear(from, target)
            PieceType.QUEEN -> isDiagonalPathClear(from, target) || isStraightPathClear(from, target)
            PieceType.KING -> {
                kotlin.math.abs(target.col - from.col) <= 1 && 
                kotlin.math.abs(target.row - from.row) <= 1
            }
        }
    }
    
    private fun isDiagonalPathClear(from: Position, to: Position): Boolean {
        if (kotlin.math.abs(to.col - from.col) != kotlin.math.abs(to.row - from.row)) return false
        if (!isInBounds(from) || !isInBounds(to)) return false
        
        val dx = if (to.col > from.col) 1 else -1
        val dy = if (to.row > from.row) 1 else -1
        var x = from.col + dx
        var y = from.row + dy
        
        while (x != to.col && y != to.row) {
            if (x !in 0..7 || y !in 0..7) return false
            if (board[x][y] != null) return false
            x += dx
            y += dy
        }
        return true
    }
    
    private fun isStraightPathClear(from: Position, to: Position): Boolean {
        if (from.col != to.col && from.row != to.row) return false
        if (!isInBounds(from) || !isInBounds(to)) return false
        
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
            if (x !in 0..7 || y !in 0..7) return false
            if (board[x][y] != null) return false
            x += dx
            y += dy
        }
        return true
    }
    
    private fun wouldBeInCheck(from: Position, to: Position, color: Color): Boolean {
        if (!isInBounds(from) || !isInBounds(to)) return true
        
        val piece = board[from.col][from.row] ?: return true
        val capturedPiece = board[to.col][to.row]
        
        // Временно делаем ход
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
        return isCheckmate() || isStalemate() || isInsufficientMaterial()
    }
    
    private fun isInsufficientMaterial(): Boolean {
        val pieces = mutableListOf<Piece>()
        for (col in 0..7) {
            for (row in 0..7) {
                board[col][row]?.let { pieces.add(it) }
            }
        }
        
        // Король против короля
        if (pieces.size == 2) return true
        
        // Король и слон/конь против короля
        if (pieces.size == 3) {
            val nonKing = pieces.find { it.type != PieceType.KING }
            if (nonKing?.type == PieceType.BISHOP || nonKing?.type == PieceType.KNIGHT) {
                return true
            }
        }
        
        return false
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
                val piece = board[col][row]
                if (piece?.type == PieceType.KING && piece.color == color) {
                    return Position(col, row)
                }
            }
        }
        return null
    }
    
    fun undoMove(): Boolean {
        if (_moveHistory.isEmpty()) return false
        
        val lastMove = _moveHistory.removeAt(_moveHistory.size - 1)
        
        // Возвращаем фигуру
        board[lastMove.from.col][lastMove.from.row] = lastMove.piece
        board[lastMove.to.col][lastMove.to.row] = lastMove.capturedPiece
        
        // Восстанавливаем en passant
        if (lastMove.piece.type == PieceType.PAWN && 
            kotlin.math.abs(lastMove.to.row - lastMove.from.row) == 2) {
            enPassantTarget = Position(lastMove.to.col, (lastMove.from.row + lastMove.to.row) / 2)
        } else {
            enPassantTarget = null
        }
        
        currentPlayer = currentPlayer.opposite()
        return true
    }
    
    fun reset() {
        // Очищаем доску
        for (col in 0..7) {
            for (row in 0..7) {
                board[col][row] = null
            }
        }
        
        // Сбрасываем состояние
        _moveHistory.clear()
        currentPlayer = Color.WHITE
        enPassantTarget = null
        whiteKingSideCastling = true
        whiteQueenSideCastling = true
        blackKingSideCastling = true
        blackQueenSideCastling = true
        
        // Устанавливаем начальную позицию
        setupInitialPosition()
    }
    
    private fun isInBounds(position: Position): Boolean {
        return position.col in 0..7 && position.row in 0..7
    }
}

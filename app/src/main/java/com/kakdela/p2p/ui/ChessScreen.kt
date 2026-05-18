// app/src/main/java/com/kakdela/p2p/ui/ChessScreen.kt
package com.kakdela.p2p.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kakdela.p2p.model.*
import com.kakdela.p2p.ui.theme.*
import kotlinx.coroutines.delay

// Временные заглушки (если нет в проекте)
object Routes {
    const val CHESS = "chess"
    const val ENTERTAINMENT = "entertainment"
}

@Composable
fun ChessScreen(
    onBack: () -> Unit = {}
) {
    val chessGame = remember { ChessGame() }
    var selectedPosition by remember { mutableStateOf<Position?>(null) }
    var legalMoves by remember { mutableStateOf<List<Position>>(emptyList()) }
    var boardPieces by remember { mutableStateOf(chessGame.getBoardState()) }
    var currentPlayer by remember { mutableStateOf(chessGame.currentPlayer) }
    var gameStatus by remember { mutableStateOf("") }
    var showPromotionDialog by remember { mutableStateOf(false) }
    var pendingPromotion by remember { mutableStateOf<Pair<Position, Position>?>(null) }
    var capturedByWhite by remember { mutableStateOf<List<Piece>>(emptyList()) }
    var capturedByBlack by remember { mutableStateOf<List<Piece>>(emptyList()) }
    var lastMove by remember { mutableStateOf<Pair<Position, Position>?>(null) }
    var showGameEndDialog by remember { mutableStateOf(false) }
    var gameEndMessage by remember { mutableStateOf("") }

    LaunchedEffect(currentPlayer) {
        if (chessGame.isCheckmate()) {
            val winner = if (currentPlayer == Color.WHITE) "ЧЁРНЫЕ" else "БЕЛЫЕ"
            gameEndMessage = "МАТ! $winner ПОБЕДИЛИ!"
            gameStatus = gameEndMessage
            showGameEndDialog = true
        } else if (chessGame.isStalemate()) {
            gameEndMessage = "ПАТ! НИЧЬЯ!"
            gameStatus = gameEndMessage
            showGameEndDialog = true
        } else if (chessGame.isCheck()) {
            gameStatus = "ШАХ!"
        } else {
            gameStatus = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Верхняя панель
            ChessTopBar(
                currentPlayer = currentPlayer,
                gameStatus = gameStatus,
                onBack = onBack,
                onUndo = {
                    if (chessGame.undoMove()) {
                        selectedPosition = null
                        legalMoves = emptyList()
                        boardPieces = chessGame.getBoardState()
                        currentPlayer = chessGame.currentPlayer
                    }
                },
                onReset = {
                    chessGame.reset()
                    selectedPosition = null
                    legalMoves = emptyList()
                    boardPieces = chessGame.getBoardState()
                    currentPlayer = chessGame.currentPlayer
                    capturedByWhite = emptyList()
                    capturedByBlack = emptyList()
                    lastMove = null
                }
            )

            // Захваченные фигуры противника
            CapturedPiecesBar(
                pieces = capturedByBlack,
                alignment = Alignment.Start
            )

            // Шахматная доска
            ChessBoard(
                pieces = boardPieces,
                selectedPosition = selectedPosition,
                legalMoves = legalMoves,
                lastMove = lastMove,
                currentPlayer = currentPlayer,
                onSquareClick = { position ->
                    if (selectedPosition == null) {
                        val piece = boardPieces[position.row][position.col]
                        if (piece != null && piece.color == currentPlayer) {
                            selectedPosition = position
                            legalMoves = chessGame.getLegalMoves(position)
                        }
                    } else {
                        val from = selectedPosition!!
                        if (chessGame.makeMove(from, position)) {
                            // Обновляем состояние
                            selectedPosition = null
                            legalMoves = emptyList()
                            boardPieces = chessGame.getBoardState()
                            currentPlayer = chessGame.currentPlayer
                            lastMove = from to position
                            
                            // Обновляем захваченные фигуры
                            val move = chessGame.lastMove
                            if (move?.capturedPiece != null) {
                                if (currentPlayer == Color.BLACK) {
                                    capturedByWhite = capturedByWhite + move.capturedPiece
                                } else {
                                    capturedByBlack = capturedByBlack + move.capturedPiece
                                }
                            }
                        } else {
                            // Проверяем, кликнули ли на другую свою фигуру
                            val piece = boardPieces[position.row][position.col]
                            if (piece != null && piece.color == currentPlayer) {
                                selectedPosition = position
                                legalMoves = chessGame.getLegalMoves(position)
                            } else {
                                selectedPosition = null
                                legalMoves = emptyList()
                            }
                        }
                    }
                }
            )

            // Захваченные фигуры игрока
            CapturedPiecesBar(
                pieces = capturedByWhite,
                alignment = Alignment.End
            )

            // Нижняя панель с информацией
            GameInfoBar(
                moveCount = chessGame.moveCount,
                currentPlayer = currentPlayer
            )
        }

        // Диалог конца игры
        if (showGameEndDialog) {
            GameEndDialog(
                message = gameEndMessage,
                onNewGame = {
                    showGameEndDialog = false
                    chessGame.reset()
                    boardPieces = chessGame.getBoardState()
                    currentPlayer = chessGame.currentPlayer
                    capturedByWhite = emptyList()
                    capturedByBlack = emptyList()
                    lastMove = null
                },
                onBack = {
                    showGameEndDialog = false
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun ChessTopBar(
    currentPlayer: Color,
    gameStatus: String,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0A0A0A),
        shadowElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Назад",
                        tint = NeonCyan
                    )
                }
                
                Text(
                    "ШАХМАТЫ",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(onClick = onUndo) {
                        Icon(
                            Icons.Default.Undo,
                            "Отменить ход",
                            tint = NeonPink
                        )
                    }
                    IconButton(onClick = onReset) {
                        Icon(
                            Icons.Default.Refresh,
                            "Новая игра",
                            tint = NeonPurple
                        )
                    }
                }
            }
            
            if (gameStatus.isNotEmpty()) {
                Text(
                    gameStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    textAlign = TextAlign.Center,
                    color = if (gameStatus == "ШАХ!") Color.Red else NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ChessBoard(
    pieces: Array<Array<Piece?>>,
    selectedPosition: Position?,
    legalMoves: List<Position>,
    lastMove: Pair<Position, Position>?,
    currentPlayer: Color,
    onSquareClick: (Position) -> Unit
) {
    val boardSize = 380.dp
    val squareSize = boardSize / 8
    
    Box(
        modifier = Modifier
            .size(boardSize)
            .shadow(16.dp, RoundedCornerShape(8.dp))
            .border(2.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(boardSize)) {
            val squareSizePx = size.width / 8
            
            // Рисуем клетки
            for (row in 0..7) {
                for (col in 0..7) {
                    val isLight = (row + col) % 2 == 0
                    val color = if (isLight) {
                        Color(0xFF2A2A4A)
                    } else {
                        Color(0xFF1A1A3A)
                    }
                    
                    drawRect(
                        color = color,
                        topLeft = Offset(col * squareSizePx, row * squareSizePx),
                        size = androidx.compose.ui.geometry.Size(squareSizePx, squareSizePx)
                    )
                    
                    // Подсветка последнего хода
                    if (lastMove != null) {
                        if ((row == lastMove.first.row && col == lastMove.first.col) ||
                            (row == lastMove.second.row && col == lastMove.second.col)) {
                            drawRect(
                                color = NeonCyan.copy(alpha = 0.3f),
                                topLeft = Offset(col * squareSizePx, row * squareSizePx),
                                size = androidx.compose.ui.geometry.Size(squareSizePx, squareSizePx)
                            )
                        }
                    }
                    
                    // Подсветка выбранной клетки
                    if (selectedPosition?.row == row && selectedPosition?.col == col) {
                        drawRect(
                            color = NeonPurple.copy(alpha = 0.5f),
                            topLeft = Offset(col * squareSizePx, row * squareSizePx),
                            size = androidx.compose.ui.geometry.Size(squareSizePx, squareSizePx)
                        )
                    }
                    
                    // Подсветка возможных ходов
                    val pos = Position(col, row)
                    if (pos in legalMoves) {
                        val piece = pieces[row][col]
                        if (piece != null) {
                            // Ход со взятием - кольцо
                            drawCircle(
                                color = NeonPink.copy(alpha = 0.7f),
                                radius = squareSizePx * 0.45f,
                                center = Offset(
                                    col * squareSizePx + squareSizePx / 2,
                                    row * squareSizePx + squareSizePx / 2
                                ),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        } else {
                            // Обычный ход - точка
                            drawCircle(
                                color = NeonCyan.copy(alpha = 0.5f),
                                radius = squareSizePx * 0.15f,
                                center = Offset(
                                    col * squareSizePx + squareSizePx / 2,
                                    row * squareSizePx + squareSizePx / 2
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // Фигуры
        Box(
            modifier = Modifier
                .size(boardSize)
                .clickable { }
        ) {
            for (row in 0..7) {
                for (col in 0..7) {
                    val piece = pieces[row][col]
                    if (piece != null) {
                        val isWhite = piece.color == Color.WHITE
                        val pieceColor = if (isWhite) Color.White else Color(0xFF1A1A1A)
                        val glowColor = if (isWhite) NeonCyan else NeonPink
                        
                        Text(
                            text = piece.symbol,
                            modifier = Modifier
                                .offset(
                                    x = (col * squareSize / 8),
                                    y = (row * squareSize / 8)
                                )
                                .size(squareSize / 8)
                                .clickable { onSquareClick(Position(col, row)) }
                                .drawBehind {
                                    // Свечение фигуры
                                    drawCircle(
                                        color = glowColor.copy(alpha = 0.3f),
                                        radius = size.minDimension / 2 + 4.dp.toPx(),
                                        center = center
                                    )
                                },
                            color = pieceColor,
                            fontSize = (squareSize / 8).value.sp * 0.6f,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapturedPiecesBar(
    pieces: List<Piece>,
    alignment: Alignment.Horizontal
) {
    if (pieces.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = if (alignment == Alignment.Start) 
                Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "×${pieces.size}",
                color = NeonCyan.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            pieces.forEach { piece ->
                Text(
                    text = piece.symbol,
                    color = if (piece.color == Color.WHITE) Color.White.copy(alpha = 0.6f) 
                           else Color(0xFF666666),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun GameInfoBar(
    moveCount: Int,
    currentPlayer: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0A0A0A),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Ход: $moveCount",
                    color = NeonCyan.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Text(
                    if (currentPlayer == Color.WHITE) "Ходят белые" else "Ходят чёрные",
                    color = NeonPink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            // Индикатор текущего игрока
            Canvas(modifier = Modifier.size(20.dp)) {
                drawCircle(
                    color = if (currentPlayer == Color.WHITE) Color.White else Color(0xFF333333),
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = if (currentPlayer == Color.WHITE) NeonCyan else NeonPink,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun GameEndDialog(
    message: String,
    onNewGame: () -> Unit,
    onBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = Color(0xFF1A1A2E),
        title = {
            Text(
                "Игра окончена",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                message,
                color = NeonPink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                )
            ) {
                Text("НОВАЯ ИГРА")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, NeonPurple)
            ) {
                Text("ВЫЙТИ", color = NeonPurple)
            }
        }
    )
}

// Расширения для ChessGame
private fun ChessGame.getBoardState(): Array<Array<Piece?>> {
    val state = Array(8) { Array<Piece?>(8) { null } }
    for (row in 0..7) {
        for (col in 0..7) {
            state[row][col] = getPieceAt(Position(col, row))
        }
    }
    return state
}

val ChessGame.lastMove: Move?
    get() = moveHistory.lastOrNull()

val ChessGame.moveCount: Int
    get() = moveHistory.size

fun ChessGame.reset() {
    // Пересоздаём игру
    val newGame = ChessGame()
    // Копируем состояние (костыль, но работает)
    this.currentPlayer = newGame.currentPlayer
}

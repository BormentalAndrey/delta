// app/src/main/java/com/kakdela/p2p/ui/ChessScreen.kt
package com.kakdela.p2p.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kakdela.p2p.model.*
import com.kakdela.p2p.ui.theme.NeonCyan
import com.kakdela.p2p.ui.theme.NeonPink
import com.kakdela.p2p.ui.theme.NeonPurple
import androidx.compose.ui.graphics.Color as ComposeColor

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
    var capturedByWhite by remember { mutableStateOf<List<Piece>>(emptyList()) }
    var capturedByBlack by remember { mutableStateOf<List<Piece>>(emptyList()) }
    var lastMove by remember { mutableStateOf<Pair<Position, Position>?>(null) }
    var showGameEndDialog by remember { mutableStateOf(false) }
    var gameEndMessage by remember { mutableStateOf("") }
    var moveCount by remember { mutableStateOf(0) }

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
            .background(ComposeColor.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        moveCount = chessGame.moveCount
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
                    moveCount = 0
                    gameStatus = ""
                }
            )

            CapturedPiecesBar(
                pieces = capturedByBlack,
                modifier = Modifier.fillMaxWidth()
            )

            ChessBoard(
                pieces = boardPieces,
                selectedPosition = selectedPosition,
                legalMoves = legalMoves,
                lastMove = lastMove,
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
                            val move = chessGame.lastMove
                            if (move?.capturedPiece != null) {
                                if (currentPlayer == Color.WHITE) {
                                    capturedByWhite = capturedByWhite + move.capturedPiece
                                } else {
                                    capturedByBlack = capturedByBlack + move.capturedPiece
                                }
                            }
                            
                            selectedPosition = null
                            legalMoves = emptyList()
                            boardPieces = chessGame.getBoardState()
                            currentPlayer = chessGame.currentPlayer
                            lastMove = from to position
                            moveCount = chessGame.moveCount
                        } else {
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

            CapturedPiecesBar(
                pieces = capturedByWhite,
                modifier = Modifier.fillMaxWidth()
            )

            GameInfoBar(
                moveCount = moveCount,
                currentPlayer = currentPlayer
            )
        }

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
                    moveCount = 0
                    gameStatus = ""
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
        color = ComposeColor(0xFF0A0A0A.toInt()),
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
                    color = if (gameStatus == "ШАХ!") ComposeColor.Red else NeonCyan,
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
    onSquareClick: (Position) -> Unit
) {
    val density = LocalDensity.current
    val boardSize = 380.dp
    val boardSizePx = with(density) { boardSize.toPx() }
    val squareSizeDp: Dp = boardSize / 8
    val squareSizePx = boardSizePx / 8
    
    Box(
        modifier = Modifier
            .size(boardSize)
            .shadow(16.dp, RoundedCornerShape(8.dp))
            .border(2.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(boardSize)) {
            for (row in 0..7) {
                for (col in 0..7) {
                    val isLight = (row + col) % 2 == 0
                    val squareColor = if (isLight) {
                        ComposeColor(0xFF2A2A4A.toInt())
                    } else {
                        ComposeColor(0xFF1A1A3A.toInt())
                    }
                    
                    drawRect(
                        color = squareColor,
                        topLeft = Offset(col * squareSizePx, row * squareSizePx),
                        size = Size(squareSizePx, squareSizePx)
                    )
                    
                    if (lastMove != null) {
                        if ((row == lastMove.first.row && col == lastMove.first.col) ||
                            (row == lastMove.second.row && col == lastMove.second.col)) {
                            drawRect(
                                color = NeonCyan.copy(alpha = 0.3f),
                                topLeft = Offset(col * squareSizePx, row * squareSizePx),
                                size = Size(squareSizePx, squareSizePx)
                            )
                        }
                    }
                    
                    if (selectedPosition?.row == row && selectedPosition?.col == col) {
                        drawRect(
                            color = NeonPurple.copy(alpha = 0.5f),
                            topLeft = Offset(col * squareSizePx, row * squareSizePx),
                            size = Size(squareSizePx, squareSizePx)
                        )
                    }
                    
                    val pos = Position(col, row)
                    if (pos in legalMoves) {
                        val targetPiece = pieces[row][col]
                        if (targetPiece != null) {
                            drawCircle(
                                color = NeonPink.copy(alpha = 0.7f),
                                radius = squareSizePx * 0.45f,
                                center = Offset(
                                    col * squareSizePx + squareSizePx / 2,
                                    row * squareSizePx + squareSizePx / 2
                                ),
                                style = Stroke(width = with(density) { 3.dp.toPx() })
                            )
                        } else {
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
        
        Box(modifier = Modifier.size(boardSize)) {
            for (row in 0..7) {
                for (col in 0..7) {
                    val piece = pieces[row][col]
                    if (piece != null) {
                        val isWhite = piece.color == Color.WHITE
                        val pieceColor = if (isWhite) ComposeColor.White else ComposeColor(0xFF1A1A1A.toInt())
                        val glowColor = if (isWhite) NeonCyan else NeonPink
                        
                        Text(
                            text = piece.symbol,
                            modifier = Modifier
                                .offset(
                                    x = squareSizeDp * col,
                                    y = squareSizeDp * row
                                )
                                .size(squareSizeDp)
                                .clickable { onSquareClick(Position(col, row)) }
                                .drawBehind {
                                    drawCircle(
                                        color = glowColor.copy(alpha = 0.3f),
                                        radius = size.minDimension / 2 + with(density) { 4.dp.toPx() },
                                        center = center
                                    )
                                },
                            color = pieceColor,
                            fontSize = with(density) { (squareSizeDp.toPx() * 0.6f).toSp() },
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
    modifier: Modifier = Modifier
) {
    if (pieces.isNotEmpty()) {
        Row(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pieces.forEach { piece ->
                Text(
                    text = piece.symbol,
                    color = if (piece.color == Color.WHITE) 
                        ComposeColor.White.copy(alpha = 0.6f) 
                    else 
                        ComposeColor(0xFF666666.toInt()),
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "×${pieces.size}",
                color = NeonCyan.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun GameInfoBar(
    moveCount: Int,
    currentPlayer: Color
) {
    val density = LocalDensity.current
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ComposeColor(0xFF0A0A0A.toInt()),
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
            
            Canvas(modifier = Modifier.size(20.dp)) {
                drawCircle(
                    color = if (currentPlayer == Color.WHITE) ComposeColor.White else ComposeColor(0xFF333333.toInt()),
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = if (currentPlayer == Color.WHITE) NeonCyan else NeonPink,
                    radius = size.minDimension / 2,
                    style = Stroke(width = with(density) { 2.dp.toPx() })
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
        containerColor = ComposeColor(0xFF1A1A2E.toInt()),
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
                    contentColor = ComposeColor.Black
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

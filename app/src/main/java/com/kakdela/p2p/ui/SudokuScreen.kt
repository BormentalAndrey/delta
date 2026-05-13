package com.kakdela.p2p.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalIndication
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

data class SudokuCell(var value: Int = 0, var isFixed: Boolean = false)

// Неоновая цветовая палитра
private val DarkBg = Color.Black
private val CardBg = Color(0xFF0A0A0A)
private val BlockHighlightBg = Color(0xFF1C1C1C)
private val NeonPrimary = Color(0xFF00FFFF)   // Яркий циан
private val NeonSecondary = Color(0xFF00FF41) // Яркий зелёный
private val NeonAccent = Color(0xFFFF00FF)    // Яркая маджента (для ошибок)
private val GridLineThick = NeonPrimary
private val GridLineThin = Color(0xFF333333)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen() {
    var board by remember { mutableStateOf(generateSudokuPuzzle()) }
    var selectedRow by remember { mutableStateOf(-1) }
    var selectedCol by remember { mutableStateOf(-1) }
    var isVictory by remember { mutableStateOf(false) }

    fun newGame() {
        board = generateSudokuPuzzle()
        selectedRow = -1
        selectedCol = -1
        isVictory = false
    }

    fun setNumber(num: Int) {
        if (selectedRow !in 0..8 || selectedCol !in 0..8) return
        if (board[selectedRow][selectedCol].isFixed) return

        val newBoard = board.map { it.toMutableList() }.toMutableList()
        newBoard[selectedRow][selectedCol].value =
            if (newBoard[selectedRow][selectedCol].value == num) 0 else num
        board = newBoard

        if (isBoardFull(board) && isValidSolution(board)) {
            isVictory = true
        }
    }

    CompositionLocalProvider(LocalIndication provides ripple()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "СУДОКУ",
                            color = NeonPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            letterSpacing = 4.sp,
                            modifier = Modifier.shadow(12.dp, clip = false, spotColor = NeonPrimary)
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBg),
                    actions = {
                        IconButton(onClick = { newGame() }) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Новая игра",
                                tint = NeonSecondary,
                                modifier = Modifier.shadow(8.dp, CircleShape, clip = false, spotColor = NeonSecondary)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBg)
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isVictory) "🎉 ПОБЕДА! ГЕНИАЛЬНО! 🎉" else "Заполни поле правильно",
                    color = if (isVictory) NeonSecondary else NeonPrimary,
                    fontSize = if (isVictory) 28.sp else 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .padding(16.dp)
                        .shadow(
                            elevation = if (isVictory) 16.dp else 4.dp,
                            spotColor = if (isVictory) NeonSecondary else NeonPrimary,
                            clip = false
                        )
                )

                Card(
                    modifier = Modifier
                        .size(380.dp)
                        .padding(8.dp)
                        .shadow(24.dp, RoundedCornerShape(20.dp), clip = false, spotColor = NeonPrimary),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(4.dp, NeonPrimary)
                ) {
                    Column {
                        for (row in 0 until 9) {
                            Row {
                                for (col in 0 until 9) {
                                    val cell = board[row][col]
                                    val isSelected = selectedRow == row && selectedCol == col
                                    val hasError = cell.value != 0 && !isValidPlacement(board, row, col, cell.value)

                                    val selectedValue = if (selectedRow in 0..8 && selectedCol in 0..8)
                                        board[selectedRow][selectedCol].value else 0
                                    val isSameValue = cell.value == selectedValue && cell.value != 0 && !isSelected
                                    val isSelectedRow = row == selectedRow
                                    val isSelectedCol = col == selectedCol
                                    val isSelectedBlock = selectedRow != -1 &&
                                            row / 3 == selectedRow / 3 && col / 3 == selectedCol / 3

                                    // Фон ячейки с неоновыми подсветками
                                    var cellBg = CardBg
                                    if (isSelectedBlock) cellBg = BlockHighlightBg
                                    if (isSelectedRow || isSelectedCol) cellBg = NeonSecondary.copy(alpha = 0.25f)
                                    if (isSameValue) cellBg = NeonAccent.copy(alpha = 0.25f)
                                    if (isSelected) cellBg = NeonPrimary.copy(alpha = 0.45f)

                                    // Цвет текста
                                    val textColor = when {
                                        cell.isFixed -> NeonPrimary
                                        hasError -> NeonAccent
                                        else -> NeonSecondary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(3.dp)
                                            .background(cellBg)
                                            .border(
                                                width = if (col % 3 == 0) 4.dp else 1.dp,
                                                color = if (col % 3 == 0) GridLineThick else GridLineThin
                                            )
                                            .border(
                                                width = if (row % 3 == 0) 4.dp else 1.dp,
                                                color = if (row % 3 == 0) GridLineThick else GridLineThin
                                            )
                                            .shadow(
                                                elevation = if (isSelected) 16.dp else if (cell.value != 0) 4.dp else 0.dp,
                                                shape = RoundedCornerShape(6.dp),
                                                clip = false,
                                                spotColor = NeonPrimary.copy(alpha = 0.6f)
                                            )
                                            .clickable { selectedRow = row; selectedCol = col },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (cell.value == 0) "" else cell.value.toString(),
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = textColor,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .shadow(
                                                    elevation = 10.dp,
                                                    spotColor = textColor,
                                                    ambientColor = textColor.copy(alpha = 0.5f),
                                                    clip = false
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Кнопки ввода цифр — неоновые круглые с свечением
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    for (chunk in listOf(1..5, 6..9)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            for (num in chunk) {
                                Button(
                                    onClick = { setNumber(num) },
                                    modifier = Modifier
                                        .size(60.dp)
                                        .shadow(12.dp, CircleShape, clip = false, spotColor = NeonPrimary),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonPrimary.copy(alpha = 0.2f),
                                        contentColor = NeonPrimary
                                    ),
                                    border = BorderStroke(3.dp, NeonPrimary)
                                ) {
                                    Text(
                                        num.toString(),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Кнопка стирания
                    Button(
                        onClick = { setNumber(0) },
                        modifier = Modifier
                            .width(220.dp)
                            .height(60.dp)
                            .shadow(14.dp, RoundedCornerShape(30.dp), clip = false, spotColor = NeonAccent),
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonAccent.copy(alpha = 0.2f),
                            contentColor = NeonAccent
                        ),
                        border = BorderStroke(3.dp, NeonAccent)
                    ) {
                        Text(
                            "СТЕРЕТЬ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

// Остальные функции без изменений (генерация, валидация и т.д.)
private fun generateSudokuPuzzle(): List<MutableList<SudokuCell>> {
    val board = MutableList(9) { MutableList(9) { SudokuCell() } }
    fillBoard(board)
    removeCells(board, 45)
    return board
}

private fun fillBoard(board: MutableList<MutableList<SudokuCell>>): Boolean {
    for (row in 0..8) {
        for (col in 0..8) {
            if (board[row][col].value == 0) {
                val nums = (1..9).shuffled()
                for (num in nums) {
                    if (isValidPlacement(board, row, col, num)) {
                        board[row][col].value = num
                        if (fillBoard(board)) return true
                        board[row][col].value = 0
                    }
                }
                return false
            }
        }
    }
    return true
}

private fun removeCells(board: MutableList<MutableList<SudokuCell>>, count: Int) {
    var removed = 0
    while (removed < count) {
        val row = Random.nextInt(9)
        val col = Random.nextInt(9)
        if (board[row][col].value != 0) {
            board[row][col].apply {
                value = 0
                isFixed = false
            }
            removed++
        }
    }
    board.forEach { rowList ->
        rowList.forEach { cell ->
            if (cell.value != 0) cell.isFixed = true
        }
    }
}

private fun isValidPlacement(board: List<List<SudokuCell>>, row: Int, col: Int, num: Int): Boolean {
    for (i in 0..8) {
        if (board[row][i].value == num || board[i][col].value == num) return false
    }
    val r = row / 3 * 3
    val c = col / 3 * 3
    for (i in r until r + 3) for (j in c until c + 3) if (board[i][j].value == num) return false
    return true
}

private fun isBoardFull(board: List<List<SudokuCell>>) = board.all { it.all { cell -> cell.value != 0 } }

private fun isValidSolution(board: List<List<SudokuCell>>): Boolean {
    for (row in 0..8) {
        for (col in 0..8) {
            if (!isValidPlacement(board, row, col, board[row][col].value)) {
                return false
            }
        }
    }
    return true
}

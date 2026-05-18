// app/src/main/java/com/kakdela/p2p/ui/ChessScreen.kt
package com.kakdela.p2p.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.kakdela.p2p.model.ChessGame
import com.kakdela.p2p.model.Piece
import com.kakdela.p2p.model.Position
import com.kakdela.p2p.model.Color as ChessColor

class ChessScreen(
    private val game: MainGame,
    private val gameMode: GameMode
) : ScreenAdapter() {
    
    private val viewport = FitViewport(480f, 800f)
    private val stage = Stage(viewport)
    private val skin = createSkin()
    private val chessGame = ChessGame() // Здесь будет логика шахмат
    private var selectedSquare: Position? = null
    private var board: Table? = null
    private var statusLabel: Label? = null
    private val squareButtons = mutableMapOf<Position, Button>()
    
    override fun show() {
        Gdx.input.inputProcessor = stage
        
        val root = Table()
        root.setFillParent(true)
        
        // Статус бар
        statusLabel = Label("Ход белых", skin)
        root.add(statusLabel).pad(10f).row()
        
        // Шахматная доска
        board = createChessBoard()
        root.add(board).size(440f).pad(10f).row()
        
        // Кнопки управления
        val controls = Table()
        
        val undoButton = TextButton("Отменить ход", skin)
        undoButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                undoMove()
            }
        })
        
        val backButton = TextButton("Назад", skin)
        backButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.returnToMenu()
            }
        })
        
        controls.add(undoButton).pad(5f).width(210f)
        controls.add(backButton).pad(5f).width(210f)
        root.add(controls).pad(10f)
        
        stage.addActor(root)
        updateBoard()
    }
    
    private fun createChessBoard(): Table {
        val boardTable = Table()
        val squareSize = 55f
        
        for (row in 7 downTo 0) {
            for (col in 0..7) {
                val position = Position(col, row)
                val button = Button(skin)
                button.setSize(squareSize, squareSize)
                
                // Цвет клетки
                val isLight = (row + col) % 2 == 0
                val color = if (isLight) Color(0.93f, 0.93f, 0.82f, 1f) 
                           else Color(0.47f, 0.59f, 0.34f, 1f)
                
                val style = Button.ButtonStyle()
                style.up = createDrawable(color)
                style.down = createDrawable(color.cpy().mul(0.8f))
                button.style = style
                
                button.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        onSquareClick(position)
                    }
                })
                
                squareButtons[position] = button
                boardTable.add(button).size(squareSize)
            }
            boardTable.row()
        }
        
        return boardTable
    }
    
    private fun onSquareClick(position: Position) {
        if (selectedSquare == null) {
            // Выбор фигуры
            val piece = chessGame.getPieceAt(position)
            if (piece != null && piece.color == chessGame.currentPlayer) {
                selectedSquare = position
                highlightLegalMoves(position)
            }
        } else {
            // Попытка сделать ход
            val from = selectedSquare!!
            if (chessGame.makeMove(from, position)) {
                selectedSquare = null
                updateBoard()
                checkGameState()
            } else {
                // Если кликнули на другую свою фигуру
                val piece = chessGame.getPieceAt(position)
                if (piece != null && piece.color == chessGame.currentPlayer) {
                    selectedSquare = position
                    highlightLegalMoves(position)
                } else {
                    selectedSquare = null
                }
            }
        }
        updateBoard()
    }
    
    private fun highlightLegalMoves(from: Position) {
        // TODO: Подсветка возможных ходов
    }
    
    private fun updateBoard() {
        for (row in 7 downTo 0) {
            for (col in 0..7) {
                val position = Position(col, row)
                val button = squareButtons[position] ?: continue
                val piece = chessGame.getPieceAt(position)
                
                // Очищаем текст на кнопке
                button.clearChildren()
                
                if (piece != null) {
                    val pieceChar = when(piece.type) {
                        com.kakdela.p2p.model.PieceType.KING -> if (piece.color == ChessColor.WHITE) "♔" else "♚"
                        com.kakdela.p2p.model.PieceType.QUEEN -> if (piece.color == ChessColor.WHITE) "♕" else "♛"
                        com.kakdela.p2p.model.PieceType.ROOK -> if (piece.color == ChessColor.WHITE) "♖" else "♜"
                        com.kakdela.p2p.model.PieceType.BISHOP -> if (piece.color == ChessColor.WHITE) "♗" else "♝"
                        com.kakdela.p2p.model.PieceType.KNIGHT -> if (piece.color == ChessColor.WHITE) "♘" else "♞"
                        com.kakdela.p2p.model.PieceType.PAWN -> if (piece.color == ChessColor.WHITE) "♙" else "♟"
                    }
                    
                    val label = Label(pieceChar, skin)
                    label.setAlignment(Align.center)
                    button.add(label).expand().fill()
                }
                
                // Подсветка выбранной клетки
                if (position == selectedSquare) {
                    val highlightColor = Color(1f, 1f, 0f, 0.5f)
                    val style = Button.ButtonStyle()
                    style.up = createDrawable(highlightColor)
                    button.style = style
                }
            }
        }
        
        statusLabel?.setText(
            if (chessGame.currentPlayer == ChessColor.WHITE) "Ход белых" 
            else "Ход чёрных"
        )
    }
    
    private fun checkGameState() {
        if (chessGame.isCheckmate()) {
            val winner = if (chessGame.currentPlayer == ChessColor.WHITE) "Чёрные" else "Белые"
            showDialog("Мат!", "$winner победили!")
        } else if (chessGame.isStalemate()) {
            showDialog("Пат!", "Ничья!")
        } else if (chessGame.isCheck()) {
            showDialog("Шах!", "Король под шахом!")
        }
    }
    
    private fun undoMove() {
        if (chessGame.undoMove()) {
            selectedSquare = null
            updateBoard()
        }
    }
    
    private fun showDialog(title: String, message: String) {
        val dialog = Dialog(title, skin)
        dialog.text(message)
        dialog.button("OK") {
            if (chessGame.isGameOver()) {
                game.returnToMenu()
            }
        }
        dialog.show(stage)
    }
    
    private fun createDrawable(color: Color): Drawable {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val drawable = TextureRegionDrawable(TextureRegion(Texture(pixmap)))
        pixmap.dispose()
        return drawable
    }
    
    private fun createSkin(): Skin {
        // TODO: Создать полноценный скин
        return Skin(Gdx.files.internal("skin/uiskin.json"))
    }
    
    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }
    
    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        stage.act(delta)
        stage.draw()
    }
    
    override fun dispose() {
        stage.dispose()
    }
}

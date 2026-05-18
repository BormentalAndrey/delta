// app/src/main/java/com/kakdela/p2p/model/Position.kt
package com.kakdela.p2p.model

data class Position(val col: Int, val row: Int) {
    init {
        require(col in 0..7) { "Column must be 0-7" }
        require(row in 0..7) { "Row must be 0-7" }
    }
    
    override fun toString(): String {
        val colChar = ('a' + col)
        return "$colChar${row + 1}"
    }
    
    companion object {
        fun fromString(str: String): Position {
            require(str.length == 2) { "Position string must be 2 characters" }
            val col = str[0] - 'a'
            val row = str[1] - '1'
            return Position(col, row)
        }
    }
}

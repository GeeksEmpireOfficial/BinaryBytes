package net.geekstools.numbers.GameView

import android.content.Context
import androidx.preference.PreferenceManager
import java.util.*

class GameLogic(private val mContext: Context, private val mView: GamePlayView) {
    var gameState = GAME_NORMAL
    var lastGameState = GAME_NORMAL
    private var bufferGameState = GAME_NORMAL
    internal val numSquaresX = 4
    internal val numSquaresY = 4
    lateinit var grid: Grid
    lateinit var aGrid: AnimationGrid
    var canUndo: Boolean = false
    var score: Long = 0
    var highScore: Long = 0
    var lastScore: Long = 0
    private var bufferScore: Long = 0

    val isActive: Boolean
        get() = !(gameWon() || gameLost())

    init {
        endingMaxValue = Math.pow(2.0, (mView.numCellTypes - 1).toDouble()).toInt()

        grid = Grid(numSquaresX, numSquaresY)
    }

    fun newGame() {
        if (grid == null) {
            grid = Grid(numSquaresX, numSquaresY)
        } else {
            grid.clearGrid()
        }
        aGrid = AnimationGrid(numSquaresX, numSquaresY)
        highScore = getHighScores()
        if (score >= highScore) {
            highScore = score
            recordHighScore()
        }
        score = 0
        gameState = GAME_NORMAL
        addStartTiles()
        mView.refreshLastTime = true
        mView.resyncTime()
        mView.invalidate()
    }

    private fun addStartTiles() {
        val startTiles = 1
        for (xx in 0 until startTiles) {
            this.addRandomTile()
        }
    }

    private fun addRandomTile() {
        if (grid.isCellsAvailable()) {
            val value = if (Math.random() < 0.9) 2 else 4
            val tile = Tile(grid.randomAvailableCell()!!, value)
            spawnTile(tile)
        }
    }

    private fun spawnTile(tile: Tile) {
        grid.insertTile(tile)
        aGrid.startAnimation(tile.x, tile.y, SPAWN_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null) //Direction: -1 = EXPANDING
    }

    private fun recordHighScore() {
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor = settings.edit()
        editor.putLong(HIGH_SCORE, highScore)
        editor.apply()
    }

    private fun getHighScores(): Long {
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        return settings.getLong(HIGH_SCORE, -1)
    }

    private fun prepareTiles() {
        for (array in grid.field) {
            for (tile in array) {
                if (grid.isCellOccupied(tile)) {
                    tile!!.mergedFrom = null
                }
            }
        }
    }

    private fun moveTile(tile: Tile, cell: Cell) {
        grid.field[tile.x][tile.y] = null
        grid.field[cell.x][cell.y] = tile
        tile.updatePosition(cell)
    }

    private fun saveUndoState() {
        grid.saveTiles()
        canUndo = true
        lastScore = bufferScore
        lastGameState = bufferGameState
    }

    private fun prepareUndoState() {
        grid.prepareSaveTiles()
        bufferScore = score
        bufferGameState = gameState
    }

    fun revertUndoState() {
        if (canUndo) {
            canUndo = false
            aGrid.cancelAnimations()
            grid.revertTiles()
            score = lastScore
            gameState = lastGameState
            mView.refreshLastTime = true
            mView.invalidate()
        }
    }

    fun gameWon(): Boolean {
        return gameState > 0 && gameState % 2 != 0
    }

    fun gameLost(): Boolean {
        return gameState == GAME_LOST
    }

    fun move(direction: Int) {
        aGrid.cancelAnimations()
        // 0: up, 1: right, 2: down, 3: left
        if (!isActive) {
            return
        }
        prepareUndoState()
        val vector = getVector(direction)
        val traversalsX = buildTraversalsX(vector)
        val traversalsY = buildTraversalsY(vector)
        var moved = false

        prepareTiles()

        for (xx in traversalsX) {
            for (yy in traversalsY) {
                val cell = Cell(xx, yy)
                val tile = grid.getCellContent(cell)

                if (tile != null) {
                    val positions = findFarthestPosition(cell, vector)
                    val next = grid.getCellContent(positions[1])

                    if (next != null && next.value == tile.value && next.mergedFrom == null) {
                        val merged = Tile(positions[1], tile.value * 2)
                        val temp = arrayOf(tile, next)
                        merged.mergedFrom = temp

                        grid.insertTile(merged)
                        grid.removeTile(tile)

                        // Converge the two tiles' positions
                        tile.updatePosition(positions[1])

                        val extras = intArrayOf(xx, yy)
                        aGrid.startAnimation(merged.x, merged.y, MOVE_ANIMATION,
                                MOVE_ANIMATION_TIME, 0, extras) //Direction: 0 = MOVING MERGED
                        aGrid.startAnimation(merged.x, merged.y, MERGE_ANIMATION,
                                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null)

                        // Update the score
                        score = score + merged.value
                        highScore = Math.max(score, highScore)

                        // The mighty 2048 tile
                        if (merged.value >= winValue() && !gameWon()) {
                            gameState = gameState + GAME_WIN // Set win state
                            endGame()
                        }
                    } else {
                        moveTile(tile, positions[0])
                        val extras = intArrayOf(xx, yy, 0)
                        aGrid.startAnimation(positions[0].x, positions[0].y, MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras) //Direction: 1 = MOVING NO MERGE
                    }

                    if (!positionsEqual(cell, tile)) {
                        moved = true
                    }
                }
            }
        }

        if (moved) {
            saveUndoState()
            addRandomTile()
            checkLose()
        } else {
            if (direction == 1) {
            }
        }
        mView.resyncTime()
        mView.invalidate()
    }

    private fun checkLose() {
        if (!movesAvailable() && !gameWon()) {
            gameState = GAME_LOST
            endGame()
        }
    }

    private fun endGame() {
        aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null)
        if (score >= highScore) {
            highScore = score
            recordHighScore()
        }
    }

    private fun getVector(direction: Int): Cell {
        val map = arrayOf(Cell(0, -1), // up
                Cell(1, 0), // right
                Cell(0, 1), // down
                Cell(-1, 0)  // left
        )
        return map[direction]
    }

    private fun buildTraversalsX(vector: Cell): List<Int> {
        val traversals = ArrayList<Int>()

        for (xx in 0 until numSquaresX) {
            traversals.add(xx)
        }
        if (vector.x == 1) {
            Collections.reverse(traversals)
        }

        return traversals
    }

    private fun buildTraversalsY(vector: Cell): List<Int> {
        val traversals = ArrayList<Int>()

        for (xx in 0 until numSquaresY) {
            traversals.add(xx)
        }
        if (vector.y == 1) {
            Collections.reverse(traversals)
        }

        return traversals
    }

    private fun findFarthestPosition(cell: Cell, vector: Cell): Array<Cell> {
        var previous: Cell
        var nextCell = Cell(cell.x, cell.y)
        do {
            previous = nextCell
            nextCell = Cell(previous.x + vector.x,
                    previous.y + vector.y)
        } while (grid.isCellWithinBounds(nextCell) && grid.isCellAvailable(nextCell))

        return arrayOf(previous, nextCell)
    }

    private fun movesAvailable(): Boolean {
        return grid.isCellsAvailable() || tileMatchesAvailable()
    }

    private fun tileMatchesAvailable(): Boolean {
        var tile: Tile?

        for (xx in 0 until numSquaresX) {
            for (yy in 0 until numSquaresY) {
                tile = grid.getCellContent(Cell(xx, yy))

                if (tile != null) {
                    for (direction in 0..3) {
                        val vector = getVector(direction)
                        val cell = Cell(xx + vector.x, yy + vector.y)

                        val other = grid.getCellContent(cell)

                        if (other != null && other.value == tile.value) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    private fun positionsEqual(first: Cell, second: Cell): Boolean {
        return first.x == second.x && first.y == second.y
    }

    private fun winValue(): Int {
        return if (!canContinue()) {
            endingMaxValue
        } else {
            startingMaxValue
        }
    }

    fun setEndlessMode() {
        gameState = GAME_ENDLESS
        mView.invalidate()
        mView.refreshLastTime = true
    }

    fun canContinue(): Boolean {
        return !(gameState == GAME_ENDLESS || gameState == GAME_ENDLESS_WON)
    }

    companion object {

        val SPAWN_ANIMATION = -1
        val MOVE_ANIMATION = 0
        val MERGE_ANIMATION = 1

        val FADE_GLOBAL_ANIMATION = 0
        private val MOVE_ANIMATION_TIME = GamePlayView.BASE_ANIMATION_TIME.toLong()
        private val SPAWN_ANIMATION_TIME = GamePlayView.BASE_ANIMATION_TIME.toLong()
        private val NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME
        private val NOTIFICATION_ANIMATION_TIME = (GamePlayView.BASE_ANIMATION_TIME * 5).toLong()
        private val startingMaxValue = 2048
        //Odd state = game is not active
        //Even state = game is active
        //Win state = active state + 1
        private val GAME_WIN = 1
        private val GAME_LOST = -1
        private val GAME_NORMAL = 0
        private val GAME_ENDLESS = 2
        private val GAME_ENDLESS_WON = 3
        private val HIGH_SCORE = "high score"
        private var endingMaxValue: Int = 0
    }
}

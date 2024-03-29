package net.geekstools.numbers.GameView

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import net.geekstools.numbers.R
import net.geekstools.trexrunner.Util.AdsInterface

class GamePlayView(context: Context, activity: Activity, adsInterface: AdsInterface) : View(context) {

    val numCellTypes = 21
    private val bitmapCell = arrayOfNulls<BitmapDrawable>(numCellTypes)
    val gameLogic: GameLogic

    //Internal variables
    private val paint = Paint()
    var hasSaveState = false
    var continueButtonEnabled = false
    var startingX: Int = 0
    var startingY: Int = 0
    var endingX: Int = 0
    var endingY: Int = 0

    //Icon variables
    var sYIcons: Int = 0
    var sXNewGame: Int = 0
    var sXUndo: Int = 0
    var iconSize: Int = 0

    //Misc
    var refreshLastTime = true

    //Timing
    private var lastFPSTime = System.nanoTime()

    //Text
    private var titleTextSize: Float = 0.toFloat()
    private var bodyTextSize: Float = 0.toFloat()
    private var headerTextSize: Float = 0.toFloat()
    private var instructionsTextSize: Float = 0.toFloat()
    private var gameOverTextSize: Float = 0.toFloat()

    //Layout variables
    private var cellSize = 0
    private var textSize = 0f
    private var cellTextSize = 0f
    private var gridWidth = 0
    private var textPaddingSize: Int = 0
    private var iconPaddingSize: Int = 0

    //Assets
    private var backgroundRectangle: Drawable? = null
    private var backgroundRectangleNew: Drawable? = null
    private var backgroundRectangleUndo: Drawable? = null
    private var lightUpRectangle: Drawable? = null
    private var fadeRectangle: Drawable? = null
    private var background: Bitmap? = null
    private var loseGameOverlay: BitmapDrawable? = null
    private var winGameContinueOverlay: BitmapDrawable? = null
    private var winGameFinalOverlay: BitmapDrawable? = null

    //Text variables
    private var sYAll: Int = 0
    private var titleStartYAll: Int = 0
    private var bodyStartYAll: Int = 0
    private var eYAll: Int = 0
    private var titleWidthHighScore: Int = 0
    private var titleWidthScore: Int = 0

    private val cellRectangleIds: IntArray
        get() {
            val cellRectangleIds = IntArray(numCellTypes)
            cellRectangleIds[0] = R.drawable.cell_rectangle
            cellRectangleIds[1] = R.drawable.cell_rectangle_2
            cellRectangleIds[2] = R.drawable.cell_rectangle_4
            cellRectangleIds[3] = R.drawable.cell_rectangle_8
            cellRectangleIds[4] = R.drawable.cell_rectangle_16
            cellRectangleIds[5] = R.drawable.cell_rectangle_32
            cellRectangleIds[6] = R.drawable.cell_rectangle_64
            cellRectangleIds[7] = R.drawable.cell_rectangle_128
            cellRectangleIds[8] = R.drawable.cell_rectangle_256
            cellRectangleIds[9] = R.drawable.cell_rectangle_512
            cellRectangleIds[10] = R.drawable.cell_rectangle_1024
            cellRectangleIds[11] = R.drawable.cell_rectangle_2048
            for (xx in 12 until cellRectangleIds.size) {
                cellRectangleIds[xx] = R.drawable.cell_rectangle_4096
            }
            return cellRectangleIds
        }

    init {

        //Loading resources
        gameLogic = GameLogic(context, this, adsInterface)
        try {
            //Getting assets
            backgroundRectangle = getDrawable(R.drawable.background_rectangle)
            backgroundRectangleNew = getDrawable(R.drawable.background_rectangle_new)
            backgroundRectangleUndo = getDrawable(R.drawable.background_rectangle_undo)
            lightUpRectangle = getDrawable(R.drawable.light_up_rectangle)
            fadeRectangle = getDrawable(R.drawable.fade_rectangle)
            this.setBackgroundColor(context.getColor(R.color.light))
            val font = Typeface.createFromAsset(resources.assets, "clear_sans_bold.ttf")
            paint.typeface = font
            paint.isAntiAlias = true

        } catch (e: Exception) {
            Log.e(TAG, "Error getting assets?", e)
        }

        setOnTouchListener(InputListener(this, activity))
        gameLogic.newGame()
    }

    public override fun onDraw(canvas: Canvas) {
        //Reset the transparency of the screen

        canvas.drawBitmap(background!!, 0f, 0f, paint)

        drawScoreText(canvas)

        if (!gameLogic.isActive && !gameLogic.aGrid.isAnimationActive()) {
            drawNewGameButton(canvas, true)
        }

        drawCells(canvas)

        if (!gameLogic.isActive) {
            drawEndGameState(canvas)
        }

        if (!gameLogic.canContinue()) {
            drawEndlessText(canvas)
        }

        //Refresh the screen if there is still an animation running
        if (gameLogic.aGrid.isAnimationActive()) {
            invalidate(startingX, startingY, endingX, endingY)
            tick()
            //Refresh one last time on game end.
        } else if (!gameLogic.isActive && refreshLastTime) {
            invalidate()
            refreshLastTime = false
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(width, height, oldW, oldH)
        getLayout(width, height)
        createBitmapCells()
        createBackgroundBitmap(width, height)
        createOverlays()
    }

    private fun getDrawable(resId: Int): Drawable {
        return context.getDrawable(resId)!!
    }

    private fun drawDrawable(canvas: Canvas, draw: Drawable, startingX: Int, startingY: Int, endingX: Int, endingY: Int) {
        draw.setBounds(startingX, startingY, endingX, endingY)
        draw.draw(canvas)
    }

    private fun drawCellText(canvas: Canvas, value: Int) {
        val textShiftY = centerText()
        paint.color = context.getColor(R.color.dark)

        canvas.drawText("" + value, (cellSize / 2).toFloat(), (cellSize / 2 - textShiftY).toFloat(), paint)
    }

    private fun drawScoreText(canvas: Canvas) {
        //Drawing the score text: Ver 2
        paint.textSize = bodyTextSize
        paint.textAlign = Paint.Align.CENTER

        val bodyWidthHighScore = paint.measureText("" + gameLogic.highScore).toInt()
        val bodyWidthScore = paint.measureText("" + gameLogic.score).toInt()

        val textWidthHighScore = Math.max(titleWidthHighScore, bodyWidthHighScore) + textPaddingSize * 2
        val textWidthScore = Math.max(titleWidthScore, bodyWidthScore) + textPaddingSize * 2

        val textMiddleHighScore = textWidthHighScore / 2
        val textMiddleScore = textWidthScore / 2

        val eXHighScore = sXNewGame - iconSize * 3
        val sXHighScore = eXHighScore - textWidthHighScore

        val eXScore = sXHighScore - textPaddingSize
        val sXScore = eXScore - textWidthScore

        //Best
        backgroundRectangle!!.setBounds(sXHighScore - 3, sYAll - 5, eXHighScore + 3, eYAll + 3)
        backgroundRectangle!!.draw(canvas)
        paint.textSize = titleTextSize + 5
        paint.color = context.getColor(R.color.light)
        canvas.drawText(context.getString(R.string.high_score),
                (sXHighScore + textMiddleHighScore).toFloat(), titleStartYAll.toFloat(), paint)
        paint.textSize = bodyTextSize
        paint.color = context.getColor(R.color.light)
        canvas.drawText(gameLogic.highScore.toString(), (sXHighScore + textMiddleHighScore).toFloat(), bodyStartYAll.toFloat(), paint)

        //Score
        backgroundRectangle!!.setBounds(sXScore - 13, sYAll - 5, eXScore - 1, eYAll + 3)
        backgroundRectangle!!.draw(canvas)
        paint.textSize = titleTextSize + 5
        paint.color = context.getColor(R.color.light)
        canvas.drawText(context.getString(R.string.score),
                (sXScore + textMiddleScore - 7).toFloat(), titleStartYAll.toFloat(), paint)
        paint.textSize = bodyTextSize
        paint.color = context.getColor(R.color.light)
        canvas.drawText(gameLogic.score.toString(), (sXScore + textMiddleScore - 7).toFloat(), bodyStartYAll.toFloat(), paint)
    }

    private fun drawNewGameButton(canvas: Canvas, lightUp: Boolean) {
        if (lightUp) {
            drawDrawable(canvas,
                    lightUpRectangle!!,
                    sXNewGame - iconSize * 3,
                    sYIcons - 3,
                    sXNewGame,
                    sYIcons + iconSize + 3
            )
        } else {
            drawDrawable(canvas,
                    backgroundRectangleNew!!,
                    sXNewGame - iconSize * 3,
                    sYIcons - 3,
                    sXNewGame,
                    sYIcons + iconSize + 3
            )
        }

        drawDrawable(canvas,
                getDrawable(R.drawable.ic_action_refresh),
                sXNewGame - iconSize * 3 + iconPaddingSize * 5 / 2,
                sYIcons + iconPaddingSize - 3,
                sXNewGame - iconPaddingSize * 5 / 2,
                sYIcons + iconSize - iconPaddingSize + 3
        )
    }

    private fun drawUndoButton(canvas: Canvas) {

        drawDrawable(canvas,
                backgroundRectangleUndo!!,
                sXUndo,
                sYIcons - 3,
                sXUndo + iconSize * 3,
                sYIcons + iconSize + 3
        )

        drawDrawable(canvas,
                getDrawable(R.drawable.ic_action_undo),
                sXUndo + iconPaddingSize * 3,
                sYIcons + iconPaddingSize - 1,
                sXUndo + iconSize * 3 - iconPaddingSize * 3,
                sYIcons + iconSize - iconPaddingSize + 1
        )
    }

    private fun drawHeader(canvas: Canvas) {
        paint.textSize = headerTextSize
        paint.color = context.getColor(R.color.red)
        paint.textAlign = Paint.Align.LEFT
        val textShiftY = centerText() * 2
        val headerStartY = sYAll - textShiftY
        canvas.drawText(context.getString(R.string.app_name), startingX.toFloat(), headerStartY.toFloat(), paint)
    }

    private fun drawInstructions(canvas: Canvas) {
        paint.textSize = instructionsTextSize
        paint.textAlign = Paint.Align.LEFT
        val textShiftY = centerText() * 2
        canvas.drawText(context.getString(R.string.app_name),
                startingX.toFloat(), (endingY - textShiftY + textPaddingSize).toFloat(), paint)
    }

    private fun drawBackground(canvas: Canvas) {
        drawDrawable(canvas, backgroundRectangle!!, startingX, startingY, endingX, endingY)
    }

    //Renders the set of 16 background squares.
    private fun drawBackgroundGrid(canvas: Canvas) {
        val resources = resources
        val backgroundCell = getDrawable(R.drawable.cell_rectangle)
        // Outputting the game grid
        for (xx in 0 until gameLogic.numSquaresX) {
            for (yy in 0 until gameLogic.numSquaresY) {
                val sX = startingX + gridWidth + (cellSize + gridWidth) * xx
                val eX = sX + cellSize
                val sY = startingY + gridWidth + (cellSize + gridWidth) * yy
                val eY = sY + cellSize

                drawDrawable(canvas, backgroundCell, sX, sY, eX, eY)
            }
        }
    }

    private fun drawCells(canvas: Canvas) {
        paint.textSize = textSize
        paint.textAlign = Paint.Align.CENTER
        // Outputting the individual cells
        for (xx in 0 until gameLogic.numSquaresX) {
            for (yy in 0 until gameLogic.numSquaresY) {
                val sX = startingX + gridWidth + (cellSize + gridWidth) * xx
                val eX = sX + cellSize
                val sY = startingY + gridWidth + (cellSize + gridWidth) * yy
                val eY = sY + cellSize

                val currentTile = gameLogic.grid!!.getCellContent(xx, yy)
                if (currentTile != null) {
                    //Get and represent the value of the tile
                    val value = currentTile.value
                    val index = log2(value)

                    //Check for any active animations
                    val aArray = gameLogic.aGrid.getAnimationCell(xx, yy)
                    var animated = false
                    for (i in aArray.indices.reversed()) {
                        val aCell = aArray[i]
                        //If this animation is not active, skip it
                        if (aCell.animationType == GameLogic.SPAWN_ANIMATION) {
                            animated = true
                        }
                        if (!aCell.isActive) {
                            continue
                        }

                        if (aCell.animationType == GameLogic.SPAWN_ANIMATION) { // Spawning animation
                            val percentDone = aCell.percentageDone
                            val textScaleSize = percentDone.toFloat()
                            paint.textSize = textSize * textScaleSize

                            val cellScaleSize = cellSize / 2 * (1 - textScaleSize)
                            bitmapCell[index]!!.setBounds((sX + cellScaleSize).toInt(), (sY + cellScaleSize).toInt(), (eX - cellScaleSize).toInt(), (eY - cellScaleSize).toInt())
                            bitmapCell[index]!!.draw(canvas)
                        } else if (aCell.animationType == GameLogic.MERGE_ANIMATION) { // Merging Animation
                            val percentDone = aCell.percentageDone
                            val textScaleSize = (1.0 + INITIAL_VELOCITY * percentDone
                                    + MERGING_ACCELERATION.toDouble() * percentDone * percentDone / 2).toFloat()
                            paint.textSize = textSize * textScaleSize

                            val cellScaleSize = cellSize / 2 * (1 - textScaleSize)
                            bitmapCell[index]!!.setBounds((sX + cellScaleSize).toInt(), (sY + cellScaleSize).toInt(), (eX - cellScaleSize).toInt(), (eY - cellScaleSize).toInt())
                            bitmapCell[index]!!.draw(canvas)
                        } else if (aCell.animationType == GameLogic.MOVE_ANIMATION) {  // Moving animation
                            val percentDone = aCell.percentageDone
                            var tempIndex = index
                            if (aArray.size >= 2) {
                                tempIndex = tempIndex - 1
                            }
                            val previousX = aCell.extras?.get(0)
                            val previousY = aCell.extras?.get(1)
                            val currentX = currentTile.x
                            val currentY = currentTile.y
                            val dX = ((currentX - previousX!!).toDouble() * (cellSize + gridWidth).toDouble() * (percentDone - 1) * 1.0).toInt()
                            val dY = ((currentY - previousY!!).toDouble() * (cellSize + gridWidth).toDouble() * (percentDone - 1) * 1.0).toInt()
                            bitmapCell[tempIndex]!!.setBounds(sX + dX, sY + dY, eX + dX, eY + dY)
                            bitmapCell[tempIndex]!!.draw(canvas)
                        }
                        animated = true
                    }

                    //No active animations? Just draw the cell
                    if (!animated) {
                        bitmapCell[index]!!.setBounds(sX, sY, eX, eY)
                        bitmapCell[index]!!.draw(canvas)
                    }
                }
            }
        }
    }

    private fun drawEndGameState(canvas: Canvas) {
        var alphaChange = 1.0
        continueButtonEnabled = false
        for (animation in gameLogic.aGrid.globalAnimation) {
            if (animation.animationType == GameLogic.FADE_GLOBAL_ANIMATION) {
                alphaChange = animation.percentageDone
            }
        }
        var displayOverlay: BitmapDrawable? = null
        if (gameLogic.gameWon()) {
            if (gameLogic.canContinue()) {
                continueButtonEnabled = true
                displayOverlay = winGameContinueOverlay
            } else {
                displayOverlay = winGameFinalOverlay
            }
        } else if (gameLogic.gameLost()) {
            displayOverlay = loseGameOverlay
        }

        if (displayOverlay != null) {
            displayOverlay.setBounds(startingX, startingY, endingX, endingY)
            displayOverlay.alpha = (255 * alphaChange).toInt()
            displayOverlay.draw(canvas)
        }
    }

    private fun drawEndlessText(canvas: Canvas) {
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = bodyTextSize
        paint.color = context.getColor(R.color.red)
        canvas.drawText(context.getString(R.string.endless), startingX.toFloat(), (sYIcons - centerText() * 2).toFloat(), paint)
    }

    private fun createEndGameStates(canvas: Canvas, win: Boolean, showButton: Boolean) {
        val width = endingX - startingX
        val length = endingY - startingY
        val middleX = width / 2
        val middleY = length / 2
        if (win) {
            lightUpRectangle!!.alpha = 127
            drawDrawable(canvas, lightUpRectangle!!, 0, 0, width, length)
            lightUpRectangle!!.alpha = 255
            paint.color = context.getColor(R.color.dark)
            paint.alpha = 255
            paint.textSize = gameOverTextSize
            paint.textAlign = Paint.Align.CENTER
            val textBottom = middleY - centerText()
            canvas.drawText(context.getString(R.string.you_win), middleX.toFloat(), textBottom.toFloat(), paint)
            paint.textSize = bodyTextSize
            val text = if (showButton) context.getString(R.string.go_on) else context.getString(R.string.for_now)
            canvas.drawText(text, middleX.toFloat(), (textBottom + textPaddingSize * 2 - centerText() * 2).toFloat(), paint)
        } else {
            fadeRectangle!!.alpha = 127
            drawDrawable(canvas, fadeRectangle!!, 0, 0, width, length)
            fadeRectangle!!.alpha = 255
            paint.color = context.getColor(R.color.dark)
            paint.alpha = 255
            paint.textSize = gameOverTextSize
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(context.getString(R.string.game_over), middleX.toFloat(), (middleY - centerText()).toFloat(), paint)
        }
    }

    private fun createBackgroundBitmap(width: Int, height: Int) {
        background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(background!!)
        drawNewGameButton(canvas, false)
        drawUndoButton(canvas)
        drawBackground(canvas)
        drawBackgroundGrid(canvas)
    }

    private fun createBitmapCells() {
        val resources = resources
        val cellRectangleIds = cellRectangleIds
        paint.textAlign = Paint.Align.CENTER
        for (xx in 1 until bitmapCell.size) {
            val value = Math.pow(2.0, xx.toDouble()).toInt()
            paint.textSize = cellTextSize
            val tempTextSize = cellTextSize * cellSize.toFloat() * 0.9f / Math.max(cellSize * 0.9f, paint.measureText(value.toString()))
            paint.textSize = tempTextSize
            val bitmap = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawDrawable(canvas, getDrawable(cellRectangleIds[xx]), 0, 0, cellSize, cellSize)
            drawCellText(canvas, value)
            bitmapCell[xx] = BitmapDrawable(resources, bitmap)
        }
    }

    private fun createOverlays() {
        val resources = resources
        //Initialize overlays
        var bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)
        createEndGameStates(canvas, true, true)
        winGameContinueOverlay = BitmapDrawable(resources, bitmap)
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
        createEndGameStates(canvas, true, false)
        winGameFinalOverlay = BitmapDrawable(resources, bitmap)
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
        createEndGameStates(canvas, false, false)
        loseGameOverlay = BitmapDrawable(resources, bitmap)
    }

    private fun tick() {
        val currentTime = System.nanoTime()
        gameLogic.aGrid.tickAll(currentTime - lastFPSTime)
        lastFPSTime = currentTime
    }

    fun resyncTime() {
        lastFPSTime = System.nanoTime()
    }

    private fun getLayout(width: Int, height: Int) {
        cellSize = Math.min(width / (gameLogic.numSquaresX + 1), height / (gameLogic.numSquaresY + 3))
        gridWidth = cellSize / 7
        val screenMiddleX = width / 2
        val screenMiddleY = height / 2
        val boardMiddleY = screenMiddleY + cellSize / 2
        iconSize = cellSize / 2

        //Grid Dimensions
        val halfNumSquaresX = gameLogic.numSquaresX / 2.0
        val halfNumSquaresY = gameLogic.numSquaresY / 2.0
        startingX = (screenMiddleX.toDouble() - (cellSize + gridWidth) * halfNumSquaresX - (gridWidth / 2).toDouble()).toInt()
        endingX = (screenMiddleX.toDouble() + (cellSize + gridWidth) * halfNumSquaresX + (gridWidth / 2).toDouble()).toInt()
        startingY = (boardMiddleY.toDouble() - (cellSize + gridWidth) * halfNumSquaresY - (gridWidth / 2).toDouble()).toInt()
        endingY = (boardMiddleY.toDouble() + (cellSize + gridWidth) * halfNumSquaresY + (gridWidth / 2).toDouble()).toInt()

        val widthWithPadding = (endingX - startingX).toFloat()

        // Text Dimensions
        paint.textSize = cellSize.toFloat()
        textSize = cellSize * cellSize / Math.max(cellSize.toFloat(), paint.measureText("0000"))

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 1000f
        instructionsTextSize = Math.min(
                1000f * (widthWithPadding / paint.measureText(context.getString(R.string.app_name))),
                textSize / 1.5f
        )
        gameOverTextSize = Math.min(
                Math.min(
                        1000f * ((widthWithPadding - gridWidth * 2) / paint.measureText(context.getString(R.string.game_over))),
                        textSize * 2
                ),
                1000f * ((widthWithPadding - gridWidth * 2) / paint.measureText(context.getString(R.string.you_win)))
        )

        paint.textSize = cellSize.toFloat()
        cellTextSize = textSize
        titleTextSize = textSize / 2
        bodyTextSize = (textSize / 1.5).toInt().toFloat()
        headerTextSize = textSize * 2
        textPaddingSize = (textSize / 5).toInt()
        iconPaddingSize = (textSize / 5).toInt()

        paint.textSize = titleTextSize

        var textShiftYAll = centerText()
        //static variables
        sYAll = (startingY - cellSize * 1.5).toInt()
        titleStartYAll = (sYAll.toFloat() + textPaddingSize.toFloat() + titleTextSize / 2 - textShiftYAll).toInt()
        bodyStartYAll = (titleStartYAll.toFloat() + textPaddingSize.toFloat() + titleTextSize / 2 + bodyTextSize / 2).toInt()

        titleWidthHighScore = paint.measureText(context.getString(R.string.high_score)).toInt()
        titleWidthScore = paint.measureText(context.getString(R.string.score)).toInt()
        paint.textSize = bodyTextSize
        textShiftYAll = centerText()
        eYAll = (bodyStartYAll.toFloat() + textShiftYAll.toFloat() + bodyTextSize / 2 + textPaddingSize.toFloat()).toInt()

        sYIcons = (startingY + eYAll) / 2 - iconSize / 2
        //sXNewGame = (endingX - iconSize);
        sXNewGame = endingX
        //sXUndo = sXNewGame - iconSize * 3 / 2 - iconPaddingSize;
        sXUndo = startingX
        resyncTime()
    }

    private fun centerText(): Int {
        return ((paint.descent() + paint.ascent()) / 2).toInt()
    }

    companion object {

        //Internal Constants
        val BASE_ANIMATION_TIME = 100000000
        private val TAG = GamePlayView::class.java.simpleName
        private val MERGING_ACCELERATION = (-0.5).toFloat()
        private val INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4

        private fun log2(n: Int): Int {
            if (n <= 0) throw IllegalArgumentException()
            return 31 - Integer.numberOfLeadingZeros(n)
        }
    }
}
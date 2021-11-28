package com.aoc4456.othellokotlin.board

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.aoc4456.othellokotlin.ai.AI
import com.aoc4456.othellokotlin.model.Cell
import com.aoc4456.othellokotlin.model.Difficulty
import com.aoc4456.othellokotlin.model.Stone
import com.aoc4456.othellokotlin.model.Turn
import com.aoc4456.othellokotlin.util.PublishLiveData
import timber.log.Timber

class BoardViewModel() : ViewModel() {

    /* 盤面の状態を２次元リストで表す */
    val cellList: MutableList<MutableList<Cell>> = mutableListOf()

    /** 今どちらの番か */
    private var nowTurn = Turn.BLACK

    /** プレイヤーの色 */
    private var playerColor = Turn.BLACK

    /** 今プレイヤーの番かどうか */
    private val isPlayerTurn: Boolean get() = nowTurn == playerColor

    /** 盤を更新したいとき */
    private val _updateBoard = PublishLiveData<Boolean>()
    val updateBoard: PublishLiveData<Boolean> = _updateBoard

    /** CPUのAI */
    private var ai = AI(Difficulty.WEAK)

    /** 入力を受け付けるか */
    private var clickable = false

    /** 黒のターンで置く場所があるか */
    private fun isCanPlayBlack(): Boolean = FlipOverUtils.getAllCellsCanPut(cellList, Turn.BLACK).isNotEmpty()

    /** 白のターンで置く場所があるか */
    private fun isCanPlayWhite(): Boolean = FlipOverUtils.getAllCellsCanPut(cellList, Turn.WHITE).isNotEmpty()

    /** ゲームを続けられるか */
    private fun isCanContinueGame() : Boolean = isCanPlayBlack() || isCanPlayWhite()

    /** 先行/後攻を決定 */
    fun decideTheOrder(isBlack: Boolean) {
        playerColor = when (isBlack) {
            true -> Turn.BLACK
            else -> Turn.WHITE
        }
        gameStart()
    }

    /** ゲーム開始 */
    fun gameStart() {
        initializeBoard()
        _updateBoard.value = true

        next()
    }

    private fun next() {
        // ゲーム自体が続けられるか
        if(!isCanContinueGame()){
            Timber.d("置ける場所がなくなったので、ゲームを終了します")
            Timber.d("黒 : ${cellList.flatten().filter { it.stone == Stone.BLACK }.size}")
            Timber.d("白 : ${cellList.flatten().filter { it.stone == Stone.WHITE }.size}")

            return
        }

        // このターンのプレイヤーがプレイできるか
        if(nowTurn == Turn.BLACK && !isCanPlayBlack()){
            Timber.d("黒の置ける場所がありません　パスします")
            nowTurn = Turn.WHITE
            next()
            return
        }

        if(nowTurn == Turn.WHITE && !isCanPlayWhite()){
            Timber.d("白の置ける場所がありません　パスします")
            nowTurn = Turn.BLACK
            next()
            return
        }

        requestPutStone()
    }

    /** プレイヤー または CPUに、石を置くように要求 */
    private fun requestPutStone() {
        if (nowTurn == Turn.BLACK) {
            if (isPlayerTurn) {
                Timber.d("黒のターンです : プレイヤーの番です")
            } else {
                Timber.d("黒のターンです : CPUの番です")
            }
        }
        if (nowTurn == Turn.WHITE) {
            if (isPlayerTurn) {
                Timber.d("白のターンです : プレイヤーの番です")
            } else {
                Timber.d("白のターンです : CPUの番です")
            }
        }

        if (isPlayerTurn) {
            clickable = true
        } else {
            clickable = false
            cpuPutStone()
        }
    }

    fun onClickCell(vertical: Int, horizontal: Int) {
        val color = when (nowTurn) {
            Turn.BLACK -> Stone.BLACK
            else -> Stone.WHITE
        }
        val cell = Cell(vertical, horizontal, color)
        val flipOverList = FlipOverUtils.getCellsToFlip(cellList, cell)
        if (flipOverList.isEmpty()) {
            Timber.d("そこには置けません $cell")
            return
        } else {
            clickable = false
            cellList[vertical][horizontal] = cell
            _updateBoard.value = true

            Handler(Looper.getMainLooper()).postDelayed({
                flipOverList.forEach {
                    cellList[it.vertical][it.horizontal] = Cell(it.vertical, it.horizontal, color)
                }
                _updateBoard.value = true
                nowTurn = when (nowTurn) {
                    Turn.BLACK -> Turn.WHITE
                    else -> Turn.BLACK
                }
                next()
            }, 750)
        }
    }

    /** 初期配置 */
    private fun initializeBoard() {
        cellList.clear()
        for (i in 0 until BOARD_SIZE) {
            cellList.add(i, mutableListOf())
            for (j in 0 until BOARD_SIZE) {
                cellList[i].add(Cell(i, j, Stone.NONE))
            }
        }
        val upperLeftPosition = BOARD_SIZE / 2 - 1
        cellList[upperLeftPosition][upperLeftPosition].stone = Stone.WHITE
        cellList[upperLeftPosition + 1][upperLeftPosition + 1].stone = Stone.WHITE
        cellList[upperLeftPosition + 1][upperLeftPosition].stone = Stone.BLACK
        cellList[upperLeftPosition][upperLeftPosition + 1].stone = Stone.BLACK
    }

    /** CPUに石を置かせる */
    private fun cpuPutStone() {
        Handler(Looper.getMainLooper()).postDelayed({
            val position = ai.getNextPosition(cellList, nowTurn)
            Timber.d("CPUが置く場所を決めました : ${position.first} / ${position.second}")
            onClickCell(position.first, position.second)
        }, 1500)
    }
}

const val BOARD_SIZE = 8

package minesweeper
import kotlin.random.Random

data class Tile(
    val location: Pair<Int, Int> = Pair(0, 0),
    var triggered: Boolean = false, // отобразить мину на поле
    var armed: Boolean = false,
    var flagged: Boolean = false,
    var opened: Boolean = false,
    var minesNearby: Int = 0,
) {
    fun mineNear() = minesNearby++

    fun flag() { flagged = !flagged }

     override fun toString(): String = when {
        triggered -> "X" // отобразить мину на поле
        flagged -> "*"
        !opened -> "."
        minesNearby > 0 -> minesNearby.toString()
        else -> "/"
    }
}

data class MineBoard(val rows: Int, val columns: Int, val mines: Int) {
    private val fieldSize = rows * columns
    var endOfGame: String = ""
    var countOfOpenedTiles = 0

    private val mineField = Array(rows) { row -> Array(columns) { column -> Tile(Pair(row, column)) } }

    operator fun get(pair: Pair<Int, Int>) = runCatching { mineField[pair.first][pair.second] }.getOrNull()

    private fun generateMines(startTile: Pair<Int, Int>) {
        //создаем временный список координат, чтобы по этим координатам прописать мины в основной список
        val coordinates = List(rows) { row -> List(columns) {  column -> Pair(row, column) } }.flatten().toMutableList()
        coordinates.remove(startTile) // удаляем из списка координат стартовую плитку, чтобы она не была миной
        //перемешали список с координатами, взяли с начала списка кол-во координат по кол-ву мин
        // и каждую координату (мину) пометили что это мина и добавили подсказку вокруг
        coordinates.shuffled().take(mines).forEach {location ->
            this[location]?.let { tile ->
                tile.armed = true           // отмечаем что в плитке мина /mineField[location].armed
                insertHintsAroundTile(tile) // вставляем подсказки вокруг плитки /insertHints..(mineField[location])
            }
        }
    }

    private fun insertHintsAroundTile(tile: Tile) = with(tile) {
        getTilesAroundTile(location).forEach{it.mineNear()} //location - координаты текущего экземпляра класса Tile
    }

    //Получает координаты плитки как пару проходит по плиткам вокруг этих координат и
    //возвращает набор конкретных плиток вокруг как набор экземпляров класса Tile, а не просто координаты !!!
    private fun getTilesAroundTile(coordinate: Pair<Int, Int>) = with(this) {
        val tilesAround = mutableSetOf<Tile>() // создаем пустую коллекцию класса Tile, чтобы потом вернуть координаты
        val (x, y) = coordinate // назначаем координаты из полученной пары
        val rowsArroundTile = (x - 1..x + 1)
        val columnsArroundTile = (y - 1..y + 1)
        rowsArroundTile.forEach { row ->
            columnsArroundTile.forEach { column ->  // проход по всем плиткам вокруг и центра
                if (row in 0 until rows && column in 0 until columns) { // если координаты в пределах поля
                    // добавляем экземпляр класс MineBoard в коллекцию с типом класс Tile
                    this[Pair(row, column)]?.let { tilesAround.add(it) }
                }
            }
        }
        tilesAround//возвращаем коллекцию экземпляров класса Tile
    }

    // функция открытия плиток
    fun open (tile: Tile) {
        with(tile) {
            if (countOfOpenedTiles == 0) generateMines(location)
            if (opened) return // если плитка открыта прервать функцию
            opened = true   // открыть плитку без условий
            countOfOpenedTiles++ // увеличить счетчик открытых плиток
            if (countOfOpenedTiles == (fieldSize - mines)) endOfGame = "win"
            if (armed) endOfGame = "lose"
            flagged = false  // сбросить отметку *
            //если это пустая плитка то проходит плитки вокруг и запускает fun open заново для каждой плитки вокруг
            if (minesNearby == 0) getTilesAroundTile(location).forEach { open(it) }
        }
    }

   //отобразить все мины на поле при game over
    fun revealMines() { // проходит по всем плиткам и если есть мина отображает ее
        for (row in mineField) {
            for (tile in row) {
                if (tile.armed) {
                    tile.triggered = true // отобразить мину
                }
            }
        }
    }

    fun printField() {
        val topAndBottomm = "-|${"-".repeat(columns)}|"
        println(" |${(1..columns).joinToString("")}|")
        println(topAndBottomm)
        for (row in mineField.indices) {
            print("${row + 1}|")
            for (column in mineField[row].indices) {
                print(mineField[row][column])
            }
            print("|\n")
        }
        println(topAndBottomm)
    }
}

//класс поведения пользователя для проверки ввода координат и действий, а так же функции проставления отметки *
class Player(private val mineField: MineBoard, val flaggedMines: MutableSet<Pair<Int, Int>> = mutableSetOf()) {

    //получает и возвращает от игрока координаты и действия
    fun getUserAction(): Pair<Pair<Int, Int>, String> {
        "Set/unset mine marks or claim a cell as free: ".let(::println)
        val input = readln().split(" ")
        val result = checkUserInputAction(input)
        return result ?: getUserAction() //если пользователь ввел что-то не то, запускает еще раз функцию
    }

    //проверяет координаты и действия пользователя и возвращает их
    private fun checkUserInputAction(input: List<String>) = when (input.size) {
        3 -> { // если пользователь ввел 3 значения через пробел
            val row = input[0].toIntOrNull()?.minus(1) // проверка, что это число
            val column = input[1].toIntOrNull()?.minus(1) // проверка, что это число
            val command = input[2].lowercase()                  // действие перевести в нижний регистр
            //проверка что, введены числа и правильные команды
            if ((row != null && row > mineField.rows) || (column != null && column > mineField.columns)) {
                println("Input coordinates not in field")
                null
            }
            else if (row != null && column != null && (command == "mine" || command == "free"))
                Pair(Pair(column, row), command)
            else {
                println("Please enter the Y a space then the X a space then free or mine only")
                null
            }
        }
        else -> {
            println("Please enter the Y a space then the X a space then free or mine only")
            null
        }
    }

   // функция отметки мин если пользователь ввел mine, получает координаты от пользователя
    fun flagTile(location: Pair<Int, Int>) {
        mineField[location]?.apply {
            // если плитка в списке отмеченных мин, удаляем плитку из списка отмеченных действительных мин
            if (location in flaggedMines) flaggedMines.remove(location)
            else if (armed && !flagged) { // если на плитке мина и она не отмечена
                flaggedMines.add(location) // добавляем координаты в список отмеченных действительных мин
            }
            //если попали на цифру подсказку в открытой плитке
            if (minesNearby > 0 && opened && !armed) println("There is a number here!")
            else flag()        // отмечаем/снимаем отметку плитки
        }
    }
}

// запускает игру
class Game(private val player: Player, private val mineField: MineBoard) {

    // запускает поимку исключений для победы или проигрыша и внутри запускает игру через запуск handleAction
    fun playGame() {
        while (player.flaggedMines.size < mineField.mines && mineField.endOfGame == "") handleAction()
        if (mineField.endOfGame == "lose") {
            mineField.revealMines()
            mineField.printField()
            println("You stepped on a mine and failed!")
        }
        else if (mineField.endOfGame == "win"|| player.flaggedMines.size == mineField.mines)
            mineField.revealMines()
            mineField.printField()
            "Congratulations! You found all the mines!".let(::println)
    }

    // обработка полученных действий mine or free и их запуск
    private fun handleAction() {
        mineField.printField() // печать поля
        val action = player.getUserAction() // запуск запроса на получение строки с действием mine or free и координатами
        when (action.second) {
            "mine" -> player.flagTile(action.first) // если mine отмечаем плитку с координатами
            "free" -> mineField[action.first]?.let { mineField.open(it) } // если free открываем плитку
        }
    }
}


const val DEFAULT_ROWS = 9
const val DEFAULT_COLUMNS = 9
const val DEFAULT_FIELD_SIZE = DEFAULT_COLUMNS * DEFAULT_ROWS

fun main() {
    val mineField = MineBoard(DEFAULT_ROWS, DEFAULT_COLUMNS, userInputNumberOfMines()) // формирование поля с опросом кол-ва мин
    val player = Player(mineField) // запуск возможности получение координат и действий НО игра еще не запустилась
    Game(player, mineField).playGame() // запуск игры
}

private fun userInputNumberOfMines() = run {
    var mines: Int?
    do {
        "How many mines do you want on the field? ".let(::println)
        mines = readln().toIntOrNull() // защита от ввода сиволов со стороны пользователя
        if (mines == null) "error: incorrect input".let(::println)//вывод ошибки если пользователь ввел символы
        else if (mines > DEFAULT_FIELD_SIZE) println("error: Too many bombs for number of spaces.") //вывод ошибки если пользователь ввел мин больше размера поля
    } while (mines == null || mines > DEFAULT_FIELD_SIZE) //защита от кол-ва мин больше размера поля
    mines
}


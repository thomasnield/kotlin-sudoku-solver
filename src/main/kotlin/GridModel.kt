import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import tornadofx.*


object GridModel {

    // displays statusProperty message on success of solver
    val statusProperty = SimpleStringProperty()
    var status by statusProperty

    // the current selected algorithm implementation
    val selectedSolverProperty = SimpleObjectProperty(Solver.TOMS_BRANCH_AND_BOUND)
    var selecteSolver by selectedSolverProperty

    // generates the entire Sudoku grid of GridCells
    val grid =  (0..2).asSequence().flatMap { parentX -> (0..2).asSequence().map { parentY -> parentX to parentY } }
            .flatMap { (parentX,parentY) ->
                (0..2).asSequence().flatMap { x -> (0..2).asSequence().map { y -> x to y } }
                        .map { (x,y) -> GridCell(parentX,parentY,x,y) }
            }.toList()

    // retrieves a GridCell
    fun cellFor(parentX: Int, parentY: Int, x: Int, y: Int) = grid.first {
                it.squareX == parentX &&
                it.squareY == parentY  &&
                it.x == x &&
                it.y == y
        }

    fun solve() {
        status = null

        runAsync {
            selecteSolver.solve()
        }
    }
}

data class GridCell(val squareX: Int, val squareY: Int, val x: Int, val y: Int) {
    var value by property<Int?>()
    fun valueProperty() = getProperty(GridCell::value)

    val allRow by lazy { GridModel.grid.filter { it.y == y && it.squareY == squareY }.toSet() }
    val allColumn by lazy { GridModel.grid.filter { it.x == x && it.squareX== squareX }.toSet() }
    val allSquare by lazy { GridModel.grid.filter { it.squareY == squareY && it.squareX== squareX }.toSet() }

    val nextValidValue get() = ((value?:0)..8).asSequence().map { it + 1 }.firstOrNull { candidate ->
        allRow.all { it.value != candidate }
                && allColumn.all { it.value != candidate }
                && allSquare.all { it.value != candidate }
    }

    val candidatesLeft get() = if (value != null)
        setOf()
    else
        allRow.asSequence()
                .plus(allColumn.asSequence())
                .plus(allSquare.asSequence())
                .map { it.value }
                .filterNotNull()
                .distinct()
                .toSet().let { taken -> (1..9).asSequence().filter { it !in taken } }.toSet()

    fun increment() {
        value = nextValidValue
    }
}

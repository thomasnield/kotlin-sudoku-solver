import javafx.beans.property.SimpleStringProperty
import org.ojalgo.optimisation.Variable
import tornadofx.*


object GridModel {

    // displays status message on success of solver
    val status = SimpleStringProperty()

    // generates the entire Sudoku grid of GridCells
    val grid =  (0..2).asSequence().flatMap { parentX -> (0..2).asSequence().map { parentY -> parentX to parentY } }
            .flatMap { (parentX,parentY) ->
                (0..2).asSequence().flatMap { x -> (0..2).asSequence().map { y -> x to y } }
                        .map { (x,y) -> GridCell(parentX,parentY,x,y) }
            }.toList()

    // retrieves a GridCell
    fun cellFor(parentX: Int, parentY: Int, x: Int, y: Int) = grid.first {
                it.parentX == parentX &&
                it.parentY == parentY  &&
                it.x == x &&
                it.y == y
        }

    fun solve() {

        status.set(null)

        // if no inputs provided, provide a few random as baseline.
        val setCount = GridModel.grid.count { it.value != null }

        if (setCount < 20) {
            GridModel.grid.shuffled().take(20 - setCount).forEach { it.increment() }
        }

        // run model
        expressionsbasedmodel {

            // A VariableItem is used to pair a GridCell to each candidate number 1-9, and it's corresponding binary variable to be optimized

            data class VariableItem(val cell: GridCell, val candidateInt: Int, val variable: Variable)

            val variableItems = grid.asSequence().flatMap { cell ->
                (1..9).asSequence()
                        .map { VariableItem(cell,it,variable()) }
                        .onEach { v ->
                            when {
                                v.cell.value != null && v.cell.value != v.candidateInt -> v.variable.level(0)
                                v.cell.value != null && v.cell.value == v.candidateInt -> v.variable.level(1)
                                else -> v.variable.binary()
                            }
                        }
            }.toList()


            // constrain individual cell so its variables only add to 1
            variableItems.groupBy { it.cell }.values.forEach { grp ->
                expression(level=1) {
                    grp.forEach { set(it.variable, 1) }
                }
            }

            // constrain each row so variables of each number only add to 1
            variableItems.groupBy { TripletKey(it.cell.parentY, it.cell.y, it.candidateInt) }.values.forEach { grp ->

                expression(level=1) {
                    grp.forEach { set(it.variable,1) }
                }
            }

            // constrain each column so variables of each number only add to 1
            variableItems.groupBy { TripletKey(it.cell.parentX, it.cell.x, it.candidateInt) }.values.forEach { grp ->
                expression(level=1) {
                    grp.forEach { set(it.variable,1) }
                }
            }

            // constrain each square so variables of each number only add to 1
            variableItems.groupBy { TripletKey(it.cell.parentX, it.cell.parentY, it.candidateInt) }.values.forEach { grp ->
                expression(level=1) {
                    grp.forEach { set(it.variable,1) }
                }
            }

            // minimize number of solutions to seek
            options.iterations_suffice = 1

            // no optimization objective, but just call minimize()
            minimise().also {
                status.set(it.state.toString())
            }

            // set optimized variables back to GridCells for display
            variableItems.asSequence().filter { it.variable.value.toInt() == 1 }.forEach {
                it.cell.value = it.candidateInt
            }
        }
    }
}

data class GridCell(val parentX: Int, val parentY: Int, val x: Int, val y: Int) {
    var value by property<Int?>()
    fun valueProperty() = getProperty(GridCell::value)

    val allRow by lazy { GridModel.grid.filter { it.y == y && it.parentY == parentY }.toSet() }
    val allColumn by lazy { GridModel.grid.filter { it.x == x && it.parentX== parentX }.toSet() }
    val allParent by lazy { GridModel.grid.filter { it.parentY == parentY && it.parentX== parentX }.toSet() }

    val nextValidValue get() = ((value?:0)..8).asSequence().map { it + 1 }.firstOrNull { candidate ->
        allRow.all { it.value != candidate }
        && allColumn.all { it.value != candidate }
        && allParent.all { it.value != candidate }
    }

    fun increment() {
        value = nextValidValue
    }
}


data class TripletKey(val i1: Int, val i2: Int, val i3: Int)


import javafx.application.Application
import javafx.geometry.Orientation
import javafx.scene.control.Button
import javafx.scene.layout.GridPane
import org.ojalgo.optimisation.Variable
import tornadofx.*


fun main(args: Array<String>) = Application.launch(MainApp::class.java, *args)

class MainApp: App(SudokuView::class)

object GridModel {

    val grid =  (0..2).asSequence().flatMap { parentX -> (0..2).asSequence().map { parentY -> parentX to parentY } }
            .flatMap { (parentX,parentY) ->
                (0..2).asSequence().flatMap { x -> (0..2).asSequence().map { y -> x to y } }
                        .map { (x,y) -> GridCell(parentX,parentY,x,y) }
            }.toList()


    fun cellFor(parentX: Int, parentY: Int, x: Int, y: Int) = grid.first {
                it.parentX == parentX &&
                it.parentY == parentY  &&
                it.x == x &&
                it.y == y
        }

    fun solve() {

        expressionsbasedmodel {


            data class VariableItem(val cell: GridCell, val candidateInt: Int, val variable: Variable)

            val variableItems = grid.asSequence().flatMap { cell ->
                (1..9).asSequence()
                        .map { VariableItem(cell,it,variable()) }
                        .onEach { v ->
                            if (v.cell.value == null) v.variable.binary() else v.variable.level(1)
                        }
            }.toList()

            variableItems.groupBy { it.cell.parentRow }.values.forEach { grp ->
                expression {
                    level(1)
                    grp.forEach { set(it.variable,1) }
                }
            }

            variableItems.groupBy { it.cell.parentCol }.values.forEach { grp ->
                expression {
                    level(1)
                    grp.forEach { set(it.variable,1) }
                }
            }

            variableItems.groupBy { it.cell.parentX to it.cell.parentY }.values.forEach { grp ->
                expression {
                    level(1)
                    grp.forEach { set(it.variable,1) }
                }
            }

            solve()

            variableItems.forEach {
                it.cell.value = it.variable.value.toInt()
            }
        }
    }
}

class GridCell(val parentX: Int, val parentY: Int, val x: Int, val y: Int) {
    var value by property<Int?>()
    fun valueProperty() = getProperty(GridCell::value)

    val parentRow = (parentY * 3) + y
    val parentCol = (parentX * 3) + x

    val allRow by lazy { GridModel.grid.filter { it.y == y && it.parentY == parentY }}
    val allColumn by lazy { GridModel.grid.filter { it.y == x && it.parentX== parentX }}
    val allParent by lazy { GridModel.grid.filter { it.parentY == parentY && it.parentX== parentX }}

    val nextValidValue get() = ((value?:0)..8).asSequence().map { it + 1 }.firstOrNull { candidate ->
        allRow.all { it.value != candidate }
        && allColumn.all { it.value != candidate }
        && allParent.all { it.value != candidate }
    }

    fun increment() {
        value = nextValidValue
    }
}



class SudokuView : View() {

    override val root = borderpane {

        left = toolbar {
            orientation = Orientation.VERTICAL

            button("Solve!") {
                setOnAction { GridModel.solve() }
            }
        }

        center = gridpane {

            (0..2).asSequence().flatMap { parentX -> (0..2).asSequence().map { parentY -> parentX to parentY } }
                    .forEach { (parentX, parentY) ->

                        val childGrid = GridPane()

                        (0..2).asSequence().flatMap { x -> (0..2).asSequence().map { y -> x to y } }
                                .forEach { (x, y) ->

                                    val cell = GridModel.cellFor(parentX,parentY,x,y)

                                    val button = Button().apply {
                                        minWidth = 60.0
                                        minHeight = 60.0
                                        cell.valueProperty().onChange { text = it?.toString() }

                                        style { fontSize = 24.px}

                                        setOnAction {
                                            cell.increment()
                                        }
                                    }

                                    childGrid.add(button, x, y, 1, 1)
                                }

                        childGrid.paddingRight = 10.0
                        childGrid.paddingBottom = 10.0

                        add(childGrid, parentX, parentY, 1, 1)
                    }
        }
    }
}

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
                            when {
                                v.cell.value != null && v.cell.value != v.candidateInt -> v.variable.level(0)
                                v.cell.value != null && v.cell.value == v.candidateInt -> v.variable.level(1)
                                else -> v.variable.binary()
                            }

                        }
            }.toList()

            data class TripletKey(val i1: Int, val i2: Int, val i3: Int)


            //entire row
            variableItems.groupBy { TripletKey(it.cell.parentY, it.cell.y, it.candidateInt) }.values.forEach { grp ->
                expression {
                    level(1)
                    grp.forEach { set(it.variable,1) }
                }
            }

            //entire  col
            variableItems.groupBy { TripletKey(it.cell.parentX, it.cell.x, it.candidateInt) }.values.forEach { grp ->
                expression {
                    level(1)
                    grp.forEach { set(it.variable,1) }
                }
            }

            //entire square
            variableItems.groupBy { TripletKey(it.cell.parentX, it.cell.parentY, it.candidateInt) }.values.forEach { grp ->
                expression {
                    level(1)
                    grp.forEach { set(it.variable,1) }
                }
            }

            minimise().run(::println)

            variableItems.forEach {

                if (it.variable.value.toInt() == 1) {
                    it.cell.value = it.candidateInt
                }
            }
        }
    }
}

data class GridCell(val parentX: Int, val parentY: Int, val x: Int, val y: Int) {
    var value by property<Int?>()
    fun valueProperty() = getProperty(GridCell::value)

    val allRow by lazy { GridModel.grid.filter { it.y == y && it.parentY == parentY }}
    val allColumn by lazy { GridModel.grid.filter { it.x == x && it.parentX== parentX }}
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

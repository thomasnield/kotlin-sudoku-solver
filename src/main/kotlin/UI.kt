import javafx.application.Application
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.geometry.Orientation
import javafx.scene.control.Button
import javafx.scene.layout.GridPane
import tornadofx.*

fun main(args: Array<String>) = Application.launch(MainApp::class.java, *args)

class MainApp: App(SudokuView::class)

class SudokuView : View() {

    override val root = borderpane {

        left = toolbar {
            orientation = Orientation.VERTICAL

            button("Solve!") {
                useMaxWidth = true
                setOnAction { GridModel.solve() }
            }
            button("Reset") {
                useMaxWidth = true
                setOnAction {
                    GridModel.grid.forEach { it.value = null }
                }
            }
            label(GridModel.status)
        }

        // build GridPane view
        center = gridpane {

            (0..2).asSequence().flatMap { parentX -> (0..2).asSequence().map { parentY -> parentX to parentY } }
                    .forEach { (parentX, parentY) ->

                        val childGrid = GridPane()

                        (0..2).asSequence().flatMap { x -> (0..2).asSequence().map { y -> x to y } }
                                .forEach { (x, y) ->

                                    val cell = GridModel.cellFor(parentX, parentY, x, y)

                                    val button = Button().apply {
                                        minWidth = 60.0
                                        minHeight = 60.0

                                        textProperty().bind(cell.valueProperty().select { ReadOnlyStringWrapper(it?.toString()) })

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
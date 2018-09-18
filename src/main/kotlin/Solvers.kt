import javafx.application.Platform
import org.ojalgo.okalgo.expression
import org.ojalgo.okalgo.expressionsbasedmodel
import org.ojalgo.okalgo.variable
import org.ojalgo.optimisation.Variable

enum class Solver {

    TOMS_BRANCH_AND_BOUND {

        // A branch-and-bound algorithm
        inner class GridCellBranch(val selectedValue: Int,
                             val gridCell: GridCell,
                             val previous: GridCellBranch? = null) {

            val x = gridCell.x
            val y = gridCell.y
            val squareX = gridCell.squareX
            val squareY = gridCell.squareY

            // traverses this entire branch backwards, revealing the solution so far
            val traverseBackwards =  generateSequence(this) { it.previous }.toList()

            val allRow  = traverseBackwards.filter { it.y == y && it.squareY == squareY }
            val allColumn = traverseBackwards.filter { it.x == x && it.squareX == squareX }
            val allSquare = traverseBackwards.filter { it.squareY == squareY && it.squareX== squareX }

            val constraintsMet = allRow.filter { it.selectedValue == selectedValue }.count() <= 1
                    && allColumn.filter { it.selectedValue == selectedValue }.count() <= 1
                    && allSquare.filter { it.selectedValue == selectedValue }.count() <= 1

            val isContinuable =  constraintsMet && traverseBackwards.count() < 81

            val isSolution = traverseBackwards.count() == 81 && constraintsMet

            fun applyToCell() {
                Platform.runLater { gridCell.value = selectedValue }
            }

            init {
                if (isContinuable) applyToCell()
            }

        }

        override fun solve() {

            // Order Sudoku cells by count of how many candidate values they have left
            // Starting with the most constrained cells (with fewest possible values left) will greatly reduce the search space
            // Fixed cells will have only 1 candidate and will be processed first
            val sortedByCandidateCount = GridModel.grid.asSequence()
                    .sortedBy { it.candidatesLeft.count() }
                    .toList()

            // hold onto fixed values snapshot as they are going to mutate during animation
            val fixedCellValues =  GridModel.grid.asSequence().map { it to it.value }
                    .filter { it.second != null }
                    .toMap()

            // this is a recursive function for exploring nodes in a branch-and-bound tree
            fun traverse(index: Int, currentBranch: GridCellBranch): GridCellBranch? {

                val nextCell = sortedByCandidateCount[index+1]

                val fixedValue = fixedCellValues[nextCell]

                // we want to explore possible values 1..9 unless this cell is fixed already
                // infeasible values should terminate the branch
                val range = if (fixedValue == null) (1..9) else (fixedValue..fixedValue)

                for (candidateValue in range) {

                    val nextBranch = GridCellBranch(candidateValue, nextCell, currentBranch)

                    if (nextBranch.isSolution)
                        return nextBranch

                    if (nextBranch.isContinuable) {
                        val terminalBranch = traverse(index + 1, nextBranch)
                        if (terminalBranch?.isSolution == true) {
                            return terminalBranch
                        }
                    }
                }
                return null
            }

            // start with the first sorted Sudoku cell and set it as the seed
            val seed = sortedByCandidateCount.first()
                    .let { GridCellBranch(it.value?:1, it) }

            // recursively traverse from the seed and get a solution
            val solution = traverse(0, seed)

            solution?.traverseBackwards?.forEach { it.applyToCell() }

            Platform.runLater { GridModel.status = if (solution == null) "INFEASIBLE" else "FEASIBLE" }
        }
    },
    OJALGO {

        inner class TripletKey(val i1: Int, val i2: Int, val i3: Int)

        override fun solve() {
            expressionsbasedmodel {

                // A VariableItem is used to pair a GridCell to each candidate number 1-9, and it's corresponding binary variable to be optimized

                data class VariableItem(val cell: GridCell, val candidateInt: Int, val variable: Variable)

                val variableItems = GridModel.grid.asSequence().flatMap { cell ->
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
                    expression {
                        level(1)
                        grp.forEach { set(it.variable, 1) }
                    }
                }

                // constrain each row so variables of each number only add to 1
                variableItems.groupBy { TripletKey(it.cell.squareY, it.cell.y, it.candidateInt) }.values.forEach { grp ->

                    expression {
                        level(1)
                        grp.forEach { set(it.variable,1) }
                    }
                }

                // constrain each column so variables of each number only add to 1
                variableItems.groupBy { TripletKey(it.cell.squareX, it.cell.x, it.candidateInt) }.values.forEach { grp ->
                    expression {
                        level(1)
                        grp.forEach { set(it.variable,1) }
                    }
                }

                // constrain each square so variables of each number only add to 1
                variableItems.groupBy { TripletKey(it.cell.squareX, it.cell.squareY, it.candidateInt) }.values.forEach { grp ->
                    expression {
                        level(1)
                        grp.forEach { set(it.variable,1) }
                    }
                }

                // minimize number of solutions to seek
                options.iterations_suffice = 1

                // no optimization objective, but just call minimize()
                minimise().also {
                    Platform.runLater { GridModel.statusProperty.set(it.state.toString()) }
                }

                // set optimized variables back to GridCells for display
                variableItems.asSequence().filter { it.variable.value.toInt() == 1 }.forEach {
                    Platform.runLater { it.cell.value = it.candidateInt }
                }
            }
        }
    };

    override fun toString() = name.replace("_", " ")
    abstract fun solve()
}



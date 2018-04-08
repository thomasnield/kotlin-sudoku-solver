
import org.ojalgo.optimisation.Expression
import org.ojalgo.optimisation.ExpressionsBasedModel
import org.ojalgo.optimisation.Variable
import java.util.concurrent.atomic.AtomicInteger


class AutoNameState {
    // custom DSL for  expression and variable inputs, eliminate naming and adding
    val funcId = AtomicInteger(0)
    val variableId = AtomicInteger(0)
    fun generateVariableName() = variableId.incrementAndGet().toString().let { "Variable$it" }
    fun generateExpressionName() = funcId.incrementAndGet().let { "Func$it"}
}

val autoNameStates = mutableMapOf<ExpressionsBasedModel, AutoNameState>()
fun ExpressionsBasedModel.getAutoNameState() = autoNameStates.computeIfAbsent(this) { AutoNameState() }

fun expressionsbasedmodel(op: ExpressionsBasedModel.() -> Unit): ExpressionsBasedModel {
    val ebm = ExpressionsBasedModel()
    ebm.op()
    return ebm
}
/*
fun ExpressionsBasedModel.objective(name: String? = null, lower: Number? = null, upper: Number? = null, op: Expression.() -> Unit = {}): Expression {
    val expr = objective()
    expr.op()
    lower?.let { expr.lower(it) }
    upper?.let { expr.upper(it) }
    return expr
}
*/

fun ExpressionsBasedModel.expression(expression: ExpressionBuilder? = null,
                                     name: String? = null,
                                     lower: Number? = null,
                                     upper: Number? = null,
                                     level: Number? = null,
                                     weight: Number? = null,
                                     op: Expression.() -> Unit = {}): Expression {

    val expr = addExpression(name ?: getAutoNameState().generateExpressionName())

    expression?.also {
        it.items.forEach {
            expr.it()
        }
    }

    expr.op()
    lower?.let { expr.lower(it) }
    upper?.let { expr.upper(it) }
    level?.let { expr.level(it) }
    weight?.let { expr.weight(it) }

    return expr
}


fun ExpressionsBasedModel.variable(name: String? = null, lower: Number? = null, upper: Number? = null, weight: Number? = null, isBinary: Boolean? = null, isInteger: Boolean? = null, op: Variable.() -> Unit = {}): Variable {

    val variable = Variable(name ?: getAutoNameState().generateVariableName())
    variable.op()
    lower?.let { variable.lower(it) }
    upper?.let { variable.upper(it) }
    weight?.let { variable.weight(it) }
    isBinary?.let { if (it) variable.binary() }
    isInteger?.let { variable.integer(it) }

    addVariable(variable)
    return variable
}


operator fun Variable.plus(other: Variable): ExpressionBuilder {
    val eb = ExpressionBuilder()
    eb += this
    eb += other
    return eb
}

operator fun Variable.minus(other: Variable): ExpressionBuilder {
    val eb = ExpressionBuilder()
    eb += this
    eb - other
    return eb
}

operator fun Variable.plus(expression: ExpressionBuilder): ExpressionBuilder {
    val eb = expression
    eb += this
    return eb
}


operator fun Variable.minus(expression: ExpressionBuilder): ExpressionBuilder {
    val eb = expression
    eb += this
    return eb
}


operator fun Number.times(variable: Variable): ExpressionBuilder {
    val eb = ExpressionBuilder()
    val multiplier = this
    eb.items += { set(variable, multiplier) }
    return eb
}

infix fun Number.LTE(variable: Variable): ExpressionBuilder {
    val eb = ExpressionBuilder()
    val lower = this
    eb.items += {
        lower(lower)
        set(variable, 1)
    }
    return eb
}

infix fun Number.GTE(variable: Variable): ExpressionBuilder {
    val eb = ExpressionBuilder()
    val upper = this
    eb.items += {
        upper(upper)
        set(variable, 1)
    }
    return eb
}

infix fun Number.EQ(variable: Variable): ExpressionBuilder {
    val eb = ExpressionBuilder()
    val upper = this
    eb.items += {
        level(upper)
        set(variable, 1)
    }
    return eb
}


infix fun Number.LTE(expression: ExpressionBuilder): ExpressionBuilder {
    val eb = expression
    val lower = this
    eb.items += {
        lower(lower)
    }
    return eb
}

infix fun Number.GTE(expression: ExpressionBuilder): ExpressionBuilder {
    val eb = expression
    val upper = this
    eb.items += {
        upper(upper)
    }
    return eb
}

infix fun Number.EQ(expression: ExpressionBuilder): ExpressionBuilder {
    val eb = expression
    val upper = this
    eb.items += {
        level(upper)
    }
    return eb
}

fun ExpressionsBasedModel.set(expression: ExpressionBuilder) {
    expression.addToModel(this)
}


class ExpressionBuilder {
    val items = mutableListOf<Expression.() -> Unit>()

    operator fun plusAssign(variable: Variable) {
        items += { set(variable, 1) }
    }

    operator fun plus(variable: Variable): ExpressionBuilder {
        items += { set(variable, 1) }
        return this
    }

    operator fun plus(expressionBuilder: ExpressionBuilder): ExpressionBuilder {
        items.addAll(expressionBuilder.items)
        return this
    }
    operator fun minus(variable: Variable): ExpressionBuilder {
        items += { set(variable, -1) }
        return this
    }

    fun weight(weight: Number): ExpressionBuilder {
        items += { weight(weight) }
        return this
    }
    infix fun EQ(number: Int): ExpressionBuilder {
        items += { level(number) }
        return this
    }

    infix fun GTE(number: Int): ExpressionBuilder {
        items += { upper(number) }
        return this
    }

    infix fun LTE(number: Int): ExpressionBuilder {
        items += { lower(number) }
        return this
    }

    fun addToModel(model: ExpressionsBasedModel, name: String? = null) {

        model.expression(name = name) {
            items.forEach {
                this.it()
            }
        }
    }
}
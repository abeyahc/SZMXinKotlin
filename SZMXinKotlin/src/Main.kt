// ---------- SZMX in KOTLIN ----------


// ---- AST ----
// sealed class: only allows certain types of subclasses
sealed class ExprC {
    data class NumC(val n: Double) : ExprC() // use: ExprC.NumC
    data class StringC(val s: String) : ExprC()
    data class IdC(val name: String) : ExprC()
    data class IfC(val test: ExprC, val thenBranch: ExprC, val elseBranch: ExprC) : ExprC()
    data class FunC(val params: List<String>, val body: ExprC) : ExprC()
    data class AppC(val function: ExprC, val args: List<ExprC>) : ExprC()
}


// ---- Values ----
typealias Environment = List<Pair<String, Value>> // delete this line when writing interp (just for compiling)

// sealed class: only allows certain types of subclasses
sealed class Value {
    data class NumV(val n: Double) : Value() // use: Value.NumV
    data class BoolV(val b: Boolean) : Value()
    data class StringV(val s: String) : Value()
    data class ClosV(val params: List<String>, val body: ExprC, val env: Environment) : Value() // fill in Environment when writing interp
    data class PrimopV(val name: String, val arity: Int, val op: (List<Value>) -> Value) : Value()
}

// Helpers
fun getNum(v: Value, primName: String): Double = when (v) {
    is Value.NumV -> v.n
    else -> error("SZMX $primName expects number, got ${serialize(v)}")
}

fun getString(v: Value, primName: String): String = when (v) {
    is Value.StringV -> v.s
    else -> error("SZMX $primName expects string, got ${serialize(v)}")
}

fun getBool(v: Value, primName: String): Boolean = when (v) {
    is Value.BoolV -> v.b
    else -> error("SZMX $primName expects boolean, got ${serialize(v)}")
}

// Serialize
fun serialize(v: Value): String = when (v) {
    is Value.NumV -> if (v.n == v.n.toLong().toDouble()) v.n.toLong().toString() else v.n.toString()
    is Value.BoolV -> if (v.b) "true" else "false"
    is Value.StringV -> "\"${v.s}\""
    is Value.ClosV -> "#<procedure>"
    is Value.PrimopV -> "#<primop>"
}

// Prim Ops
val primPlus: (List<Value>) -> Value = { args ->
    Value.NumV(getNum(args[0], "+") + getNum(args[1], "+"))
}

val primMinus: (List<Value>) -> Value = { args ->
    Value.NumV(getNum(args[0], "-") - getNum(args[1], "-"))
}

val primMult: (List<Value>) -> Value = { args ->
    Value.NumV(getNum(args[0], "*") * getNum(args[1], "*"))
}

val primDiv: (List<Value>) -> Value = { args ->
    val divisor = getNum(args[1], "/")
    if (divisor == 0.0) error("SZMX division by zero")
    Value.NumV(getNum(args[0], "/") / divisor)
}

val primLte: (List<Value>) -> Value = { args ->
    Value.BoolV(getNum(args[0], "<=") <= getNum(args[1], "<="))
}

val primSubstring: (List<Value>) -> Value = { args ->
    val s = getString(args[0], "substring")
    val startVal = getNum(args[1], "substring")
    val stopVal = getNum(args[2], "substring")
    val startInt = startVal.toInt()
    val stopInt = stopVal.toInt()
    if (startVal != startInt.toDouble() || startVal < 0 || stopVal != stopInt.toDouble() || stopVal < 0)
        error("SZMX substring: indices must be natural numbers")
    if (startInt > s.length || stopInt > s.length || startInt > stopInt)
        error("SZMX substring: indices out of range for string \"$s\"")
    Value.StringV(s.substring(startInt, stopInt))
}

val primStrlen: (List<Value>) -> Value = { args ->
    Value.NumV(getString(args[0], "strlen").length.toDouble())
}

val primEqual: (List<Value>) -> Value = { args ->
    val a = args[0]
    val b = args[1]
    Value.BoolV(
        when {
            a is Value.NumV && b is Value.NumV -> a.n == b.n
            a is Value.BoolV && b is Value.BoolV -> a.b == b.b
            a is Value.StringV && b is Value.StringV -> a.s == b.s
            else -> false
        }
    )
}

val primError: (List<Value>) -> Value = { args ->
    error("SZMX user-error: ${serialize(args[0])}")
}


// Top Level Environment
val topEnv: Environment = listOf(
    Pair("+",         Value.PrimopV("+",         2, primPlus)),
    Pair("-",         Value.PrimopV("-",         2, primMinus)),
    Pair("*",         Value.PrimopV("*",         2, primMult)),
    Pair("/",         Value.PrimopV("/",         2, primDiv)),
    Pair("<=",        Value.PrimopV("<=",        2, primLte)),
    Pair("substring", Value.PrimopV("substring", 3, primSubstring)),
    Pair("strlen",    Value.PrimopV("strlen",    1, primStrlen)),
    Pair("equal?",    Value.PrimopV("equal?",    2, primEqual)),
    Pair("error",     Value.PrimopV("error",     1, primError)),
    Pair("true",      Value.BoolV(true)),
    Pair("false",     Value.BoolV(false)),
)


// ---- Tests ----
fun main() {

}
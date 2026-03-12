
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
//typealias Environment = List<Pair<String, Value>> // delete this line when writing interp (just for compiling)

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


// ---------- Environment helpers ----------

typealias Binding = Pair<String, Value>

typealias Environment = List<Binding>

fun mtEnv(): Environment = emptyList()

fun extendEnv(env: Environment, name: String, value: Value): Environment =
        listOf(name to value) + env //adds list<pair<string, value>> to list<pair<string, value>>
//adds the new pair to the beginning of the env

fun extendEnvStar(env: Environment, names: List<String>, values: List<Value>): Environment {
    if (names.size != values.size) {
        error("SZMX extend-env*: names/values length mismatch: ${names.size} vs ${values.size}")
    }
    var acc = env
    for (i in names.indices) {
        acc = extendEnv(acc, names[i], values[i])
    }
    return acc
}

fun lookup(name: String, env: Environment): Value {
    for ((boundName, v) in env) {
        if (boundName == name) return v //look for given name in the environment
    }
    error("SZMX unbound identifier: $name")
}

fun hasDuplicates(xs: List<String>): Boolean =
        xs.size != xs.toSet().size //converts to set which gets rid of duplicates


// ---------- Let desugaring (AST-level) ----------

fun desugarLet(bindings: List<Pair<String, ExprC>>, body: ExprC): ExprC {
    val names = bindings.map { it.first } //initializes list of names
    if (hasDuplicates(names)) {
        error("SZMX let: duplicate binding names")
    }
    val rhsExprs = bindings.map { it.second } //initializes list of values that names are binded to
    return ExprC.AppC(
            function = ExprC.FunC(params = names, body = body), //makes an appC with a funC that has names
            args = rhsExprs //and associated values 
    )
}


// Interpreter
// when matches on what kind of expression it is
fun interp(expr: ExprC, env: Environment): Value = when (expr) {

    // Literals evaluate to themselves
    is ExprC.NumC -> Value.NumV(expr.n)
    is ExprC.StringC -> Value.StringV(expr.s)

    // Identifiers: look up in environment for identifiers like + and x
    is ExprC.IdC -> lookup(expr.name, env)

    // If: evaluate test, must be boolean, then branch
    is ExprC.IfC -> {
        val testVal = interp(expr.test, env)
        if (getBool(testVal, "if")) interp(expr.thenBranch, env)
        else interp(expr.elseBranch, env)
    }

    // Function literal: gets current environment as closure
    is ExprC.FunC -> {
        if (expr.params.size != expr.params.toSet().size)
            error("SZMX function: duplicate parameter names")
        Value.ClosV(expr.params, expr.body, env)
    }

    // Application: evaluate function + args, then apply
    is ExprC.AppC -> {
        val funVal = interp(expr.function, env)
        val argVals = expr.args.map { interp(it, env) }
        when (funVal) {
            
            is Value.ClosV -> {
                if (funVal.params.size != argVals.size)
                    error("SZMX wrong arity: expected ${funVal.params.size} args, got ${argVals.size}")
                interp(funVal.body, extendEnvStar(funVal.env, funVal.params, argVals))
            }

            is Value.PrimopV -> {
                if (funVal.arity != argVals.size)
                    error("SZMX wrong arity: ${funVal.name} expected ${funVal.arity} args, got ${argVals.size}")
                funVal.op(argVals)
            }

            else -> error("SZMX application of non-procedure: ${serialize(funVal)}")
        }
    }
}

fun main() {
    // (+ 3 4) -> 7
    println(serialize(interp(ExprC.AppC(ExprC.IdC("+"), listOf(ExprC.NumC(3.0), ExprC.NumC(4.0))), topEnv)))

    // (let x = 10 in (+ x 5)) desugared into ((fun (x) => (+ x 5)) 10)
    val letExpr = ExprC.AppC(
        ExprC.FunC(listOf("x"), ExprC.AppC(ExprC.IdC("+"), listOf(ExprC.IdC("x"), ExprC.NumC(5.0)))),
        listOf(ExprC.NumC(10.0))
    )
    println(serialize(interp(letExpr, topEnv))) // result: 15

}
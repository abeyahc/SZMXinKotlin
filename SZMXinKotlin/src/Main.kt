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


// ---- Tests ----
fun main() {

}
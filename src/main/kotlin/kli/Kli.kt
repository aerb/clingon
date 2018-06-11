package kli

import kotlin.reflect.KProperty



class FlagDelegate(arg: OptionalArgument) {
    operator fun getValue(kli: Kli?, property: KProperty<*>): Int {

    }
}

class PosDelegate(arg: PositionalArgument) {
    operator fun getValue(kli: Kli?, property: KProperty<*>): String {

    }
}

class MapDelegate<T>(private val delegate: ArgumentDelegate, private val transform: (String) -> T) {
    operator fun getValue(kli: Kli?, property: KProperty<*>): T =
        transform(delegate.getValue(kli, property))
}

class ArgumentDelegate(arg: OptionalArgument) {
    operator fun getValue(_: Kli?, property: KProperty<*>): String {

    }

    infix fun <T> mappedTo(transform: (String) -> T): MapDelegate<T> =
        MapDelegate(this, transform)
}


class PositionalArgument(
    val name: String,
    val help: String
)

class OptionalArgument(
    val flags: List<String>,
    val message: String,
    val requiresValue: Boolean
)


private val validFlag = Regex("^-{1,2}[a-zA-Z0-9-_]+")

private fun parseFlagString(s: String): List<String> {
    return s.split("|").map {
        it.trim().also {
            require(it.matches(validFlag)) { "Illegal flag name $it" }
        }
    }
}

class Kli {

    private val pos = ArrayList<PositionalArgument>()
    private val args = ArrayList<OptionalArgument>()
    private val maps = HashMap<String, OptionalArgument>()

    private fun addArgument(arg: OptionalArgument) {
        args += arg
        arg.flags.associateTo(maps) { it to arg }
    }

    fun flag(flags: String, help: String = ""): FlagDelegate {
        val arg = OptionalArgument(
            flags = parseFlagString(flags),
            message = help,
            requiresValue = false
        )
        addArgument(arg)
        return FlagDelegate(arg)
    }

    fun optional(flags: String, help: String): ArgumentDelegate {
        val arg = OptionalArgument(
            flags = parseFlagString(flags),
            message = help,
            requiresValue = true
        )
        addArgument(arg)
        return ArgumentDelegate(arg)
    }

    fun positional(name: String, help: String): PosDelegate {
        val arg = PositionalArgument(name, help)
        pos += arg
        return PosDelegate(arg)
    }

    fun parse(args: Array<String>) {

    }
}
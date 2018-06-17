package kli

class PositionalDefinition(
    val name: String,
    val help: String
)

class OptionDefinition(
    val flags: List<String>,
    val help: String,
    val takesArg: Boolean
)

private val validFlag = Regex("^-{1,2}[a-zA-Z0-9-_?]+")

internal fun MatchGroupCollection.requiredGroup(i: Int): String =
    this[i]?.value ?: throw IllegalStateException("Expected group of index $i.")

private fun parseFlagString(s: String): List<String> =
    s.split("|").map {
        it.trim().also {
            require(it.matches(validFlag)) { "Illegal flag name $it" }
        }
    }

class Clingon {
    data class CPos(val arg: PositionalDefinition, val delegate: StringDelegate)
    data class COpt(val arg: OptionDefinition, val delegate: SimpleDelegate<String, *>)

    private val positions = ArrayList<CPos>()
    private val options = HashMap<String, COpt>()

    fun flag(flags: String, help: String = ""): BoolDelegate {
        val delegate = BoolDelegate()
        val argument = OptionDefinition(parseFlagString(flags), help, takesArg = false)
        argument.flags.associateTo(options) { it to COpt(argument, delegate) }
        return delegate
    }

    fun option(flags: String, help: String = ""): StringDelegate {
        val delegate = StringDelegate()
        val argument = OptionDefinition(parseFlagString(flags), help, true)
        argument.flags.associateTo(options) { it to COpt(argument, delegate) }
        return delegate
    }

    fun positional(name: String, help: String = ""): StringDelegate {
        val delegate = StringDelegate()
        val arg = PositionalDefinition(name, help)
        positions += CPos(arg, delegate)
        return delegate
    }

    private var positionIndex = 0

    fun parse(args: Array<String>) {
        val tokenizer = CliTokenizer(args)

        while(tokenizer.hasNext()) {
            val next = tokenizer.nextType()
            when(next) {
                Token.ShortFlag, Token.Flag -> {
                    val flag = if(next == Token.ShortFlag) "-${tokenizer.readShortFlag()}" else "--${tokenizer.readFlag()}"
                    val option = options[flag] ?: throw IllegalArgumentException("Unknown flag $flag")
                    if(option.arg.takesArg) {
                        if(!tokenizer.hasNext()) throw IllegalArgumentException("Argument required for $flag")
                        option.delegate.setValue(tokenizer.readOptionArgument())
                    } else {
                        option.delegate.setValue("")
                    }
                }
                Token.Positional -> {
                    positions[positionIndex++].delegate.setValue(tokenizer.readPositional())
                }
                else -> throw IllegalStateException("Unexpected $next")
            }
        }

        options.values.forEach { it.delegate.onParseDone() }
        positions.forEach { it.delegate.onParseDone() }
    }
}
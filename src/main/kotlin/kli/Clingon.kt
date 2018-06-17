package kli


class PositionalArgument(
    val name: String,
    val help: String
)

class OptionArgument(
    val flags: List<String>,
    val help: String,
    val requiresValue: Boolean
)

private val validFlag = Regex("^-{1,2}[a-zA-Z0-9-_?]+")

private fun parseFlagString(s: String): List<String> =
    s.split("|").map {
        it.trim().also {
            require(it.matches(validFlag)) { "Illegal flag name $it" }
        }
    }

class Clingon {
    data class CPos(val arg: PositionalArgument, val delegate: StringDelegate)
    private sealed class COpt {
        abstract val arg: OptionArgument
        abstract val delegate: ParserLifecycle
        data class Arg(override val arg: OptionArgument, override val delegate: StringDelegate): COpt()
        data class NoArg(override val arg: OptionArgument, override val delegate: BoolDelegate): COpt()
    }

    private val positions = ArrayList<CPos>()
    private val options = HashMap<String, COpt>()

    fun flag(flags: String, help: String = ""): BoolDelegate {
        val delegate = BoolDelegate()
        val argument = OptionArgument(
            flags = parseFlagString(flags),
            help = help,
            requiresValue = false
        )
        argument.flags.associateTo(options) { it to COpt.NoArg(argument, delegate) }
        return delegate
    }

    fun option(flags: String, help: String = ""): StringDelegate {
        val delegate = StringDelegate()
        val argument = OptionArgument(
            flags = parseFlagString(flags),
            help = help,
            requiresValue = true
        )
        argument.flags.associateTo(options) { it to COpt.Arg(argument, delegate) }
        return delegate
    }

    fun positional(name: String, help: String = ""): StringDelegate {
        val delegate = StringDelegate()
        val arg = PositionalArgument(name, help)
        positions += CPos(arg, delegate)
        return delegate
    }

    private val argumentPattern = Regex("^((-{1,2})[a-zA-Z0-9-_?]+)(=(.*))?", RegexOption.MULTILINE)

    enum class ParserState {
        Initial,
        PendingValue
    }

    private fun parseMultiChar(name: String, value: String?) {
        val holder = options[name] ?: throw IllegalArgumentException("Unknown arg $name")
        when(holder) {
            is COpt.Arg -> {
                if (value != null) {
                    holder.delegate.setValue(value)
                    waitingForValue = null
                    parserState = ParserState.Initial
                } else {
                    waitingForValue = holder
                    parserState = ParserState.PendingValue
                }
            }
            is COpt.NoArg -> {
                holder.delegate.setValue(Unit)
            }
        }
    }

    private fun parseSingleChar(name: String) {
        var i = 1
        while(i < name.length) {
            val c = "-${name[i]}"
            val holder = options[c] ?: throw IllegalArgumentException("Unknown arg $c")
            when(holder) {
                is COpt.Arg -> {
                    if(i == name.lastIndex) {
                        waitingForValue = holder
                        parserState = ParserState.PendingValue
                        return
                    } else {
                        val j = i + 1
                        val value =
                            if(name[j] == '=') {
                                if(j + 1 < name.length) name.substring(j + 1)
                                else ""
                            } else name.substring(j)
                        holder.delegate.setValue(value)
                    }
                    return
                }
                is COpt.NoArg -> {
                    holder.delegate.setValue(Unit)
                }
            }
            i++
        }
    }

    private var parserState = ParserState.Initial
    private var waitingForValue: COpt? = null
    private var positionIndex = 0

    fun parse(args: Array<String>) {
        for(arg in args) {
            when(parserState) {
                ParserState.Initial -> {
                    val match = argumentPattern.matchEntire(arg)?.groups
                    if(match != null) {
                        val fullMatch = checkNotNull(match[0]?.value) { "Missed required" }
                        val name = checkNotNull(match[1]?.value) { "Missed required" }
                        val dash = checkNotNull(match[2]?.value) { "Missed required" }
                        val value = match[4]?.value
                        when (dash.length) {
                            1 -> parseSingleChar(fullMatch)
                            2 -> parseMultiChar(name, value)
                            else -> throw IllegalArgumentException("Invalid flag $arg")
                        }
                    } else {
                        if(arg.isNotEmpty()) {
                            positions[positionIndex++].delegate.setValue(arg)
                        }
                    }
                }
                ParserState.PendingValue -> {
                    val holder = checkNotNull(waitingForValue) { "Waiting value not set" }
                    holder as COpt.Arg

                    holder.delegate.setValue(arg)
                    parserState = ParserState.Initial
                    waitingForValue = null
                }
            }
        }

        check(parserState != ParserState.PendingValue) { "Missing value for ${waitingForValue?.arg?.flags}" }

        options.values.forEach { it.delegate.onParseDone() }
        positions.forEach { it.delegate.onParseDone() }
    }
}
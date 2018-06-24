package clingon

data class ArgumentDefinition(
    val name: String,
    val flags: List<String>,
    val help: String,
    val takesArg: Boolean
)

class ParseException(message: String, val option: ArgumentDefinition? = null): Exception(message)

class Clingon {

    private val positions = ArrayList<ArgumentStore<String, *>>()
    private val options = HashMap<String, ArgumentStore<String, *>>()

    fun flag(flags: String, help: String = ""): BoolStore {
        val argument = ArgumentDefinition("", parseFlagString(flags), help, takesArg = false)
        val delegate = BoolStore(argument)
        argument.flags.associateTo(options) { it to delegate }
        return delegate
    }

    fun option(flags: String, help: String = ""): SingleStore {
        val argument = ArgumentDefinition("", parseFlagString(flags), help, takesArg = true)
        val delegate = SingleStore(argument)
        argument.flags.associateTo(options) { it to delegate }
        return delegate
    }

    fun positional(name: String, help: String = ""): SingleStore {
        val argument = ArgumentDefinition(name, emptyList(), help, takesArg = false)
        val delegate = SingleStore(argument)
        positions += delegate
        return delegate
    }

    private var positionIndex = 0

    fun parse(args: Array<String>) {
        val tokenizer = CliTokenizer(args)
        options.values.forEach { it.onPreParse() }
        positions.forEach { it.onPreParse() }

        while(tokenizer.hasNext()) {
            val next = tokenizer.nextType()
            when(next) {
                TokenType.ShortFlag, TokenType.Flag -> {
                    val flag =
                        if(next == TokenType.ShortFlag) "-${tokenizer.readShortFlag()}"
                        else "--${tokenizer.readFlag()}"
                    val option = options[flag] ?: throw IllegalArgumentException("Unknown flag $flag")
                    if(option.argument.takesArg) {
                        if(!tokenizer.hasNext()) throw IllegalArgumentException("Argument required for $flag")
                        option.storeValue(tokenizer.readOptionArgument())

                        while (option.willAcceptValue()) {
                            if(tokenizer.hasNext() &&
                               tokenizer.nextType().let { it == TokenType.Positional || it == TokenType.Equals }
                            ) {
                                option.storeValue(tokenizer.readPositional())
                            } else break
                        }
                    } else {
                        if(tokenizer.hasNext() && tokenizer.nextType() == TokenType.Equals)
                            throw ParseException(
                                "Option $flag does not take an argument, but was given '=${tokenizer.readOptionArgument()}'",
                                option = option.argument
                            )
                        option.storeValue("")
                    }
                }
                TokenType.Positional -> {
                    while (true) {
                        if(positionIndex >= positions.size) throw IllegalArgumentException()
                        val arg = positions[positionIndex]
                        if(arg.willAcceptValue()) {
                            arg.storeValue(tokenizer.readPositional())
                            break
                        } else {
                            positionIndex ++
                        }
                    }
                }
                else -> throw IllegalStateException("Unexpected $next")
            }
        }

        options.values.forEach { it.onPostParse() }
        positions.forEach { it.onPostParse() }
    }
}
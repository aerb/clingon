package kli

enum class Token {
    ShortFlag,
    Flag,
    OptionArgument,
    Positional,
    OptionTerminator
}

enum class Context {
    TopLevel,
    ShortFlag,
    Flag
}

class CliTokenizer(private val args: Array<String>) {

    private var i: Int = 0
    private var cI: Int = 0
    private val argumentPattern = Regex("^(-{1,2})([a-zA-Z0-9-_?=]*)", RegexOption.MULTILINE)

    fun hasNext(): Boolean = i < args.size

    private var context: Context = Context.TopLevel

    fun nextType(): Token {
        val arg = args[i]
        return when(context) {
            Context.TopLevel -> {
                val match = argumentPattern.matchEntire(arg)?.groups
                if (match != null) {
                    val dash = match.requiredGroup(1)
                    val name = match[2]?.value

                    if(dash.length == 1 && name != null) {
                        Token.ShortFlag
                    } else if(dash.length == 2) {
                        if(name == null) {
                            Token.OptionTerminator
                        } else {
                            Token.Flag
                        }
                    } else {
                        Token.Positional
                    }
                } else {
                    Token.Positional
                }
            }
            Context.ShortFlag -> {
                val c = arg[cI]
                if(c == '=') Token.OptionArgument
                else Token.ShortFlag
            }
            Context.Flag -> Token.OptionArgument
        }
    }

    private fun illegalState(): Nothing {
        throw IllegalStateException("Should not be in state $context")
    }

    fun readOptionArgument(): String {
        when(context) {
            Context.ShortFlag, Context.Flag -> {
                val arg = args[i]
                val c = arg[cI]
                val startIndex = if(c == '=') cI + 1 else cI
                i ++
                cI = 0
                context = Context.TopLevel
                return arg.substring(startIndex)
            }
            Context.TopLevel -> {
                val a = args[i]
                i ++
                return a
            }
            else -> illegalState()
        }
    }



    fun readShortFlag(): Char {
        val arg = args[i]
        when(context) {
            Context.TopLevel -> {
                val c = arg[cI + 1]
                if(arg.length == 2) i ++
                else {
                    cI += 2
                    context = Context.ShortFlag
                }
                return c
            }
            Context.ShortFlag -> {
                val c = arg[cI]
                if (cI < arg.lastIndex) cI ++
                else {
                    cI = 0
                    i ++
                    context = Context.TopLevel
                }
                return c
            }
            else -> illegalState()
        }
    }

    fun readFlag(): String {
        if(context != Context.TopLevel) illegalState()
        val arg = args[i]
        val flag = arg.drop(2).takeWhile { it != '=' }
        if(flag.length == arg.length - 2) {
            i ++
        } else {
            context = Context.Flag
            cI = flag.length + 2
        }
        return flag
    }

    fun readPositional(): String {
        check(context == Context.TopLevel)
        val value = args[i]
        i ++
        return value
    }
}



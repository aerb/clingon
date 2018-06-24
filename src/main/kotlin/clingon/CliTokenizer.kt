package clingon

enum class TokenType {
    ShortFlag,
    Flag,
    Equals,
    Positional,
    OptionTerminator
}

class CliTokenizer(private val args: Array<String>) {

    enum class ParseContext {
        TopLevel,
        ShortFlag,
        Flag
    }

    private var i: Int = 0
    private var cI: Int = 0
    private val argumentPattern = Regex("^(-{1,2})([a-zA-Z0-9-_?=]*)", RegexOption.MULTILINE)

    fun hasNext(): Boolean = i < args.size

    private var context: ParseContext = ParseContext.TopLevel

    fun nextType(): TokenType {
        val arg = args[i]
        return when(context) {
            ParseContext.TopLevel -> {
                val match = argumentPattern.matchEntire(arg)?.groups
                if (match != null) {
                    val dash = match.requiredGroup(1)
                    val name = match[2]?.value

                    if(dash.length == 1 && name != null) {
                        TokenType.ShortFlag
                    } else if(dash.length == 2) {
                        if(name == null) {
                            TokenType.OptionTerminator
                        } else {
                            TokenType.Flag
                        }
                    } else {
                        TokenType.Positional
                    }
                } else {
                    TokenType.Positional
                }
            }
            ParseContext.ShortFlag -> {
                val c = arg[cI]
                if(c == '=') TokenType.Equals
                else TokenType.ShortFlag
            }
            ParseContext.Flag -> TokenType.Equals
        }
    }

    private fun illegalState(): Nothing {
        throw IllegalStateException("Should not be in state $context")
    }

    fun readOptionArgument(): String {
        when(context) {
            ParseContext.ShortFlag, ParseContext.Flag -> {
                val arg = args[i]
                val c = arg[cI]
                val startIndex = if(c == '=') cI + 1 else cI
                i ++
                cI = 0
                context = ParseContext.TopLevel
                return arg.substring(startIndex)
            }
            ParseContext.TopLevel -> {
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
            ParseContext.TopLevel -> {
                val c = arg[cI + 1]
                if(arg.length == 2) i ++
                else {
                    cI += 2
                    context = ParseContext.ShortFlag
                }
                return c
            }
            ParseContext.ShortFlag -> {
                val c = arg[cI]
                if (cI < arg.lastIndex) cI ++
                else {
                    cI = 0
                    i ++
                    context = ParseContext.TopLevel
                }
                return c
            }
            else -> illegalState()
        }
    }

    fun readFlag(): String {
        if(context != ParseContext.TopLevel) illegalState()
        val arg = args[i]
        val flag = arg.drop(2).takeWhile { it != '=' }
        if(flag.length == arg.length - 2) {
            i ++
        } else {
            context = ParseContext.Flag
            cI = flag.length + 2
        }
        return flag
    }

    fun readPositional(): String {
        check(context == ParseContext.TopLevel)
        val value = args[i]
        i ++
        return value
    }
}



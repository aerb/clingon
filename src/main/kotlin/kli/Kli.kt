package kli

data class CliArg(
    val name: String,
    val requiresValue: Boolean = true,
    val short: String? = null,
    val description: String? = null
) {
    val longFlag: String = "--$name"
    val shortFlag: String? = if (short == null) null else "-$short"
}

data class CliResult(
    val positional: List<String>,
    val optional: Map<String, String?>
)

class CliParseException(message: String): Exception(message)

fun buildCli(vararg args: CliArg): Kli = Kli(args.toList())

class Kli(val options: List<CliArg>) {
    private val map: Map<String, CliArg> = kotlin.run {
        val map = HashMap<String, CliArg>()
        for (opt in options) {
            val existing = map[opt.longFlag] ?: map[opt.shortFlag]
            if(existing != null) {
                throw IllegalArgumentException("Conflict between $opt and $existing")
            } else {
                map[opt.longFlag] = opt
                if(opt.shortFlag != null) {
                    map[opt.shortFlag] = opt
                }
            }
        }
        map
    }

    fun parse(args: Array<String>): CliResult {
        val pos = ArrayList<String>()
        val opt = HashMap<String, String?>()

        var awaitingValue = false
        var awaitingFor = ""
        for(arg in args) {
            if(awaitingValue) {
                require(awaitingFor.isNotEmpty())
                opt[awaitingFor] = arg
                awaitingValue = false
                awaitingFor= ""
            } else if(arg.startsWith("-")) {
                val flag = map[arg]
                if(flag == null) {
                    throw CliParseException("Unknown arg $arg")
                } else {
                    if(!flag.requiresValue) {
                        opt[flag.name] = null
                    } else {
                        awaitingValue = true
                        awaitingFor = flag.name
                    }
                }
            } else {
                pos += arg
            }
        }

        if(awaitingValue) {
            throw CliParseException("Expecting value for $awaitingFor")
        }

        return CliResult(pos, opt)
    }
}
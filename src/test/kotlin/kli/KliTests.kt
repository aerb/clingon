package kli

import org.junit.Test

class KliTests {

    @Test
    fun complex() {
        val cli = Kli()

        val help by cli.flag("--help | -? | -h", help = "").count()
        val name by cli.optional("--name | -n", help = "").mappedTo { it.toInt() }
        val pos1 by cli.positional("pos1", help = "")
        val pos2 by cli.positional("pos2", help = "")

        cli.parse(arrayOf(""))
    }
}
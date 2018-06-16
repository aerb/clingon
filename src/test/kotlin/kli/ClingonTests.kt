package kli

import org.junit.Assert.*
import org.junit.Test

class ClingonTests {
    private val whitespace = Regex("\\s+")
    private fun String.toArgArray(): Array<String> =
        split(whitespace).toTypedArray()

    @Test
    fun complex() {
        val cli = Clingon()

//        val help by cli.flag("--help | -? | -h", help = "").count()
        val name by cli.option("--name | -n")
        val age by cli.option("--age | -a").map { it.toInt() }
        val tags by cli.option("--tag").map { it }.collect()

//        val pos1 by cli.positional("pos1", help = "")
//        val pos2 by cli.positional("pos2", help = "")

        cli.parse(arrayOf("--name", "ringo", "--age=10", "--tag", "a", "--tag", "b"))

        assertEquals("ringo", name)
        assertEquals(10, age)
        assertEquals(listOf("a", "b"), tags)
    }


    @Test
    fun single() {
        val cli = Clingon()

//        val help by cli.flag("--help | -? | -h", help = "").count()
        val name by cli.option("--name | -n")
        val age by cli.option("--age | -a").map { it.toInt() }
        val day by cli.option("-d").map { it.toInt() }

//        val pos1 by cli.positional("pos1", help = "")
//        val pos2 by cli.positional("pos2", help = "")

        cli.parse("-a=10 -n ringo -d10".toArgArray())

        assertEquals("ringo", name)
        assertEquals(10, age)
        assertEquals(10, day)
    }

    @Test
    fun `no arg`() {
        val cli = Clingon()

        val help by cli.flag("--help | -? | -h")
        val j by cli.flag("-j")
        val verbose by cli.flag("-v | --verbose").count()


//        val pos1 by cli.positional("pos1", help = "")
//        val pos2 by cli.positional("pos2", help = "")

        cli.parse("-? -vv --verbose".toArgArray())

        assertTrue(help)
        assertFalse(j)
        assertEquals(3, verbose)
    }

    @Test
    fun positions() {
        val cli = Clingon()

        val help by cli.flag("--help | -? | -h")
        val j by cli.flag("-j")
        val verbose by cli.flag("-v | --verbose").count()
        val first by cli.positional("first")
        val second by cli.positional("second").map { it.toInt() }


        cli.parse("-? -v hello 10".toArgArray())

        assertEquals("hello", first)
        assertEquals(10, second)
    }
}



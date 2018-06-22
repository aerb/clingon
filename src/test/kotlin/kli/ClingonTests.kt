package kli

import kotlin.test.*

@Suppress("UNUSED_VARIABLE")
class ClingonTests {
    private val whitespace = Regex("\\s+")
    private fun String.toArgArray(): Array<String> =
        if(isEmpty()) emptyArray() else split(whitespace).toTypedArray()

    @Test
    fun complex() {
        val cli = Clingon()

        val name by cli.option("--name | -n")
        val age by cli.option("--age | -a").map { it.toInt() }
        val tags by cli.option("--tag").map { it }.collect()
        val z by cli.option("-z").default { "ZZZ" }

        cli.parse("--name ringo --age=10 --tag a --tag b".toArgArray())

        assertEquals("ringo", name)
        assertEquals(10, age)
        assertEquals(listOf("a", "b"), tags)
        assertEquals("ZZZ", z)
    }

    @Test
    fun require() {
        val cli = Clingon()

        val x by cli.option("-x").require()

        assertFailsWith(IllegalArgumentException::class) {
            cli.parse("".toArgArray())
        }
    }

    @Test
    fun errors() {
        val cli = Clingon()
        val x by cli.flag("-x")
        val e = assertFailsWith(ParseException::class) { cli.parse("-x=".toArgArray()) }
        println(e.message)
        assertTrue(e.option != null && "-x" in e.option!!.flags)

    }

    @Test
    fun single() {
        val cli = Clingon()

        val name by cli.option("--name | -n")
        val age by cli.option("--age | -a").map { it.toInt() }
        val day by cli.option("-d").map { it.toInt() }

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

        cli.parse("-? -vv --verbose".toArgArray())

        assertTrue(help)
        assertFalse(j)
        assertEquals(3, verbose)
    }

    @Test
    fun combined() {
        val cli = Clingon()

        val a by cli.flag("-a")
        val b by cli.flag("-b")
        val c by cli.flag("-c")
        val d by cli.option("-d")

        cli.parse("-abcdHello".toArgArray())

        assertTrue(a)
        assertTrue(b)
        assertTrue(c)
        assertEquals("Hello", d)
    }

    @Test
    fun positions() {
        val cli = Clingon()

        val help by cli.flag("--help | -? | -h")
        val verbose by cli.flag("-v | --verbose").count()

        val first by cli.positional("first")
        val second by cli.positional("second").map { it.toInt() }
        val third by cli.positional("third").collect()

        cli.parse("-? -v hello 10 a b c d".toArgArray())

        assertEquals("hello", first)
        assertEquals(10, second)
        assertEquals(listOf("a", "b", "c", "d"), third)
    }
}



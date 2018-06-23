package kli

import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import kotlin.test.Test

@Suppress("UNUSED_VARIABLE")
class ClingonTests {
    @Test
    fun complex() {
        val cli = Clingon()

        val name by cli.option("--name | -n")
        val age by cli.option("--age | -a").map { it.toInt() }
        val tags by cli.option("--tag").map { it }.collect()
        val z by cli.option("-z").default { "ZZZ" }

        cli.parse("--name ringo --age=10 --tag a --tag b")

        name shouldEqual "ringo"
        age shouldEqual 10
        tags shouldEqual listOf("a", "b")
        z shouldEqual "ZZZ"
    }

    @Test
    fun require() {
        val cli = Clingon()
        val x by cli.option("-x").require()

        invoking { cli.parse("") } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun errors() {
        val cli = Clingon()
        val x by cli.flag("-x")

        val e = invoking { cli.parse("-x=") } shouldThrow ParseException::class

        "-x" shouldBeInNullable e.exception.option?.flags
    }

    @Test
    fun single() {
        val cli = Clingon()

        val name by cli.option("--name | -n")
        val age by cli.option("--age | -a").map { it.toInt() }
        val day by cli.option("-d").map { it.toInt() }

        cli.parse("-a=10 -n ringo -d10")

        name shouldEqual "ringo"
        age shouldEqual 10
        day shouldEqual 10
    }

    @Test
    fun `no arg`() {
        val cli = Clingon()

        val help by cli.flag("--help | -? | -h")
        val j by cli.flag("-j")
        val verbose by cli.flag("-v | --verbose").count()

        cli.parse("-? -vv --verbose")


        help shouldEqual true
        j shouldEqual false
        verbose shouldEqual 3
    }

    @Test
    fun combined() {
        val cli = Clingon()

        val a by cli.flag("-a")
        val b by cli.flag("-b")
        val c by cli.flag("-c")
        val d by cli.option("-d")

        cli.parse("-abcdHello")

        a shouldEqual true
        b shouldEqual true
        c shouldEqual true
        d shouldEqual "Hello"
    }

    @Test
    fun positions() {
        val cli = Clingon()

        val help by cli.flag("--help | -? | -h")
        val verbose by cli.flag("-v | --verbose").count()

        val first by cli.positional("first")
        val second by cli.positional("second").map { it.toInt() }
        val third by cli.positional("third").collect()

        cli.parse("-? -v hello 10 a b c d")

        first shouldEqual "hello"
        second shouldEqual 10
        third shouldEqual listOf("a", "b", "c", "d")
    }

    @Test
    fun multiple() {
        val cli = Clingon()
        val i by cli.option("-i").collect()
        val o by cli.option("-o").collect()

        cli.parse("-i a b c -o d e -o f")

        i shouldEqual listOf("a", "b", "c")
        o shouldEqual listOf("d", "e", "f")
    }
}


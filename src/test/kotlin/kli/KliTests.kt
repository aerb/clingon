package kli

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KliTests {
    @Test
    fun `parse one optional`() {
        val cli = buildCli(
            CliArg(
                name = "name",
                description = "this is a description."
            )
        )

        val (pos, opt) = cli.parse(arrayOf("--name", "ringo"))

        assertEquals(pos.size, 0)
        assertEquals(opt["name"], "ringo")
    }

    @Test
    fun `parse short optional`() {
        val cli = buildCli(
            CliArg(
                name = "name",
                short = "n",
                description = "this is a description."
            )
        )



        val (pos, opt) = cli.parse(arrayOf("-n", "ringo"))

        assertEquals(pos.size, 0)
        assertEquals(opt["name"], "ringo")
    }

    @Test
    fun `parse one no value`() {
        val cli = buildCli(
            CliArg(
                name = "name",
                description = "this is a description.",
                requiresValue = false
            )
        )

        val (pos, opt) = cli.parse(arrayOf("--name"))

        assertEquals(pos.size, 0)
        assertTrue("name" in opt)
        assertEquals(opt["name"], null)
    }

    @Test
    fun complex() {
        val cli = buildCli(
            CliArg(
                name = "name",
                description = "The name."
            ),
            CliArg(
                name = "age",
                description = "The age.",
                short = "a"
            ),
            CliArg(
                name = "verbose",
                description = "Increase verbosity.",
                requiresValue = false
            )
        )

        val (pos, opt) = cli.parse(arrayOf("--name", "ringo", "-a", "50", "--verbose", "action"))

        assertEquals(pos, listOf("action"))
        assertEquals(opt, mapOf(
            "name" to "ringo",
            "age" to "50",
            "verbose" to null
        ))
    }
}
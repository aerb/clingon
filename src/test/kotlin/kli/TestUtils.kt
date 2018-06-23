package kli

import org.amshove.kluent.shouldBeIn

val whitespace = Regex("\\s+")
fun String.toArgArray(): Array<String> =
    if(isEmpty()) emptyArray() else split(whitespace).toTypedArray()

fun Clingon.parse(args: String) = parse(args.toArgArray())

fun invoking(function: () -> Unit): () -> Unit = function

infix fun <T> Any?.shouldBeInNullable(iterable: List<T>?) { shouldBeIn(iterable ?: emptyList()) }
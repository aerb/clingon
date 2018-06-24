package clingon

internal fun MatchGroupCollection.requiredGroup(i: Int): String =
    this[i]?.value ?: throw IllegalStateException("Expected group of index $i.")

private val validFlag = Regex("^-{1,2}[a-zA-Z0-9-_?]+")
internal fun parseFlagString(s: String): List<String> =
    s.split("|").map {
        it.trim().also {
            require(it.matches(validFlag)) { "Illegal flag name $it" }
        }
    }

internal fun ArgumentStore<*,*>.any(predicate: (ArgumentStore<*,*>) -> Boolean): Boolean =
    if(predicate(this)) true
    else {
        if(this is PropagatingStore) {
            next?.any(predicate) ?: false
        } else false
    }
package clingon

import kotlin.reflect.KProperty

interface ParserLifecycle {
    fun onPreParse() {}
    fun onPostParse() {}
}

interface ArgumentDelegate<T> {
    val argument: ArgumentDefinition
    operator fun getValue(a: Any?, property: KProperty<*>?): T?
}

interface ChainableArgumentDelegate<TOut>: ArgumentDelegate<TOut> {

    var next: ArgumentStore<TOut, *>?

    fun default(function: () -> TOut): ChainableArgumentDelegate<TOut> =
        DefaultStore(function, argument)
            .also { next = it }

    fun require(): ArgumentDelegate<TOut> =
        RequiredStore<TOut>(argument)
            .also { next = it }

    fun <TMapped> map(transform: (TOut) -> TMapped): ChainableArgumentDelegate<TMapped> =
        MapStore(transform, argument)
            .also { next = it }

    fun collect(atLeast: Int = 0, atMost: Int = Int.MAX_VALUE): ArgumentDelegate<List<TOut>> =
        CollectStore<TOut>(atLeast..atMost, argument)
            .also { next = it }

    fun collectExactly(count: Int): ArgumentDelegate<List<TOut>> =
        CollectStore<TOut>(count..count, argument)
            .also { next = it }

    fun count(): ArgumentDelegate<Int> =
        CountStore<TOut>(argument)
            .also { next = it }
}

interface ArgumentStore<in TIn, TOut>: ArgumentDelegate<TOut>, ParserLifecycle {
    override val argument: ArgumentDefinition
    fun storeValue(v: TIn)
    fun willAcceptValue(): Boolean = getValue(null, null) == null
}

interface AggregateStore {
    val range: IntRange
}

interface PropagatingStore<TIn, TOut>: ArgumentStore<TIn, TOut>, ChainableArgumentDelegate<TOut> {
    override fun willAcceptValue(): Boolean = next?.willAcceptValue() ?: super.willAcceptValue()
    override fun onPreParse() { next?.onPreParse() }
    override fun onPostParse() { next?.onPostParse() }
}

class RequiredStore<TIn>(
    override val argument: ArgumentDefinition
): ArgumentStore<TIn, TIn> {
    private var value: TIn? = null
    override fun storeValue(v: TIn) { value = v }
    override fun getValue(a: Any?, property: KProperty<*>?): TIn =
        value ?: throw IllegalStateException("value should not be null.")
    override fun willAcceptValue(): Boolean = value != null
    override fun onPreParse() {}
    override fun onPostParse() {
        if(value == null) {
            throw IllegalArgumentException("Argument required.")
        }
    }
}

class DefaultStore<TIn>(
    private val function: () -> TIn,
    override val argument: ArgumentDefinition
): PropagatingStore<TIn, TIn> {
    override var next: ArgumentStore<TIn, *>? = null
    private var value: TIn? = null
    override fun storeValue(v: TIn) {
        value = v
        next?.storeValue(v)
    }
    override fun getValue(a: Any?, property: KProperty<*>?): TIn =
        value ?: throw IllegalStateException("_val is null.")

    override fun onPostParse() {
        if(value == null) {
            val v = function()
            value = v
            next?.storeValue(v)
        }
        next?.onPostParse()
    }
}

class MapStore<TIn, TOut>(
    private val transform: (TIn) -> TOut,
    override val argument: ArgumentDefinition
): PropagatingStore<TIn, TOut> {
    override var next: ArgumentStore<TOut, *>? = null
    private var value: TOut? = null
    override fun storeValue(v: TIn) {
        val transformed = transform(v)
        value = transformed
        next?.storeValue(transformed)
    }
    override fun getValue(a: Any?, property: KProperty<*>?): TOut? = value
}

class CollectStore<TIn>(
    override val range: IntRange,
    override val argument: ArgumentDefinition
): ArgumentStore<TIn, List<TIn>>, AggregateStore {
    private val arguments = ArrayList<TIn>()
    override fun storeValue(v: TIn) { arguments += v }
    override fun getValue(a: Any?, property: KProperty<*>?): List<TIn> = arguments
    override fun willAcceptValue(): Boolean = arguments.size < range.endInclusive
    override fun onPostParse() {
        super.onPostParse()
        if(arguments.size !in range) {
            throw IllegalArgumentException("Arguments no in range $range")
        }
    }
}

class CountStore<TIn>(
    override val argument: ArgumentDefinition
): ArgumentStore<TIn, Int>, AggregateStore {
    private var count = 0
    override fun storeValue(v: TIn) { count ++ }
    override fun getValue(a: Any?, property: KProperty<*>?): Int = count
    override fun willAcceptValue(): Boolean = true
    override val range: IntRange = IntRange(0, Int.MAX_VALUE)
}

class SingleStore(
    override val argument: ArgumentDefinition
): PropagatingStore<String, String> {
    override var next: ArgumentStore<String, *>? = null
    private var value: String? = null
    private var enforceSingle = true

    override fun onPreParse() {
        next?.onPreParse()
        enforceSingle = !any { it is AggregateStore }
    }

    override fun storeValue(v: String) {
        if(value != null && enforceSingle) {
            throw IllegalArgumentException("Only single value expected for $this")
        } else {
            value = v
            next?.storeValue(v)
        }
    }
    override fun getValue(a: Any?, property: KProperty<*>?): String? = value
}

class BoolStore(
    override val argument: ArgumentDefinition
): PropagatingStore<Any?, Boolean> {
    override var next: ArgumentStore<Boolean, *>? = null
    private var value: Boolean = false
    override fun storeValue(v: Any?) {
        value = true
        next?.storeValue(true)
    }
    override fun getValue(a: Any?, property: KProperty<*>?): Boolean = value
}
package kli

import kotlin.reflect.KProperty

interface ParserLifecycle {
    fun onPreParse() {}
    fun onPostParse() {}
}

interface ArgumentDelegate<T> {
    operator fun getValue(a: Any?, property: KProperty<*>?): T?
}

interface ChainableArgumentDelegate<TOut>: ArgumentDelegate<TOut> {

    var next: ArgumentStore<TOut, *>?

    fun default(function: () -> TOut): ChainableArgumentDelegate<TOut> =
        DefaultStore(function)
            .also { next = it }

    fun require(): ArgumentDelegate<TOut> =
        RequiredStore<TOut>()
            .also { next = it }

    fun <TMapped> map(transform: (TOut) -> TMapped): ChainableArgumentDelegate<TMapped> =
        MapStore(transform)
            .also { next = it }

    fun collect(range: IntRange = 0..Int.MAX_VALUE): ArgumentDelegate<List<TOut>> =
        CollectStore<TOut>(range)
            .also { next = it }

    fun count(): ArgumentDelegate<Int> =
        CountStore<TOut>()
            .also { next = it }
}

interface ArgumentStore<in TIn, TOut>: ArgumentDelegate<TOut>, ParserLifecycle {
    fun storeValue(v: TIn)
    fun willAcceptValue(): Boolean =
        getValue(null, null) == null
}

interface AggregateStore {

}

interface PropagatingStore<TIn, TOut>: ArgumentStore<TIn, TOut>, ChainableArgumentDelegate<TOut> {
    override fun willAcceptValue(): Boolean =
        next?.willAcceptValue() ?: (getValue(null, null) == null)
    

    override fun onPreParse() {
        next?.onPreParse()
    }

    override fun onPostParse() {
        next?.onPostParse()
    }
}

class RequiredStore<TIn>: ArgumentStore<TIn, TIn> {
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
    private val function: () -> TIn
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
    private val transform: (TIn) -> TOut
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

class CollectStore<TIn>(private val range: IntRange): ArgumentStore<TIn, List<TIn>>, AggregateStore {
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

class CountStore<TIn>: ArgumentStore<TIn, Int>, AggregateStore {
    private var count = 0
    override fun storeValue(v: TIn) { count ++ }
    override fun getValue(a: Any?, property: KProperty<*>?): Int = count
    override fun willAcceptValue(): Boolean = true
}

class SingleStore: PropagatingStore<String, String> {
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

class BoolStore: PropagatingStore<Any?, Boolean> {
    override var next: ArgumentStore<Boolean, *>? = null
    private var value: Boolean = false
    override fun storeValue(v: Any?) {
        value = true
        next?.storeValue(true)
    }
    override fun getValue(a: Any?, property: KProperty<*>?): Boolean = value
}
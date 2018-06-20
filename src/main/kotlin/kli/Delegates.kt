package kli

import kotlin.reflect.KProperty

interface ParserLifecycle {
    fun onPreParse()
    fun onPostParse()
}

interface SimpleDelegate<in TIn, TOut>: ParserLifecycle {
    fun setValue(v: TIn)
    operator fun getValue(a: Any?, property: KProperty<*>): TOut?
}

interface AggregateDelegate

interface PropagatingDelegate<TIn, TOut>: SimpleDelegate<TIn, TOut> {
    var delegate: SimpleDelegate<TOut, *>?
    
    override fun onPreParse() {
        delegate?.onPreParse()
    }

    override fun onPostParse() {
        delegate?.onPostParse()
    }

    fun default(function: () -> TOut): DefaultDelegate<TOut> =
        DefaultDelegate(function)
            .also { delegate = it }

    fun require(): RequireDelegate<TOut> =
        RequireDelegate<TOut>()
            .also { delegate = it }

    fun <TMapped> map(transform: (TOut) -> TMapped): MapDelegate<TOut, TMapped> =
        MapDelegate(transform)
            .also { delegate = it }

    fun collect(range: IntRange = 0..Int.MAX_VALUE): CollectDelegate<TOut> =
        CollectDelegate<TOut>(range)
            .also { delegate = it }

    fun count(): CountDelegate<TOut> =
        CountDelegate<TOut>()
            .also { delegate = it }
}

class RequireDelegate<TIn>: SimpleDelegate<TIn, TIn> {
    private var _val: TIn? = null
    override fun setValue(v: TIn) { _val = v }
    override fun getValue(a: Any?, property: KProperty<*>): TIn =
        _val ?: throw IllegalStateException("_val is null.")
    override fun onPreParse() {}
    override fun onPostParse() {
        if(_val == null) {
            throw IllegalArgumentException("")
        }
    }
}

class DefaultDelegate<TIn>(
    private val function: () -> TIn
): PropagatingDelegate<TIn, TIn> {
    override var delegate: SimpleDelegate<TIn, *>? = null
    private var _val: TIn? = null
    override fun setValue(v: TIn) {
        _val = v
        delegate?.setValue(v)
    }
    override fun getValue(a: Any?, property: KProperty<*>): TIn =
        _val ?: throw IllegalStateException("_val is null.")

    override fun onPostParse() {
        if(_val == null) {
            val v = function()
            _val = v
            delegate?.setValue(v)

        }
    }
}

class MapDelegate<TIn, TOut>(
    private val transform: (TIn) -> TOut
): PropagatingDelegate<TIn, TOut> {
    override var delegate: SimpleDelegate<TOut, *>? = null
    private var _val: TOut? = null
    override fun setValue(v: TIn) {
        val transformed = transform(v)
        _val = transformed
        delegate?.setValue(transformed)
    }
    override fun getValue(a: Any?, property: KProperty<*>): TOut? = _val
}

class CollectDelegate<TIn>(private val range: IntRange): PropagatingDelegate<TIn, List<TIn>>, AggregateDelegate {
    override var delegate: SimpleDelegate<List<TIn>, *>? = null
    private val _val = ArrayList<TIn>()
    override fun setValue(v: TIn) { _val += v }
    override fun getValue(a: Any?, property: KProperty<*>): List<TIn> = _val
    override fun onPostParse() {
        super.onPostParse()
        if(_val.size !in range) {
            throw IllegalArgumentException("")
        }
    }
}

class CountDelegate<TIn>: PropagatingDelegate<TIn, Int>, AggregateDelegate {
    override var delegate: SimpleDelegate<Int, *>? = null
    private var _val = 0
    override fun setValue(v: TIn) { _val ++ }
    override fun getValue(a: Any?, property: KProperty<*>): Int = _val
}

class StringDelegate: PropagatingDelegate<String, String> {
    override var delegate: SimpleDelegate<String, *>? = null
    private var _v: String? = null
    private var enforceSingle = true

    override fun onPreParse() {
        delegate?.onPreParse()
        enforceSingle = !any { it is AggregateDelegate }
    }

    override fun setValue(v: String) {
        if(_v != null && enforceSingle) {
            throw IllegalArgumentException("Only single value expected for $this")
        } else {
            _v = v
            delegate?.setValue(v)
        }
    }
    override fun getValue(a: Any?, property: KProperty<*>): String? = _v
}

class BoolDelegate: PropagatingDelegate<Any?, Boolean> {
    override var delegate: SimpleDelegate<Boolean, *>? = null
    private var _v: Boolean = false
    override fun setValue(v: Any?) {
        _v = true
        delegate?.setValue(true)
    }
    override fun getValue(a: Any?, property: KProperty<*>): Boolean = _v
}
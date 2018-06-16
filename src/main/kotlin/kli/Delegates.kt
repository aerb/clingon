package kli

import kotlin.reflect.KProperty

interface Delegate<in TIn, TOut> {
    fun pushValue(v: TIn)
    fun setDelegate(d: Delegate<TOut, *>)
    operator fun getValue(c: Clingon?, property: KProperty<*>): TOut?

    fun <TMapped> map(transform: (TOut) -> TMapped): MapDelegate<TOut, TMapped> =
        MapDelegate(transform)
            .also { setDelegate(it) }

    fun collect(): CollectDelegate<TOut> =
        CollectDelegate<TOut>()
            .also { setDelegate(it) }

    fun count(): CountDelegate<TOut> =
        CountDelegate<TOut>()
            .also { setDelegate(it) }
}

class MapDelegate<It, Ot>(
    private val transform: (It) -> Ot
): Delegate<It, Ot> {

    private var _val: Ot? = null
    private var _d: Delegate<Ot, *>? = null

    override fun setDelegate(d: Delegate<Ot, *>) {
        _d = d
    }

    override fun pushValue(v: It) {
        _val = transform(v)
        _d?.pushValue(_val!!)
    }

    override fun getValue(c: Clingon?, property: KProperty<*>): Ot? = _val
}

class CollectDelegate<It>: Delegate<It, List<It>> {

    private val _val = ArrayList<It>()
    private var _d: Delegate<List<It>, *>? = null

    override fun pushValue(v: It) {
        _val += v
    }

    override fun setDelegate(d: Delegate<List<It>, *>) {
        _d = d
    }

    override fun getValue(c: Clingon?, property: KProperty<*>): List<It> = _val
}

class CountDelegate<TIn>: Delegate<TIn, Int> {
    private var _val = 0
    private var _d: Delegate<Int, *>? = null
    override fun pushValue(v: TIn) {
        _val ++
    }

    override fun setDelegate(d: Delegate<Int, *>) {
        _d = d
    }

    override fun getValue(c: Clingon?, property: KProperty<*>): Int = _val
}

class StringDelegate: Delegate<String, String> {
    private var _v: String? = null
    private var _d: Delegate<String, *>? = null
    override fun pushValue(v: String) {
        _v = v
        _d?.pushValue(v)
    }

    override fun setDelegate(d: Delegate<String, *>) {
        _d = d
    }

    override fun getValue(c: Clingon?, property: KProperty<*>): String? = _v
}


class BoolDelegate: Delegate<Unit, Boolean> {

    private var _v: Boolean = false
    private var _d: Delegate<Boolean, *>? = null

    override fun pushValue(v: Unit) {
        _v = true
        _d?.pushValue(true)
    }

    override fun setDelegate(d: Delegate<Boolean, *>) {
        _d = d
    }

    override fun getValue(c: Clingon?, property: KProperty<*>): Boolean = _v
}
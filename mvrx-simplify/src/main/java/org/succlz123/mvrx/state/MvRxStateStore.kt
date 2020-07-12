package org.succlz123.mvrx.state

interface MvRxStateStore<S : Any> {
    var state: S
    var listener: MvRxStateListener<S>
    fun get(block: (S) -> Unit)
    fun set(stateReducer: S.() -> S)
    fun dispose()
}

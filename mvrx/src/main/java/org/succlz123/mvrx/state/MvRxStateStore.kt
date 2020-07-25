package org.succlz123.mvrx.state

import org.succlz123.mvrx.lifecycle.MvRxLifecycleAwareObserver

/**
 * Interface that has to be implemented by all Kotlin data classes that will be used as state.
 *
 * This is currently only used to configure Proguard to correctly work with MvRx.
 *
 */
interface MvRxState

interface MvRxStateStore<S : Any> {
    fun get(): S
    fun set(stateReducer: S.() -> S)
    var listener: MvRxStateListener<S>
    fun dispose()
}

interface MvRxStateListener<S> {

    fun add(subscriber: (S) -> Unit)

    fun remove(subscriber: (S) -> Unit)

    fun add(subscriber: MvRxLifecycleAwareObserver<S>)

    fun remove(subscriber: MvRxLifecycleAwareObserver<S>)

    fun newValue(s: S)

    fun clear()
}

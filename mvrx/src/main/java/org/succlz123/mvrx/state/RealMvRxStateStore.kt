package org.succlz123.mvrx.state

import org.succlz123.mvrx.lifecycle.MvRxLifecycleAwareObserver
import java.util.*

class RealMvRxStateStore<S : Any>(var state: S) : MvRxStateStore<S> {

    override var listener: MvRxStateListener<S> =
        RealMvRxStateListener()

    override fun get(): S {
        return state
    }

    override fun set(stateReducer: S.() -> S) {
        val newState = state.stateReducer()
        if (newState != state) {
            state = newState
            listener.newValue(state)
        }
    }

    override fun dispose() {
        listener.clear()
    }

    private class RealMvRxStateListener<S> : MvRxStateListener<S> {
        private val subscriberList1 = ArrayList<(S) -> Unit>()
        private val subscriberList2 = ArrayList<MvRxLifecycleAwareObserver<S>>()

        override fun add(subscriber: (S) -> Unit) {
            subscriberList1.add(subscriber)
        }

        override fun remove(subscriber: (S) -> Unit) {
            subscriberList1.remove(subscriber)
        }

        override fun add(subscriber: MvRxLifecycleAwareObserver<S>) {
            subscriberList2.add(subscriber)
        }

        override fun remove(subscriber: MvRxLifecycleAwareObserver<S>) {
            subscriberList2.remove(subscriber)
        }

        override fun newValue(s: S) {
            for (item in subscriberList1) {
                item.invoke(s)
            }
            for (mvRxLifecycleAwareObserver in subscriberList2) {
                mvRxLifecycleAwareObserver.onChange(s)
            }
        }

        override fun clear() {
            subscriberList1.clear()
            subscriberList2.clear()
        }
    }
}

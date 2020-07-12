package org.succlz123.mvrx.state

import java.util.*

/**
 * This is a container class around the actual state itself. It has a few optimizations to ensure
 * safe usage of state.
 *
 * All state reducers are run in a single background thread to ensure that they don't have race
 * conditions with each other.
 *
 */
class RealMvRxStateStore<S : Any>(initialState: S) : MvRxStateStore<S> {
    private val jobs = Jobs<S>()

    override var listener: MvRxStateListener<S> = RealMvRxStateListener()

    override var state: S = initialState

    override fun get(block: (S) -> Unit) {
        jobs.enqueueGetStateBlock(block)
        flushQueues()
    }

    override fun set(stateReducer: S.() -> S) {
        jobs.enqueueSetStateBlock(stateReducer)
        flushQueues()
    }

    private tailrec fun flushQueues() {
        flushSetStateQueue()
        val block = jobs.dequeueGetStateBlock() ?: return
        block(state)
        flushQueues()
    }

    private fun flushSetStateQueue() {
        val blocks = jobs.dequeueAllSetStateBlocks() ?: return
        for (block in blocks) {
            val newState = state.block()
            // do not coalesce state change. it's more expected to notify for every state change.
            if (newState != state) {
                state = newState
                listener.newValue(state)
            }
        }
    }

    override fun dispose() {
        listener.clear()
        jobs.clear()
    }

    private class Jobs<S> {
        private val getStateQueue = LinkedList<(state: S) -> Unit>()
        private var setStateQueue = LinkedList<S.() -> S>()

        fun enqueueGetStateBlock(block: (state: S) -> Unit) {
            getStateQueue.add(block)
        }

        fun enqueueSetStateBlock(block: S.() -> S) {
            setStateQueue.add(block)
        }

        fun dequeueGetStateBlock(): ((state: S) -> Unit)? {
            return getStateQueue.poll()
        }

        fun dequeueAllSetStateBlocks(): List<(S.() -> S)>? {
            // do not allocate empty queue for no-op flushes
            if (setStateQueue.isEmpty()) {
                return null
            }
            val queue = setStateQueue
            setStateQueue = LinkedList()
            return queue
        }

        fun clear() {
            getStateQueue.clear()
            setStateQueue.clear()
        }
    }

    private class RealMvRxStateListener<S> : MvRxStateListener<S> {
        private val subscriberList = ArrayList<(S) -> Unit>()

        override fun add(subscriber: (S) -> Unit) {
            subscriberList.add(subscriber)
        }

        override fun newValue(s: S) {
            for (item in subscriberList) {
                item.invoke(s)
            }
        }

        override fun clear() {
            subscriberList.clear()
        }
    }
}


/**
 * Job scheduling algorithm
 * We use double-queue design to prioritize `setState` blocks over `getState` blocks.
 * `setStateQueue` has higher priority and needs to be flushed before taking tasks from `getStateQueue`.
 * If a `getState` block enqueues a `setState` block, it should be executed before executing the next `getState` block.
 * This is to prevent a race condition when `getState` itself enqueues a `setState`, for example:
 * ```
 * getState { state ->
 *   if (state.isLoading) return
 *   setState { state ->
 *     state.copy(isLoading = true)
 *   }
 *   // make a network call
 * }
 * ```
 * In the above example, we have to run the inner `setState` before the next `getState`.
 * Otherwise if we call this code twice consecutively, we could end up with making network call twice.
 *
 * Let's simplify the scenario as following:
 * ```
 * getStateA {
 *   setStateA {}
 * }
 * getStateB {
 *   setStateB {}
 * }
 * ```
 * With a single queue design, the execution order is the same as enqueuing order.
 * i.e. `getStateA -> getStateB -> setStateA -> setStateB`
 *
 * With our double queue design, what is happening is:
 *
 * 1) after both `getState`s are enqueued
 *   - setStateQueue: []
 *   - getStateQueue: [A, B]
 *
 * 2) after first `getState` is executed
 *   - setStateQueue: [A]
 *   - getStateQueue: [B]
 * 3) since reducer has higher priority, we execute it
 *   - setStateQueue: []
 *   - getStateQueue: [B]
 * 4) setStateB is executed
 *  - setStateQueue: []
 *  - getStateQueue: []
 *
 * The execution order is `getStateA->setStateA->getStateB ->setStateB`
 *
 * Note that the race condition can also be solved by not introducing the `getState` API, as following:
 * ```
 * setState { state -> // `setState` is simply a more "powerful" version of `getState`
 *   if (state.isLoading) return
 *   state.copy(isLoading = true)
 *   // make a network call
 * }
 * ```
 * The above code will run without race condition using single queue design.
 * However, we think it's valuable to have a separate `getState` API,
 * as it has a different semantic meaning and improves readability.
 */
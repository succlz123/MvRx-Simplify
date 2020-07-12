package org.succlz123.mvrx.lifecycle

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import org.succlz123.mvrx.base.DeliveryMode
import org.succlz123.mvrx.base.RedeliverOnStart
import org.succlz123.mvrx.base.UniqueOnly
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An wrapper around an [Observer] associated with a [LifecycleOwner]. It has an [activeState], and when in a lifecycle state greater
 * than the [activeState] (as defined by [Lifecycle.State.isAtLeast()]) it will deliver values to the [sourceObserver] or [onNext] lambda.
 * When in a lower lifecycle state, the most recent update will be saved, and delivered when active again.
 */
abstract class MvRxLifecycleAwareObserver<T>(
    private var owner: LifecycleOwner?,
    private val activeState: State = DEFAULT_ACTIVE_STATE,
    private val deliveryMode: DeliveryMode,
    private var lastDeliveredValue: T?
) : LifecycleObserver {
    private var isDestroy: Boolean = false
    private var lastUndeliveredValue: T? = null
    private val locked = AtomicBoolean(true)

    var lastValue: T? = null

    init {
        owner?.let {
            requireOwner().lifecycle.addObserver(this)
        }
    }

    @OnLifecycleEvent(Event.ON_DESTROY)
    fun onDestroy() {
        requireOwner().lifecycle.removeObserver(this)
        lastDeliveredValue = null
        owner = null
        isDestroy = true
    }

    @OnLifecycleEvent(Event.ON_ANY)
    fun onLifecycleEvent() {
        updateLock()
    }

    private fun updateLock() {
        if (owner?.lifecycle?.currentState?.isAtLeast(activeState) == true) {
            unlock()
        } else {
            lock()
        }
    }

    fun onNext(nextValue: T) {
        lastValue = nextValue
        if (locked.get()) {
            lastUndeliveredValue = nextValue
        } else {
            val suppressRepeatedFirstValue = deliveryMode is UniqueOnly && lastDeliveredValue == nextValue
            lastDeliveredValue = null
            if (!suppressRepeatedFirstValue) {
                onChange(nextValue)
            }
        }
    }

    abstract fun onChange(nextValue: T)

    abstract fun onComplete()

    private fun unlock() {
        if (!locked.getAndSet(false)) {
            return
        }
        if (!isDestroy) {
            val valueToDeliverOnUnlock = when {
                deliveryMode is UniqueOnly -> lastUndeliveredValue
                deliveryMode is RedeliverOnStart && lastUndeliveredValue != null -> lastUndeliveredValue
                deliveryMode is RedeliverOnStart && lastUndeliveredValue == null -> lastValue
                else -> throw IllegalStateException("Value to deliver on unlock should be exhaustive.")
            }
            lastUndeliveredValue = null
            if (valueToDeliverOnUnlock != null) {
                onChange(valueToDeliverOnUnlock)
            }
        }
    }

    private fun lock() {
        locked.set(true)
    }

    private fun requireOwner(): LifecycleOwner = requireNotNull(owner) { "Cannot access lifecycleOwner after onDestroy." }

    companion object {
        private val DEFAULT_ACTIVE_STATE = State.STARTED
    }
}

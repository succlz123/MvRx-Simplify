package org.succlz123.mvrx.base

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import org.succlz123.mvrx.extension.FORCE_DEBUG
import org.succlz123.mvrx.extension.FORCE_DISABLE_LIFECYCLE_AWARE_OBSERVER
import org.succlz123.mvrx.extension.MutableStateChecker
import org.succlz123.mvrx.extension.assertImmutability
import org.succlz123.mvrx.lifecycle.MvRxLifecycleAwareObserver
import org.succlz123.mvrx.state.MvRxState
import org.succlz123.mvrx.state.MvRxStateStore
import org.succlz123.mvrx.state.RealMvRxStateStore
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import kotlin.collections.HashMap
import kotlin.reflect.KProperty1

abstract class BaseMvRxViewModel<S : MvRxState>(
        initialState: S,
        debugMode: Boolean = false,
        private val stateStore: MvRxStateStore<S> = RealMvRxStateStore(initialState)
) : ViewModel() {
    private val debugMode = if (FORCE_DEBUG == null) debugMode else FORCE_DEBUG

    private val tag by lazy { javaClass.simpleName }

    private val mainHandler: Handler = Handler(Looper.getMainLooper())

    private lateinit var mutableStateChecker: MutableStateChecker<S>

    private val lastDeliveredStates = ConcurrentHashMap<String, Any>()

    private val activeSubscriptions = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private val lifecycleOwner: LifecycleOwner = LifecycleOwner { lifecycleRegistry }

    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(lifecycleOwner).apply { currentState = Lifecycle.State.RESUMED }

    internal val state: S
        get() = stateStore.state

    init {
        if (this.debugMode) {
            mutableStateChecker = MutableStateChecker(initialState)
            Thread { validateState() }.start()
        }
    }

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        stateStore.dispose()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    protected fun setState(reducer: S.() -> S) {
        if (debugMode) {
            // Must use `set` to ensure the validated state is the same as the actual state used in reducer
            // Do not use `get` since `getState` queue has lower priority and the validated state would be the state after reduced
            stateStore.set {
                val firstState = this.reducer()
                val secondState = this.reducer()

                if (firstState != secondState) {
                    @Suppress("UNCHECKED_CAST")
                    val changedProp = firstState::class.java.declaredFields.asSequence()
                            .onEach { it.isAccessible = true }
                            .firstOrNull { property ->
                                @Suppress("Detekt.TooGenericExceptionCaught")
                                try {
                                    property.get(firstState) != property.get(secondState)
                                } catch (e: Throwable) {
                                    false
                                }
                            }
                    if (changedProp != null) {
                        throw IllegalArgumentException(
                                "Impure reducer set on ${this@BaseMvRxViewModel::class.java.simpleName}! " +
                                        "${changedProp.name} changed from ${changedProp.get(firstState)} " +
                                        "to ${changedProp.get(secondState)}. " +
                                        "Ensure that your state properties properly implement hashCode."
                        )
                    } else {
                        throw IllegalArgumentException(
                                "Impure reducer set on ${this@BaseMvRxViewModel::class.java.simpleName}! Differing states were provided by the same reducer." +
                                        "Ensure that your state properties properly implement hashCode. First state: $firstState -> Second state: $secondState"
                        )
                    }
                }
                mutableStateChecker.onStateChanged(firstState)
                firstState
            }
        } else {
            stateStore.set(reducer)
        }
    }

    protected fun postState(reducer: S.() -> S) {
        mainHandler.post {
            setState(reducer)
        }
    }

    protected fun withState(block: (state: S) -> Unit) {
        stateStore.get(block)
    }

    /**
     * Validates a number of properties on the state class. This cannot be called from the main thread because it does
     * a fair amount of reflection.
     */
    private fun validateState() {
        state::class.assertImmutability()
    }

    fun logStateChanges() {
        if (!debugMode) {
            return
        }
        subscribe { Log.d(tag, "New State: $it") }
    }

    protected fun <V> execute(executor: Executor, call: () -> V?, start: (() -> Unit)? = null, result: ((v: V?) -> Unit)? = null, error: ((e: Exception) -> Unit)? = null) {
        executor.execute {
            mainHandler.post { start?.invoke() }
            try {
                val v = call.invoke()
                mainHandler.post { result?.invoke(v) }
            } catch (e: Exception) {
                mainHandler.post { error?.invoke(e) }
            }
        }
    }

    /**
     * For ViewModels that want to subscribe to itself.
     */
    protected fun subscribe(subscriber: (S) -> Unit) {
        stateStore.resolveSubscription(null, RedeliverOnStart, subscriber)
    }

    /**
     * For ViewModels that want to subscribe to another ViewModel.
     */
    protected fun <S : MvRxState> subscribe(viewModel: BaseMvRxViewModel<S>, subscriber: (S) -> Unit) {
        assertSubscribeToDifferentViewModel(viewModel)
        viewModel.subscribe(lifecycleOwner, RedeliverOnStart, subscriber)
    }

    fun subscribe(owner: LifecycleOwner, deliveryMode: DeliveryMode = RedeliverOnStart, subscriber: (S) -> Unit) {
        stateStore.resolveSubscription(owner, deliveryMode, subscriber)
    }

    fun selectProperty(vararg prop1s: KProperty1<S, Any>): PropertyStore {
        val store = PropertyStore()
        for (prop1 in prop1s) {
            store.map[prop1] = null
        }
        return store
    }

    inner class PropertyStore {
        val map = HashMap<KProperty1<S, Any>, Any?>()

        operator fun plus(other: PropertyStore): PropertyStore {
            val store = PropertyStore()
            this.map.forEach { store.map[it.key] = it.value }
            other.map.forEach { store.map[it.key] = it.value }
            return store
        }

        fun subscribe(deliveryMode: DeliveryMode = RedeliverOnStart, subscriber: (S) -> Unit) {
            selectSubscribe(map, deliveryMode, subscriber)
        }

        fun subscribe(otherVm: BaseMvRxViewModel<S>, deliveryMode: DeliveryMode = RedeliverOnStart, subscriber: (S) -> Unit) {
            selectSubscribe(map, otherVm, deliveryMode, subscriber)
        }

        fun subscribe(owner: LifecycleOwner?, deliveryMode: DeliveryMode = RedeliverOnStart, subscriber: (S) -> Unit) {
            selectSubscribe(map, owner, deliveryMode, subscriber = subscriber)
        }
    }

    protected fun selectSubscribe(map: HashMap<KProperty1<S, Any>, Any?>, deliveryMode: DeliveryMode = RedeliverOnStart, subscriber: (S) -> Unit) {
        selectSubscribe(map, null, deliveryMode, subscriber = subscriber)
    }

    protected fun selectSubscribe(map: HashMap<KProperty1<S, Any>, Any?>, viewModel: BaseMvRxViewModel<S>, deliveryMode: DeliveryMode = RedeliverOnStart, subscriber: (S) -> Unit) {
        assertSubscribeToDifferentViewModel(viewModel)
        viewModel.selectSubscribeInternal(lifecycleOwner, map, deliveryMode, subscriber)
    }

    protected fun selectSubscribe(map: HashMap<KProperty1<S, Any>, Any?>, owner: LifecycleOwner?, deliveryMode: DeliveryMode = RedeliverOnStart, subscriber: (S) -> Unit) {
        selectSubscribeInternal(owner, map, deliveryMode, subscriber)
    }

    private fun selectSubscribeInternal(
        owner: LifecycleOwner?,
        map: HashMap<KProperty1<S, Any>, Any?>,
        deliveryMode: DeliveryMode,
        subscriber: (S) -> Unit
    ) {
        stateStore.resolveSubscription(owner, deliveryMode) {
            var hasChange = false
            for (mutableEntry in map) {
                val value = mutableEntry.value
                val get = mutableEntry.key.get(it)
                if (value != get) {
                    map[mutableEntry.key] = get
                    hasChange = true
                }
            }
            if (hasChange) {
                subscriber(it)
            }
        }
    }

    fun MvRxStateStore<S>.resolveSubscription(
        lifecycleOwner: LifecycleOwner? = null,
        deliveryMode: DeliveryMode,
        subscriber: (S) -> Unit
    ) {
        if (lifecycleOwner == null || FORCE_DISABLE_LIFECYCLE_AWARE_OBSERVER) {
            listener.add(subscriber)
        } else {
            val lastDeliveredValue = if (deliveryMode is UniqueOnly) {
                if (activeSubscriptions.contains(deliveryMode.subscriptionId)) {
                    throw IllegalStateException(
                            "Subscribing with a duplicate subscription id: ${deliveryMode.subscriptionId}. " +
                                    "If you have multiple uniqueOnly subscriptions in a MvRx view that listen to the same properties " +
                                    "you must use a custom subscription id. If you are using a custom MvRxView, make sure you are using the proper" +
                                    "lifecycle owner. See BaseMvRxFragment for an example."
                    )
                }
                activeSubscriptions.add(deliveryMode.subscriptionId)
                lastDeliveredStates[deliveryMode.subscriptionId] as? S
            } else {
                null
            }
            val observer = object : MvRxLifecycleAwareObserver<S>(lifecycleOwner, deliveryMode = deliveryMode, lastDeliveredValue = lastDeliveredValue) {
                override fun onChange(nextValue: S) {
                    if (deliveryMode is UniqueOnly) {
                        lastDeliveredStates[deliveryMode.subscriptionId] = nextValue
                    }
                    subscriber.invoke(nextValue)
                }

                override fun onComplete() {
                    if (deliveryMode is UniqueOnly) {
                        activeSubscriptions.remove(deliveryMode.subscriptionId)
                    }
                }
            }
            listener.add { observer.onChange(it) }
        }
    }

    private fun <S : MvRxState> assertSubscribeToDifferentViewModel(viewModel: BaseMvRxViewModel<S>) {
        require(this != viewModel) {
            "This method is for subscribing to other view models. Please pass a different instance as the argument."
        }
    }

    override fun toString(): String = "${this::class.java.simpleName} $state"
}

// Defines what updates a subscription should receive.
sealed class DeliveryMode {

    internal fun appendPropertiesToId(vararg properties: KProperty1<*, *>): DeliveryMode {
        return when (this) {
            is RedeliverOnStart -> RedeliverOnStart
            is UniqueOnly -> UniqueOnly(subscriptionId + "_" + properties.joinToString(",") { it.name })
        }
    }
}

//  每次从 stopped -> started 时候都会收到回调
object RedeliverOnStart : DeliveryMode()

/**
 * 每次从 (stopped -> started), 只有 stopped 期间有变化, 才会在 started 时候收到回调
 *
 * @param subscriptionId A uniqueIdentifier for this subscription. It is an error for two unique only subscriptions to
 * have the same id.
 */
class UniqueOnly(val subscriptionId: String) : DeliveryMode()

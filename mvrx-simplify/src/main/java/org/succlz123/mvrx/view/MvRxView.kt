package org.succlz123.mvrx.view

import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.succlz123.mvrx.base.BaseMvRxViewModel
import org.succlz123.mvrx.base.DeliveryMode
import org.succlz123.mvrx.base.RedeliverOnStart
import org.succlz123.mvrx.base.UniqueOnly
import org.succlz123.mvrx.state.MvRxState
import kotlin.reflect.KProperty1

// Set of MvRxView identity hash codes that have a pending invalidate.
private val pendingInvalidates = HashSet<Int>()
private val handler = Handler(Looper.getMainLooper(), Handler.Callback { message ->
    val view = message.obj as MvRxView
    pendingInvalidates.remove(System.identityHashCode(view))
    if (view.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) view.invalidate()
    true
})

/**
 * Implement this in your MvRx capable Fragment.
 *
 * When you get a ViewModel with fragmentViewModel, activityViewModel, or existingViewModel, it
 * will automatically subscribe to all state changes in the ViewModel and call [invalidate].
 */
interface MvRxView : LifecycleOwner {

    val mvrxViewId: String

    fun invalidate()

    /**
     * The [LifecycleOwner] to use when making new subscriptions. You may want to return different owners depending
     * on what state your [MvRxView] is in. For fragments, subscriptions made in `onCreate` should use
     * the fragment's lifecycle owner so that the subscriptions are cleared in `onDestroy`. Subscriptions made in or after
     * `onCreateView` should use the fragment's _view's_ lifecycle owner so that they are cleared in `onDestroyView`.
     *
     * For example, if you are using a fragment as a MvRxView the proper implementation is:
     * ```
     *     override val subscriptionLifecycleOwner: LifecycleOwner
     *        get() = this.viewLifecycleOwnerLiveData.value ?: this
     * ```
     *
     * By default [subscriptionLifecycleOwner] is the same as the MvRxView's standard lifecycle owner.
     */
    val subscriptionLifecycleOwner: LifecycleOwner
        get() = this

    fun postInvalidate() {
        if (pendingInvalidates.add(System.identityHashCode(this@MvRxView))) {
            handler.sendMessage(Message.obtain(handler, System.identityHashCode(this@MvRxView), this@MvRxView))
        }
    }

    /**
     * Subscribes to all state updates for the given viewModel.
     *
     * @param deliveryMode If [UniqueOnly] when this MvRxView goes from a stopped to started lifecycle a state value
     * will only be emitted if the state changed. This is useful for transient views that should only
     * be shown once (toasts, poptarts), or logging. Most other views should use false, as when a view is destroyed
     * and recreated the previous state is necessary to recreate the view.
     *
     * Use [uniqueOnly] to automatically create a [UniqueOnly] mode with a unique id for this view.
     *
     * Default: [RedeliverOnStart].
     */
    fun <S : MvRxState> BaseMvRxViewModel<S>.subscribe(deliveryMode: DeliveryMode = RedeliverOnStart, subscriber: (S) -> Unit) {
        subscribe(this@MvRxView.subscriptionLifecycleOwner, deliveryMode, subscriber)
    }

    fun <S : MvRxState> BaseMvRxViewModel<S>.selectSubscribe(
        vararg prop1s: KProperty1<S, Any>,
        deliveryMode: DeliveryMode = RedeliverOnStart,
        subscriber: (Any) -> Unit
    ) {
        selectProperty(*prop1s).subscribe(this@MvRxView.subscriptionLifecycleOwner, deliveryMode, subscriber)
    }

    /**
     * Return a [UniqueOnly] delivery mode with a unique id for this fragment. In rare circumstances, if you
     * make two identical subscriptions with the same (or all) properties in this fragment, provide a customId
     * to avoid collisions.
     *
     * @param An additional custom id to identify this subscription. Only necessary if there are two subscriptions
     * in this fragment with exact same properties (i.e. two subscribes, or two selectSubscribes with the same properties).
     */
    fun uniqueOnly(customId: String? = null): UniqueOnly {
        return UniqueOnly(listOfNotNull(mvrxViewId, customId).joinToString("_"))
    }
}

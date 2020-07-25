package org.succlz123.mvrx.view

import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.succlz123.mvrx.base.UniqueOnly

// Set of MvRxView identity hash codes that have a pending invalidate.
private val pendingInvalidates = HashSet<Int>()

private val handler = Handler(Looper.getMainLooper(), Handler.Callback { message ->
    val view = message.obj as MvRxView
    pendingInvalidates.remove(System.identityHashCode(view))
    if (view.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        view.invalidate()
    }
    true
})

/**
 * Implement this in your MvRx capable Fragment.
 *
 * When you get a ViewModel with fragmentViewModel, activityViewModel, or existingViewModel, it
 * will automatically subscribe to all state changes in the ViewModel and call [invalidate].
 */
interface MvRxView : LifecycleOwner {

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
            handler.sendMessage(
                Message.obtain(handler, System.identityHashCode(this@MvRxView), this@MvRxView)
            )
        }
    }

    /**
     * Return a [UniqueOnly] delivery mode with a unique id for this fragment. In rare circumstances, if you
     * make two identical subscriptions with the same (or all) properties in this fragment, provide a customId
     * to avoid collisions.
     *
     * @param An additional custom id to identify this subscription. Only necessary if there are two subscriptions
     * in this fragment with exact same properties (i.e. two subscribes, or two selectSubscribes with the same properties).
     */
    fun uniqueOnly(rxViewId: String, customId: String? = null): UniqueOnly {
        return UniqueOnly(
            listOfNotNull(
                rxViewId,
                customId
            ).joinToString("_")
        )
    }
}
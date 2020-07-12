package org.succlz123.mvrx.extension

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import org.succlz123.mvrx.base.BaseMvRxViewModel
import org.succlz123.mvrx.lifecycle.lifecycleAwareLazy
import org.succlz123.mvrx.state.MvRxState
import org.succlz123.mvrx.view.MvRxView
import org.succlz123.mvrx.viewmodel.ActivityViewModelContext
import org.succlz123.mvrx.viewmodel.FragmentViewModelContext
import org.succlz123.mvrx.viewmodel.MvRxViewModelProvider
import org.succlz123.mvrx.viewmodel.ViewModelDoesNotExistException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * This should only be set by the MvRxTestRule from the mvrx-testing artifact.
 *
 * This can be used to force MvRxViewModels to be or not to be in debug mode for tests.
 * This is Java so it can be package private.
 */
var FORCE_DEBUG: Boolean = true

/**
 * This should only be set by the MvRxTestRule from the mvrx-testing artifact.
 *
 * This can be used to force MvRxViewModels to disable lifecycle aware observer for unit testing.
 * This is Java so it can be package private.
 */
var FORCE_DISABLE_LIFECYCLE_AWARE_OBSERVER = false

/**
 * If your initial state needs to use Fragment arguments, store your arguments
 * as a parcelable class stored at this key.
 */
const val KEY_ARG = "mvrx:arg"

inline fun <T, reified VM : BaseMvRxViewModel<S>, S : MvRxState> T.fragmentViewModel(
        viewModelClass: KClass<VM> = VM::class,
        noinline creator: (state: SavedStateHandle?) -> VM,
        enableSavedStateHandle: Boolean = false,
        noinline keyFactory: () -> String = { viewModelClass.java.name }
): lifecycleAwareLazy<VM> where T : Fragment, T : MvRxView {
    return lifecycleAwareLazy(this) {
        MvRxViewModelProvider.get(
                viewModelClass.java,
                FragmentViewModelContext(requireActivity(), this, null),
                creator,
                keyFactory(),
                enableSavedStateHandle
        ).apply { subscribe(this@fragmentViewModel, subscriber = { invalidate() }) }
    }
}

/**
 * Gets or creates a ViewModel scoped to a parent fragment. This delegate will walk up the parentFragment hierarchy
 * until it finds a Fragment that can provide the correct ViewModel. If no parent fragments can provide the ViewModel,
 * a new one will be created in top-most parent Fragment.
 */
inline fun <T, reified VM : BaseMvRxViewModel<S>, S : MvRxState> T.parentFragmentViewModel(
        viewModelClass: KClass<VM> = VM::class,
        noinline creator: (state: SavedStateHandle?) -> VM,
        enableSavedStateHandle: Boolean = false,
        crossinline keyFactory: () -> String = { viewModelClass.java.name }
): Lazy<VM> where T : Fragment, T : MvRxView = lifecycleAwareLazy(this) {
    requireNotNull(parentFragment) { "There is no parent fragment for ${this::class.java.simpleName}!" }
    var fragment: Fragment? = parentFragment
    val key = keyFactory()
    while (fragment != null) {
        try {
            return@lifecycleAwareLazy MvRxViewModelProvider.get(
                    viewModelClass.java,
                    FragmentViewModelContext(this.requireActivity(), fragment, null),
                    creator,
                    key,
                    enableSavedStateHandle,
                    forExistingViewModel = true
            ).apply { subscribe(this@parentFragmentViewModel, subscriber = { invalidate() }) }
        } catch (e: ViewModelDoesNotExistException) {
            fragment = fragment.parentFragment
        }
    }

    // ViewModel was not found. Create a new one in the top-most parent.
    var topParentFragment = parentFragment
    while (topParentFragment?.parentFragment != null) {
        topParentFragment = topParentFragment.parentFragment
    }
    val viewModelContext = FragmentViewModelContext(this.requireActivity(), topParentFragment!!, null)
    return@lifecycleAwareLazy MvRxViewModelProvider.get(viewModelClass.java, viewModelContext, creator, keyFactory(), enableSavedStateHandle)
            .apply { subscribe(this@parentFragmentViewModel, subscriber = { invalidate() }) }
}

/**
 * Gets or creates a ViewModel scoped to a target fragment. Throws [IllegalStateException] if there is no target fragment.
 */
inline fun <T, reified VM : BaseMvRxViewModel<S>, S : MvRxState> T.targetFragmentViewModel(
        viewModelClass: KClass<VM> = VM::class,
        noinline creator: (state: SavedStateHandle?) -> VM,
        enableSavedStateHandle: Boolean = false,
        crossinline keyFactory: () -> String = { viewModelClass.java.name }
): Lazy<VM> where T : Fragment, T : MvRxView = lifecycleAwareLazy(this) {
    val targetFragment = requireNotNull(targetFragment) { "There is no target fragment for ${this::class.java.simpleName}!" }
    MvRxViewModelProvider.get(
            viewModelClass.java,
            FragmentViewModelContext(this.requireActivity(), targetFragment, null),
            creator,
            keyFactory(),
            enableSavedStateHandle
    ).apply { subscribe(this@targetFragmentViewModel, subscriber = { invalidate() }) }
}

/**
 * [activityViewModel] except it will throw [IllegalStateException] if the ViewModel doesn't already exist.
 * Use this for screens in the middle of a flow that cannot reasonably be an entrypoint to the flow.
 */
inline fun <T, reified VM : BaseMvRxViewModel<S>, S : MvRxState> T.existingViewModel(
        viewModelClass: KClass<VM> = VM::class,
        noinline creator: (state: SavedStateHandle?) -> VM,
        enableSavedStateHandle: Boolean = false,
        crossinline keyFactory: () -> String = { viewModelClass.java.name }
) where T : Fragment, T : MvRxView = lifecycleAwareLazy(this) {
    MvRxViewModelProvider.get(
            viewModelClass.java,
            ActivityViewModelContext(requireActivity(), null),
            creator,
            keyFactory(),
            enableSavedStateHandle,
            forExistingViewModel = true
    ).apply { subscribe(this@existingViewModel, subscriber = { invalidate() }) }
}

/**
 * [fragmentViewModel] except scoped to the current Activity. Use this to share state between different Fragments.
 */
inline fun <T, reified VM : BaseMvRxViewModel<S>, S : MvRxState> T.activityViewModel(
        viewModelClass: KClass<VM> = VM::class,
        noinline creator: (state: SavedStateHandle?) -> VM,
        enableSavedStateHandle: Boolean = false,
        noinline keyFactory: () -> String = { viewModelClass.java.name }
) where T : Fragment, T : MvRxView = lifecycleAwareLazy(this) {
    MvRxViewModelProvider.get(
            viewModelClass.java,
            ActivityViewModelContext(requireActivity(), null),
            creator,
            keyFactory(),
            enableSavedStateHandle
    ).apply { subscribe(this@activityViewModel, subscriber = { invalidate() }) }
}

/**
 * [fragmentViewModel] except scoped to the current Activity. Use this to share state between different Fragments.
 */
inline fun <T, reified VM : BaseMvRxViewModel<S>, S : MvRxState> T.viewModel(
        viewModelClass: KClass<VM> = VM::class,
        noinline creator: (state: SavedStateHandle?) -> VM,
        enableSavedStateHandle: Boolean = false,
        crossinline keyFactory: () -> String = { viewModelClass.java.name }
) where T : FragmentActivity = lifecycleAwareLazy(this) {
    MvRxViewModelProvider.get(
            viewModelClass.java,
            ActivityViewModelContext(this, null),
            creator,
            keyFactory(),
            enableSavedStateHandle
    )
}

fun <V : Any> args() = object : ReadOnlyProperty<Fragment, V> {
    var value: V? = null

    override fun getValue(thisRef: Fragment, property: KProperty<*>): V {
        if (value == null) {
            val args = thisRef.arguments
                    ?: throw IllegalArgumentException("There are no fragment arguments!")
            val argUntyped = args.get(KEY_ARG)
            argUntyped
                    ?: throw IllegalArgumentException("MvRx arguments not found at key MvRx.KEY_ARG!")
            @Suppress("UNCHECKED_CAST")
            value = argUntyped as V
        }
        return value ?: throw IllegalArgumentException("")
    }
}

/**
 * Helper to handle pagination. Use this when you want to append a list of results at a given offset.
 * This is safer than just appending blindly to a list because it guarantees that the data gets added
 * at the offset it was requested at.
 *
 * This will replace *all contents* starting at the offset with the new list.
 * For example: [1,2,3].appendAt([4], 1) == [1,4]]
 */
fun <T : Any> List<T>.appendAt(other: List<T>?, offset: Int) = subList(0, offset.coerceIn(0, size)) + (other
        ?: emptyList())

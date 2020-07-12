package org.succlz123.mvrx.viewmodel

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import org.succlz123.mvrx.base.BaseMvRxViewModel
import org.succlz123.mvrx.state.MvRxState

/**
 * Helper ViewModelProvider that has a single method for taking either a [Fragment] or [FragmentActivity] instead
 * of two separate ones. The logic for providing the correct scope is inside the method.
 */
object MvRxViewModelProvider {
    /**
     * MvRx specific ViewModelProvider used for creating a BaseMvRxViewModel scoped to either a [Fragment] or [FragmentActivity].
     * If this is in a [Fragment], it cannot be called before the Fragment has been added to an Activity or wrapped in a [Lazy] call.
     *
     * @param viewModelClass The class of the ViewModel you would like an instance of.
     * @param stateClass The class of the State used by the ViewModel.
     * @param viewModelContext The [ViewModelContext] which contains arguments and the owner of the ViewModel.
     *                         Either [ActivityViewModelContext] or [FragmentViewModelContext].
     * @param key An optional key for the ViewModel in the store. This is optional but should be used if you have multiple of the same
     *            ViewModel class in the same scope.
     * @param forExistingViewModel If true the viewModel should already have been created. If it has not been created already,
     *                             a [ViewModelDoesNotExistException] will be thrown
     * @param initialStateFactory A way to specify how to create the initial state, can be mocked out for testing.
     *
     */
    fun <VM : BaseMvRxViewModel<S>, S : MvRxState> get(
        viewModelClass: Class<out VM>,
        viewModelContext: ViewModelContext,
        creator: (state: SavedStateHandle?) -> VM,
        key: String = viewModelClass.name,
        enableSavedStateHandle: Boolean = false,
        forExistingViewModel: Boolean = false
    ): VM {
        val savedStateRegistry = viewModelContext.savedStateRegistry
        if (!savedStateRegistry.isRestored) {
            error("You can only access a view model after super.onCreate of your activity/fragment has been called.")
        }
        var savedStateRegistryOwner: SavedStateRegistryOwner? = null
        if (viewModelContext is ActivityViewModelContext) {
            savedStateRegistryOwner = viewModelContext.activity()
        } else if (viewModelContext is FragmentViewModelContext) {
            savedStateRegistryOwner = viewModelContext.fragment()
        }
        if (savedStateRegistryOwner == null) {
            throw Exception("ViewModel Context is null!")
        }
        return ViewModelProvider(
                viewModelContext.owner,
                MvRxVMFactory(viewModelClass, viewModelContext, creator,
                        enableSavedStateHandle, forExistingViewModel, savedStateRegistryOwner, null
                )
        ).get(key, viewModelClass)
    }
}

sealed class ViewModelContext {

    abstract val activity: FragmentActivity

    internal abstract val savedStateRegistry: SavedStateRegistry

    internal abstract val owner: ViewModelStoreOwner

    @Suppress("UNCHECKED_CAST")
    fun <A : FragmentActivity> activity(): A = activity as A

    @Suppress("UNCHECKED_CAST")
    fun <A : Application> app(): A = activity.application as A

    abstract val args: Any?

    @Suppress("UNCHECKED_CAST")
    fun <A> args(): A = args as A
}

data class ActivityViewModelContext(
        override val activity: FragmentActivity,
        override val args: Any?
) : ViewModelContext() {

    override val owner get() = activity

    override val savedStateRegistry get() = activity.savedStateRegistry
}

data class FragmentViewModelContext(
        override val activity: FragmentActivity,
        val fragment: Fragment,
        override val args: Any?
) : ViewModelContext() {

    override val owner get() = fragment

    override val savedStateRegistry get() = fragment.savedStateRegistry

    @Suppress("UNCHECKED_CAST")
    fun <F : Fragment> fragment(): F = fragment as F
}

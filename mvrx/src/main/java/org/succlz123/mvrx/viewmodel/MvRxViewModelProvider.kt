package org.succlz123.mvrx.viewmodel

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import org.succlz123.mvrx.base.BaseMvRxViewModel
import org.succlz123.mvrx.state.MvRxState

object MvRxViewModelProvider {

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
            MvRxVMFactory(
                viewModelClass, viewModelContext, creator,
                enableSavedStateHandle, forExistingViewModel, savedStateRegistryOwner, null
            )
        ).get(key, viewModelClass)
    }
}

class MvRxVMFactory<VM : BaseMvRxViewModel<S>, S : MvRxState>(
    private val viewModelClass: Class<out VM>,
    private val viewModelContext: ViewModelContext,
    private val creator: (handle: SavedStateHandle?) -> VM,
    private val enableSavedStateHandle: Boolean = false,
    private val forExistingViewModel: Boolean = false,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle?
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        // doesn't need SavedStateHandle
        if (!enableSavedStateHandle && !forExistingViewModel) {
            return creator.invoke(null) as T
        }
        if (forExistingViewModel && handle.get<String>("123") == null) {
            throw ViewModelDoesNotExistException(
                viewModelClass,
                viewModelContext,
                key
            )
        }
        val vm = creator.invoke(handle) as T
        if (vm is BaseMvRxViewModel<*>) {
            vm.stateHandle = handle
        }
        return vm
    }
}

internal class ViewModelDoesNotExistException(
    viewModelClass: Class<*>,
    viewModelContext: ViewModelContext,
    key: String
) : IllegalStateException("ViewModel of type ${viewModelClass.name} for ${viewModelContext.owner}[$key] does not exist yet!")

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

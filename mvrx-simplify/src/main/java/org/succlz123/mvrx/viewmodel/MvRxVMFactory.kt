package org.succlz123.mvrx.viewmodel

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import org.succlz123.mvrx.base.BaseMvRxViewModel
import org.succlz123.mvrx.state.MvRxState

class MvRxVMFactory<VM : BaseMvRxViewModel<S>, S : MvRxState>(
    private val viewModelClass: Class<out VM>,
    private val viewModelContext: ViewModelContext,
    private val creator: (handle: SavedStateHandle?) -> VM,
    private val enableSavedStateHandle: Boolean = false,
    private val forExistingViewModel: Boolean = false,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle?
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        // doesn't need SavedStateHandle
        if (!enableSavedStateHandle && !forExistingViewModel) {
            return creator.invoke(null) as T
        }
        if (forExistingViewModel && handle.get<String>("123") == null) {
            throw ViewModelDoesNotExistException(viewModelClass, viewModelContext, key)
        }
        return creator.invoke(handle) as T
    }
}

internal class ViewModelDoesNotExistException(
    viewModelClass: Class<*>,
    viewModelContext: ViewModelContext,
    key: String
) : IllegalStateException("ViewModel of type ${viewModelClass.name} for ${viewModelContext.owner}[$key] does not exist yet!")

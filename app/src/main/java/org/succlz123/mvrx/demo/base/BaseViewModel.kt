package org.succlz123.mvrx.demo.base

import org.succlz123.mvrx.base.BaseMvRxViewModel
import org.succlz123.mvrx.state.MvRxState

abstract class BaseViewModel<S : MvRxState>(initialState: S) :
    BaseMvRxViewModel<S>(initialState, debugMode = true)
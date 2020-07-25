package org.succlz123.mvrx.demo.sample

import org.succlz123.mvrx.base.BaseMvRxViewModel
import org.succlz123.mvrx.result.Loading
import org.succlz123.mvrx.result.Success
import org.succlz123.mvrx.demo.http.Api

class SampleViewModel(initialState: SampleState) :
    BaseMvRxViewModel<SampleState>(initialState, debugMode = true) {

    companion object {

        fun create(): SampleViewModel {
            return SampleViewModel(SampleState())
        }
    }

    init {
        /**
         *  logStateChanges 打印 State 变化日志 debugMode = true 时打印
         */
        logStateChanges()
    }

    fun changeName(newName: String) {
        setState { copy(name = newName) }
    }

    fun changeAge(newAge: Int) {
        setState { copy(age = newAge) }
    }

    fun getArticleData() {
        withState().let {
            if (it.articleData is Loading) {
                return
            }
            Thread {
                val result = Api.api.getArticleList().execute().body() ?: return@Thread
                postState {
                    copy(articleData = Success(result))
                }
            }.start()
        }
    }
}
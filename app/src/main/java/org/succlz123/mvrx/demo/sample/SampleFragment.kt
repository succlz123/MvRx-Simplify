package org.succlz123.mvrx.demo.sample

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_sample.*
import org.succlz123.mvrx.async.*
import org.succlz123.mvrx.base.BaseMvRxFragment
import org.succlz123.mvrx.base.BaseMvRxViewModel
import org.succlz123.mvrx.demo.R
import org.succlz123.mvrx.demo.http.Api
import org.succlz123.mvrx.extension.fragmentViewModel
import org.succlz123.mvrx.extension.withState
import org.succlz123.mvrx.state.MvRxState

/**
 *
 *  取消 Async<T> 的异步功能, 但是名字还是保持一致
 *
 *          分别有：Uninitialized（未初始化）
 *                  Loading （请求中）
 *                  Success （请求成功）
 *                  Fail    （请求失败）
 */
data class SampleState(
    val name: String = "--",
    val age: Int = 0,
    val articleData: Async<ArticleData> = Uninitialized
) : MvRxState

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
        withState { state ->
            setState { copy(name = newName) }
        }
    }

    fun changeAge(newAge: Int) {
        withState {
            setState { copy(age = newAge) }
        }
    }

    fun getArticleData() {
        withState {
            if (it.articleData is Loading) {
                return@withState
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

class SampleFragment : BaseMvRxFragment() {

    val sampleViewModel: SampleViewModel by fragmentViewModel(creator = { SampleViewModel.create() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sample, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bnChangeName.setOnClickListener { sampleViewModel.changeName("Sunhy") }
        bnChangeAge.setOnClickListener { sampleViewModel.changeAge(21) }
        bnRequest.setOnClickListener { sampleViewModel.getArticleData() }
    }

    override fun invalidate() {
        withState(sampleViewModel) { state ->
            tvName.text = state.name
            tvAge.text = state.age.toString()

            when (state.articleData) {
                is Success -> {
                    tvData.text = state.articleData.invoke().data.toString()
                }
                is Fail -> {
                    Log.e("网络请求失败", "网络请求失败")
                }
                is Loading -> {
                    Log.e("网络请求中", "网络请求中")
                }
                else -> {
                }
            }
        }
    }
}
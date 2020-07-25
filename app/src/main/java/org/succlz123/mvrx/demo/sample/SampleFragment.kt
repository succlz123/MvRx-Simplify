package org.succlz123.mvrx.demo.sample

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.succlz123.mvrx.extension.fragmentViewModel
import org.succlz123.mvrx.extension.withState
import org.succlz123.mvrx.state.MvRxState
import org.succlz123.mvrx.view.MvRxView
import kotlinx.android.synthetic.main.fragment_sample.*
import org.succlz123.mvrx.demo.R
import org.succlz123.mvrx.demo.base.BaseFragment
import org.succlz123.mvrx.result.*

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
    val articleData: Result<ArticleData> = Uninitialized
) : MvRxState

class SampleFragment : BaseFragment(), MvRxView {

    val sampleViewModel: SampleViewModel by fragmentViewModel(
        enableSavedStateHandle = true,
        creator = { SampleViewModel.create() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sample, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sampleViewModel.changeAge(33)
        bnChangeName.setOnClickListener { sampleViewModel.changeName("Sunhy") }
        bnChangeAge.setOnClickListener { sampleViewModel.changeAge(21) }
        bnRequest.setOnClickListener { sampleViewModel.getArticleData() }
    }

    override fun invalidate() {
        val xx = 3
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
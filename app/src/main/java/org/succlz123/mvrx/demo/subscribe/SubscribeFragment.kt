package org.succlz123.mvrx.demo.subscribe

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_subscribe.*
import org.succlz123.mvrx.demo.R
import org.succlz123.mvrx.demo.base.BaseFragment
import org.succlz123.mvrx.demo.base.BaseViewModel
import org.succlz123.mvrx.demo.http.Api
import org.succlz123.mvrx.demo.http.ApiService
import org.succlz123.mvrx.demo.http.HttpUtils
import org.succlz123.mvrx.demo.sample.ArticleData
import org.succlz123.mvrx.extension.fragmentViewModel
import org.succlz123.mvrx.extension.withState
import org.succlz123.mvrx.result.*
import org.succlz123.mvrx.state.MvRxState
import org.succlz123.mvrx.view.MvRxView

data class SubscribeState(
    val name: String = "Shy",
    val yy: YY = YY(),
    val age: Int = 21,
    val articleData: Result<ArticleData> = Uninitialized
) : MvRxState

class YY(var xx: String = "333")

class SubscribeViewModel(state: SubscribeState, private val apiService: ApiService) :
    BaseViewModel<SubscribeState>(state) {

    init {
        logStateChanges()
    }

    fun changeName(newName: String) {
        withState().let {
            val yy = it.yy
            setState {
                copy(name = newName).apply {
                    val xxxxx = yy
                    val xxfsdfas = this.yy
                    val xx = 3
                }
            }
        }
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

    companion object {

        fun create(): SubscribeViewModel {
            val service: ApiService by lazy { HttpUtils.retrofit.create(ApiService::class.java) }
            return SubscribeViewModel(SubscribeState(), service)
        }
    }
}

class SubscribeFragment : BaseFragment(), MvRxView {

    val subscribeViewModel: SubscribeViewModel by fragmentViewModel(creator = {
        SubscribeViewModel.create()
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subscribe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bnChangeName.setOnClickListener {
            if (tvName.text == "Shy") {
                subscribeViewModel.changeName("Sunhy")
            } else {
                subscribeViewModel.changeName("Shy")
            }
        }
        bnChangeAge.setOnClickListener {
            if (tvAge.text == "21") {
                subscribeViewModel.changeAge(99)
            } else {
                subscribeViewModel.changeAge(21)
            }
        }

        bnRequest.setOnClickListener {
            subscribeViewModel.getArticleData()
        }

        subscribeViewModel.selectProperty(SubscribeState::name, SubscribeState::age).subscribe {
            Toast.makeText(context, "name：-> ${it.name}，age：-> ${it.age}", Toast.LENGTH_SHORT)
                .show()
        }

        subscribeViewModel.selectProperty(SubscribeState::articleData).subscribe {
            Toast.makeText(context, "name：-> ${it.name}，age：-> ${it.age}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun invalidate() {
        withState(subscribeViewModel) {
            tvName.text = it.name
            tvAge.text = it.age.toString()
            when (it.articleData) {
                is Success -> {
                    tvData.text = it.articleData.invoke().data.toString()
                }
                is Fail -> {
                    tvData.text = "请求失败"
                    Log.e("网络请求失败", "网络请求失败")
                }
                is Loading -> {
                    tvData.text = "请求中..."
                    Log.e("网络请求中", "网络请求中")
                }
                else -> {
                }
            }
        }
    }
}
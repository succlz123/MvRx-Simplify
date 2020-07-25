package org.succlz123.mvrx.demo.transmit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.succlz123.mvrx.demo.R
import org.succlz123.mvrx.demo.base.BaseFragment
import org.succlz123.mvrx.demo.base.BaseViewModel
import org.succlz123.mvrx.demo.http.ApiService
import org.succlz123.mvrx.demo.http.HttpUtils
import kotlinx.android.synthetic.main.fragment_second.*
import org.succlz123.mvrx.extension.args
import org.succlz123.mvrx.extension.fragmentViewModel
import org.succlz123.mvrx.extension.withState
import org.succlz123.mvrx.state.MvRxState
import org.succlz123.mvrx.view.MvRxView

data class SecondState(
    val name: String = "",
    val state_person: Person? = null
) : MvRxState {

    constructor(person: Person) : this(
        name = "Sunhy",
        state_person = person
    )
}

class SecondViewModel(secondState: SecondState, private val apiService: ApiService) :
    BaseViewModel<SecondState>(secondState) {

    init {
        logStateChanges()
    }

    companion object {

        fun create(): SecondViewModel {
            val service: ApiService by lazy { HttpUtils.retrofit.create(ApiService::class.java) }
            return SecondViewModel(SecondState(), service)
        }
    }
}

class SecondFragment : BaseFragment(), MvRxView {

    /**
     * 接受传递过来的数据  只需要指定类型 从 args()中取出
     *  在BaseFragment中 我们把数据存在了 MvRx.KEY_ARG 里
     *  @see [BaseFragment]
     */
    val person: Person by args("123")
    val secondViewModel: SecondViewModel by fragmentViewModel(creator = { SecondViewModel.create() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun invalidate() {
        if (person != null) {
            tvName.text = person.name
            tvAge.text = person.age
            tvSex.text = person.sex
        }

        withState(secondViewModel) {
            if (it.state_person != null) {
                tvName2.text = it.state_person.name
                tvAge2.text = it.state_person.age
                tvSex2.text = it.state_person.sex
            }
            tvName3.text = it.name
        }
    }
}
package org.succlz123.mvrx.demo.transmit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import org.succlz123.mvrx.demo.R
import org.succlz123.mvrx.demo.base.BaseFragment
import org.succlz123.mvrx.demo.base.BaseViewModel
import org.succlz123.mvrx.demo.http.ApiService
import org.succlz123.mvrx.demo.http.HttpUtils
import kotlinx.android.synthetic.main.fragment_first.*
import org.succlz123.mvrx.state.MvRxState
import org.succlz123.mvrx.view.MvRxView

data class FirstState(val name: String = "--") : MvRxState

class FirstViewModel(firstState: FirstState, private val apiService: ApiService) :
    BaseViewModel<FirstState>(firstState) {

    init {
        logStateChanges()
    }

    companion object {

        fun create(): FirstViewModel {
            val service: ApiService by lazy {
                HttpUtils.retrofit.create(ApiService::class.java)
            }
            return FirstViewModel(FirstState(), service)
        }
    }
}

class FirstFragment : BaseFragment(), MvRxView {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bnIntent.setOnClickListener {
            if (etName.text.toString().isNotEmpty()
                || etAge.text.toString().isNotEmpty()
                || etSex.text.toString().isNotEmpty()
            ) {
                var person = Person(
                    etName.text.toString(),
                    etAge.text.toString(),
                    etSex.text.toString()
                )
                navigateTo(R.id.action_firstFragment_to_secondFragment, person)
            } else {
                Toast.makeText(context, "请输入要传递的数据", Toast.LENGTH_SHORT)
            }
        }
    }

    override fun invalidate() {

    }
}
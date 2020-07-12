package org.succlz123.mvrx.demo.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.navigation.fragment.findNavController
import org.succlz123.mvrx.base.BaseMvRxFragment
import org.succlz123.mvrx.extension.KEY_ARG
import java.io.Serializable

abstract class BaseFragment : BaseMvRxFragment(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    protected fun navigateTo(@IdRes actionId: Int, arg: Serializable? = null) {
        val bundle = arg?.let { Bundle().apply { putSerializable(KEY_ARG, it) } }
        findNavController().navigate(actionId, bundle)
    }
}
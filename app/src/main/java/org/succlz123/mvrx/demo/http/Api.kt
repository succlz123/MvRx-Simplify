package org.succlz123.mvrx.demo.http

object Api{
    val api : ApiService by lazy {
        HttpUtils.retrofit.create(ApiService::class.java)
    }
}
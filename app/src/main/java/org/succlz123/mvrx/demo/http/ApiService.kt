package org.succlz123.mvrx.demo.http

import org.succlz123.mvrx.demo.sample.ArticleData
import retrofit2.Call
import retrofit2.http.GET

interface ApiService {

    @GET("https://www.wanandroid.com/article/list/0/json")
    fun getArticleList(): Call<ArticleData>
}
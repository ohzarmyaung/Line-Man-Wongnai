package com.linemanwongnai.app.data.network
import retrofit2.http.GET

interface CoinApi {

    @GET("/v2/coins")
    suspend fun getCoinList() : List<String>
}
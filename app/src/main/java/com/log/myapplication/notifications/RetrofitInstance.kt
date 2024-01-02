package com.log.myapplication.notifications

import com.log.myapplication.constant.Constant
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitInstance {
    companion object {
        private val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(Constant.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        val api: NotificationsAPI by lazy {
            retrofit.create(NotificationsAPI::class.java)
        }
    }
}
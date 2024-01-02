package com.log.myapplication.notifications

import com.log.myapplication.constant.Constant.Companion.API_KEY
import com.log.myapplication.constant.Constant.Companion.CONTENT_TYPE
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface NotificationsAPI {

    @Headers("Authorization: key=$API_KEY", "Content-Type:$CONTENT_TYPE")
    @POST("fcm/send")
    suspend fun postNotification(
        @Body bodyNotifications: PushNotifications
    ): Response<ResponseBody>
}

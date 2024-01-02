package com.log.myapplication.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.log.myapplication.activity.MainActivity
import com.log.myapplication.constant.Constant.Companion.DATABASE_URL
import com.log.myapplication.notifications.NotificationsData
import com.log.myapplication.notifications.PushNotifications
import com.log.myapplication.notifications.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class Chat (val chatId: String,
            val userId: String) {

    val listMessage = mutableListOf<Message>()

    var senderEmail: String? = null
    var avarta: Bitmap? = null

    fun sendMessage(message: String) {
        val time = System.currentTimeMillis().toString()
        // generate message id
        val messageId = chatId + time
        val databaseReference = FirebaseDatabase.getInstance(DATABASE_URL).getReference("Chats/$chatId/$messageId")
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["senderId"] = MainActivity.user.uid
        hashMap["receiverId"] = userId
        hashMap["message"] = message
        hashMap["time"] = time
        databaseReference.setValue(hashMap)

        // send notification
        PushNotifications(
            NotificationsData(MainActivity.displayName, message),
            "/topics/$userId"
        ).also {
            sendNotification(it)
        }

    }

    fun getInfo(context: Context, callback: (Boolean) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance(DATABASE_URL).getReference("Users/$userId")
        databaseReference.get().addOnSuccessListener {
            val hashMap = it.value as HashMap<*, *>
            val string = hashMap["email"] as String
            senderEmail = string
            val hasAvatar = hashMap["avatar"] as Long
            if (hasAvatar == 1L) {
                downloadImage(callback)
            }
            callback(true)
        }.addOnFailureListener {
            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            callback(false)
        }
    }

    private fun downloadImage(callback: (Boolean) -> Unit) {
        val storageRef = FirebaseStorage.getInstance("gs://basicmessenger-6fda0.appspot.com").reference
        val imageRef = storageRef.child("images/${userId}.jpg")
        // get inage and put in image view
        val localFile = File.createTempFile("images", "jpg")
        imageRef.getFile(localFile)
            .addOnSuccessListener {
                // Local temp file has been created
                // Now, you can set the downloaded image to an ImageView
                avarta = BitmapFactory.decodeFile(localFile.absolutePath)
                callback(true)
            }
            .addOnFailureListener {
                // Handle any errors
                callback(false)
            }

    }


    private fun sendNotification(notifications: PushNotifications) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notifications)
            if (response.isSuccessful) {
                Log.d("MainActivity", "Response111: $response")
            } else {
                Log.i("MainActivity", response.toString())
            }
        } catch (e: Exception) {
            Log.e("MainActivity", e.toString())
        }
    }
}
package com.log.myapplication.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.log.myapplication.R
import com.log.myapplication.adapter.ChatAdapter
import com.log.myapplication.constant.Constant.Companion.DATABASE_URL
import com.log.myapplication.constant.Constant.Companion.STORAGE_URL
import com.log.myapplication.databinding.ActivityMainBinding
import com.log.myapplication.model.Chat
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var user: FirebaseUser
        lateinit var displayName: String
        lateinit var email: String
        var avatar: Bitmap? = null

        val listChat = mutableListOf<Chat>()
    }

    private var uri: Uri? = null
    private lateinit var userImage: Bitmap
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        askNotificationPermission()
        checkUser()
        binding.rvChats.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        getChats()

        binding.btnLogout.setOnClickListener {
            Firebase.auth.signOut()
            Intent(this, LoginActivity::class.java).also {
                startActivity(it)
            }
        }

        binding.ibtnAddChat.setOnClickListener {
            // Show dialog to get email
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Add chat")

            val input = android.widget.EditText(this)
            input.hint = "Email"
            builder.setView(input)

            builder.setPositiveButton("OK") { _, _ ->
                val email = input.text.toString()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (email == MainActivity.email) {
                    Toast.makeText(this, "Cannot add yourself", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                createChat(email)
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            builder.show()
        }

        binding.iwAvatar.setOnClickListener {
            chooseImage()
            binding.btnSave.visibility = android.view.View.VISIBLE
            binding.btnCancel.visibility = android.view.View.VISIBLE
        }

        binding.btnSave.setOnClickListener {
            binding.btnSave.visibility = android.view.View.GONE
            binding.btnCancel.visibility = android.view.View.GONE
            if (uri == null) {
                return@setOnClickListener
            }
            uploadImage()
        }
        binding.btnCancel.setOnClickListener {
            binding.btnSave.visibility = android.view.View.GONE
            binding.btnCancel.visibility = android.view.View.GONE
            if (avatar != null) {
                binding.iwAvatar.setImageBitmap(avatar)
            }
            binding.iwAvatar.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }


    private fun checkUser() {
        if (Firebase.auth.currentUser == null) {
            // User isn't signed in
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show()
            Intent(this, LoginActivity::class.java).also {
                startActivity(it)
            }
        }
        user = Firebase.auth.currentUser!!
        user.let { firebaseUser ->
            val databaseReference = FirebaseDatabase.getInstance(DATABASE_URL).getReference("Users/${firebaseUser.uid}")
            databaseReference.get().addOnSuccessListener {
                if (it.value == null) {
                    return@addOnSuccessListener
                }
                val user1 = it.value as HashMap<*, *>
                email = user1["email"] as String
                displayName = user1["name"] as String
                // check if avatar exists
                val hasAvatar = user1["avatar"] as Long
                if (hasAvatar == 1L) {
                    downloadImage()
                }

                binding.displayName.text = displayName
                binding.email.text = email

                // Clear all previous subscriptions
                FirebaseMessaging.getInstance().subscribeToTopic("/topics/${user.uid}")
                    .addOnCompleteListener { task ->
                        var msg = "Subscribed"
                        if (!task.isSuccessful) {
                            msg = "Failed"
                        }
                        Log.d("MainActivity", msg)
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }


    private fun getChats() {
        val databaseReference = FirebaseDatabase.getInstance(DATABASE_URL).getReference("Users/${user.uid}/chats")

        databaseReference.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (snapshot.value == null) {
                    return
                }
                val listChatId = snapshot.value as HashMap<*, *>
                listChat.clear()
                listChatId.forEach { (key, value) ->
                    val userId = key as String
                    val chatId = value as String
                    val newChat = Chat(chatId, userId)
                    listChat.add(newChat)
                }
                Log.d("MainActivity", "Get chats success ${listChat.size}")
                binding.rvChats.adapter = ChatAdapter(this@MainActivity, listChat)
                Toast.makeText(this@MainActivity, "Get chats success ${listChat.size}", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun createChat(mail: String) {
        val databaseReference = FirebaseDatabase.getInstance(DATABASE_URL).getReference("Users")
        databaseReference.get().addOnSuccessListener { it ->
            val listUser = it.value as HashMap<*, *>
            var exits = false
            listUser.forEach { (key, value) ->
                val userId = key as String
                val user1 = value as HashMap<*, *>
                val email = user1["email"] as String
                if (email == mail) {
                    exits = true
                    // check if chat already exists
                    if (checkChatExists(userId)) {
                        Toast.makeText(this, "Chat already exists", Toast.LENGTH_SHORT).show()
                        Intent(this, ChatActivity::class.java).also {
                            it.putExtra("userId", userId)
                            startActivity(it)
                        }
                    }

                    // generate chat id
                    val chatId = user1["uid"] as String + user1["uid"] as String

                    databaseReference.child(user1["uid"] as String).child("chats").child(user.uid).setValue(chatId)
                    databaseReference.child(user.uid).child("chats").child(user1["uid"] as String).setValue(chatId)

                    // add chat to list
                    val newChat = Chat(chatId, userId)
                    listChat.add(newChat)
                    newChat.sendMessage("${user.email} started a chat with you")
                    binding.rvChats.adapter?.notifyItemInserted(listChat.size - 1)
                    Intent(this, ChatActivity::class.java).also {
                        it.putExtra("userId", userId)
                        startActivity(it)
                    }
                }
            }
            if (!exits) {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun checkChatExists(userId: String): Boolean {
        val databaseReference = FirebaseDatabase.getInstance(DATABASE_URL).getReference("Users/${user.uid}/chats")
        var result = false
        databaseReference.get().addOnSuccessListener {
            if (it.value == null) {
                return@addOnSuccessListener
            }
            val listChatId = it.value as HashMap<*, *>
            listChatId.forEach { (key, _) ->
                val id = key as String
                if (id == userId) {
                    result = true
                    return@addOnSuccessListener
                }
            }
        }
        return result
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            // TODO: Inform user that that your app will not show notifications.
            Toast.makeText(this, "You can not receive notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }



    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            uri = data!!.data!!
            try {
                val source = ImageDecoder.createSource(this.contentResolver, uri!!)
                userImage = ImageDecoder.decodeBitmap(source)
                binding.iwAvatar.setImageBitmap(userImage)
                binding.btnSave.visibility = android.view.View.VISIBLE
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun chooseImage() {
        Intent(Intent.ACTION_GET_CONTENT).also {
            it.type = "image/*"
            it.action = Intent.ACTION_GET_CONTENT
            resultLauncher.launch(it)
        }
    }

    private fun uploadImage() {
        val storageRef = FirebaseStorage.getInstance(STORAGE_URL).reference
        val imageRef = storageRef.child("images/${user.uid}.jpg")
        val uploadTask = imageRef.putFile(uri!!)
        uploadTask.addOnFailureListener {
            Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener {
            val databaseReference = FirebaseDatabase.getInstance(DATABASE_URL).getReference("Users/${user.uid}/avatar")
            databaseReference.setValue(1).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Upload success", Toast.LENGTH_SHORT).show()
                    avatar = userImage
                }
                else {
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun downloadImage() {
        val storageRef = FirebaseStorage.getInstance(STORAGE_URL).reference
        val imageRef = storageRef.child("images/${user.uid}.jpg")
        // get inage and put in image view
        val localFile = File.createTempFile("images", "jpg")
        imageRef.getFile(localFile)
            .addOnSuccessListener {
                // Local temp file has been created
                // Now, you can set the downloaded image to an ImageView
                avatar = BitmapFactory.decodeFile(localFile.absolutePath)
                binding.iwAvatar.setImageBitmap(avatar)
            }
            .addOnFailureListener {
                // Handle any errors
                Toast.makeText(this, "Failed to get Avatar", Toast.LENGTH_SHORT).show()
            }
    }

}

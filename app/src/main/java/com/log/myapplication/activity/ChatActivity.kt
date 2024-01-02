package com.log.myapplication.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.log.myapplication.adapter.MessageAdapter
import com.log.myapplication.constant.Constant.Companion.DATABASE_URL
import com.log.myapplication.databinding.ActivityChatBinding
import com.log.myapplication.model.Chat
import com.log.myapplication.model.Message

class ChatActivity : AppCompatActivity() {
    lateinit var binding: ActivityChatBinding
    lateinit var chat: Chat
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        // get chat from intent
        val userId = intent.getStringExtra("userId")
        chat = MainActivity.listChat.find { it.userId == userId }!!

        setContentView(binding.root)

        binding.ibtnBack.setOnClickListener {
            finish()
        }

        binding.rvMessages.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)


        binding.displayName.text = chat.senderEmail
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString()
            if (message.isEmpty()) {
                return@setOnClickListener
            }
            chat.sendMessage(message)
            binding.etMessage.text.clear()
        }


        getMessages()
    }

    private fun getMessages() {
        // get messages from database
        val databaseReference = FirebaseDatabase.getInstance(DATABASE_URL).getReference("Chats/${chat.chatId}")
        databaseReference.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (snapshot.value == null) {
                    return
                }

                val listMessage = snapshot.value as HashMap<*, *>
                chat.listMessage.clear()

                // Convert HashMap to a list of messages
                val messages = listMessage.mapNotNull { (_, value) ->
                    val message = value as HashMap<*, *>
                    val senderId = message["senderId"] as String
                    val messageContent = message["message"] as String
                    val time = message["time"] as String
                    Message(senderId, messageContent, time)
                }

                // Sort messages by timestamp reverse order
                val sortedMessages = messages.sortedBy { it.time }

                // Update the chat.listMessage with sorted messages
                chat.listMessage.addAll(sortedMessages)

                // Set the sorted messages to the adapter
                binding.rvMessages.adapter = MessageAdapter(this@ChatActivity, chat.listMessage, chat.avarta)
                // Scroll to the end of the list
                binding.rvMessages.smoothScrollToPosition(chat.listMessage.size - 1)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Toast.makeText(this@ChatActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
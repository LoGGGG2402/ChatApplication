package com.log.myapplication.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.log.myapplication.activity.ChatActivity
import com.log.myapplication.databinding.ChatViewBinding
import com.log.myapplication.model.Chat

class ChatAdapter(
    private val context: Context,
    private val listChat: MutableList<Chat>,
): RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(binding: ChatViewBinding): RecyclerView.ViewHolder(binding.root) {
        val root = binding.root
        val sender = binding.displayName
        val lastMessage = binding.tvLastMessage
        val avatar = binding.iwAvatar
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ChatViewBinding.inflate(LayoutInflater.from(context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = listChat[position]
        if (chat.senderEmail == null) {
            chat.getInfo(context) {
                if (it) {
                    holder.sender.text = chat.senderEmail
                    if (chat.avarta != null) {
                        holder.avatar.setImageBitmap(chat.avarta)
                    }
                }
            }
        }
        holder.sender.text = chat.senderEmail
        holder.root.setOnClickListener {
            Intent(context, ChatActivity::class.java).also {
                it.putExtra("userId", chat.userId)
                context.startActivity(it)
            }
        }
    }

    override fun getItemCount(): Int {
        return listChat.size
    }

}
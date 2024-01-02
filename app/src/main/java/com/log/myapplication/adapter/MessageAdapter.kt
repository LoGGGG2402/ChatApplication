package com.log.myapplication.adapter


import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.log.myapplication.activity.MainActivity
import com.log.myapplication.databinding.LeftSideBinding
import com.log.myapplication.databinding.RightSideBinding
import com.log.myapplication.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MessageAdapter(
    private val context: Context,
    private val listMessage: MutableList<Message>,
    private val avatar: Bitmap?
): RecyclerView.Adapter<ViewHolder>() {

    companion object {
        private const val MESSAGE_TYPE_LEFT = 0
        private const val MESSAGE_TYPE_RIGHT = 1
    }

    class LeftViewHolder(binding: LeftSideBinding): ViewHolder(binding.root) {
        val message = binding.tvMessage
        val time = binding.tvTime
        val avatar = binding.ivAvatar
    }

    class RightViewHolder(binding: RightSideBinding): ViewHolder(binding.root) {
        val message = binding.tvMessage
        val time = binding.tvTime
        val avatar = binding.ivAvatar
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == MESSAGE_TYPE_LEFT) {
            val binding = LeftSideBinding.inflate(LayoutInflater.from(context), parent, false)
            return LeftViewHolder(binding)
        }
        val binding = RightSideBinding.inflate(LayoutInflater.from(context), parent, false)
        return RightViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = listMessage[position]
        // convert time to date: hour:minute:second
        val date = Date(message.time.toLong())
        // Locale = Viet Nam

        val sdf = SimpleDateFormat("yyyy-MM-dd   HH:mm:ss", Locale.CHINA)
        val time = sdf.format(date)

        if (holder is LeftViewHolder) {
            holder.message.text = message.message
            holder.time.text = time
            if (avatar != null) {
                holder.avatar.setImageBitmap(avatar)
            }
        } else if (holder is RightViewHolder) {
            holder.message.text = message.message
            holder.time.text = time
            holder.avatar.setImageBitmap(MainActivity.avatar)
        }
    }

    override fun getItemCount(): Int {
        return listMessage.size
    }

    override fun getItemViewType(position: Int): Int {
        val message = listMessage[position]
        if (message.senderId == MainActivity.user.uid) {
            return MESSAGE_TYPE_RIGHT
        }
        return MESSAGE_TYPE_LEFT
    }
}
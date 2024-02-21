package com.yjy.forestory.feature.viewPost

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yjy.forestory.R
import com.yjy.forestory.databinding.ItemCommentBinding
import com.yjy.forestory.model.Comment

class CommentAdapter() : ListAdapter<Comment, CommentAdapter.MyViewHolder>(diffUtil) {

    inner class MyViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment, isLastItem: Boolean) {
            binding.comment = comment

            // 이름에 따라 이미지 지정
            val resourceId = when (comment.writerName) {
                "아지" -> R.drawable.ic_dog
                "코코" -> R.drawable.ic_cat
                "터미" -> R.drawable.ic_bear
                "울프" -> R.drawable.ic_wolf
                "미니" -> R.drawable.ic_fox
                "콩이" -> R.drawable.ic_rabbit
                "랑이" -> R.drawable.ic_tiger
                else -> R.drawable.ic_panda
            }

            // 마지막 아이템인 경우 구분선을 숨김
            if (isLastItem) binding.divider.visibility = View.GONE

            binding.circleImageViewWriterPicture.setImageResource(resourceId)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(getItem(position), position == itemCount - 1)
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<Comment>() {
            override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
                return oldItem.commentId == newItem.commentId
            }

            override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
                return oldItem == newItem
            }
        }
    }
}
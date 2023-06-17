package com.yjy.forestory.feature.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yjy.forestory.databinding.ItemCommentBinding
import com.yjy.forestory.model.db.dto.CommentDTO

class CommentAdapter() : ListAdapter<CommentDTO, CommentAdapter.MyViewHolder>(diffUtil) {

    inner class MyViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: CommentDTO) {
            binding.commentDto = comment
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<CommentDTO>() {
            override fun areItemsTheSame(oldItem: CommentDTO, newItem: CommentDTO): Boolean {
                return oldItem.commentId == newItem.commentId
            }

            override fun areContentsTheSame(oldItem: CommentDTO, newItem: CommentDTO): Boolean {
                return oldItem == newItem
            }
        }
    }
}
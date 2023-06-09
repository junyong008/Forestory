package com.yjy.forestory.feature.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yjy.forestory.databinding.ItemGridPostBinding
import com.yjy.forestory.databinding.ItemLinearPostBinding
import com.yjy.forestory.model.db.dto.PostDTO
import com.yjy.forestory.model.db.dto.PostWithComments

class PostAdapter(private val listener: PostItemClickListener, private val isLinearView: Boolean) : ListAdapter<PostWithComments, RecyclerView.ViewHolder>(diffUtil) {

    private val VIEW_TYPE_LINEAR = 0
    private val VIEW_TYPE_GRID = 1

    inner class LinearViewHolder(private val binding: ItemLinearPostBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.buttonAddComment.setOnClickListener {
                listener.onGetCommentClicked(getItem(adapterPosition).post)
            }


        }

        fun bind(postWithComments: PostWithComments) {
            binding.postDto = postWithComments.post
        }
    }

    inner class GridViewHolder(private val binding: ItemGridPostBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                // listener.onClicked(getItem(adapterPosition))
            }
        }

        fun bind(postWithComments: PostWithComments) {
            binding.postDto = postWithComments.post
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LINEAR -> {
                val binding = ItemLinearPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                LinearViewHolder(binding)
            }
            VIEW_TYPE_GRID -> {
                val binding = ItemGridPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                GridViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val postWithComments = getItem(position)
        when (holder) {
            is LinearViewHolder -> {
                holder.bind(postWithComments)
            }
            is GridViewHolder -> {
                holder.bind(postWithComments)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLinearView) VIEW_TYPE_LINEAR else VIEW_TYPE_GRID
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<PostWithComments>() {
            override fun areItemsTheSame(oldItem: PostWithComments, newItem: PostWithComments): Boolean {
                return oldItem.post.postId == newItem.post.postId
            }

            override fun areContentsTheSame(oldItem: PostWithComments, newItem: PostWithComments): Boolean {
                return oldItem == newItem
            }
        }
    }
}


interface PostItemClickListener {
    fun onGetCommentClicked(post: PostDTO)
}
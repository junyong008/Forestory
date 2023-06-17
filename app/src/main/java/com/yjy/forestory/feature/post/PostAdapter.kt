package com.yjy.forestory.feature.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yjy.forestory.databinding.ItemGridPostBinding
import com.yjy.forestory.databinding.ItemLinearPostBinding
import com.yjy.forestory.model.db.dto.PostWithComments

class PostAdapter(private val listener: PostItemClickListener, private val isLinearView: Boolean) : ListAdapter<PostWithComments, RecyclerView.ViewHolder>(diffUtil) {

    private val VIEW_TYPE_LINEAR = 0
    private val VIEW_TYPE_GRID = 1

    inner class LinearViewHolder(private val binding: ItemLinearPostBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // 댓글 추가 버튼 클릭 리스너
            binding.buttonAddComment.setOnClickListener {
                listener.onGetCommentClicked(getItem(adapterPosition))
            }

            // 이미지 클릭 리스너
            binding.imageViewPost.setOnClickListener {
                listener.onPostImageClicked(getItem(adapterPosition))
            }
        }

        fun bind(postWithComments: PostWithComments) {

            // 댓글이 있다면 댓글 추가 버튼 숨기기
            if (postWithComments.comments.isNotEmpty()) {
                binding.buttonAddComment.visibility = View.GONE
            } else {
                binding.buttonAddComment.visibility = View.VISIBLE
            }

            // 댓글이 없고 만약 추가중이라면 버튼 비활성화 및 프로그레스 활성화
            if (postWithComments.comments.isEmpty() && postWithComments.post.isAddingComments) {
                binding.buttonAddComment.setText("")
                binding.buttonAddComment.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.buttonAddComment.setText("숲속 친구들에게 알리기")
                binding.buttonAddComment.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }

            binding.postWithComments = postWithComments
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
    fun onGetCommentClicked(postWithComments: PostWithComments)
    fun onPostImageClicked(postWithComments: PostWithComments)
}
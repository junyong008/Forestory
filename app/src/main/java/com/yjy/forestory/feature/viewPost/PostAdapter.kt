package com.yjy.forestory.feature.viewPost

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yjy.forestory.databinding.ItemGridPostBinding
import com.yjy.forestory.databinding.ItemLinearPostBinding
import com.yjy.forestory.model.PostWithTagsAndComments

class PostAdapter(private val listener: PostItemClickListener, private val isLinearView: Boolean) : PagingDataAdapter<PostWithTagsAndComments, RecyclerView.ViewHolder>(diffUtil) {

    private val VIEW_TYPE_LINEAR = 0
    private val VIEW_TYPE_GRID = 1

    inner class LinearViewHolder(private val binding: ItemLinearPostBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // 댓글 추가 버튼 클릭 리스너
            binding.buttonAddComment.setOnClickListener {
                listener.onGetCommentClicked(getItem(adapterPosition)!!)
            }

            // 이미지 클릭 리스너
            binding.imageViewPost.setOnClickListener {
                listener.onPostImageClicked(getItem(adapterPosition)!!)
            }

            // 옵션 메뉴 클릭 리스너
            binding.ibuttonMenu.setOnClickListener {
                showMenuDialog(binding.ibuttonMenu, getItem(adapterPosition)!!)
            }
        }

        fun bind(postWithTagsAndComments: PostWithTagsAndComments) {

            // 댓글이 없고 만약 추가중이라면 프로그레스 띄우기
            binding.progressBar.isVisible = postWithTagsAndComments.comments.isEmpty() && postWithTagsAndComments.post.isAddingComments
            binding.postWithTagsAndComments = postWithTagsAndComments
        }

        // 옵션 메뉴를 띄우고 해당 아이템 클릭시 리스너에 알림
        private fun showMenuDialog(view: View, postWithTagsAndComments: PostWithTagsAndComments) {
            val menuItems = arrayOf("삭제하기") // 메뉴 항목 배열

            MaterialAlertDialogBuilder(view.context)
                .setItems(menuItems) { dialog, which ->
                    when (which) {
                        0 -> {
                            // "삭제하기" 메뉴 항목 클릭 처리
                            showDeleteConfirmationDialog(view, postWithTagsAndComments)
                        }
                    }
                    dialog.dismiss()
                }
                .show()
        }

        // 삭제 버튼은 한번 더 확인 다이얼로그를 띄움
        private fun showDeleteConfirmationDialog(view: View, postWithTagsAndComments: PostWithTagsAndComments) {
            // 통일된 사용자 경험을 위해 [확인 / 취소] 순서로 변경
            MaterialAlertDialogBuilder(view.context)
                .setMessage("게시글을 삭제하시겠습니까?")
                .setNegativeButton("확인") { dialog, _ ->
                    listener.onDeletePostClicked(postWithTagsAndComments)
                    dialog.dismiss()
                }
                .setPositiveButton("취소") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    inner class GridViewHolder(private val binding: ItemGridPostBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // 이미지 클릭 리스너
            binding.imageViewPost.setOnClickListener {
                listener.onPostImageClicked(getItem(adapterPosition)!!)
            }
        }

        fun bind(postWithTagsAndComments: PostWithTagsAndComments) {
            binding.post = postWithTagsAndComments.post
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
        val postWithTagsAndComments = getItem(position)

        when (holder) {
            is LinearViewHolder -> {
                holder.bind(postWithTagsAndComments!!)
            }
            is GridViewHolder -> {
                holder.bind(postWithTagsAndComments!!)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLinearView) VIEW_TYPE_LINEAR else VIEW_TYPE_GRID
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<PostWithTagsAndComments>() {
            override fun areItemsTheSame(oldItem: PostWithTagsAndComments, newItem: PostWithTagsAndComments): Boolean {
                return oldItem.post.postId == newItem.post.postId
            }

            override fun areContentsTheSame(oldItem: PostWithTagsAndComments, newItem: PostWithTagsAndComments): Boolean {
                return oldItem == newItem
            }
        }
    }
}


interface PostItemClickListener {
    fun onGetCommentClicked(postWithTagsAndComments: PostWithTagsAndComments)
    fun onPostImageClicked(postWithTagsAndComments: PostWithTagsAndComments)
    fun onDeletePostClicked(postWithTagsAndComments: PostWithTagsAndComments)
}
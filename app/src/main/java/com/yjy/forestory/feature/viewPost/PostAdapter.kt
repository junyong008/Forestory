package com.yjy.forestory.feature.viewPost

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.yjy.forestory.R
import com.yjy.forestory.databinding.ItemGridPostBinding
import com.yjy.forestory.databinding.ItemLinearPostBinding
import com.yjy.forestory.model.PostWithTagsAndComments

class PostAdapter(private val listener: PostItemClickListener, private val isLinearView: Boolean) :
    PagingDataAdapter<PostWithTagsAndComments, RecyclerView.ViewHolder>(diffUtil) {

    private val recyclerViewPool = RecyclerView.RecycledViewPool()

    inner class LinearViewHolder(private val binding: ItemLinearPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // 댓글 추가 버튼 클릭 리스너
            binding.buttonAddComment.setOnClickListener {
                listener.onGetCommentClicked(getItem(absoluteAdapterPosition)!!)
            }

            // 이미지 클릭 리스너
            binding.imageViewPost.setOnClickListener { view ->
                listener.onPostImageClicked(getItem(absoluteAdapterPosition)!!, view as ImageView)
            }

            // 옵션 메뉴 클릭 리스너
            binding.ibuttonMenu.setOnClickListener { view ->
                listener.onOptionClicked(getItem(absoluteAdapterPosition)!!, view as ImageButton)
            }

            // 댓글 펼치기 클릭 리스너
            binding.ibuttonExpandComment.setOnClickListener {
                binding.constraintLayoutPreviewComments.visibility = View.GONE
                binding.recyclerViewComments.visibility = View.VISIBLE
            }

            // 각 댓글의 뷰를 재활용할 수 있도록 같은 pool로 묶어줌
            binding.recyclerViewComments.setRecycledViewPool(recyclerViewPool)
        }

        fun bind(postWithTagsAndComments: PostWithTagsAndComments, isLastItem: Boolean) {

            // 댓글이 없고 만약 추가중이라면 프로그레스 띄우기
            binding.progressBar.isVisible =
                postWithTagsAndComments.comments.isEmpty() && postWithTagsAndComments.post.isAddingComments

            // 태그 Chip 바인딩. 클릭시 태그 검색 기능 제공을 위해 리스너 전달
            val chipGroup = binding.chipgroupTags
            val chipTexts = postWithTagsAndComments.tags
            chipGroup.removeAllViews()
            for (chipText in chipTexts) {
                val newChip = LayoutInflater.from(chipGroup.context)
                    .inflate(R.layout.item_readonly_chip, chipGroup, false) as Chip
                newChip.id = ViewCompat.generateViewId()
                newChip.text = chipText.content
                newChip.setOnClickListener {
                    listener.onTagChipClicked(chipText.content)
                }
                chipGroup.addView(newChip)
            }

            // 마지막 아이템인 경우 아래 마진 적용
            val layoutParams = itemView.layoutParams as RecyclerView.LayoutParams
            layoutParams.bottomMargin = if (isLastItem) 16 else 0
            itemView.layoutParams = layoutParams

            // 댓글이 있으면 미리보기로 보여줌.
            binding.recyclerViewComments.visibility = View.GONE
            if (postWithTagsAndComments.comments.isNotEmpty()) {
                binding.textViewCommentContent1.text = postWithTagsAndComments.comments.getOrNull(0)?.content
                binding.textViewCommentContent2.text = postWithTagsAndComments.comments.getOrNull(1)?.content
                binding.constraintLayoutPreviewComments.visibility = View.VISIBLE
            } else {
                binding.constraintLayoutPreviewComments.visibility = View.GONE
            }

            binding.postWithTagsAndComments = postWithTagsAndComments
        }
    }

    inner class GridViewHolder(private val binding: ItemGridPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            // 이미지 클릭 리스너
            binding.imageViewPost.setOnClickListener { view ->
                listener.onPostImageClicked(getItem(absoluteAdapterPosition)!!, view as ImageView)
            }
        }

        fun bind(postWithTagsAndComments: PostWithTagsAndComments) {
            binding.post = postWithTagsAndComments.post
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LINEAR -> {
                val binding =
                    ItemLinearPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                LinearViewHolder(binding)
            }

            VIEW_TYPE_GRID -> {
                val binding =
                    ItemGridPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                GridViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val postWithTagsAndComments = getItem(position)

        when (holder) {
            is LinearViewHolder -> {
                holder.bind(postWithTagsAndComments!!, position == itemCount - 1)
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
        private const val VIEW_TYPE_LINEAR = 0
        private const val VIEW_TYPE_GRID = 1

        val diffUtil = object : DiffUtil.ItemCallback<PostWithTagsAndComments>() {
            override fun areItemsTheSame(
                oldItem: PostWithTagsAndComments, newItem: PostWithTagsAndComments
            ): Boolean {
                return oldItem.post.postId == newItem.post.postId
            }

            override fun areContentsTheSame(
                oldItem: PostWithTagsAndComments, newItem: PostWithTagsAndComments
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}


interface PostItemClickListener {
    fun onGetCommentClicked(postWithTagsAndComments: PostWithTagsAndComments)
    fun onPostImageClicked(postWithTagsAndComments: PostWithTagsAndComments, imageView: ImageView)
    fun onOptionClicked(postWithTagsAndComments: PostWithTagsAndComments, imageButton: ImageButton)
    fun onTagChipClicked(tagText: String)
}
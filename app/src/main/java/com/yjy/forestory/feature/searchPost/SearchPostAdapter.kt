package com.yjy.forestory.feature.searchPost

import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.yjy.forestory.R
import com.yjy.forestory.databinding.ItemSearchPostBinding
import com.yjy.forestory.model.PostWithTagsAndComments

class SearchPostAdapter(private val listener: SearchPostItemClickListener) : PagingDataAdapter<PostWithTagsAndComments, SearchPostAdapter.MyViewHolder>(diffUtil) {

    var keyword: String = ""

    inner class MyViewHolder(private val binding: ItemSearchPostBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.cardView.setOnClickListener {
                listener.onPostClicked(getItem(absoluteAdapterPosition)!!)
            }
        }

        fun bind(postWithTagsAndComments: PostWithTagsAndComments) {

            // 내용 중 검색어를 하이라이트 표시한다
            if (keyword.isNotEmpty()) {
                val highlightedText = highlightSearchText(postWithTagsAndComments.post.content, keyword)
                binding.textViewContent.setText(highlightedText, TextView.BufferType.SPANNABLE)
            } else {
                binding.textViewContent.text = postWithTagsAndComments.post.content
            }

            // 태그 Chip 바인딩. 클릭시 태그 재검색 기능 제공
            val chipGroup = binding.chipgroupTags
            val chipTexts = postWithTagsAndComments.tags
            chipGroup.removeAllViews()
            chipTexts?.let {
                for (chipText in chipTexts) {
                    val newChip = LayoutInflater.from(chipGroup.context).inflate(R.layout.item_readonly_chip, chipGroup, false) as Chip
                    newChip.id = ViewCompat.generateViewId()
                    newChip.text = chipText.content
                    newChip.setOnClickListener {
                        listener.onTagChipClicked(chipText.content)
                    }
                    chipGroup.addView(newChip)
                }
            }

            binding.postWithTagsAndComments = postWithTagsAndComments
        }

        private fun highlightSearchText(fullText: String, searchText: String): SpannableString {
            val spannableString = SpannableString(fullText)
            var start = fullText.indexOf(searchText)
            val colorInt = ContextCompat.getColor(binding.root.context, R.color.lightgreen)

            while (start != -1) {
                val end = start + searchText.length
                spannableString.setSpan(BackgroundColorSpan(colorInt), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                start = fullText.indexOf(searchText, end)
            }

            return spannableString
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemSearchPostBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(getItem(position)!!)
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<PostWithTagsAndComments>() {
            override fun areItemsTheSame(oldItem: PostWithTagsAndComments, newItem: PostWithTagsAndComments): Boolean {
                return oldItem.post.postId == newItem.post.postId
            }

            override fun areContentsTheSame(oldItem: PostWithTagsAndComments, newItem: PostWithTagsAndComments): Boolean {
                return false // 항상 false를 반환하여 내용의 갱신을 강제한다. 동일포스트 재검색시 단어 하이라이트를 변경하기 위함
            }
        }
    }
}

interface SearchPostItemClickListener {
    fun onPostClicked(postWithTagsAndComments: PostWithTagsAndComments)
    fun onTagChipClicked(tagText: String)
}
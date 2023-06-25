package com.yjy.forestory.feature.searchPost

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yjy.forestory.databinding.ItemSearchTagBinding
import com.yjy.forestory.model.Tag

class SearchTagAdapter(private val listener: SearchTagItemClickListener) : ListAdapter<Tag, SearchTagAdapter.MyViewHolder>(diffUtil) {

    inner class MyViewHolder(private val binding: ItemSearchTagBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.cardView.setOnClickListener {
                listener.onTagClicked(getItem(absoluteAdapterPosition))
            }
        }

        fun bind(tag: Tag) {
            binding.tag = tag
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemSearchTagBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<Tag>() {
            override fun areItemsTheSame(oldItem: Tag, newItem: Tag): Boolean {
                return oldItem.tagId == newItem.tagId
            }

            override fun areContentsTheSame(oldItem: Tag, newItem: Tag): Boolean {
                return oldItem == newItem
            }
        }
    }
}

interface SearchTagItemClickListener {
    fun onTagClicked(tag: Tag)
}
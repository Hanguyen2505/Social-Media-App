package com.example.socialmediaapp.adapter.post

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.socialmediaapp.databinding.PostImageOnViewPagerBinding

class PostImageViewPagerAdapter(
) : RecyclerView.Adapter<PostImageViewPagerAdapter.PostViewHolder>() {

    private val imageUrls = mutableListOf<Uri>()

    inner class PostViewHolder(
        private val binding: PostImageOnViewPagerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        val image = binding.image
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = PostImageOnViewPagerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun getItemCount(): Int = imageUrls.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        Glide.with(holder.image.context)
            .load(imageUrls[position].toString())
            .into(holder.image)
    }

    fun updateList(newList: List<Uri>) {
        imageUrls.clear()
        imageUrls.addAll(newList)
        notifyDataSetChanged()
    }
}
package com.example.socialmediaapp.data.entity.comment

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val username: String = "",
    val profilePictureUrl: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

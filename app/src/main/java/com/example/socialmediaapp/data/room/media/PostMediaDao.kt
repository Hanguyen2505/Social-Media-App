package com.example.socialmediaapp.data.room.media

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.socialmediaapp.data.entity.PostMedia

@Dao
interface PostMediaDao {

    @Upsert
    suspend fun upsertPostMedia(postMedia: PostMedia)

    @Query("SELECT * FROM `Post Medias` WHERE postId = :postId")
    fun getPostMediaByPostId(postId: String): List<PostMedia>

    @Delete
    suspend fun deletePostMedia(postMedia: PostMedia)

}
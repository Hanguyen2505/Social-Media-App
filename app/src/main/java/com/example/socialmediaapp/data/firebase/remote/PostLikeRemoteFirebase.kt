package com.example.socialmediaapp.data.firebase.remote

import android.util.Log
import com.example.socialmediaapp.data.entity.PostLike
import com.example.socialmediaapp.other.Constant.COLLECTION_POSTS
import com.example.socialmediaapp.other.Constant.COLLECTION_POST_LIKES
import com.example.socialmediaapp.other.Constant.COLLECTION_USERS
import com.example.socialmediaapp.other.FirebaseChangeType
import com.example.socialmediaapp.other.FirebaseChangeType.ADDED
import com.example.socialmediaapp.other.FirebaseChangeType.REMOVED
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostLikeRemoteFirebase @Inject constructor(
    db: FirebaseFirestore
) {
    private val userCollection = db.collection(COLLECTION_USERS)
    private val postCollection = db.collectionGroup(COLLECTION_POST_LIKES)

    fun addPostLike(currentUserId: String, userId: String, postId: String) {
        val postLikesCollection = userCollection
            .document(userId)
            .collection(COLLECTION_POSTS)
            .document(postId)
            .collection(COLLECTION_POST_LIKES)

        val postLike = PostLike(
            postLikesCollection.document().id,
            currentUserId,
            postId
        )
        postLikesCollection.document(postLike.likeId).set(postLike).addOnSuccessListener {
            Log.d("post", "addPostLike: success")
        }.addOnFailureListener {
            Log.d("post", "addPostLike: failed")
        }
    }

    fun deletePostLike(currentUserId: String, userId: String, postId: String) {
        val postLikesCollection = userCollection
            .document(userId)
            .collection(COLLECTION_POSTS)
            .document(postId)
            .collection(COLLECTION_POST_LIKES)

        postLikesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("postId", postId)
                .get().addOnSuccessListener {
                    for (document in it) {
                        postLikesCollection.document(document.id).delete()
                    }
                }
    }

    fun updateLikeCount(userId: String, postId: String, onUpdate: (Int) -> Unit) {
        val postLikesCollection = userCollection
            .document(userId)
            .collection(COLLECTION_POSTS)
            .document(postId)
            .collection(COLLECTION_POST_LIKES)

        postLikesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("postId", postId)
            .get().addOnSuccessListener {
                onUpdate(it.count())
            }.addOnFailureListener {
                onUpdate(0)
            }


    }

    fun observeLikeCount(postOwnerId: String, postId: String, onUpdate: (Int) -> Unit) {
        val postLikesCollection = userCollection
            .document(postOwnerId)
            .collection(COLLECTION_POSTS)
            .document(postId)
            .collection(COLLECTION_POST_LIKES)

        postLikesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val likeCount = snapshot?.count() ?: 0
            onUpdate(likeCount)
        }
    }

    fun isLiked(currentUserId: String, userId: String, postId: String, onResult: (Boolean) -> Unit) {
        val postLikesCollection = userCollection
            .document(userId)
            .collection(COLLECTION_POSTS)
            .document(postId)
            .collection(COLLECTION_POST_LIKES)

        postLikesCollection
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("postId", postId)
            .get().addOnSuccessListener {
                onResult(!it.isEmpty)
            }.addOnFailureListener {
                onResult(false)
            }

    }

    fun getLikedPostIdsByUser(userId: String, onUpdate: (List<String>) -> Unit) {
        postCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.d("post", "Listen failed.", error)
                    return@addSnapshotListener
                }
                val likedPostIds = snapshot?.documents?.mapNotNull {
                    it.getString("postId")
                }
                if (likedPostIds != null) {
                    onUpdate(likedPostIds)
                }
            }
    }


}
﻿package com.example.socialmediaapp.data.firebase.remote

import android.util.Log
import com.example.socialmediaapp.data.entity.follower.Follower
import com.example.socialmediaapp.data.firebase.authentication.UserAuthentication
import com.example.socialmediaapp.other.Constant.COLLECTION_FOLLOWERS
import com.example.socialmediaapp.other.Constant.COLLECTION_USERS
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FollowerRemoteDatabase @Inject constructor(
    db: FirebaseFirestore,
    private val auth: UserAuthentication,
) {

    private val userCollection = db.collection(COLLECTION_USERS)
    private val followersGroupCollection = db.collectionGroup(COLLECTION_FOLLOWERS)

    // *store follow in current user's collection

    suspend fun followUser(followerId: String, followingId: String) {
        val followersCollection = userCollection.document(followerId).collection(COLLECTION_FOLLOWERS)

        val follower = Follower(
            fid = followersCollection.document().id,
            followerId = followerId,
            followingId = followingId
        )
        followersCollection.document(follower.fid).set(follower).await()

    }

    fun unfollowUser(followerId: String, followingId: String) {
        val followersCollection = userCollection.document(followerId).collection(COLLECTION_FOLLOWERS)

        followersCollection.whereEqualTo("followerId", followerId).whereEqualTo("followingId", followingId).get().addOnSuccessListener {
            for (document in it) {
                followersCollection.document(document.id).delete()
            }
        }
    }

    fun checkIfFollowing(followerId: String, followingId: String, onResult: (Boolean) -> Unit) {
        val followersCollection = userCollection.document(followerId).collection(COLLECTION_FOLLOWERS)
        followersCollection
            .whereEqualTo("followerId", followerId)
            .whereEqualTo("followingId", followingId)
            .addSnapshotListener{ snapshot, e->
                if (e != null) return@addSnapshotListener
                val isFollowing = snapshot?.isEmpty == false
                onResult(isFollowing)
            }

    }

    fun getFollowingUserIds(userId: String, onResult: (List<String>) -> Unit) {
        val followingCollection = userCollection.document(userId).collection(COLLECTION_FOLLOWERS)

        followingCollection.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val followingUserIds =
                snapshot?.documents?.map { it.getString("followingId") ?: "" } ?: emptyList()
            onResult(followingUserIds)
        }
    }

    fun getFollowingCountUpdate(userId: String, onFollowerChange: (Int) -> Unit) {
        userCollection.document(userId).collection(COLLECTION_FOLLOWERS).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val followerCount = snapshot?.count() ?: 0
            onFollowerChange(followerCount)
        }

    }

    fun getFollowerCountUpdate(userId: String, onFollowingChange: (Int) -> Unit) {
        followersGroupCollection.whereEqualTo("followingId", userId)
            .addSnapshotListener { snapshot, error ->
                Log.d("FollowerRemoteDatabase", "error: $error")
                if (error != null) return@addSnapshotListener
                val followingCount = snapshot?.count() ?: 0
                onFollowingChange(followingCount)
            }
    }

}

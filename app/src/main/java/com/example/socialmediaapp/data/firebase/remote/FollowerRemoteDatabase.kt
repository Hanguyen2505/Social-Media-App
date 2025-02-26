﻿package com.example.socialmediaapp.data.firebase.remote

import android.util.Log
import com.example.socialmediaapp.data.entity.Follower
import com.example.socialmediaapp.data.firebase.authentication.UserAuthentication
import com.example.socialmediaapp.other.Constant.COLLECTION_FOLLOWERS
import com.example.socialmediaapp.other.FirebaseChangeType
import com.example.socialmediaapp.other.FirebaseChangeType.ADDED
import com.example.socialmediaapp.other.FirebaseChangeType.NOT_DETECTED
import com.example.socialmediaapp.other.FirebaseChangeType.REMOVED
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FollowerRemoteDatabase @Inject constructor(
    private val db: FirebaseFirestore,
    private val userAuthentication: UserAuthentication,
) {

    private val followersCollection = db.collection(COLLECTION_FOLLOWERS)

    private fun getFollower(followerId: String, followingId: String): Follower {
        return Follower(
            fid = followersCollection.document().id,
            followerId = followerId,
            followingId = followingId,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun followUser(followerId: String, followingId: String) {
        val follower =getFollower(followerId, followingId)
        followersCollection.document(follower.fid).set(follower).await()

    }

    suspend fun getAllFollower(): List<Follower> {
        return try {
            followersCollection.get().await().toObjects(Follower::class.java)
        }
        catch (e: Exception) {
            emptyList()
        }
    }

    fun unfollowUser(followerId: String, followingId: String) {
        followersCollection.whereEqualTo("followerId", followerId).whereEqualTo("followingId", followingId).get().addOnSuccessListener {
            for (document in it) {
                followersCollection.document(document.id).delete()
            }
        }
    }

    fun listenForFollowerChanges(onFollowerChange: (FirebaseChangeType, Follower) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection(COLLECTION_FOLLOWERS)
//            .whereEqualTo("followingId", followingId) // Listen for changes related to the current user
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("Firestore", "Error listening for follower changes", error)
                    return@addSnapshotListener
                }

                snapshots?.let {
                    for (docChange in it.documentChanges) {
                        val fid = docChange.document.getString("fid") ?: continue
                        val flrId = docChange.document.getString("followerId") ?: continue
                        val flingId = docChange.document.getString("followingId") ?: continue
                        val timestamp = docChange.document.getLong("timestamp") ?: continue

                        val follower = Follower(fid, flrId, flingId, timestamp)

                        val result: FirebaseChangeType = when (docChange.type) {
                            DocumentChange.Type.ADDED -> {
                                ADDED
                            }
                            DocumentChange.Type.REMOVED -> {
                                REMOVED
                            }
                            else -> {
                                NOT_DETECTED
                            }
                        }
                        onFollowerChange(result, follower)

                    }
                }
            }
    }

}

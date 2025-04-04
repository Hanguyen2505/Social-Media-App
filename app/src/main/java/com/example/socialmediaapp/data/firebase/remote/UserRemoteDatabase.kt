package com.example.socialmediaapp.data.firebase.remote

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.socialmediaapp.data.entity.user.User
import com.example.socialmediaapp.other.Constant.COLLECTION_USERS
import com.example.socialmediaapp.other.FirebaseChangeType
import com.example.socialmediaapp.other.FirebaseChangeType.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRemoteDatabase @Inject constructor(
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val firebaseMessaging: FirebaseMessaging
) {
    private val usersCollection = db.collection(COLLECTION_USERS)
    private val storageRef = storage.reference

    private val userId = auth.currentUser?.uid ?: ""

    suspend fun fetchUserInfo(userId: String): User {
        return try {
            usersCollection.document(userId).get().await().toObject(User::class.java) ?: User()
        }
        catch (e: Exception) {
            User()
        }
    }

    fun observeUser(userId: String): LiveData<User?> {
        val liveData = MutableLiveData<User?>()
        usersCollection.document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    liveData.postValue(snapshot.toObject(User::class.java))
                }
            }
        return liveData
    }

    suspend fun upsertUser(user: User) {
        usersCollection.document(user.userId).set(user).await()
    }

    suspend fun updateName(userId: String, name: String) {
        usersCollection.document(userId).update("name", name).await()
    }

    suspend fun updateUsername(userId: String, username: String) {
        usersCollection.document(userId).update("username", username).await()
    }

    suspend fun updateBio(userId: String, bio: String) {
        usersCollection.document(userId).update("bio", bio).await()
    }

    suspend fun updateGender(uid: String, gender: Boolean) {
        usersCollection.document(uid).update("gender", gender).await()
    }

    private suspend fun updateProfilePicture(userId: String, profilePicture: String) {
        try {
            usersCollection.document(userId).update("profilePictureUrl", profilePicture).await()
        }
        catch (e: Exception) {
            Log.d("Fail to update", "Failed to update profile picture: $e")
        }
    }

    private fun uploadPfpToStorage(fileName: String, imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val imageRef = storageRef.child(fileName)
        imageRef.putFile(imageUri).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { url ->
                onSuccess(url.toString())
            }.addOnFailureListener { exception ->
                Log.d("Fail to upload", "Failed to upload image: $exception")
                onFailure(exception)
            }
        }
    }

    fun setProfilePictureAsDefault(userId: String, gender: Boolean) {
        val imageRef = if (gender) {
            storageRef.child("pfp/default/man.png")
        } else {
            storageRef.child("pfp/default/woman.png")
        }
        imageRef.downloadUrl.addOnSuccessListener { url ->
            Log.d("UserPfp", "Success: $url")
            CoroutineScope(Dispatchers.IO).launch {
                updateProfilePicture(userId, url.toString())
            }
        }.addOnFailureListener { exception ->
            Log.d("UserPfp", "Failed to get download URL: $exception")
        }
    }

    fun handlePfpUpload(userId: String, imageUri: Uri?) {
        uploadPfpToStorage(
            "pfp/$userId/${System.currentTimeMillis()}.jpg",
            imageUri.toString().toUri(),
            onSuccess = { downloadUrl ->
                Log.d("Upload", "Success: $downloadUrl")
                CoroutineScope(Dispatchers.IO).launch {
                    updateProfilePicture(userId, downloadUrl)
                }
            },
            onFailure = { exception ->
                Log.e("Upload", "Failed to upload image", exception)
            }
        )
    }

    fun listenForUsersChanges(onUserChange: (FirebaseChangeType, User) -> Unit) {
        usersCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e("Firestore_ERROR", "Error listening for user changes", error)
                return@addSnapshotListener
            }

            snapshots?.let {
                for (docChange in it.documentChanges) {
                    val userId = docChange.document.getString("userId") ?: continue
                    val name = docChange.document.getString("name") ?: continue
                    val username = docChange.document.getString("username") ?: continue
                    val bio = docChange.document.getString("bio") ?: continue
                    val gender = docChange.document.getBoolean("gender") ?: continue
                    val email = docChange.document.getString("email") ?: continue
                    val profilePictureUrl = docChange.document.getString("profilePictureUrl") ?: continue
                    Log.d("user_listener", "pfp changed: $profilePictureUrl")

                    val user = User(userId, name, username, gender, email, bio, profilePictureUrl)

                    val result: FirebaseChangeType = when (docChange.type) {
                        DocumentChange.Type.ADDED -> {
                            ADDED
                        }
                        DocumentChange.Type.MODIFIED -> {
                            MODIFIED
                        }
                        DocumentChange.Type.REMOVED -> {
                            REMOVED
                        }
                        else -> {
                            NOT_DETECTED
                        }
                    }
                    onUserChange(result, user)
                }
            }
        }
    }

    suspend fun searchUser(query: String): List<User> {
        return try {
            val lowercaseQuery = query.lowercase()

            usersCollection
                .whereGreaterThanOrEqualTo("username", lowercaseQuery)
                .whereLessThanOrEqualTo("username", lowercaseQuery + "\uf8ff")
                .get()
                .await()
                .toObjects(User::class.java)
        }
        catch (e: Exception) {
            emptyList()
        }
    }

    fun saveFcmTokenToFirebase() {
        firebaseMessaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                CoroutineScope(Dispatchers.IO).launch {
                    usersCollection.document(userId).update("fcmToken", token).await()
                }
            }
        }
    }

    fun getTokensFromFollower(followerIds: List<String>): List<String> {
        val tokens = mutableListOf<String>()
        for (id in followerIds) {
            usersCollection.document(id).get().addOnSuccessListener {
                val user = it.getString("fcmToken")
                if (user != null) {
                    tokens.add(user)
                }
            }
        }
        return tokens
    }

}
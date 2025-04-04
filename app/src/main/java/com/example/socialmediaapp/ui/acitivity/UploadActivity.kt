package com.example.socialmediaapp.ui.acitivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmediaapp.R
import com.example.socialmediaapp.adapter.PostImageAdapter
import com.example.socialmediaapp.data.entity.notification.FcmMessage
import com.example.socialmediaapp.data.entity.notification.NotificationData
import com.example.socialmediaapp.data.entity.notification.PushNotification
import com.example.socialmediaapp.data.firebase.api.FirebaseAuthTokenProvider
import com.example.socialmediaapp.data.firebase.authentication.UserAuthentication
import com.example.socialmediaapp.databinding.ActivityPostBinding
import com.example.socialmediaapp.retrofit.RetrofitInstance
import com.example.socialmediaapp.viewmodel.PostViewModel
import com.example.socialmediaapp.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UploadActivity : AppCompatActivity() {

    private var _binding: ActivityPostBinding?= null
    private val binding get() = _binding!!

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    private val mUserViewModel: UserViewModel by viewModels()
    private val mPostViewModel: PostViewModel by viewModels()

    private val imageUris = mutableListOf<Uri>()
    private lateinit var postImageAdapter: PostImageAdapter

    private val fAuthTokenProvider = FirebaseAuthTokenProvider(this)

    @Inject
    lateinit var userAuthentication: UserAuthentication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.bottomBar.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)


        binding.cancel.setOnClickListener {
            finish()
        }

    }

    override fun onStart() {
        super.onStart()
        imagePicker()
        setRecyclerView()
        mUserViewModel.fetchUserInfo(userAuthentication.getCurrentUser()!!.uid)

        postImageAdapter.setOnCancelClickListener {
            imageUris.remove(it)
            postImageAdapter.updateList(imageUris)
        }


    }

    private fun setRecyclerView() {
        postImageAdapter = PostImageAdapter()
        binding.recyclerView.adapter = postImageAdapter

        binding.recyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

    }

    private fun imagePicker() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val clipData = result.data?.clipData

            if (clipData != null) {
                // Multiple images selected
                for (i in 0 until clipData.itemCount) {
                    imageUris.add(clipData.getItemAt(i).uri)
                }
                Log.d("image picked", "imagePicker: ${postImageAdapter.itemCount}")
            } else {
                // Single image selected
                result.data?.data?.let { uri ->
                    imageUris.add(uri)
                }
            }
            postImageAdapter.updateList(imageUris)
        }

        binding.post.setOnClickListener {
            imageUris.let { uris ->
                if (uris.isNotEmpty()) {
                    uploadPost(uris)
                    finish()
                }
                else {
                    Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
                }
            }
            Log.d("image picked", "imagePicker: $imageUris")
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Enable multiple selection
        }
        imagePickerLauncher.launch(intent)
    }

    private fun uploadPost(imageUrl: List<Uri>) {
        val content = binding.content.text.toString()
        val userId = userAuthentication.getCurrentUser()!!.uid
        val postState = true
        mPostViewModel.uploadPost(userId, content, imageUrl, postState)

        sendNotification()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.post_bottom_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.image -> {
                openImagePicker()
                return true
            }
            R.id.text -> {
                return true
            }
            R.id.add -> {
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun sendNotification() = CoroutineScope(Dispatchers.IO).launch {
        try {

            val oauthToken = fAuthTokenProvider.getAccessToken()

            val username = userAuthentication.getCurrentUser()!!.displayName

            val notification = FcmMessage(
                message = PushNotification(
                    topic = "user_" + userAuthentication.getCurrentUser()!!.uid,
                    notification = NotificationData(
                        title = "New Post!",
                        body = buildString {
                            append(username)
                            append(" you follow just posted something new!")
                        }
                    ),
                    data = NotificationData(
                        title = "New Post!",
                        body = buildString {
                            append(username)
                            append(" you follow just posted something new!")
                        }
                    )
                )
            )

            val response = RetrofitInstance.api.sendNotification("Bearer $oauthToken", notification)
            if (response.isSuccessful) {
                Log.d("Notification", "Notification sent successfully")
            } else {
                Log.e("Notification", "Failed to send notification: ${response.errorBody()?.string()}")
            }
        }
        catch (e: Exception) {
            Log.e("Notification", "Error sending notification", e)

        }

    }

}
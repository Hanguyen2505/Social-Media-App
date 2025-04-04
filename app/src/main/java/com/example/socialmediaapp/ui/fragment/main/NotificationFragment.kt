package com.example.socialmediaapp.ui.fragment.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socialmediaapp.adapter.notification.NotificationAdapter
import com.example.socialmediaapp.data.firebase.authentication.UserAuthentication
import com.example.socialmediaapp.databinding.FragmentNotificationBinding
import com.example.socialmediaapp.viewmodel.FollowerViewModel
import com.example.socialmediaapp.viewmodel.PostViewModel
import com.example.socialmediaapp.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var userAuthentication: UserAuthentication

    private val newNotificationAdapter = NotificationAdapter()
    private val earlyNotificationAdapter = NotificationAdapter()

    private val mPostViewModel: PostViewModel by viewModels()
    private val mFollowViewModel: FollowerViewModel by viewModels()
    private val mUserViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViewModel()

        binding.swipeRefreshLayout.setOnRefreshListener {
            setUpViewModel()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        subscribeToRecyclerView()
        onClickListener()

        mUserViewModel.user.observe(viewLifecycleOwner) {
            binding.userName.text = it!!.username
        }
    }

    private fun setUpViewModel() {
        mUserViewModel.fetchUserInfo(userAuthentication.getCurrentUser()!!.uid)
        mFollowViewModel.getFollowingUserIds(userAuthentication.getCurrentUser()!!.uid)
        lifecycleScope.launch {
            mFollowViewModel.followingIds.collectLatest {
                Log.d("NotificationFragment", "followingIds: $it")
                mPostViewModel.getNotification(it)
            }
        }
    }

    private fun onClickListener() {
        earlyNotificationAdapter.setOnItemClickListener {
            val action = NotificationFragmentDirections.actionNotificationFragmentToPostFragment(it, true)
            findNavController().navigate(action)
        }

        newNotificationAdapter.setOnItemClickListener {
            val action = NotificationFragmentDirections.actionNotificationFragmentToPostFragment(it, true)
            findNavController().navigate(action)
        }
    }

    private fun subscribeToRecyclerView() {
        val earlyRecyclerView = binding.earlyNotificationRecyclerview
        val newRecyclerView = binding.newNotificationRecyclerview

        earlyRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )

        newRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )

        mPostViewModel.notification.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                Log.d("NotificationFragment", "notification: $it")
                newNotificationAdapter.updateList(it)
                newRecyclerView.adapter = newNotificationAdapter
            }
        }
    }
}
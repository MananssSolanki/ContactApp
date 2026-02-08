package com.example.contactapp.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactapp.Adapter.CallLogAdapter
import com.example.contactapp.ViewModel.CallLogViewModel
import com.example.contactapp.databinding.FragmentRecentBinding

class RecentFragment : Fragment() {

    private lateinit var binding: FragmentRecentBinding
    private lateinit var viewModel: CallLogViewModel
    private lateinit var adapter: CallLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[CallLogViewModel::class.java]

        setupRecyclerView()
        setupSwipeRefresh()
        observeData()
        checkPermissionAndLoad()
    }

    private fun setupRecyclerView() {
        adapter = CallLogAdapter(
            onCallClick = { phoneNumber ->
                makeCall(phoneNumber)
            },
            onItemClick = { callLog ->
                // TODO: Show call details or options
                Toast.makeText(
                    requireContext(),
                    "Call from ${callLog.contactName ?: callLog.phoneNumber}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        binding.rvCallLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCallLogs.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observeData() {
        viewModel.callLogs.observe(viewLifecycleOwner) { callLogs ->
            adapter.submitList(callLogs)
            binding.swipeRefresh.isRefreshing = false
            
            // Show empty state if no call logs
            if (callLogs.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvCallLogs.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvCallLogs.visibility = View.VISIBLE
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (!binding.swipeRefresh.isRefreshing) {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun checkPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            binding.tvPermissionDenied.visibility = View.GONE
            viewModel.loadCallLogs()
        } else {
            binding.tvPermissionDenied.visibility = View.VISIBLE
            binding.rvCallLogs.visibility = View.GONE
        }
    }

    private fun makeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Failed to make call: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Call permission required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload call logs when fragment resumes
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadCallLogs()
        }
    }
}
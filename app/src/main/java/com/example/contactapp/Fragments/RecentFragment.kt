package com.example.contactapp.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactapp.Adapter.CallLogAdapter
import com.example.contactapp.ViewModel.CallLogViewModel
import com.example.contactapp.databinding.FragmentRecentBinding

class RecentFragment : Fragment() {

    private var _binding: FragmentRecentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CallLogViewModel
    private lateinit var adapter: CallLogAdapter

    // Requests READ_CALL_LOG + WRITE_CALL_LOG permissions
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms[Manifest.permission.READ_CALL_LOG] == true) {
                binding.layoutPermissionDenied.visibility = View.GONE
                binding.rvCallLogs.visibility = View.VISIBLE
                viewModel.loadCallLogs(force = true)
            } else {
                binding.layoutPermissionDenied.visibility = View.VISIBLE
                binding.rvCallLogs.visibility = View.GONE
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[CallLogViewModel::class.java]

        setupRecyclerView()
        setupSelectionToolbar()
        setupSwipeRefresh()
        observeData()
        checkPermissionAndLoad()
    }

    // ─── RecyclerView ────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = CallLogAdapter(
            onCallClick = { phoneNumber -> makeCall(phoneNumber) },
            onItemClick = { callLog ->
                Toast.makeText(
                    requireContext(),
                    callLog.contactName ?: callLog.phoneNumber,
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSelectionChanged = { count -> updateSelectionUI(count) },
            style = 1
        )
        binding.rvCallLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCallLogs.adapter = adapter
    }

    // ─── Selection toolbar ───────────────────────────────────────────────────────

    private fun setupSelectionToolbar() {
        // Cancel — exits selection mode without deleting
        binding.btnCancelSelection.setOnClickListener {
            adapter.clearSelection()
            hideSelectionToolbar()
        }

        // Select all items
        binding.btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }

        // Delete selected items
        binding.btnDelete.setOnClickListener {
            val selected = adapter.selectedIds.toSet()
            if (selected.isEmpty()) return@setOnClickListener
            confirmAndDelete(selected)
        }

        // "Grant Permission" button inside permission denied panel
        binding.btnGrantPermission.setOnClickListener {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.WRITE_CALL_LOG
                )
            )
        }
    }

    private fun updateSelectionUI(count: Int) {
        if (count > 0) {
            showSelectionToolbar(count)
        } else {
            hideSelectionToolbar()
        }
    }

    private fun showSelectionToolbar(count: Int) {
        binding.selectionToolbar.visibility = View.VISIBLE
        binding.tvSelectedCount.text = "$count selected"
        // Disable pull-to-refresh while in selection mode
        binding.swipeRefresh.isEnabled = false
    }

    private fun hideSelectionToolbar() {
        binding.selectionToolbar.visibility = View.GONE
        binding.tvSelectedCount.text = "0 selected"
        binding.swipeRefresh.isEnabled = true
    }

    // ─── Delete ──────────────────────────────────────────────────────────────────

    private fun confirmAndDelete(ids: Set<String>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Call Logs")
            .setMessage("Delete ${ids.size} selected call log(s)?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCallLogs(ids) { success ->
                    if (success) {
                        // Update adapter list in-place — no full reload needed
                        adapter.removeItemsByIdAndExitSelection(ids)
                        hideSelectionToolbar()
                    } else {
                        Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── Swipe to refresh ────────────────────────────────────────────────────────

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh() // force = true inside refresh()
        }
    }

    // ─── Observers ───────────────────────────────────────────────────────────────

    private fun observeData() {
        viewModel.callLogs.observe(viewLifecycleOwner) { callLogs ->
            adapter.submitList(callLogs)
            binding.swipeRefresh.isRefreshing = false
            binding.tvEmptyState.visibility = if (callLogs.isEmpty()) View.VISIBLE else View.GONE
            binding.rvCallLogs.visibility = if (callLogs.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            // Don't show spinner if swipe-refresh indicator is already showing
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

    // ─── Permission ──────────────────────────────────────────────────────────────

    private fun checkPermissionAndLoad() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            binding.layoutPermissionDenied.visibility = View.GONE
            // ViewModel's hasLoaded guard prevents duplicate content-resolver calls
            viewModel.loadCallLogs()
        } else {
            binding.layoutPermissionDenied.visibility = View.VISIBLE
            binding.rvCallLogs.visibility = View.GONE
        }
    }

    /**
     * onResume: delegate to ViewModel.loadCallLogs() which is idempotent.
     * If already loaded, it skips the content-resolver query entirely.
     */
    override fun onResume() {
        super.onResume()
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.loadCallLogs()
    }

    // ─── Calling ─────────────────────────────────────────────────────────────────

    private fun makeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                startActivity(Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                })
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Call permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
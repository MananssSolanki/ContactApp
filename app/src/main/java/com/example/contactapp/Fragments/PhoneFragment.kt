package com.example.contactapp.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.contactapp.Activities.AddContactActivity
import com.example.contactapp.ViewModel.ContactsViewModel
import com.example.contactapp.ViewModel.DialpadViewModel
import com.example.contactapp.databinding.FragmentPhoneBinding

class PhoneFragment : Fragment() {

    private var _binding: FragmentPhoneBinding? = null
    private val binding get() = _binding!!

    private lateinit var dialpadViewModel: DialpadViewModel
    private lateinit var contactsViewModel: ContactsViewModel // FIX: for add-contact intent with prefill

    private val currentNumber = StringBuilder()

    private val callPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) makeCall()
            else Toast.makeText(requireContext(), "Call permission denied", Toast.LENGTH_SHORT).show()
        }

    // FIX: Launcher for add-contact; invalidates contacts cache when returning
    private val addContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            contactsViewModel.invalidateCache()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPhoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialpadViewModel = ViewModelProvider(this)[DialpadViewModel::class.java]
        contactsViewModel = ViewModelProvider(requireActivity())[ContactsViewModel::class.java]

        setupDialpad()
        setupActionButtons()
        observeViewModel()
    }

    private fun setupDialpad() {
        val numberButtons = listOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9", binding.btnStar to "*", binding.btnHash to "#"
        )
        numberButtons.forEach { (button, digit) ->
            button.setOnClickListener { appendDigit(digit) }
        }
        // Long press 0 → "+"
        binding.btn0.setOnLongClickListener {
            if (currentNumber.isEmpty()) appendDigit("+")
            true
        }
    }

    private fun setupActionButtons() {
        binding.btnBackspace.setOnClickListener {
            if (currentNumber.isNotEmpty()) {
                currentNumber.deleteCharAt(currentNumber.length - 1)
                updateDisplay()
                scheduleLookup()
            }
        }
        binding.btnBackspace.setOnLongClickListener {
            currentNumber.clear()
            updateDisplay()
            scheduleLookup()
            true
        }

        binding.btnCall.setOnClickListener {
            if (currentNumber.isNotEmpty()) checkCallPermissionAndCall()
            else Toast.makeText(requireContext(), "Enter a phone number", Toast.LENGTH_SHORT).show()
        }

        binding.btnAddContact.setOnClickListener {
            // Open in-app AddContactActivity and pre-fill the dialed number
            val intent = Intent(requireContext(), AddContactActivity::class.java).apply {
                putExtra("PREFILL_PHONE", currentNumber.toString())
            }
            addContactLauncher.launch(intent)
        }
    }

    private fun observeViewModel() {
        dialpadViewModel.contactName.observe(viewLifecycleOwner) { name ->
            if (name != null) {
                binding.tvContactName.text = name
                binding.tvContactName.visibility = View.VISIBLE
                binding.btnAddContact.visibility = View.GONE
            } else {
                binding.tvContactName.visibility = View.GONE
                binding.btnAddContact.visibility =
                    if (currentNumber.isNotEmpty() && dialpadViewModel.isValidPhoneNumber(currentNumber.toString()))
                        View.VISIBLE else View.GONE
            }
        }
    }

    private fun appendDigit(digit: String) {
        currentNumber.append(digit)
        updateDisplay()
        scheduleLookup()
    }

    private fun updateDisplay() {
        binding.etPhoneNumber.setText(currentNumber.toString())
        binding.etPhoneNumber.setSelection(currentNumber.length)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val lookupRunnable = Runnable {
        dialpadViewModel.lookupContact(currentNumber.toString())
    }

    private fun scheduleLookup() {
        handler.removeCallbacks(lookupRunnable)
        handler.postDelayed(lookupRunnable, 300)
    }

    private fun checkCallPermissionAndCall() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) makeCall()
        else callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
    }

    private fun makeCall() {
        val number = currentNumber.toString()
        if (!dialpadViewModel.isValidPhoneNumber(number)) {
            Toast.makeText(requireContext(), "Invalid phone number", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val telecomManager = requireContext().getSystemService(TelecomManager::class.java)
            val uri = Uri.fromParts("tel", number, null)
            telecomManager.placeCall(uri, Bundle())
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to make call: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(lookupRunnable)
        _binding = null
    }
}
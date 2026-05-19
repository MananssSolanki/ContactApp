package com.example.contactapp.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.contactapp.Activities.AddContactActivity
import com.example.contactapp.R
import com.example.contactapp.ViewModel.ContactsViewModel
import com.example.contactapp.ViewModel.DialpadViewModel
import com.example.contactapp.databinding.FragmentPhoneBinding

class PhoneFragment : Fragment() {

    private var _binding: FragmentPhoneBinding? = null
    private val binding get() = _binding!!

    private lateinit var dialpadViewModel: DialpadViewModel
    private lateinit var contactsViewModel: ContactsViewModel

    private val currentNumber = StringBuilder()

    private val callPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) makeCall()
            else Toast.makeText(requireContext(), "Call permission denied", Toast.LENGTH_SHORT).show()
        }

    private val addContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            contactsViewModel.invalidateCache()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        updateDisplay()
    }

    private fun setupDialpad() {
        styleDialpadKeys()

        val numberButtons = listOf(
            binding.btn0 to "0",
            binding.btn1 to "1",
            binding.btn2 to "2",
            binding.btn3 to "3",
            binding.btn4 to "4",
            binding.btn5 to "5",
            binding.btn6 to "6",
            binding.btn7 to "7",
            binding.btn8 to "8",
            binding.btn9 to "9",
            binding.btnStar to "*",
            binding.btnHash to "#"
        )

        numberButtons.forEach { (button, digit) ->
            button.setOnClickListener { appendDigit(digit) }
        }

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
                    if (currentNumber.isNotEmpty() && dialpadViewModel.isValidPhoneNumber(currentNumber.toString())) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
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

        val hasNumber = currentNumber.isNotEmpty()
        binding.btnBackspace.visibility = if (hasNumber) View.VISIBLE else View.INVISIBLE
        binding.btnCall.alpha = if (hasNumber) 1f else 0.55f
    }

    private fun styleDialpadKeys() {
        val secondaryColor = ContextCompat.getColor(requireContext(), R.color.brand_text_secondary)
        val keyLabels = mapOf(
            binding.btn1 to "1\n",
            binding.btn2 to "2\nABC",
            binding.btn3 to "3\nDEF",
            binding.btn4 to "4\nGHI",
            binding.btn5 to "5\nJKL",
            binding.btn6 to "6\nMNO",
            binding.btn7 to "7\nPQRS",
            binding.btn8 to "8\nTUV",
            binding.btn9 to "9\nWXYZ",
            binding.btnStar to "*",
            binding.btn0 to "0\n+",
            binding.btnHash to "#"
        )

        keyLabels.forEach { (view, label) ->
            view.text = buildKeyLabel(label, secondaryColor)
        }
    }

    private fun buildKeyLabel(label: String, secondaryColor: Int): SpannableString {
        val newlineIndex = label.indexOf('\n')
        return SpannableString(label).apply {
            if (newlineIndex >= 0 && newlineIndex < label.lastIndex) {
                setSpan(
                    AbsoluteSizeSpan(12, true),
                    newlineIndex + 1,
                    label.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    ForegroundColorSpan(secondaryColor),
                    newlineIndex + 1,
                    label.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
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
        ) {
            makeCall()
        } else {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun makeCall() {
        val number = currentNumber.toString()
        if (!dialpadViewModel.isValidPhoneNumber(number)) {
            Toast.makeText(requireContext(), "Invalid phone number", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.fromParts("tel", number, null)
            }
            startActivity(intent)
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

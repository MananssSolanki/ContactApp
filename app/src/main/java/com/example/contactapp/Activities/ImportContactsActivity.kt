package com.example.contactapp.Activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.Model.ContactEnhanced
import com.example.contactapp.EdgeToEdgeActivity
import com.example.contactapp.R
import com.example.contactapp.Repository.ImportRepository
import com.example.contactapp.databinding.ActivityImportContactsBinding
import com.example.contactapp.databinding.ItemImportAccountBinding
import com.example.contactapp.databinding.ItemImportContactBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportContactsActivity : EdgeToEdgeActivity() {

    private lateinit var binding: ActivityImportContactsBinding
    private lateinit var importRepository: ImportRepository

    private var sourceAccount: ImportRepository.ImportAccount? = null
    private var targetAccount: ImportRepository.ImportAccount? = null
    private var selectedContacts = mutableListOf<ContactEnhanced>()
    private var allSourceContacts = listOf<ContactEnhanced>()

    private val FROM_STEP = 0
    private val SELECTION_STEP = 1
    private val TO_STEP = 2
    private val SUCCESS_STEP = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        importRepository = ImportRepository(this)

        setupUI()
        loadFromAccounts()
    }

    private val vcfPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                importVcfFile(it)
            }
        }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSuccessOk.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            val adapter = binding.rvContactsToImport.adapter as? ContactSelectionAdapter
            adapter?.selectAll(isChecked)
        }

        binding.btnDoneSelection.setOnClickListener {
            if (selectedContacts.isEmpty()) {
                Toast.makeText(this, "Select at least one contact", Toast.LENGTH_SHORT).show()
            } else {
                goToStep(TO_STEP)
                loadToAccounts()
            }
        }

        binding.btnImport.setOnClickListener {
            if (targetAccount != null) {
                performImport()
            }
        }
    }

    private fun goToStep(step: Int) {
        binding.viewAnimator.displayedChild = step
        when (step) {
            FROM_STEP -> {
                binding.tvHeaderTitle.text = "Import contacts"
            }
            SELECTION_STEP -> {
                binding.tvHeaderTitle.text = "Select contacts"
            }
            TO_STEP -> {
                binding.tvHeaderTitle.text = "Import contacts"
                binding.tvSelectedFromName.text = sourceAccount?.name
                binding.tvSelectedFromCount.text = "${selectedContacts.size} contacts"
                // Icon for source
                val icon = when {
                    sourceAccount?.isSim == true -> R.drawable.ic_sim
                    sourceAccount?.name == "Phone" -> R.drawable.ic_phone_storage
                    else -> R.drawable.ic_google
                }
//                binding.ivSelectedFromIcon.setImageResource(icon)
            }
            SUCCESS_STEP -> {
                binding.tvHeaderTitle.text = "Import contacts"
                binding.tvSuccessMessage.text = "Contacts added to your ${targetAccount?.name ?: "account"}"
            }
        }
    }

    private fun loadFromAccounts() {
        lifecycleScope.launch {

            val simAccounts = importRepository.getAvailableAccounts(includeSim = true)
                .filter { it.isSim }

            val phoneAccount = ImportRepository.ImportAccount(
                name = "Phone",
                type = "Local",
                isSim = false,
                simSlot = 0
            )

            val finalList = mutableListOf<ImportRepository.ImportAccount>()

            finalList.addAll(simAccounts) // SIM 1, SIM 2 if available
            finalList.add(phoneAccount)

            binding.rvFromAccounts.layoutManager =
                LinearLayoutManager(this@ImportContactsActivity)

            binding.rvFromAccounts.adapter =
                AccountAdapter(finalList) { selected ->
                    sourceAccount = selected
                    loadSourceContacts(selected)
                }
        }
    }

    private fun loadSourceContacts(account: ImportRepository.ImportAccount) {
        lifecycleScope.launch {

            if (ContextCompat.checkSelfPermission(
                    this@ImportContactsActivity,
                    Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@ImportContactsActivity,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    101
                )
                return@launch
            }

            // ✅ If SIM → load SIM contacts
            if (account.isSim) {

                allSourceContacts = importRepository.fetchSimContacts(account.simSlot)

                if (allSourceContacts.isEmpty()) {
                    Toast.makeText(
                        this@ImportContactsActivity,
                        "No contacts found in ${account.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                setupSelectionScreen()
                goToStep(SELECTION_STEP)

            } else {
                // ✅ If Phone → open VCF picker
                openVcfPicker()
            }
        }
    }

    private fun openVcfPicker() {
        vcfPickerLauncher.launch(arrayOf("text/x-vcard", "text/vcard"))
    }

    private fun importVcfFile(uri: Uri) {
        try {

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/x-vcard")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to open vCard", Toast.LENGTH_SHORT).show()
        }
    }



    private fun setupSelectionScreen() {
        selectedContacts.clear()
        binding.tvSelectionCount.text = "0 selected"
        binding.rvContactsToImport.layoutManager = LinearLayoutManager(this)
        val adapter = ContactSelectionAdapter(allSourceContacts) { contact, isSelected ->
            if (isSelected) selectedContacts.add(contact)
            else selectedContacts.remove(contact)
            binding.tvSelectionCount.text = "${selectedContacts.size} selected"
        }
        binding.rvContactsToImport.adapter = adapter
    }

    private fun loadToAccounts() {
        lifecycleScope.launch {

            val accounts = importRepository.getAvailableAccounts(includeSim = true)

            val filteredAccounts = accounts.filter { account ->

                val isPhone = account.name.equals("Phone", true)
                val isSim = account.isSim
                val isGoogle = account.type.contains("google", true)

                (isPhone || isSim || isGoogle) &&
                        account.name != sourceAccount?.name
            }

            binding.rvToAccounts.layoutManager =
                LinearLayoutManager(this@ImportContactsActivity)

            binding.rvToAccounts.adapter =
                AccountAdapter(filteredAccounts, showRadioButton = true) { selected ->
                    targetAccount = selected
                    binding.btnImport.visibility = View.VISIBLE
                }
        }
    }

    private fun performImport() {
        val target = targetAccount ?: return
        val contacts = ArrayList(selectedContacts)

        lifecycleScope.launch(Dispatchers.IO) {
            showNotification(0, contacts.size)

            var successCount = 0
            for ((index, contact) in contacts.withIndex()) {
                val success = importContactToAccount(contact, target)
                if (success) successCount++

                // Update notification every 5 items
                if (index % 5 == 0) {
                    showNotification(index + 1, contacts.size)
                }
                delay(10) // Small delay to prevent UI freeze and allow cancellation
            }

            showFinalNotification(successCount)

            withContext(Dispatchers.Main) {
                goToStep(SUCCESS_STEP)
            }
        }
    }

    private fun importContactToAccount(contact: ContactEnhanced, account: ImportRepository.ImportAccount): Boolean {
        try {
            val ops = ArrayList<ContentProviderOperation>()

            // 1. RawContact
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, if (account.type == "Local") null else account.name)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, if (account.type == "Local") null else account.type)
                .build())

            // 2. Name
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                .build())

            // 3. Phone
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build())

            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun showNotification(current: Int, total: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "import_progress"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Contact Import", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Importing contacts")
            .setContentText("Importing $current of $total")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(total, current, false)
            .setOngoing(true)
            .build()

        manager.notify(1, notification)
    }

    private fun showFinalNotification(count: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "import_progress")
            .setContentTitle("Import finished")
            .setContentText("$count contacts imported to ${targetAccount?.name}")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()

        manager.cancel(1)
        manager.notify(2, notification)
    }

    // --- Adapters ---

    inner class AccountAdapter(
        private val list: List<ImportRepository.ImportAccount>,
        private val showRadioButton: Boolean = false,
        private val onClick: (ImportRepository.ImportAccount) -> Unit
    ) : RecyclerView.Adapter<AccountAdapter.VH>() {

        private var selectedPos = -1

        inner class VH(val b: ItemImportAccountBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(ItemImportAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.b.tvAccountName.text = item.name

            val icon = when {
                item.isSim -> R.drawable.ic_sim
                item.name == "Phone" -> R.drawable.ic_phone_storage
                item.type.contains("google", true) -> R.drawable.ic_google
                else -> R.drawable.ic_launcher_foreground
            }
            holder.b.ivAccountIcon.setImageResource(icon)

            if (showRadioButton) {
                holder.b.rbSelected.visibility = View.VISIBLE
                holder.b.rbSelected.isChecked = position == selectedPos
            }

            holder.b.root.setOnClickListener {
                if (showRadioButton) {
                    selectedPos = holder.adapterPosition
                    notifyDataSetChanged()
                }
                onClick(item)
            }
        }

        override fun getItemCount() = list.size
    }

    inner class ContactSelectionAdapter(
        private val list: List<ContactEnhanced>,
        private val onCheckChange: (ContactEnhanced, Boolean) -> Unit
    ) : RecyclerView.Adapter<ContactSelectionAdapter.VH>() {

        private val checkedList = BooleanArray(list.size)

        inner class VH(val b: ItemImportContactBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(ItemImportContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.b.tvName.text = item.name
            holder.b.tvPhone.text = item.phoneNumber
            holder.b.tvAvatar.text = item.name.firstOrNull()?.uppercase() ?: "?"

            holder.b.checkbox.isChecked = checkedList[position]
            holder.b.ivCheck.visibility = if (checkedList[position]) View.VISIBLE else View.GONE

            holder.b.root.setOnClickListener {
                checkedList[position] = !checkedList[position]
                notifyItemChanged(position)
                onCheckChange(item, checkedList[position])
            }
        }

        fun selectAll(checked: Boolean) {
            checkedList.fill(checked)
            notifyDataSetChanged()
            selectedContacts.clear()
            if (checked) selectedContacts.addAll(list)
            binding.tvSelectionCount.text = "${selectedContacts.size} selected"
        }

        override fun getItemCount() = list.size
    }
}

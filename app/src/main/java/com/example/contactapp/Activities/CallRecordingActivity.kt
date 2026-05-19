package com.example.contactapp.Activities

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.EdgeToEdgeActivity
import com.example.contactapp.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RecordingFile(
    val file: File,
    val displayName: String,
    val duration: Long,   // milliseconds
    val size: Long,       // bytes
    val dateCreated: Long // epoch millis
)

class CallRecordingActivity : EdgeToEdgeActivity() {

    private lateinit var rvRecordings: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvPhoneTitle: TextView
    private lateinit var adapter: RecordingAdapter
    private var phoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_recording)

        phoneNumber = intent.getStringExtra("PHONE_NUMBER")

        initViews()
        loadRecordings()
    }

    private fun initViews() {
        tvPhoneTitle = findViewById(R.id.tvPhoneTitle)
        rvRecordings = findViewById(R.id.rvRecordings)
        tvEmpty = findViewById(R.id.tvEmpty)

        val safeNumber = phoneNumber?.replace(Regex("[^0-9+]"), "") ?: ""
        tvPhoneTitle.text = if (safeNumber.isNotEmpty()) "Recordings: $safeNumber" else "All Recordings"

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        adapter = RecordingAdapter(
            onPlay = { recording -> playRecording(recording) },
            onDelete = { recording -> confirmDelete(recording) },
            onShare = { recording -> shareRecording(recording) }
        )
        rvRecordings.layoutManager = LinearLayoutManager(this)
        rvRecordings.adapter = adapter
    }

    private fun loadRecordings() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        val safeNumber = phoneNumber?.replace(Regex("[^0-9+]"), "") ?: ""

        val files = dir.listFiles { file ->
            file.name.startsWith("REC_") &&
                    file.name.endsWith(".m4a") &&
                    (safeNumber.isEmpty() || file.name.contains(safeNumber))
        } ?: emptyArray()

        val recordings = files.map { file ->
            val duration = getAudioDuration(file)
            RecordingFile(
                file = file,
                displayName = formatFileName(file.name),
                duration = duration,
                size = file.length(),
                dateCreated = file.lastModified()
            )
        }.sortedByDescending { it.dateCreated }

        if (recordings.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvRecordings.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvRecordings.visibility = View.VISIBLE
            adapter.submitList(recordings)
        }
    }

    private fun getAudioDuration(file: File): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(file.absolutePath)
            val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            mmr.release()
            duration
        } catch (e: Exception) {
            0L
        }
    }

    private fun formatFileName(name: String): String {
        // REC_+918780617760_20260301_153000.m4a → +918780617760 - Mar 01, 2026 15:30
        return try {
            val parts = name.removePrefix("REC_").removeSuffix(".m4a").split("_")
            val number = parts.getOrNull(0) ?: "Unknown"
            val date = parts.getOrNull(1) ?: ""
            val time = parts.getOrNull(2) ?: ""
            val parsed = if (date.length == 8 && time.length == 6) {
                val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                sdf.format(sdf.parse("$date$time") ?: Date())
            } else ""
            "$number - $parsed"
        } catch (e: Exception) {
            name.removeSuffix(".m4a")
        }
    }

    private fun playRecording(recording: RecordingFile) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Playing Recording")
            .setMessage("${recording.displayName}\n${formatDuration(recording.duration)}")
            .setPositiveButton("Close") { d, _ ->
                d.dismiss()
            }
            .create()

        var mediaPlayer: MediaPlayer? = null
        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(recording.file.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                dialog.dismiss()
                it.release()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot play: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }

        dialog.setOnDismissListener { mediaPlayer?.release() }
        dialog.show()
    }

    private fun shareRecording(recording: RecordingFile) {
        val uri = Uri.fromFile(recording.file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Recording"))
    }

    private fun confirmDelete(recording: RecordingFile) {
        AlertDialog.Builder(this)
            .setTitle("Delete Recording")
            .setMessage("Delete \"${recording.displayName}\"?")
            .setPositiveButton("Delete") { _, _ ->
                if (recording.file.delete()) {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    loadRecordings()
                } else {
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatDuration(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) - minutes * 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000f)
            bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000f)
            else -> "$bytes B"
        }
    }

    // ─── Adapter ────────────────────────────────────────────────────────────────

    inner class RecordingAdapter(
        private val onPlay: (RecordingFile) -> Unit,
        private val onDelete: (RecordingFile) -> Unit,
        private val onShare: (RecordingFile) -> Unit
    ) : RecyclerView.Adapter<RecordingAdapter.VH>() {

        private val items = mutableListOf<RecordingFile>()

        fun submitList(list: List<RecordingFile>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recording, parent, false)
            return VH(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val tvName: TextView = view.findViewById(R.id.tvRecordingName)
            private val tvMeta: TextView = view.findViewById(R.id.tvRecordingMeta)
            private val btnPlay: ImageButton = view.findViewById(R.id.btnPlayRecording)
            private val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteRecording)
            private val btnShare: ImageButton = view.findViewById(R.id.btnShareRecording)

            fun bind(r: RecordingFile) {
                tvName.text = r.displayName
                val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(Date(r.dateCreated))
                tvMeta.text = "$date · ${formatDuration(r.duration)} · ${formatSize(r.size)}"
                btnPlay.setOnClickListener { onPlay(r) }
                btnDelete.setOnClickListener { onDelete(r) }
                btnShare.setOnClickListener { onShare(r) }
            }
        }
    }
}

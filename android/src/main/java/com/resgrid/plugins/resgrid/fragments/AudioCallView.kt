package com.resgrid.plugins.resgrid.activites

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.resgrid.plugins.resgrid.ParticipantItem
import com.resgrid.plugins.resgrid.R
import com.resgrid.plugins.resgrid.databinding.AudioCallActivityBinding
import com.resgrid.plugins.resgrid.dialog.showDebugMenuDialogFrag
import com.resgrid.plugins.resgrid.dialog.showSelectAudioDeviceDialogFrag
import com.resgrid.plugins.resgrid.models.ConfigData
import com.resgrid.plugins.resgrid.vms.CallViewModel
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.parcelize.Parcelize


class AudioCallFragment : Fragment(R.layout.audio_call_activity)
{
    //val viewModel: CallViewModel by viewModelByFactory {
    //    val arg = intent.getStringExtra(KEY_ARGS)
    //        ?: throw NullPointerException("args is null!")
    //    val gson = Gson()
//
    //    CallViewModel(gson.fromJson(arg, ConfigData::class.java), application)
    //}

    private lateinit var viewModel: CallViewModel// by activityViewModels()


    lateinit var binding: AudioCallActivityBinding
    private val screenCaptureIntentLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val resultCode = result.resultCode
            val data = result.data
            if (resultCode != Activity.RESULT_OK || data == null) {
                return@registerForActivityResult
            }
            viewModel.startScreenCapture(data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = AudioCallActivityBinding.inflate(layoutInflater)

        if (getArguments() != null) {
            val gson = Gson()

            viewModel = ViewModelProvider(this)[CallViewModel::class.java]
            viewModel.configData = gson.fromJson(requireArguments().getString(KEY_ARGS), ConfigData::class.java)
        }

        //setContentView(binding.root)

        // Audience row setup
        val audienceAdapter = GroupieAdapter()
        binding.audienceRow.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = audienceAdapter
        }

        lifecycleScope.launchWhenCreated {
            viewModel.participants
                .collect { participants ->
                    val items = participants.map { participant -> ParticipantItem(viewModel.room, participant) }
                    audienceAdapter.update(items)
                }
        }

        // speaker view setup
        val speakerAdapter = GroupieAdapter()
        binding.speakerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = speakerAdapter
        }
        lifecycleScope.launchWhenCreated {
            viewModel.primarySpeaker.collectLatest { speaker ->
                val items = listOfNotNull(speaker)
                    .map { participant -> ParticipantItem(viewModel.room, participant, speakerView = true) }
                speakerAdapter.update(items)
            }
        }

        // Controls setup
        viewModel.cameraEnabled.observe(this) { enabled ->
            binding.camera.setOnClickListener { viewModel.setCameraEnabled(!enabled) }
            binding.camera.setImageResource(
                if (enabled) R.drawable.outline_videocam_24
                else R.drawable.outline_videocam_off_24
            )
            binding.flipCamera.isEnabled = enabled
        }
        viewModel.micEnabled.observe(this) { enabled ->
            binding.mic.setOnClickListener { viewModel.setMicEnabled(!enabled) }
            binding.mic.setImageResource(
                if (enabled) R.drawable.outline_mic_24
                else R.drawable.outline_mic_off_24
            )
        }

        binding.flipCamera.setOnClickListener { viewModel.flipCamera() }
        viewModel.screenshareEnabled.observe(this) { enabled ->
            binding.screenShare.setOnClickListener {
                if (enabled) {
                    viewModel.stopScreenCapture()
                } else {
                    requestMediaProjection()
                }
            }
            binding.screenShare.setImageResource(
                if (enabled) R.drawable.baseline_cast_connected_24
                else R.drawable.baseline_cast_24
            )
        }

        binding.message.setOnClickListener {
            val editText = EditText(requireContext())
            AlertDialog.Builder(requireContext())
                .setTitle("Send Message")
                .setView(editText)
                .setPositiveButton("Send") { dialog, _ ->
                    viewModel.sendData(editText.text?.toString() ?: "")
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .create()
                .show()
        }

        //binding.exit.setOnClickListener { finish() }

        // Controls row 2
        binding.audioSelect.setOnClickListener {
            showSelectAudioDeviceDialogFrag(viewModel)
        }
        lifecycleScope.launchWhenCreated {
            viewModel.permissionAllowed.collect { allowed ->
                val resource = if (allowed) R.drawable.account_cancel_outline else R.drawable.account_cancel
                binding.permissions.setImageResource(resource)
            }
        }
        binding.permissions.setOnClickListener {
            viewModel.toggleSubscriptionPermissions()
        }

        binding.debugMenu.setOnClickListener {
            showDebugMenuDialogFrag(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenResumed {
            viewModel.error.collect {
                if (it != null) {
                    Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
                    viewModel.dismissError()
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.dataReceived.collect {
                Toast.makeText(context, "Data received: $it", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestMediaProjection() {
        //val mediaProjectionManager =
       //     getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        //screenCaptureIntentLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    override fun onDestroy() {
        binding.audienceRow.adapter = null
        binding.speakerView.adapter = null
        super.onDestroy()
    }

    companion object {
        const val KEY_ARGS = "args"
    }

    @Parcelize
    data class BundleArgs(val url: String, val token: String) : Parcelable
}
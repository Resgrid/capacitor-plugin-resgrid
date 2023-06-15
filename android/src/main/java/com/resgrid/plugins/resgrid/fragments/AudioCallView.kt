package com.resgrid.plugins.resgrid.activites

import android.R.attr.button
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Parcelable
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.resgrid.plugins.resgrid.ParticipantItem
import com.resgrid.plugins.resgrid.R
import com.resgrid.plugins.resgrid.api.VoiceApiService
import com.resgrid.plugins.resgrid.databinding.AudioCallActivityBinding
import com.resgrid.plugins.resgrid.dialog.showBluetoothDeviceDialogFrag
import com.resgrid.plugins.resgrid.dialog.showDebugMenuDialog
import com.resgrid.plugins.resgrid.dialog.showSelectAudioDeviceDialogFrag
import com.resgrid.plugins.resgrid.models.ConfigData
import com.resgrid.plugins.resgrid.models.RoomData
import com.resgrid.plugins.resgrid.models.SpinAdapter
import com.resgrid.plugins.resgrid.vms.CallViewModel
import com.xwray.groupie.GroupieAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.parcelize.Parcelize


class AudioCallFragment: Fragment(R.layout.audio_call_activity)
{
    //val viewModel: CallViewModel by viewModelByFactory {
    //    val arg = intent.getStringExtra(KEY_ARGS)
    //        ?: throw NullPointerException("args is null!")
    //    val gson = Gson()
//
    //    CallViewModel(gson.fromJson(arg, ConfigData::class.java), application)
    //}

    private lateinit var viewModel: CallViewModel// by activityViewModels()
    private val defaultRoom: RoomData = RoomData("0", "Select Channel to Join", null)
    private val roomsList: MutableList<RoomData> = ArrayList()
    private val audienceAdapter: GroupieAdapter = GroupieAdapter()
    private val speakerAdapter: GroupieAdapter = GroupieAdapter()
    private var startTransmittingSound: MediaPlayer? = null
    private var stopTransmittingSound: MediaPlayer? = null
    private var voiceApi: VoiceApiService? = null

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

        //val audienceAdapter = GroupieAdapter()
        //val speakerAdapter = GroupieAdapter()

        //window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = AudioCallActivityBinding.inflate(layoutInflater)

        if (getArguments() != null) {
            val gson = Gson()

            viewModel = ViewModelProvider(this)[CallViewModel::class.java]
            viewModel.configData = gson.fromJson(requireArguments().getString(KEY_ARGS), ConfigData::class.java)
        }

        if (requireActivity().packageManager != null) {
            if (requireActivity().packageManager!!.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                viewModel.bluetoothAdapter = (requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            }
        }
        //setRoomsDropdown()
        //setContentView(binding.root)

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

        startTransmittingSound = MediaPlayer.create(context, R.raw.start_transmit)
        stopTransmittingSound = MediaPlayer.create(context, R.raw.stop_transmit)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRoomsDropdown()

        val connectButton = view.findViewById(R.id.connect_button) as Button
        connectButton.setOnClickListener {
            if (viewModel.selectedRoom.value != defaultRoom) {
                lifecycleScope.launchWhenCreated {
                    if (canConnectToVoice()) {
                        connect(view)

                        val selectionContainer =
                            view.findViewById(R.id.selection_container) as RelativeLayout
                        val roomContainer = view.findViewById(R.id.room_container) as LinearLayout

                        selectionContainer.isVisible = false;
                        roomContainer.isVisible = true;
                    } else {
                        showNoAvailableSeatsDialog(view.context, context!!)
                    }
                }
            }
        }

        val pttButton = view.findViewById(R.id.ptt_button) as Button
        //pttButton.setOnLongClickListener{
        //    startTransmitting(pttButton)
        //    true
        //}

        pttButton.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                startTransmitting(pttButton)
            }
            if (event.action == MotionEvent.ACTION_UP) {
                stopTransmitting(pttButton)
            }
            true
        }

        val permissionsButton = view.findViewById(R.id.permissions) as ImageView
        permissionsButton.setOnClickListener {
            viewModel.toggleSubscriptionPermissions()
        }
        lifecycleScope.launchWhenCreated {
            viewModel.permissionAllowed.collect { allowed ->
                val resource = if (allowed) R.drawable.account_cancel_outline else R.drawable.account_cancel
                permissionsButton.setImageResource(resource)
            }
        }

        val audioSelectButton = view.findViewById(R.id.audio_select) as ImageView
        audioSelectButton.setOnClickListener {
            showSelectAudioDeviceDialogFrag(viewModel)
        }

        val micButton = view.findViewById(R.id.mic) as ImageView
        micButton.setOnClickListener { viewModel.setMicEnabled(!viewModel.micEnabled.value!!) }
        viewModel.micEnabled.observe(this) { enabled ->
            micButton.setImageResource(
                    if (enabled) R.drawable.outline_mic_24
                    else R.drawable.outline_mic_off_24
            )
        }

        val exitButton = view.findViewById(R.id.exit) as ImageView
        exitButton.setOnClickListener { disconnect(view) }

        val bluetoothButton = view.findViewById(R.id.bluetooth_menu) as ImageView
        bluetoothButton.setOnClickListener {
            showBluetoothDeviceDialogFrag(viewModel)
        }
    }

    private fun startTransmitting(pttButton: Button) {
        startTransmittingSound?.start()
        startTransmittingSound?.seekTo(0)

        viewModel.setMicEnabled(true)
        pttButton.setBackgroundColor(Color.RED);
        pttButton.text = "Transmitting"
    }

    private fun stopTransmitting(pttButton: Button) {
        stopTransmittingSound?.start()
        stopTransmittingSound?.seekTo(0)

        viewModel.setMicEnabled(false)
        pttButton.text = "Push and Hold To Talk"
        pttButton.setBackgroundColor(Color.parseColor("#ff0099cc"))//17170453)
    }

    private fun setRoomsDropdown() {
        val spinner = view?.findViewById<Spinner>(R.id.rooms_spinner)

        if (spinner != null) {
            roomsList.clear()
            roomsList.add(defaultRoom)
            viewModel.configData?.rooms?.let { roomsList.addAll(it) }

            val adapter = context?.let {
                        SpinAdapter(it,
                                //R.id.rooms_spinner,
                                R.layout.item_room,//android.R.layout.simple_spinner_item,
                                roomsList.toTypedArray())
            }
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    viewModel.selectedRoom.value = defaultRoom
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.selectedRoom.value = roomsList[position]
                }
            }
        }
    }

    private fun connect(view: View) {
        //binding.debugMenu.setOnClickListener {
        //    showDebugMenuDialogFrag(viewModel)
        //}
                val roomName = view.findViewById(R.id.text_active_room) as TextView

                roomName.setText(
                    "Connected to " + (viewModel.selectedRoom.value?.name ?: "Connected")
                );

                lifecycleScope.launchWhenCreated {
                    viewModel.connectToRoom()
                    viewModel.setMicEnabled(false)
                    viewModel.setCameraEnabled(false)
                }
    }

    private suspend fun canConnectToVoice(): Boolean {
        run {
            if (voiceApi == null)
                voiceApi = VoiceApiService(viewModel.configData!!);

            val response = voiceApi?.getCanConnectToVoiceSession()

            if (response != null && response.Data != null && response.Data.CanConnect)
                return true;

            return false
        }
    }

    private fun disconnect(view: View) {
        val selectionContainer = view.findViewById(R.id.selection_container) as RelativeLayout
        val roomContainer = view.findViewById(R.id.room_container) as LinearLayout

        selectionContainer.isVisible = true;
        roomContainer.isVisible = false;

        lifecycleScope.launchWhenCreated {
            viewModel.setMicEnabled(false)
            viewModel.setCameraEnabled(false)
            viewModel.disconnect()
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

    private fun showNoAvailableSeatsDialog(context: Context, applicationContext: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Unable to Connect")
        builder.setMessage("There are no available seats to connect to a voice session. Please try again later.")

        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton("OK") { dialog, which ->
            //Toast.makeText(applicationContext,
            //    android.R.string.yes, Toast.LENGTH_SHORT).show()
        }

        builder.show()
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
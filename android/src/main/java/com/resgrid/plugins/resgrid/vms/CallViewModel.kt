package com.resgrid.plugins.resgrid.vms

import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.ParcelUuid
import android.preference.PreferenceManager
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.ajalt.timberkt.Timber
import com.resgrid.plugins.resgrid.R
import com.resgrid.plugins.resgrid.api.VoiceApi
import com.resgrid.plugins.resgrid.api.VoiceApiService
import com.resgrid.plugins.resgrid.bluetooth.Device
import com.resgrid.plugins.resgrid.bluetooth.DeviceScanner
import com.resgrid.plugins.resgrid.bluetooth.DisplayStrings
import com.resgrid.plugins.resgrid.data.*
import com.resgrid.plugins.resgrid.models.ConfigData
import com.resgrid.plugins.resgrid.models.RoomData
import com.resgrid.plugins.resgrid.service.ForegroundService
import io.livekit.android.LiveKit
import io.livekit.android.LiveKitOverrides
import io.livekit.android.RoomOptions
import io.livekit.android.audio.AudioSwitchHandler
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalScreencastVideoTrack
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.util.flow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*


class CallViewModel(
        application: Application,
        //private val preferences: PreferenceDataStoreHelper = PreferenceDataStoreHelper(application),
        //private val dataStore: DataStoreRepository = DataStoreRepository(application)
) : AndroidViewModel(application) {

    var bluetoothAdapter: BluetoothAdapter? = null
    private var stateReceiver: BroadcastReceiver? = null
    private var deviceScanner: DeviceScanner? = null
    private var displayStrings: DisplayStrings? = null
    private var newDevice: Device? = null
    private var startTransmittingSound: MediaPlayer? = null
    private var stopTransmittingSound: MediaPlayer? = null

    var configData: ConfigData? = null
    val selectedRoom = MutableLiveData<RoomData>(null)
    val audioHandler = AudioSwitchHandler(application)
    val room = LiveKit.create(
        appContext = application,
        options = RoomOptions(adaptiveStream = true, dynacast = true),
        overrides = LiveKitOverrides(
            audioHandler = audioHandler
        )
    )

    val participants = room::remoteParticipants.flow
        .map { remoteParticipants ->
            listOf<Participant>(room.localParticipant) +
                    remoteParticipants
                        .keys
                        .sortedBy { it }
                        .mapNotNull { remoteParticipants[it] }
        }

    private val mutableError = MutableStateFlow<Throwable?>(null)
    val error = mutableError.hide()

    private val mutablePrimarySpeaker = MutableStateFlow<Participant?>(null)
    val primarySpeaker: StateFlow<Participant?> = mutablePrimarySpeaker

    val activeSpeakers = room::activeSpeakers.flow

    private var localScreencastTrack: LocalScreencastVideoTrack? = null

    // Controls
    private val mutableMicEnabled = MutableLiveData(true)
    val micEnabled = mutableMicEnabled.hide()

    private val mutableCameraEnabled = MutableLiveData(true)
    val cameraEnabled = mutableCameraEnabled.hide()

    private val mutableFlipVideoButtonEnabled = MutableLiveData(true)
    val flipButtonVideoEnabled = mutableFlipVideoButtonEnabled.hide()

    private val mutableScreencastEnabled = MutableLiveData(false)
    val screenshareEnabled = mutableScreencastEnabled.hide()

    // Emits a string whenever a data message is received.
    private val mutableDataReceived = MutableSharedFlow<String>()
    val dataReceived = mutableDataReceived

    // Whether other participants are allowed to subscribe to this participant's tracks.
    private val mutablePermissionAllowed = MutableStateFlow(true)
    val permissionAllowed = mutablePermissionAllowed.hide()

    val preferences = Preferences(application.applicationContext, PreferencesConfiguration.DEFAULTS!!);

    init {
        startTransmittingSound = MediaPlayer.create(getApplication(), R.raw.start_transmit)
        stopTransmittingSound = MediaPlayer.create(getApplication(), R.raw.stop_transmit)

        viewModelScope.launch {
            // Collect any errors.
            launch {
                error.collect { Timber.e(it) }
            }

            // Handle any changes in speakers.
            launch {
                combine(participants, activeSpeakers) { participants, speakers -> participants to speakers }
                    .collect { (participantsList, speakers) ->
                        handlePrimarySpeaker(
                            participantsList,
                            speakers,
                            room
                        )
                    }
            }

            launch {
                // Handle room events.
                room.events.collect {
                    when (it) {
                        is RoomEvent.FailedToConnect -> mutableError.value = it.error
                        is RoomEvent.DataReceived -> {
                            val identity = it.participant?.identity ?: "server"
                            val message = it.data.toString(Charsets.UTF_8)
                            mutableDataReceived.emit("$identity: $message")
                        }
                        else -> {
                            Timber.e { "Room event: $it" }
                        }
                    }
                }
            }

            //configData?.url?.let { configData!!.rooms[0].token?.let { it1 -> connectToRoom(it, it1) } }
        }

        // Start a foreground service to keep the call from being interrupted if the
        // app goes into the background.
        val foregroundServiceIntent = Intent(application, ForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(foregroundServiceIntent)
        } else {
            application.startService(foregroundServiceIntent)
        }

        //voiceApi = VoiceApiService(configData!!);
    }

    suspend fun connectToRoom() {
        try {
                configData?.url?.let {
                    selectedRoom.value?.token?.let { it1 ->
                        room.connect(
                            url = it,
                            token = it1,
                        )
                    }
                }

                // Create and publish audio/video tracks
                val localParticipant = room.localParticipant
                localParticipant.setMicrophoneEnabled(true)
                mutableMicEnabled.postValue(localParticipant.isMicrophoneEnabled())

                localParticipant.setCameraEnabled(true)
                mutableCameraEnabled.postValue(localParticipant.isCameraEnabled())

                // Update the speaker
                handlePrimarySpeaker(emptyList(), emptyList(), room)
        } catch (e: Throwable) {
            mutableError.value = e
        }
    }

    private fun handlePrimarySpeaker(participantsList: List<Participant>, speakers: List<Participant>, room: Room?) {

        var speaker = mutablePrimarySpeaker.value

        // If speaker is local participant (due to defaults),
        // attempt to find another remote speaker to replace with.
        if (speaker is LocalParticipant) {
            val remoteSpeaker = participantsList
                .filterIsInstance<RemoteParticipant>() // Try not to display local participant as speaker.
                .firstOrNull()

            if (remoteSpeaker != null) {
                speaker = remoteSpeaker
            }
        }

        // If previous primary speaker leaves
        if (!participantsList.contains(speaker)) {
            // Default to another person in room, or local participant.
            speaker = participantsList.filterIsInstance<RemoteParticipant>()
                .firstOrNull()
                ?: room?.localParticipant
        }

        if (speakers.isNotEmpty() && !speakers.contains(speaker)) {
            val remoteSpeaker = speakers
                .filterIsInstance<RemoteParticipant>() // Try not to display local participant as speaker.
                .firstOrNull()

            if (remoteSpeaker != null) {
                speaker = remoteSpeaker
            }
        }

        mutablePrimarySpeaker.value = speaker
    }

    /**
     * Start a screen capture with the result intent from
     * [MediaProjectionManager.createScreenCaptureIntent]
     */
    fun startScreenCapture(mediaProjectionPermissionResultData: Intent) {
        val localParticipant = room.localParticipant
        viewModelScope.launch {
            val screencastTrack =
                localParticipant.createScreencastTrack(mediaProjectionPermissionResultData = mediaProjectionPermissionResultData)
            localParticipant.publishVideoTrack(
                screencastTrack
            )

            // Must start the foreground prior to startCapture.
            screencastTrack.startForegroundService(null, null)
            screencastTrack.startCapture()

            this@CallViewModel.localScreencastTrack = screencastTrack
            mutableScreencastEnabled.postValue(screencastTrack.enabled)
        }
    }

    fun stopScreenCapture() {
        viewModelScope.launch {
            localScreencastTrack?.let { localScreencastVideoTrack ->
                localScreencastVideoTrack.stop()
                room.localParticipant.unpublishTrack(localScreencastVideoTrack)
                mutableScreencastEnabled.postValue(localScreencastTrack?.enabled ?: false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        room.disconnect()
        room.release()

        // Clean up foreground service
        val application = getApplication<Application>()
        val foregroundServiceIntent = Intent(application, ForegroundService::class.java)
        application.stopService(foregroundServiceIntent)
    }

    fun setMicEnabled(enabled: Boolean) {
        viewModelScope.launch {
            room.localParticipant.setMicrophoneEnabled(enabled)
            mutableMicEnabled.postValue(enabled)
        }
    }

    fun setCameraEnabled(enabled: Boolean) {
        viewModelScope.launch {
            room.localParticipant.setCameraEnabled(enabled)
            mutableCameraEnabled.postValue(enabled)
        }
    }

    fun flipCamera() {
        val videoTrack = room.localParticipant.getTrackPublication(Track.Source.CAMERA)
            ?.track as? LocalVideoTrack
            ?: return

        val newPosition = when (videoTrack.options.position) {
            CameraPosition.FRONT -> CameraPosition.BACK
            CameraPosition.BACK -> CameraPosition.FRONT
            else -> null
        }

        videoTrack.switchCamera(position = newPosition)
    }

    fun dismissError() {
        mutableError.value = null
    }

    fun sendData(message: String) {
        viewModelScope.launch {
            room.localParticipant.publishData(message.toByteArray(Charsets.UTF_8))
        }
    }

    fun toggleSubscriptionPermissions() {
        mutablePermissionAllowed.value = !mutablePermissionAllowed.value
        room.localParticipant.setTrackSubscriptionPermissions(mutablePermissionAllowed.value)
    }

    fun connectToHeadset(context: android.content.Context) {
        setDisplayStrings()

        if (bluetoothAdapter != null) {
            val filters: ArrayList<ScanFilter> = ArrayList()
            val filter = ScanFilter.Builder()
            filter.setServiceUuid(ParcelUuid.fromString("127FACE1-CB21-11E5-93D0-0002A5D5C51B"))
            filters.add(filter.build())

            val scanSettings = ScanSettings.Builder()
            try {
                scanSettings.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            } catch (e: IllegalArgumentException) {
            }

            //viewModelScope.launch {
                //val prefs = dataStore.getData()
                val headsetDeviceAddress = preferences.get("HEADSET_DEVICE_ADDRESS")

                if (headsetDeviceAddress.isNullOrBlank()) {
                    deviceScanner = DeviceScanner(
                        context,
                        bluetoothAdapter!!,
                        scanDuration = 30000,
                        displayStrings = displayStrings!!,
                        showDialog = true,
                    )
                    deviceScanner?.startScanning(
                        filters, scanSettings.build(), false, "", { scanResponse ->
                            run {
                                if (scanResponse.success) {
                                    if (scanResponse.device != null) {
                                        //viewModelScope.launch {
                                        //    dataStore.saveToDataStore(scanResponse.device.address)
                                        //}
                                        preferences.set("HEADSET_DEVICE_ADDRESS", scanResponse.device.address)
                                        connectAndMonitorHeadset(context, scanResponse.device.address)
                                    }
                                }
                            }
                        }, null
                    )
                } else {
                    connectAndMonitorHeadset(context, headsetDeviceAddress)
                }
            //}
        }
    }

    private fun connectAndMonitorHeadset(context: Context, deviceAddress: String) {

        if (newDevice != null && newDevice!!.isConnected())
        {
            newDevice!!.disconnect(10000) { response ->
                run {
                    if (response.success) {
                        newDevice = null
                    }
                }
            }
        }

        newDevice = Device(
            context, bluetoothAdapter!!, deviceAddress//scanResponse.device.address
        ) {
            onDisconnect(deviceAddress)
        }

        newDevice!!.connect(10000) { response ->
            run {
                if (response.success) {
                    newDevice!!.setNotifications(UUID.fromString("127FACE1-CB21-11E5-93D0-0002A5D5C51B"), UUID.fromString("127FBEEF-CB21-11E5-93D0-0002A5D5C51B"), true, { response ->
                        run {
                            if (response != null) {
                                if (response.value.trim() == "00") {
                                    stopTransmittingSound?.start()
                                    stopTransmittingSound?.seekTo(0)

                                    setMicEnabled(false)
                                } else if (response.value.trim() == "01") {
                                    startTransmittingSound?.start()
                                    startTransmittingSound?.seekTo(0)

                                    setMicEnabled(true)
                                }
                            }
                        }
                    }, { response ->

                    })
                }
            }
        }
    }

    private fun setDisplayStrings() {
        displayStrings = DisplayStrings(
            "Scanning...",
            "Cancel",
            "Available devices",
            "No device found"
        )
    }

    fun disconnectHeadset() {
        if (newDevice != null){
            newDevice!!.disconnect(10000){ response ->
                run {
                    newDevice = null
                    preferences.clear()
                }
            }
        }
    }

    private fun onDisconnect(deviceId: String) {

    }

    // Debug functions
    fun simulateMigration() {
        room.sendSimulateScenario(Room.SimulateScenario.MIGRATION)
    }

    fun simulateNodeFailure() {
        room.sendSimulateScenario(Room.SimulateScenario.NODE_FAILURE)
    }

    fun reconnect() {
        Timber.e { "Reconnecting." }
        mutablePrimarySpeaker.value = null
        room.disconnect()
        viewModelScope.launch {
            //configData?.url?.let { configData!!.rooms[0].token?.let { it1 -> connectToRoom(it, it1) } }
            connectToRoom()
        }
    }

    fun disconnect() {
        mutablePrimarySpeaker.value = null
        room.disconnect()
    }
}

object PreferenceHelper {

    val HEADSET_DEVICE_ADDRESS = "HEADSET_DEVICE_ADDRESS"

    fun defaultPreference(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun customPreference(context: Context, name: String): SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    inline fun SharedPreferences.editMe(operation: (SharedPreferences.Editor) -> Unit) {
        val editMe = edit()
        operation(editMe)
        editMe.apply()
    }

    var SharedPreferences.headsetDeviceAddress
        get() = getString(HEADSET_DEVICE_ADDRESS, "")
        set(value) {
            editMe {
                it.putString(HEADSET_DEVICE_ADDRESS, value)
            }
        }

    var SharedPreferences.clearValues
        get() = { }
        set(value) {
            editMe {
                it.clear()
            }
        }
}

private fun <T> LiveData<T>.hide(): LiveData<T> = this
private fun <T> MutableStateFlow<T>.hide(): StateFlow<T> = this
private fun <T> Flow<T>.hide(): Flow<T> = this
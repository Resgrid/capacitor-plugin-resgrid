package com.resgrid.plugins.resgrid

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.getcapacitor.*
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import com.google.gson.Gson
import com.resgrid.plugins.resgrid.activites.AudioCallFragment
import com.resgrid.plugins.resgrid.activites.BottomAudioView
import com.resgrid.plugins.resgrid.activites.CallActivity
import com.resgrid.plugins.resgrid.models.ConfigData
import com.resgrid.plugins.resgrid.models.RoomData
import io.reactivex.disposables.CompositeDisposable


@CapacitorPlugin(name = "Resgrid",
        permissions = [
            Permission(
                strings = [
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ], alias = "ACCESS_COARSE_LOCATION"
            ),
            Permission(
                strings = [
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ], alias = "ACCESS_FINE_LOCATION"
            ),
            Permission(
                    strings = [
                        Manifest.permission.RECORD_AUDIO,
                    ], alias = "MICROPHONE"
            ),
            Permission(
                    strings = [
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        Manifest.permission.BLUETOOTH_ADMIN,
                    ], alias = "BLUETOOTH"
            ),
            Permission(
                strings = [
                    // Manifest.permission.BLUETOOTH_SCAN
                    "android.permission.BLUETOOTH_SCAN",
                ], alias = "BLUETOOTH_SCAN"
            ),
            Permission(
                strings = [
                    // Manifest.permission.BLUETOOTH_ADMIN
                    "android.permission.BLUETOOTH_CONNECT",
                ], alias = "BLUETOOTH_CONNECT"
            ),
        ])
class ResgridPlugin : Plugin() {
    companion object {
        private val TAG = ResgridPlugin::class.java.simpleName
    }

    private var aliases: Array<String> = arrayOf()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val disposables = CompositeDisposable()
    private var configData = ConfigData()
    private var audioCallFragment: AudioCallFragment? = null
    private var bottomAudioView: BottomAudioView? = null

    @PluginMethod
    public fun start(call: PluginCall) {

        val context: Context = activity.applicationContext;
        val activity: Activity = activity;

        if (activity.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            bluetoothAdapter = (activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        }

        var urlValue = call.getString("url")
        var typeValue = call.getInt("type")
        var defaultMicValue = call.getString("defaultMic")
        var defaultSpeakerValue = call.getString("defaultSpeaker")
        var apiUrlValue = call.getString("apiUrl")
        var canConnectToVoiceApiTokenValue = call.getString("canConnectToVoiceApiToken")

        var roomsJSValue = call.getArray("rooms")
        var rooms: ArrayList<RoomData> = ArrayList()

        if (roomsJSValue != null && roomsJSValue.length() > 0) {
            for (n in 0 until roomsJSValue.length()) {
                val roomObject = roomsJSValue.optJSONObject(n)
                if (roomObject != null) {
                    var room = RoomData()
                    room.id = roomObject.optString("id")
                    room.name = roomObject.optString("name")
                    room.token = roomObject.optString("token")

                    rooms.add(room)
                }
            }
        }

        configData.url = urlValue
        if (typeValue != null) {
            configData.type = typeValue
        }
        configData.defaultMic = defaultMicValue
        configData.defaultSpeaker = defaultSpeakerValue
        configData.apiUrl = apiUrlValue
        configData.canConnectToVoiceApiToken = canConnectToVoiceApiTokenValue
        configData.rooms = rooms
        //configData.bluetoothAdapter = bluetoothAdapter

        Log.i(TAG, "ResgridPlugin: Start type: $typeValue")

        if (Build.VERSION.SDK_INT >= 31) {
            val neverForLocation = call.getBoolean("androidNeverForLocation", false) as Boolean
            aliases = if (neverForLocation) {
                arrayOf(
                    "BLUETOOTH_SCAN",
                    "BLUETOOTH_CONNECT",
                    "BLUETOOTH_SCAN",
                    "MICROPHONE",
                )
            } else {
                arrayOf(
                    "BLUETOOTH_SCAN",
                    "BLUETOOTH_CONNECT",
                    "BLUETOOTH_SCAN",
                    "ACCESS_COARSE_LOCATION",
                    "ACCESS_FINE_LOCATION",
                    "MICROPHONE",
                )
            }
        } else {
            aliases = arrayOf(
                "ACCESS_COARSE_LOCATION",
                "ACCESS_FINE_LOCATION",
                "BLUETOOTH",
                "BLUETOOTH_ADMIN",
                "MICROPHONE",
                "BLUETOOTH_SCAN",
            )
        }
        requestPermissionForAliases(aliases, call, "requestPermissions")

        //call.resolve()
    }

    @PermissionCallback
    public override fun requestPermissions(call: PluginCall) {
        // Save the call to be able to access it in microphonePermissionsCallback
        bridge.saveCall(call)

        val granted: List<Boolean> = aliases.map { alias ->
            getPermissionState(alias) == PermissionState.GRANTED
        }
        // all have to be true
        if (granted.all { it }) {
            call.resolve()
        } else {
            call.reject("Permission denied.")
        }

        // If the microphone permission is defined in the manifest, then we have to prompt the user
        // or else we will get a security exception when trying to present the microphone. If, however,
        // it is not defined in the manifest then we don't need to prompt and it will just work.
        //if (isPermissionDeclared(Manifest.permission_group.MICROPHONE)) {
            // just request normally
        //    super.requestPermissions(call)
        //} else {
            // the manifest does not define microphone permissions, so we need to decide what to do
            // first, extract the permissions being requested
        //    requestPermissionForAlias(Manifest.permission_group.MICROPHONE, call, "checkPermissions")
        //}
    }

    @PluginMethod
    public fun stop(call: PluginCall) {
        Log.i(TAG,"ResgridPlugin: Stop")

        val context: Context = activity.applicationContext;
        val activity: Activity = activity;

        if (disposables != null && disposables.size() > 0) {
            disposables.dispose()
        }

        call.resolve()
    }

    @PluginMethod
    public fun showModal(call: PluginCall) {
        Log.i(TAG,"ResgridPlugin: showModal")

        //val intent = Intent(context, AudioCallActivity::class.java)

        //val bottomSheetBehavior = BottomSheetBehavior.from(intent.)
        //BottomSheet bottomSheet = BottomSheet()

        getBridge().getActivity().runOnUiThread(Runnable {
            if (audioCallFragment == null) {
                audioCallFragment = AudioCallFragment()

                val args = Bundle()
                args.putString(CallActivity.KEY_ARGS, Gson().toJson(configData))
                audioCallFragment!!.arguments = args
            }

            if (bottomAudioView == null) {
                bottomAudioView = BottomAudioView(configData, audioCallFragment!!)
                bottomAudioView!!.show(activity.supportFragmentManager, "bottomAudioView")
            } else {
                bottomAudioView!!.reShow()
            }
        })

        //// Expanded by default
        //bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        //bottomSheetBehavior.skipCollapsed = true
        //bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
        //    override fun onStateChanged(bottomSheet: View, newState: Int) {
        //        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
        //            finish()
        //            //Cancels animation on finish()
        //            overridePendingTransition(0, 0)
        //        }
        //    }
        //    override fun onSlide(bottomSheet: View, slideOffset: Float) {
        //    }
        //})

        //intent.putExtra(
        //        CallActivity.KEY_ARGS,
        //        Gson().toJson(configData)
        //)
        //activity.startActivity(intent)

        call.resolve()
    }
}
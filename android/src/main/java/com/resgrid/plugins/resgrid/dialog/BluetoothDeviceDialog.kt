package com.resgrid.plugins.resgrid.dialog

import android.app.Activity
import android.app.AlertDialog
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.resgrid.plugins.resgrid.vms.CallViewModel


fun Activity.showBluetoothDeviceDialog(callViewModel: CallViewModel) {
    val builder = with(AlertDialog.Builder(this)) {
        setTitle("Debug Menu")

        val arrayAdapter = ArrayAdapter<String>(this@showBluetoothDeviceDialog, android.R.layout.select_dialog_item)
        arrayAdapter.add("Connect Headset")
        arrayAdapter.add("Disconnect Headset")
        setAdapter(arrayAdapter) { dialog, index ->
            when (index) {
                0 -> callViewModel.connectToHeadset(this@showBluetoothDeviceDialog)
                1 -> callViewModel.disconnectHeadset()
            }
            dialog.dismiss()
        }
    }
    builder.show()
}

fun Fragment.showBluetoothDeviceDialogFrag(callViewModel: CallViewModel) {
    val builder = with(AlertDialog.Builder(context)) {
        setTitle("Debug Menu")

        val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.select_dialog_item)
        arrayAdapter.add("Connect Headset")
        arrayAdapter.add("Disconnect Headset")
        setAdapter(arrayAdapter) { dialog, index ->
            when (index) {
                0 -> callViewModel.connectToHeadset(context)
                1 -> callViewModel.disconnectHeadset()
            }
            dialog.dismiss()
        }
    }
    builder.show()
}
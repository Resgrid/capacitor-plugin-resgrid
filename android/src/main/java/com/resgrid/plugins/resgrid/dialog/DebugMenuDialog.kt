package com.resgrid.plugins.resgrid.dialog

import android.app.Activity
import android.app.AlertDialog
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.resgrid.plugins.resgrid.vms.CallViewModel


fun Activity.showDebugMenuDialog(callViewModel: CallViewModel) {
    val builder = with(AlertDialog.Builder(this)) {
        setTitle("Debug Menu")

        val arrayAdapter = ArrayAdapter<String>(this@showDebugMenuDialog, android.R.layout.select_dialog_item)
        arrayAdapter.add("Simulate Migration")
        arrayAdapter.add("Reconnect to Room")
        setAdapter(arrayAdapter) { dialog, index ->
            when (index) {
                0 -> callViewModel.simulateMigration()
                1 -> callViewModel.reconnect()
            }
            dialog.dismiss()
        }
    }
    builder.show()
}

fun Fragment.showDebugMenuDialogFrag(callViewModel: CallViewModel) {
    val builder = with(AlertDialog.Builder(context)) {
        setTitle("Debug Menu")

        val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.select_dialog_item)
        arrayAdapter.add("Simulate Migration")
        arrayAdapter.add("Reconnect to Room")
        setAdapter(arrayAdapter) { dialog, index ->
            when (index) {
                0 -> callViewModel.simulateMigration()
                1 -> callViewModel.reconnect()
            }
            dialog.dismiss()
        }
    }
    builder.show()
}
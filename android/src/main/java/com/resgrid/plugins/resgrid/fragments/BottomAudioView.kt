package com.resgrid.plugins.resgrid.activites

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.fragment.app.FragmentTransaction
import com.getcapacitor.Logger
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.resgrid.plugins.resgrid.R
import com.resgrid.plugins.resgrid.models.ConfigData


open class RoundedBottomSheetDialogFragment() : BottomSheetDialogFragment() {
    protected lateinit var dialog : BottomSheetDialog

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
    //override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme)


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        //dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog = BottomSheetDialog(requireContext(), theme)
        return dialog
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()
        isCancelable = false
        dialog!!.setCanceledOnTouchOutside(false)
        val outsideView = dialog.findViewById<View>(com.google.android.material.R.id.touch_outside)//requireDialog().findViewById<View>(com.google.android.material.R.id.touch_outside)
        if (outsideView != null) {
            outsideView.setOnTouchListener { v: View?, event: MotionEvent ->
                if (event.action == MotionEvent.ACTION_UP) dialog!!.hide()
                false
            }
        }
    }

    fun reShow() {
        if (dialog != null) {
            dialog!!.show()
        }
    }
}

class BottomAudioView(private val configData: ConfigData, private val audioView: AudioCallFragment): RoundedBottomSheetDialogFragment() {
    private var kcoViewAdded = false
    private lateinit var contentView: ViewGroup

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.modal_bottom_sheet_content, container, false)
        contentView = view.findViewById(R.id.bottomSheetContainer)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fm = childFragmentManager
        val fragmentTransaction: FragmentTransaction = fm.beginTransaction()
        fragmentTransaction.replace(R.id.bottomSheetContainer, audioView)
                .addToBackStack(null)
        fragmentTransaction.commit()

        // Update the layout
        //contentView.addView(audioView.view)
    }

    override fun onStart() {
        val behavior = (this.dialog as BottomSheetDialog).behavior
        val maxHeight = (resources.displayMetrics.heightPixels * 0.9f).toInt()
        behavior.peekHeight = maxHeight
        behavior.maxHeight = maxHeight

        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {

                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {

                        //kco.notifyWeb("closed", JSObject())
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }

        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        behavior.addBottomSheetCallback(bottomSheetCallback)

        super.onStart()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        Logger.info("Resgrid plugin: Cancel the bottom sheet dialog")
        //kco.notifyWeb("closed", JSObject())
    }

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}
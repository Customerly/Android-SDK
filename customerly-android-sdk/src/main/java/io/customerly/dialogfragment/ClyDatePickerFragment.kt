package io.customerly.dialogfragment

/**
 * Created by Gianni on 14/06/18.
 * Project: RFI
 */
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.widget.DatePicker
import io.customerly.utils.ggkext.ignoreException
import io.customerly.utils.ggkext.msAsSeconds
import io.customerly.utils.ggkext.weak
import java.util.*


internal class ClyDatePickerFragment
@SuppressLint("ValidFragment")
internal constructor(private val dateDialogListener: ((Long)->Unit)? = null) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return this.activity?.let { activity ->
            val datePicker = DatePicker(activity).also {
                this.arguments?.getLong("initialValue", -1L)
                        ?.takeIf { it != -1L }
                        ?.let { Calendar.getInstance().apply { this.timeInMillis = it * 1000 }
                        } ?: Calendar.getInstance().let { initialDate ->
                    initialDate.get(Calendar.YEAR)
                    it.updateDate(initialDate.get(Calendar.YEAR), initialDate.get(Calendar.MONTH), initialDate.get(Calendar.DAY_OF_MONTH))
                }
            }
            val weakFragment = this.weak()
            val weakDatePicker = datePicker.weak()
            return android.support.v7.app.AlertDialog.Builder(activity)
                    .setView(datePicker)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        weakDatePicker.get()?.also { datePicker ->
                            weakFragment.get()?.dateDialogListener?.invoke(
                                    Calendar.getInstance().let {
                                        it.set(Calendar.YEAR, datePicker.year)
                                        it.set(Calendar.MONTH, datePicker.month)
                                        it.set(Calendar.DAY_OF_MONTH, datePicker.dayOfMonth)
                                        it.timeInMillis.msAsSeconds
                                    }
                            )
                        }
                        ignoreException {
                            dialog.dismiss()
                        }
                    }
                    .create()
        } ?: super.onCreateDialog(savedInstanceState)
    }
}

internal fun Activity.showDateDialogListener(initialValue: Long = -1L, onDate: (Long)->Unit) {
    ClyDatePickerFragment(dateDialogListener = onDate).apply {
        this.arguments = Bundle().apply {
            this.putLong("initialValue", initialValue)
        }
    }.show(this.fragmentManager, "ClyDatePickerFragment")
}

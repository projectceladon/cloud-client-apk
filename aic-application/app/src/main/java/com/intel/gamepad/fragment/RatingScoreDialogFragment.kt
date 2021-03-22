package com.intel.gamepad.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment

import com.intel.gamepad.R
import com.mycommonlibrary.utils.LogEx
import kotlinx.android.synthetic.main.dlg_rating_score.*
import kotlinx.android.synthetic.main.dlg_rating_score.view.*

class RatingScoreDialogFragment : DialogFragment() {
    companion object {
        fun newInstance() = RatingScoreDialogFragment()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewDialog = LayoutInflater.from(context).inflate(R.layout.dlg_rating_score, null)
        val dlg = AlertDialog.Builder(activity).setView(viewDialog).create()
        viewDialog.btnOk.setOnClickListener {
            val feedback =
                if (viewDialog.etFeedBack.text.isNullOrEmpty()) ""
                else viewDialog.etFeedBack.text.toString()
            onOK(viewDialog.ratingScore.rating, feedback)
            dlg.dismiss()
        }
        viewDialog.btnCancel.setOnClickListener { dlg.dismiss() }
        return dlg
    }

    private fun onOK(rating: Float, feedback: String) {

    }
}

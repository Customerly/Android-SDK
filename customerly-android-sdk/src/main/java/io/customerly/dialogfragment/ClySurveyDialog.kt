package io.customerly.dialogfragment

/*
 * Copyright (C) 2017 Customerly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.appcompat.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.customerly.Customerly
import io.customerly.R
import io.customerly.activity.startClyWebViewActivity
import io.customerly.api.*
import io.customerly.entity.*
import io.customerly.utils.CUSTOMERLY_SURVEY_SITE
import io.customerly.utils.ggkext.activity
import io.customerly.utils.ggkext.dp2px
import io.customerly.utils.ggkext.weak
import io.customerly.utils.htmlformatter.fromHtml
import kotlinx.android.synthetic.main.io_customerly__dialog_fragment_survey.*
import kotlinx.android.synthetic.main.io_customerly__dialog_fragment_survey.view.*

/**
 * Created by Gianni on 29/11/16.
 * Project: Customerly Android SDK
 */

private const val SURVEY_FRAGMENT_TAG = "io.customerly.dialogfragment.ClySurveyDialog"
private const val SURVEY_ARGUMENT_KEY = "ClySurveyDialog"

@Throws(WindowManager.BadTokenException::class)
internal fun Activity.showClySurveyDialog(survey: ClySurvey) {
    synchronized(ClySurveyDialog.lock) {
        if(ClySurveyDialog.displayed) {
            return
        } else {
            ClySurveyDialog.displayed = true
        }
    }
    val fm = (this as? FragmentActivity)?.supportFragmentManager
    if (fm != null && (fm.findFragmentByTag(SURVEY_FRAGMENT_TAG) as? ClySurveyDialog)?.dialog?.isShowing != true) {
        val transaction = fm.beginTransaction().addToBackStack(null)
        ClySurveyDialog().apply {
            this.arguments = Bundle().apply {
                this.putParcelable(SURVEY_ARGUMENT_KEY, survey)
            }
        }.show(transaction, SURVEY_FRAGMENT_TAG)
    } else {
        synchronized(ClySurveyDialog.lock) {
            ClySurveyDialog.displayed = false
        }
    }
}

internal fun Activity.dismissClySurveyDialog() {
    ((this as? FragmentActivity)?.supportFragmentManager?.findFragmentByTag(SURVEY_FRAGMENT_TAG) as? DialogFragment)?.dismissAllowingStateLoss()
}

internal class ClySurveyDialog : DialogFragment() {

    companion object {
        val lock: Array<Any> = emptyArray()
        var displayed: Boolean = false
    }

    private var currentSurvey: ClySurvey? = null
    private var surveyCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.io_customerly__SurveyFragment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val survey = this.arguments?.getParcelable<ClySurvey>(SURVEY_ARGUMENT_KEY)?.takeUnless { it.isRejectedOrConcluded }
        return if(survey == null) {
            Customerly.log(message = "No surveys available")
            this.dismissAllowingStateLoss()
            null
        } else {
            val view = inflater.inflate(R.layout.io_customerly__dialog_fragment_survey, container, false)

            val weakDialog = this.weak()

            view.io_customerly__back.setOnClickListener { backButton ->
                weakDialog.get()
                        ?.takeIf { it.io_customerly__back.visibility == View.VISIBLE && it.io_customerly__progress_view.visibility == View.GONE }
                        ?.let { dialog ->
                            val currentSurveyId = dialog.currentSurvey?.id
                            if(currentSurveyId != null) {
                                dialog.io_customerly__input_layout.removeAllViews()
                                currentSurveyId
                            } else {
                                null
                            }
                        }?.let { currentSurveyId ->
                                ClyApiRequest(
                                        context = backButton.context,
                                        endpoint = ENDPOINT_SURVEY_BACK,
                                        requireToken = true,
                                        trials = 2,
                                        onPreExecute = {
                                            weakDialog.get()?.let { dialog ->
                                                dialog.io_customerly__progress_view.layoutParams.height =
                                                        dialog.io_customerly__title.height
                                                        + dialog.io_customerly__subtitle.height
                                                        + dialog.io_customerly__input_layout.height

                                                dialog.io_customerly__title.visibility = View.GONE
                                                dialog.io_customerly__subtitle.visibility = View.GONE
                                                dialog.io_customerly__input_layout.removeAllViews()
                                                dialog.io_customerly__progress_view.visibility = View.VISIBLE
                                            }
                                        },
                                        jsonObjectConverter = { it.parseSurvey() },
                                        callback = {
                                            weakDialog.get()?.also { dialog ->
                                                dialog.io_customerly__progress_view.visibility = View.GONE
                                                when (it) {
                                                    is ClyApiResponse.Success -> {
                                                        dialog.applySurvey(survey = it.result)
                                                    }
                                                    is ClyApiResponse.Failure -> {
                                                        dialog.activity?.applicationContext?.also { applicationContext ->
                                                            Toast.makeText(applicationContext, R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show()
                                                        }
                                                        dialog.applySurvey(survey = null)
                                                    }
                                                }
                                            }
                                        })
                                        .p(key = "survey_id", value = currentSurveyId)
                                        .start()
                        }
            }

            view.io_customerly__close.setOnClickListener {
                weakDialog.get()?.takeIf { dlg -> dlg.io_customerly__progress_view.visibility == View.GONE }?.also { dialog ->
                    dialog.takeUnless { dlg -> dlg.surveyCompleted }?.let { dialog.currentSurvey }?.let { currentSurvey ->
                        currentSurvey.isRejectedOrConcluded = true
                        ClyApiRequest(
                                context = dialog.activity,
                                endpoint = ENDPOINT_SURVEY_REJECT,
                                requireToken = true,
                                trials = 2,
                                jsonObjectConverter = { Unit })
                                .p(key = "survey_id", value = currentSurvey.id)
                                .start()
                    }
                    dialog.dismissAllowingStateLoss()
                }
            }

            if (Customerly.lastPing.poweredBy) {
                view.io_customerly__survey_by_customerly.setOnClickListener { it.activity?.startClyWebViewActivity(targetUrl = CUSTOMERLY_SURVEY_SITE) }
                view.io_customerly__survey_by_customerly.visibility = View.VISIBLE
            } else {
                view.io_customerly__survey_by_customerly.visibility = View.GONE
            }

            this.applySurvey(survey = survey, rootView = view)
            view
        }
    }

    override fun onDestroyView() {
        synchronized(ClySurveyDialog.lock) {
            ClySurveyDialog.displayed = false
        }
        super.onDestroyView()
    }

    private fun applySurvey(survey: ClySurvey?, rootView: View? = this.view) {
        val context = this.activity
        if (survey == null || context == null || rootView == null) {
            this.dismissAllowingStateLoss()
        } else {
            this.currentSurvey = survey

            if(survey.type == TSURVEY_END_SURVEY) {

                rootView.io_customerly__back.visibility = View.INVISIBLE
                this.surveyCompleted = true
                survey.isRejectedOrConcluded = true


                rootView.io_customerly__input_layout.addView(
                        TextView(context).apply {
                            this.setTextColor(Color.BLACK)
                            this.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                            this.text = fromHtml(message = survey.thankYouYext)
                            this.setPadding(10.dp2px, 10.dp2px, 10.dp2px, 10.dp2px)
                            this.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                this.topMargin = 5.dp2px
                                this.bottomMargin = 5.dp2px
                            }
                        })
            } else {

                rootView.io_customerly__back.visibility = if (survey.step == 0) View.INVISIBLE else View.VISIBLE
                this.surveyCompleted = false
                rootView.io_customerly__title.apply {
                    this.text = survey.title
                    this.visibility = View.VISIBLE
                }
                rootView.io_customerly__subtitle.apply {
                    this.text = survey.subtitle
                    this.visibility = View.VISIBLE
                }

                val weakDialog = this.weak()
                when(survey.type) {
                    TSURVEY_BUTTON -> {
                        survey.choices?.forEach { (choiceId,choiceValue) ->
                            rootView.io_customerly__input_layout.addView(
                                Button(context).apply {
                                    this.setTextColor(Color.WHITE)
                                    this.setBackgroundResource(R.drawable.io_customerly__button_blue_state)
                                    this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                                    this.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                                    this.text = choiceValue
                                    this.gravity = Gravity.CENTER
                                    this.setPadding(3.dp2px, 3.dp2px, 3.dp2px, 3.dp2px)
                                    this.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 40.dp2px).apply {
                                        this.topMargin = 5.dp2px
                                        this.bottomMargin = 5.dp2px
                                    }
                                    this.setOnClickListener { weakDialog.get()?.nextSurvey(survey = survey, choiceId = choiceId) }
                                })
                        }
                    }
                    TSURVEY_RADIO -> {
                        val inflater = context.layoutInflater
                        survey.choices?.forEach { (choiceId, choiceValue) ->
                            rootView.io_customerly__input_layout.addView(
                                (inflater.inflate(R.layout.io_customerly__surveyitem_radio, rootView.io_customerly__input_layout, false) as AppCompatRadioButton).apply {
                                    this.text = choiceValue
                                    this.setOnClickListener { weakDialog.get()?.nextSurvey(survey = survey, choiceId = choiceId) }
                                })
                        }
                    }
                    TSURVEY_LIST -> {
                        survey.choices?.let { choices ->
                            rootView.io_customerly__input_layout.addView(
                                AppCompatSpinner(context).apply {
                                    this.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 40.dp2px).apply {
                                        this.topMargin = 15.dp2px
                                        this.bottomMargin = 15.dp2px
                                    }
                                    this.setBackgroundResource(R.drawable.io_customerly__spinner_bkg)
                                    this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                                            if (id != 0L) {
                                                weakDialog.get()?.nextSurvey(survey = survey, choiceId = id.toInt())
                                            }
                                        }
                                        override fun onNothingSelected(parent: AdapterView<*>) {}
                                    }
                                    this.adapter = object : ArrayAdapter<ClySurveyChoice>(context, android.R.layout.simple_spinner_dropdown_item, choices) {

                                        override fun getCount() = super.getCount() + 1

                                        override fun getItemId(position: Int) = when (position) {
                                            0 -> 0
                                            else -> super.getItem(position - 1)?.id?.toLong() ?: 0L
                                        }

                                        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                                            return when(position) {
                                                0 -> convertView?.takeIf { it.tag == "EMPTY" }
                                                        ?: TextView(parent.context).apply {
                                                            this.tag = "EMPTY"
                                                            this.layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                                                            this.setBackgroundColor(Color.WHITE)
                                                        }
                                                else -> (super.getDropDownView(position - 1, convertView?.takeIf { it.tag != "EMPTY" }, parent) as TextView).apply {
                                                        this.setBackgroundColor(Color.WHITE)
                                                        this.setTextColor(Color.BLACK)
                                                    }
                                            }
                                        }

                                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                            return when(position) {
                                                0 -> convertView?.takeIf { it.tag == "HINT" }
                                                            ?: TextView(parent.context).apply {
                                                                this.tag = "HINT"
                                                                this.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                                                this.setText(R.string.io_customerly__choose_an_answer)
                                                                this.setTextColor(Color.GRAY)
                                                                this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                                                                this.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                                                                this.gravity = Gravity.CENTER
                                                            }
                                                else -> super.getView(position - 1, convertView?.takeIf { it.tag != "HINT" }, parent)
                                            }
                                        }
                                    }
                                })

                        }
                    }
                    TSURVEY_SCALE -> {
                        rootView.io_customerly__input_layout.addView(
                                /* Root */LinearLayout(context).apply {
                                    this.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                    this.orientation = LinearLayout.VERTICAL
                                    this.gravity = Gravity.CENTER_HORIZONTAL

                                    /* Button confirm */val confirm = Button(context).apply { //
                                        this.setTextColor(Color.WHITE)
                                        this.setBackgroundResource(R.drawable.io_customerly__button_blue_state)
                                        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                                        this.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                                        this.text = context.getString(R.string.io_customerly__confirm_x, survey.limitFrom)
                                        this.tag = survey.limitFrom
                                        this.gravity = Gravity.CENTER
                                        this.layoutParams = LinearLayout.LayoutParams(160.dp2px, 40.dp2px).apply {
                                            this.topMargin = 5.dp2px
                                            this.bottomMargin = 5.dp2px
                                        }
                                        this.setOnClickListener { btnConfirm -> weakDialog.get()?.nextSurvey(survey = survey, answer = btnConfirm.tag.toString()) }
                                    }

                                    /* Seek layout */this.addView(LinearLayout(context).apply {
                                        this.orientation = LinearLayout.HORIZONTAL
                                        this.gravity = Gravity.CENTER_VERTICAL
                                        this.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                            this.topMargin = 15.dp2px
                                            this.bottomMargin = 15.dp2px
                                        }

                                        /* Min */this.addView(TextView(context).apply {
                                            this.setTextColor(Color.BLACK)
                                            this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                                            this.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                                            this.text = survey.limitFrom.toString()
                                            this.gravity = Gravity.CENTER
                                            this.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                        })

                                        val weakConfirmButton = confirm.weak()
                                        /* SeekBar */this.addView((LayoutInflater.from(context).inflate(R.layout.io_customerly__surveyitem_scaleseekbar, this, false) as AppCompatSeekBar).apply {
                                            this.max = survey.limitTo - survey.limitFrom
                                            this.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                                                private val surveySeekLimitFrom = survey.limitFrom
                                                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                                                    if (fromUser) {
                                                        weakConfirmButton.get()?.also {
                                                            it.tag = this.surveySeekLimitFrom + seekBar.progress
                                                            it.text = seekBar.context.getString(R.string.io_customerly__confirm_x, this.surveySeekLimitFrom + progress)
                                                        }
                                                    }
                                                }
                                                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                                                override fun onStopTrackingTouch(seekBar: SeekBar) {}
                                            })
                                        })

                                        /* Max */this.addView(TextView(context).apply {
                                            this.setTextColor(Color.BLACK)
                                            this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                                            this.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                                            this.text = survey.limitTo.toString()
                                            this.gravity = Gravity.CENTER
                                            this.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                        })
                                    })

                                    this.addView(confirm)
                                })
                    }
                    TSURVEY_STAR -> {
                        rootView.io_customerly__input_layout.addView(
                                (context.layoutInflater.inflate(R.layout.io_customerly__surveyitem_ratingbar, rootView.io_customerly__input_layout, false) as AppCompatRatingBar).apply {
                                    this.setOnRatingBarChangeListener { _, rating, _ -> weakDialog.get()?.nextSurvey(survey = survey, answer = rating.toString()) }
                                })
                    }
                    TSURVEY_NUMBER, TSURVEY_TEXT_BOX, TSURVEY_TEXT_AREA -> {
                        rootView.io_customerly__input_layout.addView(
                                LinearLayout(context).apply {
                                    this.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                    this.orientation = LinearLayout.VERTICAL
                                    this.gravity = Gravity.CENTER_HORIZONTAL

                                    val editText = (context.layoutInflater.inflate(R.layout.io_customerly__surveyitem_edittext, this, false) as AppCompatEditText).apply {
                                        when (survey.type) {
                                            TSURVEY_NUMBER -> {
                                                this.inputType = InputType.TYPE_CLASS_NUMBER
                                                this.setHint(R.string.io_customerly__hint_insert_a_number)
                                            }
                                            TSURVEY_TEXT_BOX -> {
                                                this.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                                                this.setHint(R.string.io_customerly__hint_insert_a_text)
                                            }
                                            TSURVEY_TEXT_AREA -> {
                                                this.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
                                                this.setHint(R.string.io_customerly__hint_insert_a_text)
                                                this.minLines = 2
                                            }
                                        }
                                    }
                                    this.addView(editText)

                                    val weakEditText = editText.weak()
                                    this.addView(Button(context).apply {
                                        this.setTextColor(Color.WHITE)
                                        this.setBackgroundResource(R.drawable.io_customerly__button_blue_state)
                                        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                                        this.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                                        this.setText(R.string.io_customerly__confirm)
                                        this.tag = survey.limitFrom
                                        this.gravity = Gravity.CENTER
                                        this.layoutParams = LinearLayout.LayoutParams(160.dp2px, 40.dp2px).apply {
                                            this.topMargin = 5.dp2px
                                            this.bottomMargin = 5.dp2px
                                        }
                                        this.setOnClickListener {
                                            val answer = weakEditText.get()?.text?.toString()?.trim()
                                            if (answer?.isNotEmpty() == true) {
                                                weakDialog.get()?.nextSurvey(survey = survey, answer = answer)
                                            }
                                        }
                                    })
                                })
                    }
                }

                if (!survey.seen) {
                    ClyApiRequest(
                            context = context,
                            endpoint = ENDPOINT_SURVEY_SEEN,
                            requireToken = true,
                            trials = 2,
                            jsonObjectConverter = { Unit })
                            .p(key = "survey_id", value = survey.id)
                            .start()
                }
            }
        }
    }

    private fun nextSurvey(survey: ClySurvey, choiceId: Int? = null, answer: String? = null) {
        if((survey.requireAnswerByChoice && choiceId != null) || (survey.requireAnswerByString && answer != null)) {
            val weakDialog = this.weak()
            ClyApiRequest(
                    context = this.activity,
                    endpoint = ENDPOINT_SURVEY_SUBMIT,
                    requireToken = true,
                    trials = 2,
                    onPreExecute = {
                        weakDialog.get()?.let { dialog ->
                            dialog.io_customerly__progress_view.layoutParams.height =
                                    dialog.io_customerly__title.height + dialog.io_customerly__subtitle.height + dialog.io_customerly__input_layout.height

                            dialog.io_customerly__title.visibility = View.GONE
                            dialog.io_customerly__subtitle.visibility = View.GONE
                            dialog.io_customerly__input_layout.removeAllViews()
                            dialog.io_customerly__progress_view.visibility = View.VISIBLE
                        }
                    },
                    jsonObjectConverter = { survey.update(it) },
                    callback = {
                        weakDialog.get()?.also { dialog ->
                            dialog.io_customerly__progress_view.visibility = View.GONE
                            when (it) {
                                is ClyApiResponse.Success -> {
                                    dialog.applySurvey(survey = it.result)
                                }
                                is ClyApiResponse.Failure -> {
                                    dialog.activity?.applicationContext?.also { applicationContext ->
                                        Toast.makeText(applicationContext, R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show()
                                    }
                                    dialog.applySurvey(survey = null)
                                }
                            }
                        }
                    })
                    .p(key = "survey_id", value = survey.id)
                    .p(key = "choice_id", value = choiceId?.toString() ?: "")
                    .p(key = "answer", value = answer ?: "")
                    .start()
        }
    }
}
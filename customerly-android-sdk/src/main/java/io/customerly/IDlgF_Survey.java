package io.customerly;

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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatRatingBar;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatSpinner;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import static io.customerly.IE_Survey.TYPE_END_SURVEY;

/**
 * Created by Gianni on 29/11/16.
 * Project: Customerly Android SDK
 */
public class IDlgF_Survey extends DialogFragment {

    private LinearLayout _SurveyContainer;
    private TextView _Title, _Subtitle;
    private View _ProgressView, _Back;
    @Nullable
    private IE_Survey _CurrentSurvey;
    private boolean _SurveyCompleted = false;

    @SuppressLint("CommitTransaction")
    static void show(@NonNull Activity activity, @NonNull IE_Survey survey) {
        Fragment prev = activity.getFragmentManager().findFragmentByTag("io.customerly.IDlgF_Survey");
        if (prev == null || !(prev instanceof IDlgF_Survey) || ((IDlgF_Survey)prev).getDialog() == null || !((IDlgF_Survey)prev).getDialog().isShowing()) {
            IDlgF_Survey dlg = new IDlgF_Survey();
            Bundle b = new Bundle();
            b.putParcelable("IE_Survey", survey);
            dlg.setArguments(b);
            dlg.show(activity.getFragmentManager().beginTransaction().addToBackStack(null), "io.customerly.IDlgF_Survey");
        }
    }

    static void dismiss(@NonNull Activity activity) {
        Fragment existing = activity.getFragmentManager().findFragmentByTag("io.customerly.IDlgF_Survey");
        if (existing != null) {
            ((DialogFragment)existing).dismiss();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.io_customerly__SurveyFragment);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle args = this.getArguments();
        if(args != null) {
            IE_Survey survey = args.getParcelable("IE_Survey");
            if(survey != null && !survey.isRejectedOrConcluded) {
                View view = inflater.inflate(R.layout.io_customerly__dialog_fragment_survey, container, false);
                this._Title = (TextView) view.findViewById(R.id.io_customerly__title);
                this._Subtitle = (TextView) view.findViewById(R.id.io_customerly__subtitle);
                this._SurveyContainer = (LinearLayout) view.findViewById(R.id.io_customerly__input_layout);
                this._ProgressView = view.findViewById(R.id.io_customerly__progress_view);
                this._Back = view.findViewById(R.id.io_customerly__back);
                this._Back.setOnClickListener(v -> {
                    IE_Survey currentSurvey = this._CurrentSurvey;
                    if (currentSurvey != null && this._Back.getVisibility() == View.VISIBLE && this._ProgressView.getVisibility() == View.GONE) {
                        this._SurveyContainer.removeAllViews();
                        new IApi_Request.Builder<IE_Survey>(IApi_Request.ENDPOINT_SURVEY_BACK)
                                .opt_checkConn(this.getActivity())
                                .opt_tokenMandatory()
                                .opt_trials(2)
                                .opt_onPreExecute(() -> {
                                    this._ProgressView.getLayoutParams().height = this._Title.getHeight() + this._Subtitle.getHeight() + this._SurveyContainer.getHeight();
                                    this._Title.setVisibility(View.GONE);
                                    this._Subtitle.setVisibility(View.GONE);
                                    this._SurveyContainer.removeAllViews();
                                    this._ProgressView.setVisibility(View.VISIBLE);
                                })
                                .opt_converter(IE_Survey::from)
                                .opt_receiver((responseState, surveyBack) -> {
                                    if (responseState != IApi_Request.RESPONSE_STATE__OK) {
                                        Toast.makeText(this.getActivity().getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                                    }
                                    this._ProgressView.setVisibility(View.GONE);
                                    this.applySurvey(surveyBack);
                                })
                                .param("survey_id", currentSurvey.survey_id)
                                .start();
                    }
                });
                view.findViewById(R.id.io_customerly__close).setOnClickListener(v -> {
                    if (this._ProgressView.getVisibility() == View.GONE) {
                        IE_Survey currentSurvey = this._CurrentSurvey;
                        if (!this._SurveyCompleted && currentSurvey != null) {
                            currentSurvey.isRejectedOrConcluded = true;
                            new IApi_Request.Builder<IE_Survey>(IApi_Request.ENDPOINT_SURVEY_REJECT)
                                    .opt_checkConn(this.getActivity())
                                    .opt_tokenMandatory()
                                    .opt_trials(2)
                                    .param("survey_id", currentSurvey.survey_id)
                                    .start();
                        }
                        this.dismissAllowingStateLoss();
                    }
                });

                this.applySurvey(survey);

                return view;
            }
        }
        Customerly._Instance._log("No surveys available");
        this.dismissAllowingStateLoss();
        return null;
    }

    private void applySurvey(@Nullable IE_Survey survey) {
        if (survey == null) {
            this.dismissAllowingStateLoss();
            return;
        }
        this._CurrentSurvey = survey;
        if (survey.type == IE_Survey.TYPE_END_SURVEY) {
            this._Back.setVisibility(View.INVISIBLE);
            this._SurveyCompleted = true;
            survey.isRejectedOrConcluded = true;
            TextView thank_you = new TextView(this.getActivity());
            thank_you.setTextColor(Color.BLACK);
            thank_you.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
            thank_you.setText(IU_Utils.decodeHtmlStringWithEmojiTag(survey.thank_you_text));
            int _10dp = IU_Utils.px(10);
            thank_you.setPadding(_10dp, _10dp, _10dp, _10dp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = lp.topMargin = IU_Utils.px(5);
            thank_you.setLayoutParams(lp);
            this._SurveyContainer.addView(thank_you);
        } else {
            this._Back.setVisibility(survey.step == 0 ? View.INVISIBLE : View.VISIBLE);
            this._SurveyCompleted = false;
            this._Title.setText(survey.title);
            this._Title.setVisibility(View.VISIBLE);
            this._Subtitle.setText(survey.subtitle);
            this._Subtitle.setVisibility(View.VISIBLE);
            switch (survey.type) {
                case IE_Survey.TYPE_BUTTON:
                    if (survey.choices != null) {
                        for (IE_Survey.Choice c : survey.choices) {
                            Button b = new Button(this.getActivity());
                            {
                                b.setTextColor(Color.WHITE);
                                b.setBackgroundResource(R.drawable.io_customerly__button_blue_state);
                                b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                                b.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                                b.setText(c.value);
                                b.setGravity(Gravity.CENTER);
                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, IU_Utils.px(40));
                                lp.bottomMargin = lp.topMargin = IU_Utils.px(5);
                                b.setLayoutParams(lp);
                                b.setOnClickListener(v -> this.nextSurvey(survey, c.survey_choice_id, null));
                            }
                            this._SurveyContainer.addView(b);
                        }
                    }
                    break;
                case IE_Survey.TYPE_RADIO:
                    if (survey.choices != null) {
                        LayoutInflater inflater = LayoutInflater.from(this.getActivity());
                        for (IE_Survey.Choice c : survey.choices) {

                            AppCompatRadioButton radio = (AppCompatRadioButton) inflater.inflate(R.layout.io_customerly__surveyitem_radio, this._SurveyContainer, false);
                            {
                                radio.setText(c.value);
                                radio.setOnClickListener(v -> this.nextSurvey(survey, c.survey_choice_id, null));
                            }
                            this._SurveyContainer.addView(radio);
                        }
                    }
                    break;
                case IE_Survey.TYPE_LIST:
                    if (survey.choices != null) {
                        AppCompatSpinner spinner = new AppCompatSpinner(this.getActivity());
                        {
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, IU_Utils.px(40));
                            lp.bottomMargin = lp.topMargin = IU_Utils.px(15);
                            spinner.setLayoutParams(lp);
                            spinner.setBackgroundResource(R.drawable.io_customerly__spinner_bkg);
                            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    if (id != 0) {
                                        nextSurvey(survey, (int) id, null);
                                    }
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                }
                            });
                            spinner.setAdapter(new ArrayAdapter<IE_Survey.Choice>(this.getActivity(), android.R.layout.simple_spinner_dropdown_item, survey.choices) {
                                @Override
                                public long getItemId(int position) {
                                    if (position == 0) {
                                        return 0;
                                    } else {
                                        IE_Survey.Choice c = super.getItem(position - 1);
                                        return c == null ? 0 : c.survey_choice_id;
                                    }
                                }

                                @Override
                                public int getCount() {
                                    return super.getCount() + 1;
                                }

                                @Override
                                public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                                    if (position == 0) {
                                        if (convertView != null && "EMPTY".equals(convertView.getTag())) {
                                            return convertView;
                                        } else {
                                            View v = new TextView(parent.getContext());
                                            v.setTag("EMPTY");
                                            v.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                                            v.setBackgroundColor(Color.WHITE);
                                            return v;
                                        }
                                    } else {
                                        TextView tv = (TextView) super.getDropDownView(position - 1, convertView == null || "EMPTY".equals(convertView.getTag()) ? null : convertView, parent);
                                        tv.setBackgroundColor(Color.WHITE);
                                        tv.setTextColor(Color.BLACK);
                                        return tv;
                                    }
                                }

                                @NonNull
                                @Override
                                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                                    if (position == 0) {
                                        if (convertView != null && "HINT".equals(convertView.getTag())) {
                                            return convertView;
                                        } else {
                                            TextView tv = new TextView(parent.getContext());
                                            tv.setTag("HINT");
                                            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                            tv.setText(R.string.io_customerly__choose_an_answer);
                                            tv.setTextColor(Color.GRAY);
                                            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                                            tv.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                                            tv.setGravity(Gravity.CENTER);
                                            return tv;
                                        }
                                    } else {
                                        return super.getView(position - 1, convertView == null || "HINT".equals(convertView.getTag()) ? null : convertView, parent);
                                    }
                                }
                            });
                        }
                        this._SurveyContainer.addView(spinner);
                    }
                    break;
                case IE_Survey.TYPE_SCALE:
                    LinearLayout ll_root = new LinearLayout(this.getActivity());
                {
                    ll_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    ll_root.setOrientation(LinearLayout.VERTICAL);
                    ll_root.setGravity(Gravity.CENTER_HORIZONTAL);

                    Button confirm = new Button(this.getActivity());

                    LinearLayout ll_seek = new LinearLayout(this.getActivity());
                    {
                        ll_seek.setOrientation(LinearLayout.HORIZONTAL);
                        ll_seek.setGravity(Gravity.CENTER_VERTICAL);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.bottomMargin = lp.topMargin = IU_Utils.px(15);
                        ll_seek.setLayoutParams(lp);

                        TextView min = new TextView(this.getActivity());
                        {
                            min.setTextColor(Color.BLACK);
                            min.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                            min.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                            min.setText(String.valueOf(survey.limit_from));
                            min.setGravity(Gravity.CENTER);
                            min.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        }
                        ll_seek.addView(min);

                        AppCompatSeekBar seekBar = (AppCompatSeekBar) LayoutInflater.from(this.getActivity()).inflate(R.layout.io_customerly__surveyitem_scaleseekbar, ll_seek, false);
                        {
                            seekBar.setMax(survey.limit_to - survey.limit_from);
                            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @Override
                                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                    if (fromUser) {
                                        confirm.setTag(survey.limit_from + seekBar.getProgress());
                                        confirm.setText(seekBar.getContext().getString(R.string.io_customerly__confirm_x, survey.limit_from + progress));
                                    }
                                }

                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar) {
                                }

                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar) {
                                }
                            });
                        }
                        ll_seek.addView(seekBar);

                        TextView max = new TextView(this.getActivity());
                        {
                            max.setTextColor(Color.BLACK);
                            max.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                            max.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                            max.setText(String.valueOf(survey.limit_to));
                            max.setGravity(Gravity.CENTER);
                            max.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        }
                        ll_seek.addView(max);
                    }
                    ll_root.addView(ll_seek);

                    {
                        confirm.setTextColor(Color.WHITE);
                        confirm.setBackgroundResource(R.drawable.io_customerly__button_blue_state);
                        confirm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        confirm.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                        confirm.setText(this.getActivity().getString(R.string.io_customerly__confirm_x, survey.limit_from));
                        confirm.setTag(survey.limit_from);
                        confirm.setGravity(Gravity.CENTER);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(IU_Utils.px(160), IU_Utils.px(40));
                        lp.bottomMargin = lp.topMargin = IU_Utils.px(5);
                        confirm.setLayoutParams(lp);
                        confirm.setOnClickListener(v -> this.nextSurvey(survey, -1, String.valueOf(confirm.getTag())));
                    }
                    ll_root.addView(confirm);
                }
                this._SurveyContainer.addView(ll_root);
                break;
                case IE_Survey.TYPE_STAR:
                    AppCompatRatingBar ratingBar = (AppCompatRatingBar) LayoutInflater.from(this.getActivity()).inflate(R.layout.io_customerly__surveyitem_ratingbar, this._SurveyContainer, false);
                {
                    ratingBar.setOnRatingBarChangeListener((rBar, rating, fromUser) -> this.nextSurvey(survey, -1, String.valueOf(rating)));
                }
                this._SurveyContainer.addView(ratingBar);
                break;
                case IE_Survey.TYPE_NUMBER:
                case IE_Survey.TYPE_TEXT_BOX:
                case IE_Survey.TYPE_TEXT_AREA:
                    LinearLayout ll = new LinearLayout(this.getActivity());
                {
                    ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    ll.setOrientation(LinearLayout.VERTICAL);
                    ll.setGravity(Gravity.CENTER_HORIZONTAL);

                    AppCompatEditText editText = (AppCompatEditText) LayoutInflater.from(this.getActivity()).inflate(R.layout.io_customerly__surveyitem_edittext, ll, false);
                    {
                        switch (survey.type) {
                            case IE_Survey.TYPE_NUMBER:
                                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                                editText.setHint(R.string.io_customerly__hint_insert_a_number);
                                break;
                            case IE_Survey.TYPE_TEXT_BOX:
                                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                                editText.setHint(R.string.io_customerly__hint_insert_a_text);
                                break;
                            case IE_Survey.TYPE_TEXT_AREA:
                                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
                                editText.setHint(R.string.io_customerly__hint_insert_a_text);
                                editText.setMinLines(2);
                                break;
                            case IE_Survey.TYPE_BUTTON:
                            case IE_Survey.TYPE_END_SURVEY:
                            case IE_Survey.TYPE_LIST:
                            case IE_Survey.TYPE_RADIO:
                            case IE_Survey.TYPE_SCALE:
                            case IE_Survey.TYPE_STAR:
                                //Not reachable
                                break;
                        }
                    }
                    ll.addView(editText);

                    Button confirm = new Button(this.getActivity());
                    {
                        confirm.setTextColor(Color.WHITE);
                        confirm.setBackgroundResource(R.drawable.io_customerly__button_blue_state);
                        confirm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        confirm.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
                        confirm.setText(R.string.io_customerly__confirm);
                        confirm.setTag(survey.limit_from);
                        confirm.setGravity(Gravity.CENTER);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(IU_Utils.px(160), IU_Utils.px(40));
                        lp.bottomMargin = lp.topMargin = IU_Utils.px(5);
                        confirm.setLayoutParams(lp);
                        confirm.setOnClickListener(v -> {
                            if (editText.getText().length() != 0) {
                                this.nextSurvey(survey, -1, editText.getText().toString().trim());
                            }
                        });
                    }
                    ll.addView(confirm);
                }
                this._SurveyContainer.addView(ll);
                break;
                case TYPE_END_SURVEY:
                default:
                    break;
            }
            if (!survey.seen) {
                new IApi_Request.Builder<IE_Survey>(IApi_Request.ENDPOINT_SURVEY_SEEN)
                        .opt_checkConn(this.getActivity())
                        .opt_tokenMandatory()
                        .opt_trials(2)
                        .param("survey_id", survey.survey_id)
                        .start();
            }
        }
    }

    private void nextSurvey(IE_Survey pSurvey, int choice_id, @Nullable String answer) {
        IApi_Request.Builder builder = new IApi_Request.Builder<IE_Survey>(IApi_Request.ENDPOINT_SURVEY_SUBMIT)
                .opt_checkConn(this.getActivity())
                .opt_tokenMandatory()
                .opt_onPreExecute(() -> {
                    this._ProgressView.getLayoutParams().height = this._Title.getHeight() + this._Subtitle.getHeight() + this._SurveyContainer.getHeight();
                    this._Title.setVisibility(View.GONE);
                    this._Subtitle.setVisibility(View.GONE);
                    this._SurveyContainer.removeAllViews();
                    this._ProgressView.setVisibility(View.VISIBLE);
                })
                .opt_converter(pSurvey::updateFrom)
                .opt_receiver((responseState, survey) -> {
                    if (responseState != IApi_Request.RESPONSE_STATE__OK) {
                        Toast.makeText(this.getActivity().getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                    }
                    this._ProgressView.setVisibility(View.GONE);
                    this.applySurvey(survey);
                })
                .opt_trials(2)
                .param("survey_id", pSurvey.survey_id)
                .param("answer", answer != null ? answer : "")
                .param("choice_id", choice_id == -1 ? "" : String.valueOf(choice_id));
        builder.start();
    }
}
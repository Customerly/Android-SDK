package io.customerly;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
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

import static io.customerly.Internal_entity__Survey.TYPE_END_SURVEY;

/**
 * Created by Gianni on 29/11/16.
 * Project: TestApp_CRMHero
 */
public class Internal_dialogfragment__Survey_DialogFragment extends DialogFragment {

    private LinearLayout _SurveyContainer;
    private TextView _Title, _Subtitle;
    private View _ProgressView, _Back;
    @Nullable private Internal_entity__Survey _CurrentSurvey;
    private boolean _RejectEnabled = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.io_customerly__SurveyFragment);
    }

    @SuppressLint("RtlHardcoded")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Internal_entity__Survey[] surveys = Customerly._Instance.__PING__LAST_surveys;
        if(surveys != null) {
            for (Internal_entity__Survey survey : surveys) {
                if (survey != null && !survey.isRejectedOrConcluded) {
                    View view = inflater.inflate(R.layout.io_customerly__dialogfragment_survey, container, false);
                    this._Title = (TextView) view.findViewById(R.id.io_customerly__title);
                    this._Subtitle = (TextView) view.findViewById(R.id.io_customerly__subtitle);
                    this._SurveyContainer = (LinearLayout) view.findViewById(R.id.io_customerly__input_layout);
                    this._ProgressView = view.findViewById(R.id.io_customerly__progressview);
                    this._Back = view.findViewById(R.id.io_customerly__back);
                    this._Back.setOnClickListener(v -> {
                        Internal_entity__Survey currentSurvey = this._CurrentSurvey;
                        if (currentSurvey != null && this._Back.getVisibility() == View.VISIBLE && this._ProgressView.getVisibility() == View.GONE) {
                            this._SurveyContainer.removeAllViews();
                            new Internal_api__CustomerlyRequest.Builder<Internal_entity__Survey>(Internal_api__CustomerlyRequest.ENDPOINT_SURVEY_BACK)
                                    .opt_checkConn(this.getContext())
                                    .opt_trials(2)
                                    .opt_progressview(this._ProgressView, View.GONE)
                                    .opt_onPreExecute(() -> {
                                        this._ProgressView.getLayoutParams().height = this._Title.getHeight() + this._Subtitle.getHeight() + this._SurveyContainer.getHeight();
                                        this._Title.setVisibility(View.GONE);
                                        this._Subtitle.setVisibility(View.GONE);
                                        this._SurveyContainer.removeAllViews();
                                    })
                                    .opt_converter(Internal_entity__Survey::from)
                                    .opt_receiver((responseState, surveyBack) -> {
                                        if(responseState != Internal_api__CustomerlyRequest.RESPONSE_STATE__OK) {
                                            Toast.makeText(this.getContext().getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                                            this.applySurvey(surveyBack);
                                        }
                                    })
                                    .param("survey_id", currentSurvey.survey_id)
                                    .start();
                        }
                    });
                    view.findViewById(R.id.io_customerly__close).setOnClickListener(v -> {
                        if (this._ProgressView.getVisibility() == View.GONE) {
                            Internal_entity__Survey currentSurvey = this._CurrentSurvey;
                            if (this._RejectEnabled && currentSurvey != null) {
                                currentSurvey.isRejectedOrConcluded = true;
                                new Internal_api__CustomerlyRequest.Builder<Internal_entity__Survey>(Internal_api__CustomerlyRequest.ENDPOINT_SURVEY_REJECT)
                                        .opt_checkConn(this.getContext())
                                        .opt_trials(2)
                                        .opt_receiver((responseState, surveyBack) -> { })
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
        }
        Customerly._Instance._log("No surveys available");
        this.dismissAllowingStateLoss();
        return null;
    }

    private void applySurvey(@Nullable Internal_entity__Survey survey) {
        if(survey == null) {
            this.dismissAllowingStateLoss();
            return;
        }
        this._CurrentSurvey = survey;
        if(survey.type == Internal_entity__Survey.TYPE_END_SURVEY) {
            this._Back.setVisibility(View.INVISIBLE);
            this._RejectEnabled = false;
            survey.isRejectedOrConcluded = true;
            TextView thankyou = new TextView(this.getContext());
            thankyou.setTextColor(Color.BLACK);
            thankyou.setText(Internal_Utils__Utils.decodeHtmlStringWithEmojiTag(survey.thankyou_text));
            int _10dp = Internal_Utils__Utils.px(10);
            thankyou.setPadding(_10dp, _10dp, _10dp, _10dp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = lp.topMargin = Internal_Utils__Utils.px(5);
            thankyou.setLayoutParams(lp);
            this._SurveyContainer.addView(thankyou);
        } else {
            this._Back.setVisibility(survey.step == 0 ? View.INVISIBLE : View.VISIBLE);
            this._RejectEnabled = true;
            this._Title.setText(survey.title);
            this._Title.setVisibility(View.VISIBLE);
            this._Subtitle.setText(survey.subtitle);
            this._Subtitle.setVisibility(View.VISIBLE);
            switch (survey.type) {
                case Internal_entity__Survey.TYPE_BUTTON:
                    if(survey.choices != null) {
                        for (Internal_entity__Survey.Choice c : survey.choices) {
                            Button b = new Button(this.getContext());
                            {
                                b.setTextColor(Color.WHITE);
                                b.setBackgroundResource(R.drawable.io_customerly__button_blue_state);
                                b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                                b.setText(c.value);
                                b.setGravity(Gravity.CENTER);
                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Internal_Utils__Utils.px(40));
                                lp.bottomMargin = lp.topMargin = Internal_Utils__Utils.px(5);
                                b.setLayoutParams(lp);
                                b.setOnClickListener(v -> this.nextSurvey(survey, c.survey_choice_id, null));
                            }
                            this._SurveyContainer.addView(b);
                        }
                    }
                    break;
                case Internal_entity__Survey.TYPE_RADIO:
                    if(survey.choices != null) {
                        LayoutInflater inflater = LayoutInflater.from(this.getContext());
                        for (Internal_entity__Survey.Choice c : survey.choices) {

                            AppCompatRadioButton radio = (AppCompatRadioButton) inflater.inflate(R.layout.io_customerly__surveyitem_radio, this._SurveyContainer, false);
                            {
                                radio.setText(c.value);
                                radio.setOnClickListener(v -> this.nextSurvey(survey, c.survey_choice_id, null));
                            }
                            this._SurveyContainer.addView(radio);
                        }
                    }
                    break;
                case Internal_entity__Survey.TYPE_LIST:
                    if(survey.choices != null) {
                        AppCompatSpinner spinner = new AppCompatSpinner(this.getContext());
                        {
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Internal_Utils__Utils.px(40));
                            lp.bottomMargin = lp.topMargin = Internal_Utils__Utils.px(15);
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
                            spinner.setAdapter(new ArrayAdapter<Internal_entity__Survey.Choice>(this.getContext(), android.R.layout.simple_spinner_dropdown_item, survey.choices) {
                                @Override
                                public long getItemId(int position) {
                                    if (position == 0) {
                                        return 0;
                                    } else {
                                        Internal_entity__Survey.Choice c = super.getItem(position - 1);
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
                                        if (convertView != null && "VUOTA".equals(convertView.getTag())) {
                                            return convertView;
                                        } else {
                                            View v = new TextView(parent.getContext());
                                            v.setTag("VUOTA");
                                            v.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                                            v.setBackgroundColor(Color.WHITE);
                                            return v;
                                        }
                                    } else {
                                        TextView tv = (TextView) super.getDropDownView(position - 1, convertView == null || "VUOTA".equals(convertView.getTag()) ? null : convertView, parent);
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
                case Internal_entity__Survey.TYPE_SCALE:
                    LinearLayout ll_root = new LinearLayout(this.getContext());
                    {
                        ll_root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        ll_root.setOrientation(LinearLayout.VERTICAL);
                        ll_root.setGravity(Gravity.CENTER_HORIZONTAL);

                        Button confirm = new Button(this.getContext());

                        LinearLayout ll_seek = new LinearLayout(this.getContext());
                        {
                            ll_seek.setOrientation(LinearLayout.HORIZONTAL);
                            ll_seek.setGravity(Gravity.CENTER_VERTICAL);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            lp.bottomMargin = lp.topMargin = Internal_Utils__Utils.px(15);
                            ll_seek.setLayoutParams(lp);

                            TextView min = new TextView(this.getContext());
                            {
                                min.setTextColor(Color.BLACK);
                                min.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                min.setText(String.valueOf(survey.limit_from));
                                min.setGravity(Gravity.CENTER);
                                min.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                            }
                            ll_seek.addView(min);

                            AppCompatSeekBar seekbar = (AppCompatSeekBar) LayoutInflater.from(this.getContext()).inflate(R.layout.io_customerly__surveyitem_scaleseekbar, ll_seek, false);
                            {
                                seekbar.setMax(survey.limit_to - survey.limit_from);
                                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                        if(fromUser) {
                                            confirm.setTag(survey.limit_from + seekBar.getProgress());
                                            confirm.setText(seekBar.getContext().getString(R.string.io_customerly__confirm_x, survey.limit_from + progress));
                                        }
                                    }
                                    @Override public void onStartTrackingTouch(SeekBar seekBar) { }
                                    @Override public void onStopTrackingTouch(SeekBar seekBar) { }
                                });
                            }
                            ll_seek.addView(seekbar);

                            TextView max = new TextView(this.getContext());
                            {
                                max.setTextColor(Color.BLACK);
                                max.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
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
                            confirm.setText(this.getContext().getString(R.string.io_customerly__confirm_x, survey.limit_from));
                            confirm.setTag(survey.limit_from);
                            confirm.setGravity(Gravity.CENTER);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Internal_Utils__Utils.px(160), Internal_Utils__Utils.px(40));
                            lp.bottomMargin = lp.topMargin = Internal_Utils__Utils.px(5);
                            confirm.setLayoutParams(lp);
                            confirm.setOnClickListener(v -> this.nextSurvey(survey, -1, String.valueOf(confirm.getTag())));
                        }
                        ll_root.addView(confirm);
                    }
                    this._SurveyContainer.addView(ll_root);
                    break;
                case Internal_entity__Survey.TYPE_STAR:
                    AppCompatRatingBar ratingbar = (AppCompatRatingBar) LayoutInflater.from(this.getContext()).inflate(R.layout.io_customerly__surveyitem_ratingbar, this._SurveyContainer, false);
                    {
                        ratingbar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> this.nextSurvey(survey, -1, String.valueOf(rating)));
                    }
                    this._SurveyContainer.addView(ratingbar);
                    break;
                case Internal_entity__Survey.TYPE_NUMBER:
                case Internal_entity__Survey.TYPE_TEXTBOX:
                case Internal_entity__Survey.TYPE_TEXTAREA:
                    LinearLayout ll = new LinearLayout(this.getContext());
                    {
                        ll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        ll.setOrientation(LinearLayout.VERTICAL);
                        ll.setGravity(Gravity.CENTER_HORIZONTAL);

                        AppCompatEditText edittext = (AppCompatEditText) LayoutInflater.from(this.getContext()).inflate(R.layout.io_customerly__surveyitem_edittext, ll, false);
                        {
                            switch (survey.type) {
                                case Internal_entity__Survey.TYPE_NUMBER:
                                    edittext.setInputType(InputType.TYPE_CLASS_NUMBER);
                                    edittext.setHint(R.string.io_customerly__hint_insert_a_number);
                                    break;
                                case Internal_entity__Survey.TYPE_TEXTBOX:
                                    edittext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                                    edittext.setHint(R.string.io_customerly__hint_insert_a_text);
                                    break;
                                case Internal_entity__Survey.TYPE_TEXTAREA:
                                    edittext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
                                    edittext.setHint(R.string.io_customerly__hint_insert_a_text);
                                    break;
                                case Internal_entity__Survey.TYPE_BUTTON:
                                case Internal_entity__Survey.TYPE_END_SURVEY:
                                case Internal_entity__Survey.TYPE_LIST:
                                case Internal_entity__Survey.TYPE_RADIO:
                                case Internal_entity__Survey.TYPE_SCALE:
                                case Internal_entity__Survey.TYPE_STAR:
                                    //Non raggiungibile
                                    break;
                            }
                        }
                        ll.addView(edittext);

                        Button confirm = new Button(this.getContext());
                        {
                            confirm.setTextColor(Color.WHITE);
                            confirm.setBackgroundResource(R.drawable.io_customerly__button_blue_state);
                            confirm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                            confirm.setText(R.string.io_customerly__confirm);
                            confirm.setTag(survey.limit_from);
                            confirm.setGravity(Gravity.CENTER);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Internal_Utils__Utils.px(160), Internal_Utils__Utils.px(40));
                            lp.bottomMargin = lp.topMargin = Internal_Utils__Utils.px(5);
                            confirm.setLayoutParams(lp);
                            confirm.setOnClickListener(v -> {
                                if(edittext.getText().length() != 0) {
                                    this.nextSurvey(survey, -1, edittext.getText().toString().trim());
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
            if(! survey.seen) {
                new Internal_api__CustomerlyRequest.Builder<Internal_entity__Survey>(Internal_api__CustomerlyRequest.ENDPOINT_SURVEY_SEEN)
                        .opt_checkConn(this.getContext())
                        .opt_trials(2)
                        .param("survey_id", survey.survey_id)
                        .start();
            }
        }
    }

    private void nextSurvey(Internal_entity__Survey pSurvey, int choice_id, @Nullable String answer) {
        Internal_api__CustomerlyRequest.Builder builder = new Internal_api__CustomerlyRequest.Builder<Internal_entity__Survey>(Internal_api__CustomerlyRequest.ENDPOINT_SURVEY_SUBMIT)
                .opt_checkConn(this.getContext())
                .opt_progressview(this._ProgressView, View.GONE)
                .opt_onPreExecute(() -> {
                    this._ProgressView.getLayoutParams().height = this._Title.getHeight() + this._Subtitle.getHeight() + this._SurveyContainer.getHeight();
                    this._Title.setVisibility(View.GONE);
                    this._Subtitle.setVisibility(View.GONE);
                    this._SurveyContainer.removeAllViews();
                })
                .opt_converter(pSurvey::updateFrom)
                .opt_receiver((responseState, survey) -> {
                    if(responseState != Internal_api__CustomerlyRequest.RESPONSE_STATE__OK) {
                        Toast.makeText(this.getContext().getApplicationContext(), R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show();
                        this.applySurvey(survey);
                    }
                })
                .opt_trials(2)
                .param("survey_id", pSurvey.survey_id);
        if(choice_id != -1) {
            builder = builder.param("choice_id", choice_id);
        } else {
            builder = builder.param("answer", answer);
        }
        builder.start();
    }
}

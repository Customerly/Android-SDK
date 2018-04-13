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

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */
class XXXIE_Survey implements Parcelable {

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(this.survey_id);
        dest.writeInt(this.limit_from);
        dest.writeInt(this.limit_to);
        dest.writeInt(this.step);
        dest.writeInt(this.seen ? 1 : 0);
        dest.writeInt(this.type);
        dest.writeString(this.title);
        dest.writeString(this.subtitle);
        dest.writeString(this.thank_you_text);
        try {
            dest.writeTypedArray(this.choices, 0);
        } catch (Exception ignored) { }
    }
    private XXXIE_Survey(@NonNull Parcel in) {
        this.survey_id = in.readInt();
        this.limit_from = in.readInt();
        this.limit_to = in.readInt();
        this.step = in.readInt();
        this.seen = in.readInt() == 1;
        switch(in.readInt()) {
            case TYPE_END_SURVEY:
                this.type = TYPE_END_SURVEY;
                break;
            case TYPE_RADIO:
                this.type = TYPE_RADIO;
                break;
            case TYPE_LIST:
                this.type = TYPE_LIST;
                break;
            case TYPE_SCALE:
                this.type = TYPE_SCALE;
                break;
            case TYPE_STAR:
                this.type = TYPE_STAR;
                break;
            case TYPE_NUMBER:
                this.type = TYPE_NUMBER;
                break;
            case TYPE_TEXT_BOX:
                this.type = TYPE_TEXT_BOX;
                break;
            case TYPE_TEXT_AREA:
                this.type = TYPE_TEXT_AREA;
                break;
            case TYPE_BUTTON:
            default:
                this.type = TYPE_BUTTON;
                break;
        }
        this.title = in.readString();
        this.subtitle = in.readString();
        this.thank_you_text = in.readString();
        try {
            this.choices = in.createTypedArray(Choice.CREATOR);
        } catch (Exception ex) {
            this.choices = null;
        }
    }
    @Override public int describeContents() { return 0; }
    @NonNull public static final Parcelable.Creator<XXXIE_Survey> CREATOR = new Parcelable.Creator<XXXIE_Survey>() {
        @Contract(pure = true) @NonNull public XXXIE_Survey createFromParcel(@NonNull Parcel in) { return new XXXIE_Survey(in); }
        @Contract(pure = true) @NonNull public XXXIE_Survey[] newArray(int size) { return new XXXIE_Survey[size]; }
    };

    @IntDef({ TYPE_END_SURVEY, TYPE_BUTTON, TYPE_RADIO, TYPE_LIST, TYPE_SCALE, TYPE_STAR, TYPE_NUMBER, TYPE_TEXT_BOX, TYPE_TEXT_AREA})
    @Retention(RetentionPolicy.SOURCE)
    @interface SURVEY_TYPE {}

    static final int TYPE_END_SURVEY = -1;
    static final int TYPE_BUTTON = 0;
    static final int TYPE_RADIO = 1;
    static final int TYPE_LIST = 2;
    static final int TYPE_SCALE = 3;
    static final int TYPE_STAR = 4;
    static final int TYPE_NUMBER = 5;
    static final int TYPE_TEXT_BOX = 6;
    static final int TYPE_TEXT_AREA = 7;
    private static final int LAST_TYPE = TYPE_TEXT_AREA;

    @Contract("null -> null")
    @Nullable
    static XXXIE_Survey[] from(@Nullable JSONArray array) {
        if (array == null)
            return null;
        XXXIE_Survey[] surveys = new XXXIE_Survey[array.length()];
        for(int i = 0; i < array.length(); i++) {
            surveys[i] = XXXIE_Survey.from(array.optJSONObject(i));
        }
        return surveys;
    }

    @Contract("null -> null")
    @Nullable
    static XXXIE_Survey from(@Nullable JSONObject root) {
        if (root == null)
            return null;
        try {
            JSONObject survey = root.getJSONObject("survey");
            JSONObject question = root.optJSONObject("question");
            if(question != null) {
                JSONArray choices_arr = question.optJSONArray("choices");
                Choice[] choices;
                if(choices_arr != null) {
                    choices = new Choice[choices_arr.length()];
                    for(int i = 0; i < choices.length; i++) {
                        JSONObject c = choices_arr.getJSONObject(i);
                        choices[i] = new Choice(c.getInt("survey_choice_id"), c.getString("value"));
                    }
                } else {
                    choices = null;
                }
                int type = question.getInt("type");
                //noinspection SpellCheckingInspection
                return new XXXIE_Survey(survey.getInt("survey_id"), survey.getString("thankyou_text"), question.optInt("step", 0), root.optLong("seen_at", -1),
                        type < 0 ? TYPE_END_SURVEY : type > LAST_TYPE ? LAST_TYPE : type, question.getString("title"), question.getString("subtitle"),
                        question.optInt("limit_from", -1), question.optInt("limit_to", -1), choices);
            } else {//End of Survey
                //noinspection SpellCheckingInspection
                return new XXXIE_Survey(survey.getInt("survey_id"), survey.getString("thankyou_text"), Integer.MAX_VALUE, root.optLong("seen_at", -1),
                        TYPE_END_SURVEY, "", "", -1, -1, null);
            }
        } catch (JSONException json) {
            return null;
        }
    }

    XXXIE_Survey updateFrom(@Nullable JSONObject data) {
        if(data != null) {
            try {
                JSONObject question = data.optJSONObject("question");
                if(question != null) {
                    JSONArray choices_arr = question.optJSONArray("choices");
                    Choice[] choices;
                    if(choices_arr != null) {
                        choices = new Choice[choices_arr.length()];
                        for(int i = 0; i < choices.length; i++) {
                            JSONObject c = choices_arr.getJSONObject(i);
                            choices[i] = new Choice(c.getInt("survey_choice_id"), c.getString("value"));
                        }
                    } else {
                        choices = null;
                    }
                    this.step = question.optInt("step", 0);
                    this.seen = data.optLong("seen_at", -1) != -1;
                    int type = question.getInt("type");
                    this.type = type < 0 ? TYPE_END_SURVEY : type > LAST_TYPE ? LAST_TYPE : type;
                    this.title = question.getString("title");
                    this.subtitle = question.getString("subtitle");
                    this.limit_from = question.optInt("limit_from", -1);
                    this.limit_to = question.optInt("limit_to", -1);
                    this.choices = choices;
                } else {//Fine survey
                    this.step = Integer.MAX_VALUE;
                    this.seen = data.optLong("seen_at", -1) != -1;
                    this.type = TYPE_END_SURVEY;
                    this.title = "";
                    this.subtitle = "";
                    this.limit_from = -1;
                    this.limit_to = -1;
                    this.choices = null;
                }
            } catch (JSONException json) {
                return this;
            }
        }
        return this;
    }

    final int survey_id;
    int limit_from, limit_to, step;
    @SURVEY_TYPE int type;
    @NonNull final String thank_you_text;
    @NonNull String title, subtitle;
    boolean seen, isRejectedOrConcluded = false;
    @Nullable Choice[] choices;
    private XXXIE_Survey(int survey_id, @NonNull String thank_you_text, int step, long seen_at, @SURVEY_TYPE int type, @NonNull String title, @NonNull String subtitle, int limit_from, int limit_to, @Nullable Choice[] choices) {
        this.survey_id = survey_id;
        this.thank_you_text = thank_you_text;
        this.step = step;
        this.seen = seen_at != -1;
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.limit_from = limit_from;
        this.limit_to = limit_to;
        this.choices = choices;
    }

    static class Choice implements Parcelable {
        final int survey_choice_id;
        final String value;
        private Choice(int survey_choice_id, String value) {
            this.survey_choice_id = survey_choice_id;
            this.value = value;
        }
        @Override
        public String toString() {
            return this.value;
        }

        /************************************ PARCELABLE IMPLEMENTATION *******************************/
        private Choice(@NonNull Parcel in) {
            this.survey_choice_id = in.readInt();
            this.value = in.readString();
        }
        @Override public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeInt(this.survey_choice_id);
            dest.writeString(this.value);
        }
        @Override public int describeContents() { return 0; }
        @NonNull public static final Creator<Choice> CREATOR = new Creator<Choice>() {
            @Contract(pure = true) @Override public Choice createFromParcel(@NonNull Parcel in) { return new Choice(in); }
            @Contract(pure = true) @Override public Choice[] newArray(int size) { return new Choice[size]; }
        };
    }
}
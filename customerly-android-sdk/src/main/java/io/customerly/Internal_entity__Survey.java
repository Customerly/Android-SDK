package io.customerly;

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
 * Project: CustomerlySDK
 */
class Internal_entity__Survey {

    @IntDef({ TYPE_END_SURVEY, TYPE_BUTTON, TYPE_RADIO, TYPE_LIST, TYPE_SCALE, TYPE_STAR, TYPE_NUMBER, TYPE_TEXTBOX, TYPE_TEXTAREA })
    @Retention(RetentionPolicy.SOURCE)
    @interface SURVEY_TYPE {}

    static final int TYPE_END_SURVEY = -1;
    static final int TYPE_BUTTON = 0;
    static final int TYPE_RADIO = 1;
    static final int TYPE_LIST = 2;
    static final int TYPE_SCALE = 3;
    static final int TYPE_STAR = 4;
    static final int TYPE_NUMBER = 5;
    static final int TYPE_TEXTBOX = 6;
    static final int TYPE_TEXTAREA = 7;
    private static final int LASTTYPE = TYPE_TEXTAREA;

    @Contract("null -> null")
    @Nullable
    static Internal_entity__Survey[] from(@Nullable JSONArray array) {
        if (array == null)
            return null;
        Internal_entity__Survey[] surveys = new Internal_entity__Survey[array.length()];
        for(int i = 0; i < array.length(); i++) {
            surveys[i] = Internal_entity__Survey.from(array.optJSONObject(i));
        }
        return surveys;
    }

    @Contract("null -> null")
    @Nullable
    static Internal_entity__Survey from(@Nullable JSONObject root) {
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
                return new Internal_entity__Survey(survey.getInt("survey_id"), survey.getString("thankyou_text"), question.optInt("step", 0), root.optLong("seen_at", -1),
                        type < 0 ? TYPE_END_SURVEY : type > LASTTYPE ? LASTTYPE : type, question.getString("title"), question.getString("subtitle"),
                        question.optInt("limit_from", -1), question.optInt("limit_to", -1), choices);
            } else {//Fine survey
                return new Internal_entity__Survey(survey.getInt("survey_id"), survey.getString("thankyou_text"), Integer.MAX_VALUE, root.optLong("seen_at", -1),
                        TYPE_END_SURVEY, "", "", -1, -1, null);
            }
        } catch (JSONException json) {
            return null;
        }
    }

    Internal_entity__Survey updateFrom(@Nullable JSONObject data) {
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
                    this.type = type < 0 ? TYPE_END_SURVEY : type > LASTTYPE ? LASTTYPE : type;
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
    @NonNull final String thankyou_text;
    @NonNull String title, subtitle;
    boolean seen, isRejectedOrConcluded = false;
    @Nullable Choice[] choices;
    private Internal_entity__Survey(int survey_id, @NonNull String thankyou_text, int step, long seen_at, @SURVEY_TYPE int type, @NonNull String title, @NonNull String subtitle, int limit_from, int limit_to, @Nullable Choice[] choices) {
        this.survey_id = survey_id;
        this.thankyou_text = thankyou_text;
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
            @Contract("_ -> !null")
            @Override public Choice createFromParcel(@NonNull Parcel in) { return new Choice(in); }
            @Contract(value = "_ -> !null", pure = true)
            @Override public Choice[] newArray(int size) { return new Choice[size]; }
        };
    }
}
package io.customerly.entity

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

import android.os.Parcelable
import android.support.annotation.IntDef
import io.customerly.utils.ggkext.getTyped
import io.customerly.utils.ggkext.optSequence
import io.customerly.utils.ggkext.optTyped
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */

internal const val TSURVEY_END_SURVEY = -1
internal const val TSURVEY_BUTTON = 0
internal const val TSURVEY_RADIO = 1
internal const val TSURVEY_LIST = 2
internal const val TSURVEY_SCALE = 3
internal const val TSURVEY_STAR = 4
internal const val TSURVEY_NUMBER = 5
internal const val TSURVEY_TEXT_BOX = 6
internal const val TSURVEY_TEXT_AREA = 7
private const val TSURVEY_LAST = TSURVEY_TEXT_AREA


@IntDef(TSURVEY_END_SURVEY, TSURVEY_BUTTON, TSURVEY_RADIO, TSURVEY_LIST, TSURVEY_SCALE, TSURVEY_STAR, TSURVEY_NUMBER, TSURVEY_TEXT_BOX, TSURVEY_TEXT_AREA)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
internal annotation class TSurvey

internal fun JSONObject.parseSurveyChoice() = ClySurveyChoice(id = this.getTyped("survey_choice_id"), value = this.getTyped("value"))

internal fun JSONObject?.parseSurvey() : ClySurvey? {
    return this.parseSurvey { id: Int, thankYouYext: String, step: Int, seen: Boolean, type: Int, title: String, subtitle: String, limitFrom: Int, limitTo: Int, choices: Array<ClySurveyChoice>? ->
        ClySurvey(id = id, thankYouYext = thankYouYext, step = step, seen = seen, type = type, title = title, subtitle = subtitle, limitFrom = limitFrom, limitTo = limitTo, choices = choices)
    }
}

private fun JSONObject?.parseSurvey(use : (id: Int, thankYouYext: String, step: Int, seen: Boolean, type: Int, title: String, subtitle: String, limitFrom: Int, limitTo: Int, choices: Array<ClySurveyChoice>?)->ClySurvey) : ClySurvey? {
    return if(this != null) {
        try {
            val (id, thankYouYext) = this.optTyped<JSONObject>(name = "survey")?.let { s ->
                s.getTyped<Int>(name = "survey_id") to s.getTyped<String>(name = "thankyou_text")
            } ?: 0 to ""
            val seen = this.optTyped("seen_at", -1L) != -1L
            this.optTyped<JSONObject>(name = "question")?.let { q ->
                use(    /* id= */id,
                        /* thankYouYext= */thankYouYext,
                        /* step= */q.optTyped(name = "step", fallback = 0),
                        /* seen= */seen,
                        /* type= */q.getTyped<Int>(name = "type").let {
                    when {
                        it < 0              ->  TSURVEY_END_SURVEY
                        it > TSURVEY_LAST   -> TSURVEY_LAST
                        else                ->  it
                    }
                },
                        /* title= */q.getTyped(name = "title"),
                        /* subtitle= */q.getTyped(name = "subtitle"),
                        /* limitFrom= */q.optTyped(name = "limit_from", fallback = -1),
                        /* limitTo= */q.optTyped(name = "limit_to", fallback = -1),
                        /* choices= */q.optSequence<JSONObject>(name = "choices")?.map { it.parseSurveyChoice() }?.toList()?.toTypedArray() )
            } ?: {
                use(    /* id= */id,
                        /* thankYouYext= */thankYouYext,
                        /* step= */Int.MAX_VALUE,
                        /* seen= */seen,
                        /* type= */TSURVEY_END_SURVEY,
                        /* title= */"",
                        /* subtitle= */"",
                        /* limitFrom= */-1,
                        /* limitTo= */-1,
                        /* choices= */null )
            }()
        } catch (e : JSONException) {
            null
        }
    } else {
        null
    }
}

@Parcelize
internal data class ClySurveyChoice(val id: Int, val value: String) : Parcelable

@Parcelize
internal class ClySurvey internal constructor(
        val id: Int,
        val thankYouYext: String,
        var step: Int = 0,
        var seen: Boolean = false,
        @TSurvey var type: Int,
        var title: String,
        var subtitle: String,
        var limitFrom: Int = 0,
        var limitTo: Int = 0,
        var choices: Array<ClySurveyChoice>?
) : Parcelable {
    internal constructor(
            id: Int,
            thankYouYext: String,
            step: Int = 0,
            seenAt: Long = -1,
            @TSurvey type: Int,
            title: String,
            subtitle: String,
            limitFrom: Int = -1,
            limitTo: Int = -1,
            choices: Array<ClySurveyChoice>?
    ) : this(id = id, thankYouYext = thankYouYext, step = step, seen = seenAt != -1L, type = type, title = title, subtitle = subtitle, limitFrom = limitFrom, limitTo = limitTo, choices = choices)

    internal fun update(from : JSONObject?) : ClySurvey {
        from.parseSurvey { _: Int, _: String, step: Int, seen: Boolean, type: Int, title: String, subtitle: String, limitFrom: Int, limitTo: Int, choices: Array<ClySurveyChoice>? ->
            this.step = Int.MAX_VALUE
            this.seen = seen
            this.type = type
            this.title = title
            this.subtitle = subtitle
            this.limitFrom = limitFrom
            this.limitTo = limitTo
            this.choices = choices
            this
        }
        return this
    }
}
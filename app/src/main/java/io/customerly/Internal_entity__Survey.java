package io.customerly;

import android.support.annotation.Nullable;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;

/**
 * Created by Gianni on 11/09/16.
 * Project: CustomerlySDK
 */
class Internal_entity__Survey {
    @Contract("null -> null")
    @Nullable
    static Internal_entity__Survey[] from(@Nullable JSONArray jsonArray) {
        if (jsonArray == null || jsonArray.length() == 0)
            return null;
        /* TODO gestire tutti i tipi di survey, questo è uno
        {
            "survey":{
               "survey_id":126,
               "thankyou_page":null,
               "thankyou_text":"<p>Grazie messaggio di ringraziamento<\/p>"
            },
            "question":{
               "survey_question_id":215,
               "survey_id":126,
               "step":0,
               "title":"Ciao",
               "subtitle":"sadsad",
               "type":0,
               "limit_from":null,
               "limit_to":null,
               "default_goto_step":1,
               "choices":[
                  {
                     "survey_choice_id":359,
                     "survey_id":126,
                     "step":0,
                     "value":"Choices",
                     "goto_step":-2
                  }
               ]
            }
         }
         */
        return null;
    }
}
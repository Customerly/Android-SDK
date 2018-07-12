package io.customerly.entity.ping

import io.customerly.utils.ggkext.getTyped
import io.customerly.utils.ggkext.nullOnException
import org.json.JSONObject

/**
 * Created by Gianni on 07/07/18.
 * Project: Customerly-KAndroid-SDK
 */
internal fun JSONObject.parseProfilingForm(): ClyProfilingForm? {
    return nullOnException {
        ClyProfilingForm(
                id = it.getTyped(name = "user_profiling_form_id"),
                appStateName = it.getTyped(name = "state_name"))
    }
}

internal data class ClyProfilingForm(val id: Int, val appStateName: String)
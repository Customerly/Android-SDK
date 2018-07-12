package io.customerly.entity.ping

import io.customerly.utils.ggkext.getTyped
import io.customerly.utils.ggkext.nullOnException
import io.customerly.utils.ggkext.optTyped
import org.json.JSONObject

/**
 * Created by Gianni on 07/07/18.
 * Project: Customerly-KAndroid-SDK
 */
internal fun JSONObject.parseUserDataAttributes(): ClyUserDataAttributes? {
    return nullOnException {
        ClyUserDataAttributes(
                data = it.getTyped(name = "data"),
                attributes = it.optTyped(name = "attributes"))
    }
}

internal data class ClyUserDataAttributes(
        val data: JSONObject?,
        val attributes: JSONObject?) {

    fun needFormProfiling(form: ClyProfilingForm)
            = this.data?.isNull(form.appStateName) != false && this.attributes?.isNull(form.appStateName) != false
}
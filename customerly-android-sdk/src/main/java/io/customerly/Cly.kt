package io.customerly

import android.content.SharedPreferences
import io.customerly.XXXXXcancellare.XXXCustomerly
import io.customerly.entity.ClyJwtToken
import io.customerly.entity.ClyPingResponse
import io.customerly.entity.parseJwtToken
import org.json.JSONObject

/**
 * Created by Gianni on 19/04/18.
 * Project: Customerly-KAndroid-SDK
 */

internal fun checkClyConfigured(reportingErrorEnabled : Boolean = true, then: (XXXCustomerly)->Unit) {
    //TODO
    XXXCustomerly.get().takeIf { it._isConfigured(! reportingErrorEnabled) }?.let(then)
}

//TODO Rename to Customerly
object Cly {

    private var preferences: SharedPreferences? = null

    internal var widgetColorHardcoded: Int? = null

    internal var lastPing: ClyPingResponse = ClyPingResponse()

    internal var jwtToken: ClyJwtToken? = null
            private set
    internal fun jwtTokenUpdate(pingResponse: JSONObject) {
        this.jwtToken = pingResponse.parseJwtToken(preferences = this.preferences)
    }
}
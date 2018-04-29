package io.customerly.entity

import android.graphics.Color
import android.support.annotation.ColorInt
import io.customerly.Cly
import io.customerly.utils.COLORINT_DEFAULTWIDGET
import io.customerly.utils.ggkext.optArray
import io.customerly.utils.ggkext.optTyped
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Gianni on 29/04/18.
 * Project: Customerly-KAndroid-SDK
 */

/*TODO
    __PING__next_ping_allowed = root.optLong("next-ping-allowed", 0);
    __SOCKET__connect(root.optJSONObject("websocket"));
 */
internal fun JSONObject.parsePing(): ClyPingResponse {
    return try {
        val minVersion = this.optTyped(name = "min-version-android", fallback = "0.0.0")
        val activeAdmins = this.optArray<JSONObject,ClyAdmin>(name = "active_admins", map = { it.parseAdmin() })
        this.optTyped<JSONObject>(name = "app_config")?.let { appConfig ->
            @ColorInt val widgetColor: Int = Cly.widgetColorHardcoded ?: appConfig.optTyped<String>(name = "widget_color")?.takeIf { it.isNotEmpty() }?.let {
                    when {
                        it.firstOrNull() != '#' -> "#$it"
                        else -> it
                    }
                }?.let {
                    try {
                        Color.parseColor(it)
                    } catch (exception: IllegalArgumentException) {
                        clySendError(errorCode = ERROR_CODE__HTTP_RESPONSE_ERROR, description = "ClyPingResponse:data.apps.app_config.widget_color is an invalid argb color: '$it'", throwable = exception)
                        null
                    }
                } ?: COLORINT_DEFAULTWIDGET

            ClyPingResponse(
                minVersion = minVersion,
                widgetColor = widgetColor,
                widgetBackgroundUrl = appConfig.optTyped(name = "widget_background_url"),
                poweredBy = appConfig.optTyped(name = "powered_by", fallback = 0L) == 1L,
                welcomeMessageUsers = appConfig.optTyped(name = "welcome_message_users"),
                welcomeMessageVisitors = appConfig.optTyped(name = "welcome_message_visitors"),
                activeAdmins = activeAdmins)

        } ?: ClyPingResponse(minVersion = minVersion, activeAdmins = activeAdmins)
    } catch (wrongJson: JSONException) {
        ClyPingResponse()
    }
}
internal class ClyPingResponse(
    internal val minVersion: String = "0.0.0",
    @ColorInt internal val widgetColor: Int = COLORINT_DEFAULTWIDGET,
    internal val widgetBackgroundUrl: String? = null,
    internal val poweredBy: Boolean = true,
    internal val welcomeMessageUsers: String? = null,
    internal val welcomeMessageVisitors:String? = null,
    internal val activeAdmins: Array<ClyAdmin>? = null) {

}
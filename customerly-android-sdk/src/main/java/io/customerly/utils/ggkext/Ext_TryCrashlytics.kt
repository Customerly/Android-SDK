package io.customerly.utils.ggkext

/**
 * Created by Gianni on 16/04/18.
 * Project: Customerly-KAndroid-SDK
 */

private const val CRASHLYTICS_PACKAGE = "com.crashlytics.android.Crashlytics"

internal fun Throwable.tryCrashlyticsLog() {
    try {
        Class.forName(CRASHLYTICS_PACKAGE)
                .getDeclaredMethod("logException", Throwable::class.java)
                .invoke(null, this)
    } catch (ignored: Exception) { }
}

internal fun tryCrashlyticsSetString(key: String, value: String) {
    try {
        Class.forName(CRASHLYTICS_PACKAGE)
                .getDeclaredMethod("setString", String::class.java, String::class.java)
                .invoke(null, key, value)
    } catch (ignored: Exception) { }
}

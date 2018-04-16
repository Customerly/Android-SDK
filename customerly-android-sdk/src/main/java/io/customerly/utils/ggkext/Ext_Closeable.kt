package io.customerly.utils.ggkext

import java.io.Closeable

/**
 * Created by Gianni on 16/04/18.
 * Project: Customerly-KAndroid-SDK
 */
inline fun <T : Closeable?, R> T.useSkipExeption(block: (T) -> R): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        when {
            this == null -> {}
            exception == null -> close()
            else ->
                try {
                    close()
                } catch (closeException: Throwable) {
                    // cause.addSuppressed(closeException) // ignored here
                }
        }
    }
}
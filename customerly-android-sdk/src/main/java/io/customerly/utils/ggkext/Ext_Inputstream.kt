package io.customerly.utils.ggkext

import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URLConnection

/**
 * Created by Gianni on 07/05/18.
 * Project: CustomerlyApp
 */
const val BUFFER_SIZE = 1024
internal fun InputStream.write(on: OutputStream) {
    val bytes = ByteArray(BUFFER_SIZE)
    this.useSkipExeption {
        var count = it.read(bytes, 0, BUFFER_SIZE)
        while (count != -1) {
            on.write(bytes, 0, count)
            count = it.read(bytes, 0, BUFFER_SIZE)
        }
    }
}
/*
internal fun URLConnection.write(on: OutputStream) {
    on.use { BufferedInputStream(this.inputStream).write(on = it) }
}
*/
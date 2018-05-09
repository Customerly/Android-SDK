package io.customerly.utils.ggkext

import java.io.*
import java.net.URLConnection

/**
 * Created by Gianni on 07/05/18.
 * Project: CustomerlyApp
 */
const val BUFFER_SIZE = 1024
fun InputStream.write(on: OutputStream) {
    val bytes = ByteArray(BUFFER_SIZE)
    this.useSkipExeption {
        var count = it.read(bytes, 0, BUFFER_SIZE)
        while (count != -1) {
            on.write(bytes, 0, count)
            count = it.read(bytes, 0, BUFFER_SIZE)
        }
    }
}
fun URLConnection.write(on: OutputStream) {
    on.use { BufferedInputStream(this.inputStream).write(on = it) }
}
fun URLConnection.write(on: File) {
    FileOutputStream(on).use { this.write(on = it) }
}
package io.customerly.utils.network

/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Context
import android.os.SystemClock
import io.customerly.utils.ggkext.asUnsigned
import io.customerly.utils.ggkext.checkConnection
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

//private const val REFERENCE_TIME_OFFSET = 16
private const val ORIGINATE_TIME_OFFSET = 24
private const val RECEIVE_TIME_OFFSET = 32
private const val TRANSMIT_TIME_OFFSET = 40
private const val NTP_PACKET_SIZE = 48

private const val NTP_PORT = 123
private const val NTP_MODE_CLIENT = 3
private const val NTP_VERSION = 3

// Number of seconds between Jan 1, 1900 and Jan 1, 1970
// 70 years plus 17 leap days
private const val OFFSET_1900_TO_1970 = (365L * 70L + 17L) * 24L * 60L * 60L

private val SERVERS = arrayOf(
        "0.pool.ntp.org",
        "1.pool.ntp.org",
        "2.pool.ntp.org",
        "3.pool.ntp.org",
        "0.uk.pool.ntp.org",
        "1.uk.pool.ntp.org",
        "2.uk.pool.ntp.org",
        "3.uk.pool.ntp.org",
        "0.US.pool.ntp.org",
        "1.US.pool.ntp.org",
        "2.US.pool.ntp.org",
        "3.US.pool.ntp.org",
        "asia.pool.ntp.org",
        "europe.pool.ntp.org",
        "north-america.pool.ntp.org",
        "oceania.pool.ntp.org",
        "south-america.pool.ntp.org",
        "africa.pool.ntp.org",
        "time.apple.com")

/**
 *
 * Simple SNTP client class for retrieving network time.
 *
 * Sample usage:
 * <pre>SntpClient client = new SntpClient();
 * if (client.requestTime("time.foo.com")) {
 * long now = client.getNtpTime() + SystemClock.elapsedRealtime() - client.getNtpTimeReference();
 * }
</pre> *
 */
internal object SntpClient {
    /** The time computed from the NTP transaction. */
    private var ntpTime: Long = 0
    /** The reference clock value (value of SystemClock.elapsedRealtime()) corresponding to the NTP time. */
    private var ntpTimeReference: Long = 0
    /** The round trip time of the NTP transaction */
    private var roundTripTime: Long = 0

    /**
     * Sends an SNTP request to the given host and processes the response.
     *
     * @param host host name of the server.
     * @param timeout network timeout in milliseconds.
     * @return true if the transaction was successful.
     */
    private fun requestTime(host: String, timeout: Int = 5000): Boolean {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = timeout
            val buffer = ByteArray(NTP_PACKET_SIZE)
            val request = DatagramPacket(buffer, buffer.size, InetAddress.getByName(host), NTP_PORT)

            // set mode = 3 (client) and version = 3
            // mode is in low 3 bits of first byte
            // version is in bits 3-5 of first byte
            buffer[0] = (NTP_MODE_CLIENT or (NTP_VERSION shl 3)).toByte()

            // get current time and write it to the request packet
            val requestTime = System.currentTimeMillis()
            val requestTicks = SystemClock.elapsedRealtime()
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime)

            socket.send(request)

            // read the response
            socket.receive(DatagramPacket(buffer, buffer.size))
            val responseTicks = SystemClock.elapsedRealtime()
            val responseTime = requestTime + (responseTicks - requestTicks)

            // extract the results
            val originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET)
            val receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET)
            val transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET)
            val roundTripTime = responseTicks - requestTicks - (transmitTime - receiveTime)
            val clockOffset = (receiveTime - originateTime + (transmitTime - responseTime)) / 2

            // save our results - use the times on this side of the network latency
            // (response rather than request time)
            ntpTime = responseTime + clockOffset
            ntpTimeReference = responseTicks
            SntpClient.roundTripTime = roundTripTime
        } catch (e: Exception) {
            //if (false) Log.d(TAG, "request time failed: " + e);
            return false
        } finally {
            if (socket != null) {
                socket.close()
            }
        }

        return true
    }

    /**
     * Reads an unsigned 32 bit big endian number from the given offset in the buffer.
     */
    private fun read32(buffer: ByteArray, offset: Int): Long {
        val i0 = buffer[offset].asUnsigned()
        val i1 = buffer[offset + 1].asUnsigned()
        val i2 = buffer[offset + 2].asUnsigned()
        val i3 = buffer[offset + 3].asUnsigned()
        return (i0.toLong() shl 24) + (i1.toLong() shl 16) + (i2.toLong() shl 8) + i3.toLong()
    }

    /**
     * Reads the NTP time stamp at the given offset in the buffer and returns
     * it as a system time (milliseconds since January 1, 1970).
     */
    private fun readTimeStamp(buffer: ByteArray, offset: Int): Long {
        val seconds = read32(buffer, offset)
        val fraction = read32(buffer, offset + 4)
        return (seconds - OFFSET_1900_TO_1970) * 1000 + fraction * 1000L / 0x100000000L
    }

    /**
     * Writes system time (milliseconds since January 1, 1970) as an NTP time stamp
     * at the given offset in the buffer.
     */
    private fun writeTimeStamp(buffer: ByteArray, offset: Int, time: Long) {
        var offset2 = offset
        var seconds : Long = time / 1000L
        val milliseconds : Long = time - seconds * 1000L
        seconds += OFFSET_1900_TO_1970

        // write seconds in big endian format
        buffer[offset2++] = (seconds shr 24).toByte()
        buffer[offset2++] = (seconds shr 16).toByte()
        buffer[offset2++] = (seconds shr 8).toByte()
        buffer[offset2++] = seconds.toByte()

        val fraction = milliseconds * 0x100000000L / 1000L
        // write fraction in big endian format
        buffer[offset2++] = (fraction shr 24).toByte()
        buffer[offset2++] = (fraction shr 16).toByte()
        buffer[offset2++] = (fraction shr 8).toByte()
        // low order bits should be random data
        buffer[offset2] = (Math.random() * 255.0).toByte()
    }

    private fun getNtpTime() : Long? {
        return if(SERVERS.asSequence().any { requestTime(host = it) }) {
            ntpTime + SystemClock.elapsedRealtime() - ntpTimeReference
        } else {
            null
        }
    }

    internal fun getNtpTimeAsync(context : Context? = null, onTime : (Long?)->Unit) {
        if(context?.checkConnection() == false) {
            onTime(null)
        } else {
            context.doAsync {
                val ntpTime = getNtpTime()
                uiThread {
                    onTime(ntpTime)
                }
            }
        }
    }
}
































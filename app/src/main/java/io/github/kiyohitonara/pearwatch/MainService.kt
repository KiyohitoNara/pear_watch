/*
 * MIT License
 *
 * Copyright 2023 Kiyohito Nara
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.kiyohitonara.pearwatch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import timber.log.Timber

class MainService : Service() {
    companion object {
        const val EXTRA_DEVICE_NAME = "io.github.kiyohitonara.pearwatch.extra.DEVICE_NAME"
        const val EXTRA_DEVICE_ADDRESS = "io.github.kiyohitonara.pearwatch.extra.DEVICE_ADDRESS"

        private const val REQUEST_CODE = 0

        private const val NOTIFICATION_ID = 1

        private const val NOTIFICATION_CHANNEL_ID = "device_connection_channel_01"
    }

    private val binder: IBinder = LocalBinder()

    private var scannedBluetoothDevice: ScannedBluetoothDevice? = null
    private var scannedBluetoothDeviceListener: ScannedBluetoothDeviceListener? = null

    override fun onCreate() {
        super.onCreate()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT))

        Timber.d("Service created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("Service started.")

        val deviceName = intent?.getStringExtra(EXTRA_DEVICE_NAME)
        val deviceAddress = intent?.getStringExtra(EXTRA_DEVICE_ADDRESS)
        if (deviceName == null || deviceAddress == null) {
            Timber.e("Device name or address is null.")

            scannedBluetoothDevice = null
            scannedBluetoothDeviceListener?.onDeviceDisconnected()

            stopForeground(STOP_FOREGROUND_REMOVE)

            return START_NOT_STICKY
        }

        scannedBluetoothDevice = ScannedBluetoothDevice(deviceName, deviceAddress)
        scannedBluetoothDevice?.let {
            scannedBluetoothDeviceListener?.onDeviceConnected(it)
        }

        val notification = buildNotification(deviceName)
        startForeground(NOTIFICATION_ID, notification)

        return START_REDELIVER_INTENT
    }

    private fun buildNotification(deviceName: String): Notification {
        Timber.d("Notification built for device: $deviceName")

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, REQUEST_CODE, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phone_iphone)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.connected_message, deviceName))
            .setContentIntent(pendingIntent)

        return notificationBuilder.build()
    }

    override fun onBind(intent: Intent?): IBinder {
        Timber.d("Service bound.")

        return binder
    }

    fun setScannedBluetoothDeviceListener(listener: ScannedBluetoothDeviceListener?) {
        Timber.d("ScannedBluetoothDeviceListener set.")

        scannedBluetoothDeviceListener = listener
        scannedBluetoothDevice?.let {
            scannedBluetoothDeviceListener?.onDeviceConnected(it)
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.d("Service unbound.")

        scannedBluetoothDeviceListener = null

        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.d("Service destroyed.")
    }

    inner class LocalBinder : Binder() {
        fun getService(): MainService {
            return this@MainService
        }
    }
}
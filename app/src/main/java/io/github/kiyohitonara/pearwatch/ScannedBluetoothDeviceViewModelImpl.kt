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

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import timber.log.Timber

class ScannedBluetoothDeviceViewModelImpl(application: Application) : ScannedBluetoothDeviceViewModel(application) {
    companion object {
        private const val UUID_ANCS = "7905F431-B5CE-4E99-A40F-4B1E122D00D0"
    }

    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothAdapter.bluetoothLeScanner
    }

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Timber.e("Bluetooth permission not granted.")

                    return
                }
            }

            result?.device?.let { device ->
                if (device.name.isNullOrEmpty() || device.address.isNullOrEmpty()) {
                    return
                }

                val scannedBluetoothDevice = ScannedBluetoothDevice(device.name, device.address)
                if (!_devices.value.contains(scannedBluetoothDevice)) {
                    Timber.d("Found device: Name=${device.name}, Address=${device.address}")

                    _devices.value = _devices.value + scannedBluetoothDevice
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)

            Timber.e("Scan failed: errorCode=$errorCode")
        }
    }

    override fun startScan() {
        Timber.d("Bluetooth scan started.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Timber.e("Bluetooth permission not granted.")

                return
            }
        }

        bluetoothLeScanner?.startScan(callback)
    }

    override fun stopScan() {
        Timber.d("Bluetooth scan stopped.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Timber.e("Bluetooth permission not granted.")

                return
            }
        }

        bluetoothLeScanner?.stopScan(callback)
    }
}
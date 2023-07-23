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

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material.scrollAway

class MainActivity : ComponentActivity() {
    private var scannedBluetoothDevice: ScannedBluetoothDevice? by mutableStateOf(null)
    private val scannedBluetoothDeviceViewModel: ScannedBluetoothDeviceViewModelStub by viewModels()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MainService.LocalBinder

            val mainService = binder.getService()
            mainService.setScannedBluetoothDeviceListener(scannedBluetoothDeviceListener)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            scannedBluetoothDevice = null
        }
    }

    private val scannedBluetoothDeviceListener = object : ScannedBluetoothDeviceListener {
        override fun onDeviceConnected(device: ScannedBluetoothDevice) {
            scannedBluetoothDevice = device
        }

        override fun onDeviceDisconnected() {
            scannedBluetoothDevice = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScannedDevicesListScreen(device = scannedBluetoothDevice, viewModel = scannedBluetoothDeviceViewModel)
        }

        lifecycle.addObserver(scannedBluetoothDeviceViewModel)
    }

    override fun onResume() {
        super.onResume()

        val intent = Intent(this, MainService::class.java)
        bindService(intent, connection, Application.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()

        unbindService(connection)
    }
}

@Composable
fun ScannedDevicesListScreen(device: ScannedBluetoothDevice?, viewModel: ScannedBluetoothDeviceViewModel) {
    val context = LocalContext.current

    val scannedDevices by viewModel.devices.collectAsState()
    val listState = rememberScalingLazyListState()

    Scaffold(
        vignette = {
            Vignette(vignettePosition = VignettePosition.TopAndBottom)
        },
        positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        },
        timeText = {
            TimeText(modifier = Modifier.scrollAway(listState))
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            item {
                ListHeader {
                    Text(text = stringResource(id = R.string.devices))
                }
            }

            items(scannedDevices) { scannedDevice ->
                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = scannedDevice.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = scannedDevice.address,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.radioIcon(scannedDevice == device),
                            contentDescription = null
                        )
                    },
                    checked = false,
                    onCheckedChange = {
                        val intent = Intent(context, MainService::class.java)
                        intent.putExtra(MainService.EXTRA_DEVICE_NAME, scannedDevice.name)
                        intent.putExtra(MainService.EXTRA_DEVICE_ADDRESS, scannedDevice.address)
                        context.startService(intent)
                    }
                )
            }

            item {
                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = stringResource(id = R.string.disconnect),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.radioIcon(device == null),
                            contentDescription = null
                        )
                    },
                    checked = false,
                    onCheckedChange = {
                        val intent = Intent(context, MainService::class.java)
                        context.startService(intent)
                    }
                )
            }
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun ScannedDevicesListScreenPreview() {
    val viewModel = ScannedBluetoothDeviceViewModelStub(Application())

    ScannedDevicesListScreen(device = null, viewModel = viewModel)
}
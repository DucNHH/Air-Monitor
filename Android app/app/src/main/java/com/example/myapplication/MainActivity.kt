package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.ui.theme.MyApplicationTheme

const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private lateinit var deviceManager: DeviceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceManager = DeviceManager(this)
        setContent {
            MyApplicationTheme {
                MyApp(deviceManager)
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Shutdown")
        deviceManager.shutdown()
        super.onDestroy()
    }
}

@Composable
fun MyApp(deviceManager: DeviceManager) {
    val openDialog = remember { mutableStateOf(false) }

    Column {
        Box(modifier = Modifier.weight(1f)) {
            ShowListDevice(deviceManager)
        }
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 0.dp, 10.dp, 10.dp)
        ) {
            Button(onClick = { openDialog.value = true }) {
                Text("Add Device")
            }
        }
    }

    if (openDialog.value) {
        val deviceExist = remember { mutableStateOf(false) }
        val text = remember { mutableStateOf("") }
        Dialog(onDismissRequest = { openDialog.value = false }) {
            AlertDialog(
                onDismissRequest = { openDialog.value = false },
                title = { Text("Device ID") },
                text = {
                    Column {
                        TextField(
                            value = text.value,
                            onValueChange = { text.value = it },
                        )
                        if (deviceExist.value) {
                            Text(
                                text = "Device already exists",
                                modifier = Modifier.padding(0.dp, 4.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        Log.d(TAG, "Topic entered: ${text.value}")
                        if (deviceManager.addDevice(text.value)) {
                            openDialog.value = false
                        }
                        else {
                            deviceExist.value = true
                        }
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    Button(onClick = { openDialog.value = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ShowListDevice(deviceManager: DeviceManager) {
    val devices = deviceManager.devices
    LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
        items(devices) { device ->
            val renameDialog = remember { mutableStateOf(false) }
            val deleteDialog = remember { mutableStateOf(false) }
            val changeWifiDialog = remember { mutableStateOf(false) }
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                Column {
                    Row(modifier = Modifier.padding(24.dp, 10.dp, 24.dp, 0.dp)) {
                        Text(device.name)
                    }
                    Row(modifier = Modifier.padding(24.dp, 10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.info.value)
                        }
                        ElevatedButton(onClick = { device.expanded.value = !device.expanded.value }) {
                            Text(if (device.expanded.value) "Show less" else "Show more")
                        }
                    }
                    if (device.expanded.value) {
                        Row(modifier = Modifier.padding(24.dp, 10.dp)) {
                            ElevatedButton(
                                modifier = Modifier.padding(4.dp),
                                onClick = { renameDialog.value = true }) {
                                Text("Rename")
                            }
                            ElevatedButton(
                                modifier = Modifier.padding(4.dp),
                                onClick = { deleteDialog.value = true }) {
                                Text("Delete")
                            }
                            ElevatedButton(
                                modifier = Modifier.padding(4.dp),
                                onClick = { changeWifiDialog.value = true }) {
                                Text("Change wifi")
                            }
                        }
                    }
                }
            }
            if (renameDialog.value) {
                val name = remember { mutableStateOf(device.name) }
                Dialog(onDismissRequest = { renameDialog.value = false }) {
                    AlertDialog(
                        onDismissRequest = { renameDialog.value = false },
                        title = { Text("Rename Device") },
                        text = {
                            TextField(
                                value = name.value,
                                onValueChange = { name.value = it }
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                device.name = name.value
                                renameDialog.value = false
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { renameDialog.value = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
            if (deleteDialog.value) {
                Dialog(onDismissRequest = { deleteDialog.value = false }) {
                    AlertDialog(
                        onDismissRequest = { deleteDialog.value = false },
                        title = { Text("Delete Device") },
                        text = { Text("Are you sure you want to remove this device?") },
                        confirmButton = {
                            Button(onClick = {
                                deviceManager.removeDevice(device.id)
                                deleteDialog.value = false
                            }) {
                                Text("Yes")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { deleteDialog.value = false }) {
                                Text("No")
                            }
                        }
                    )
                }
            }
            if (changeWifiDialog.value) {
                Dialog(onDismissRequest = { changeWifiDialog.value = false }) {
                    val ssid = remember { mutableStateOf("") }
                    val password = remember { mutableStateOf("") }
                    val errorPwd = remember { mutableStateOf(false) }
                    AlertDialog(
                        onDismissRequest = { changeWifiDialog.value = false },
                        title = { Text("Change Wifi") },
                        text = {
                            Column {
                                TextField(
                                    placeholder = { Text("SSID") },
                                    value = ssid.value,
                                    onValueChange = { ssid.value = it }
                                )
                                TextField(
                                    placeholder = { Text("Password") },
                                    value = password.value,
                                    onValueChange = { password.value = it }
                                )
                                if (errorPwd.value) {
                                    Text(
                                        text = "Password must be at least 8 characters long",
                                        modifier = Modifier.padding(0.dp, 4.dp),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (password.value.length >= 8) {
                                    deviceManager.changeWifi(device.id, ssid.value, password.value)
                                    changeWifiDialog.value = false
                                } else {
                                    errorPwd.value = true
                                }
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { changeWifiDialog.value = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
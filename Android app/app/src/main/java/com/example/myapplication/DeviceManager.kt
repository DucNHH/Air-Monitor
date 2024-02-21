package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage

class DeviceManager(context: Context) {
    val devices = mutableStateListOf<Device>()
    private val mqtt = MqttClientHelper(context)
    private val context = mqtt.context

    init {
        mqtt.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                Log.d(MqttClientHelper.TAG, "Connected to $serverURI")
                loadDevices()
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(MqttClientHelper.TAG, "Connection lost")
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                Log.d(MqttClientHelper.TAG, "Message arrived: $message")
                devices.find { it.subTopic == topic }?.setInfo(message.toString())
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(MqttClientHelper.TAG, "Delivery complete")
            }
        })
    }

    fun addDevice(id: String): Boolean {
        if (devices.any { it.id == id }) return false
        val device = Device(id)
        mqtt.subscribe(device.subTopic)
        devices.add(device)
        return true
    }

    fun removeDevice(id: String) {
        val device = devices.find { it.id == id }
        device?.let {
            mqtt.unsubscribe(it.subTopic)
            devices.remove(it)
            deleteDevice(id)
        }
    }

    fun changeWifi(id: String, ssid: String, password: String) {
        val device = devices.find { it.id == id }
        device?.let {
            val gson = Gson()
            val message = gson.toJson(mapOf("ssid" to ssid, "password" to password))
            mqtt.publish(it.pubTopic, message)
        }
    }

    fun shutdown() {
        if (mqtt.isConnected()) {
            mqtt.destroy()
        }
        saveDevices()
    }

    private fun saveDevices() {
        val sharedPref = context.getSharedPreferences("Myapp", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            val gson = Gson()
            devices.forEach {
                val deviceJson = gson.toJson(it)
                putString(it.id, deviceJson)
            }
            apply()
        }
    }

    private fun loadDevices() {
        val sharedPref = context.getSharedPreferences("Myapp", Context.MODE_PRIVATE)
        val gson = GsonBuilder()
            .registerTypeAdapter(Device::class.java, Device.DeviceDeserializer())
            .create()
        sharedPref.all.forEach { (_, value) ->
            val device = gson.fromJson(value as String, Device::class.java)
            devices.add(device)
            mqtt.subscribe(device.subTopic)
        }
    }

    private fun deleteDevice(id: String) {
        val sharedPref = context.getSharedPreferences("Myapp", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove(id)
            apply()
        }
    }
}
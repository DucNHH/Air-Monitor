package com.example.myapplication

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.Timer
import kotlin.concurrent.timerTask

val DISCONNECT_TIMEOUT = 2 * 60 * 1000L

data class Device(val id: String) {
    var name: String = id
    var subTopic: String = "air-quality/$id"
    var pubTopic: String = "air-quality/$id/wifi"
    @Transient
    val info: MutableState<String> = mutableStateOf("Not connected")
    @Transient
    val expanded: MutableState<Boolean> = mutableStateOf(false)
    private var airQuality: String = "Good"
    private var temp: Double = 0.0
    private var humid: Double = 0.0
    private var timer: Timer? = null

    fun setInfo(message: String) {
        timer?.cancel()

        val gson = Gson()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val infor = gson.fromJson<Map<String, Any>>(message, type)

        val ppm = infor["ppm"] as Double
        airQuality = when {
            ppm < 150 -> "Good"
            ppm < 400 -> "Poor"
            else -> "Bad"
        }
        temp = infor["temperature"] as Double
        humid = infor["humidity"] as Double
        info.value = "Air Quality: $airQuality\nTemperature: %.1fºC\nHumidity: %.1f%%".format(temp, humid)

        timer = Timer().apply {
            schedule(timerTask {
                info.value = "Not connected"
            }, DISCONNECT_TIMEOUT) // 2 minutes
        }
    }

    class DeviceDeserializer : JsonDeserializer<Device> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Device {
            val jsonObject = json.asJsonObject
            val id = jsonObject["id"].asString
            val name = jsonObject["name"].asString
            val subTopic = jsonObject["subTopic"].asString
            val pubTopic = jsonObject["pubTopic"].asString
            return Device(id).apply {
                this.name = name
                this.subTopic = subTopic
                this.pubTopic = pubTopic
            }
        }
    }
}
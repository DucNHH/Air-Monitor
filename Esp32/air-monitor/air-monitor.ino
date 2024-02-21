#include <Adafruit_Sensor.h>
#include <DHT.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <Preferences.h>
#include <ArduinoJson.h>

/// Parameters for calculating ppm of CO2 from sensor resistance
#define PARA 116.6020682
#define PARB 2.769034857

/// Parameters to model temperature and humidity dependence
#define CORA .00035
#define CORB .02718
#define CORC 1.39538
#define CORD .0018
#define CORE -.003333333
#define CORF -.001923077
#define CORG 1.130128205

#define MQ135PIN 33
#define DHTPIN 4
#define DHTTYPE DHT11

#define DEVICE_ID "201"

#define SECOND_UNIT 1000
#define MINUTE_UNIT 60 * SECOND_UNIT
#define NEW_WIFI_TIMEOUT 2 * MINUTE_UNIT
#define PUBLISH_DELAY 1 * SECOND_UNIT
#define MQTT_CONNECT_DELAY 5 * SECOND_UNIT
#define WIFI_CONNECT_DELAY 0.5 * SECOND_UNIT

const char *mqtt_server = "broker.emqx.io";
const char *device_id = "201";

float _rzero = 76.63;
float _rload = 10.0;
int count = 0;

DHT dht(DHTPIN, DHTTYPE);
WiFiClient espClient;
PubSubClient client(espClient);
Preferences preferences;

void setup_wifi()
{
    preferences.begin("config", false);
    String ssid = preferences.getString("Wifi_ssid", "DucNHH");
    String password = preferences.getString("Wifi_password", "88888888");
    Serial.printf("Connecting to %s\n", ssid.c_str());
    WiFi.begin(ssid.c_str(), password.c_str());
    while (WiFi.status() != WL_CONNECTED)
    {
        delay(WIFI_CONNECT_DELAY);
    }
}

void reconnect()
{
    while (!client.connected())
    {
        if (client.connect(device_id))
        {
            client.subscribe("air-quality/" DEVICE_ID "/wifi");
        }
        else
        {
            delay(MQTT_CONNECT_DELAY);
        }
    }
}

void callback(char *topic, byte *payload, unsigned int length)
{
    JsonDocument doc;

    // Parse the incoming payload into the JSON document
    DeserializationError error = deserializeJson(doc, payload, length);
    if (error)
    {
        return;
    }

    // Extract the SSID and password
    const char *new_ssid = doc["ssid"];
    const char *new_password = doc["password"];

    // Disconnect from the current network
    WiFi.disconnect();

    // Connect to the new network
    Serial.printf("Connecting to %s\n", new_ssid);
    WiFi.begin(new_ssid, new_password);
    int count = 0;
    while (WiFi.status() != WL_CONNECTED && count < NEW_WIFI_TIMEOUT)
    {
        delay(WIFI_CONNECT_DELAY);
        count += WIFI_CONNECT_DELAY;
    }
    if (count == NEW_WIFI_TIMEOUT)
    {
        // If we couldn't connect to the new network, reconnect to the old one
        String old_ssid = preferences.getString("Wifi_ssid", "DucNHH");
        String old_password = preferences.getString("Wifi_password", "88888888");
        WiFi.disconnect();
        Serial.printf("Connecting to %s\n", old_ssid.c_str());
        WiFi.begin(old_ssid.c_str(), old_password.c_str());
        while (WiFi.status() != WL_CONNECTED)
        {
            delay(WIFI_CONNECT_DELAY);
        }
    }
    else
    {
        // If we could connect to the new network, save the new SSID and password
        preferences.putString("Wifi_ssid", new_ssid);
        preferences.putString("Wifi_password", new_password);
    }
}

void setup()
{
    Serial.begin(9600);
    setup_wifi();
    client.setServer(mqtt_server, 1883);
    client.setCallback(callback);
    dht.begin();
}

void loop()
{
    if (!client.connected())
    {
        reconnect();
    }
    client.loop();

    float humi = dht.readHumidity();
    float temp = dht.readTemperature();
    int ppm = (int)getPPM(temp, humi);

    JsonDocument doc;
    doc["ppm"] = ppm;
    doc["temperature"] = temp;
    doc["humidity"] = humi;

    char msg[150];
    serializeJson(doc, msg);
    client.publish("air-quality/" DEVICE_ID, msg, false);
    delay(PUBLISH_DELAY);
}

// Get the correction factor to correct for temperature and humidity
float getCorrectionFactor(float t, float h)
{
    if (t < 20)
    {
        return CORA * t * t - CORB * t + CORC - (h - 33.) * CORD;
    }
    else
    {
        return CORE * t + CORF * h + CORG;
    }
}

float getPPM(float t, float h)
{
    int val = analogRead(MQ135PIN);
    return PARA * pow((((4095. / (float)val) - 1.) * 3.6 / getCorrectionFactor(t, h)), -PARB);
}
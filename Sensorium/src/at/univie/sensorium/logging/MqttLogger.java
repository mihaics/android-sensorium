package at.univie.sensorium.logging;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;


import java.util.List;

import at.univie.sensorium.SensorRegistry;
import at.univie.sensorium.sensors.AbstractSensor;
import at.univie.sensorium.sensors.SensorChangeListener;
import at.univie.sensorium.sensors.SensorValue;
import mqtt.CloudConfig;

/**
 * Sensorium, at.univie.sensorium.logging
 * Created by mihai on 8/19/15.
 */
public class MqttLogger implements SensorChangeListener {

    private static final String TAG = "MQTT";
    private List<AbstractSensor> sensors;
    private CloudConfig quickconfig;
    private MqttAndroidClient client;
    private Context mContext;
    private String clientaddr = "abcabc";

    public boolean ready;

    public MqttLogger() {

    }

    public void init(List<AbstractSensor> sensors) {
        this.sensors = sensors;
        init();
    }

    private void init() {
        quickconfig = initPrefsWithIBMQuickStart();
        mContext = SensorRegistry.getInstance().getContext();



        for (AbstractSensor sensor : sensors) {
            sensor.addListener(this);
        }

        ready = connect();
    }

    @Override
    public void sensorUpdated(AbstractSensor sensor) {
        if (client.isConnected()) {
            sendSensorData(sensor);
        }
        else connect();
    }

    public boolean connect() {
        String url = quickconfig.brokerAddress + ":" + quickconfig.brokerPort;

        client = createClient(mContext, url, quickconfig.deviceId+clientaddr);

        Log.d(TAG, "Cloud Broker URL : " + client.getServerURI());
        Log.d(TAG, "Client ID: "+client.getClientId());
        MqttConnectOptions options = null;

        try {
            client.connect(options, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    Log.d(TAG, "Connected to cloud : " + client.getServerURI() + "," + client.getClientId());

                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Log.d(TAG, "Connection to IBM cloud failed !");
                    Log.d(TAG, "Error: " + throwable.getLocalizedMessage());
                    ready = false;
                }
            });
        }catch (MqttException e) {

            Log.d(TAG, e.toString());
            return false;
        }

        return true;

    }





    public boolean disconnect() {

        try {
            if (client != null) {
                Log.d(TAG, "Disconnecting from cloud : " + client.getServerURI() + "," + client.getClientId());
                if (client.isConnected()) client.disconnect();
                client.unregisterResources();
                client = null;

            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }


    private void sendSensorData(AbstractSensor sensor) {

        if(!client.isConnected()) return;

        List<SensorValue> valuelist = sensor.getSensorValues();
        //check valuelist size
        String json = new Gson().toJson(valuelist);

        try {
            client.publish(quickconfig.publishTopic, json.getBytes(), 0, false);
            Log.d(TAG, "Published: "+json.toString());
        } catch (MqttException e) {
            Log.d(TAG, e.toString());
        }


    }


    public CloudConfig initPrefsWithIBMQuickStart() {
        CloudConfig ibmconfig = CloudConfig.getInstance();
        ibmconfig.brokerAddress = "tcp://quickstart.messaging.internetofthings.ibmcloud.com";
        ibmconfig.brokerPort = 1883;
        //get an uniq id
        ibmconfig.deviceId = "d:quickstart:\"st-app\":";
        ibmconfig.password = "";
        ibmconfig.username = "";
        ibmconfig.publishTopic = "iot-2/evt/status/fmt/json";
        ibmconfig.service = 0;
        ibmconfig.useSSL = false;
        ibmconfig.cleanSession = true;

        return ibmconfig;
    }


    /**
     * Create a fully initialised <code>MqttAndroidClient</code> for the parameters given
     *
     * @param context   The Applications context
     * @param serverURI The ServerURI to connect to
     * @param clientId  The clientId for this client
     * @return new instance of MqttAndroidClient
     */
    public MqttAndroidClient createClient(Context context, String serverURI, String clientId) {
        MqttAndroidClient client = new MqttAndroidClient(context, serverURI, clientId);
        return client;
    }


}

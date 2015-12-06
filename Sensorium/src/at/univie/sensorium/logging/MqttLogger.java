package at.univie.sensorium.logging;

import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Iterator;
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
    public boolean ready;
    private List<AbstractSensor> sensors;
    private CloudConfig quickconfig;
    private MqttAndroidClient client;
    private Context mContext;
    private String android_id = "abcabc";

    public MqttLogger() {

    }

    public void init(List<AbstractSensor> sensors) {
        this.sensors = sensors;
        init();
    }

    private void init() {
        //quickconfig = initPrefsWithIBMQuickStart();
        quickconfig = initPrefsWithMosquitto();
        mContext = SensorRegistry.getInstance().getContext();
        android_id = Secure.getString(mContext.getContentResolver(),
                Secure.ANDROID_ID);


        if (!ready)
            ready = connect();

        for (AbstractSensor sensor : sensors) {
            sensor.addListener(this);
        }
    }

    @Override
    public void sensorUpdated(AbstractSensor sensor) {
        if (client.isConnected()) {
            sendSensorData(sensor);
        } else connect();
    }

    public boolean connect() {
        String url = quickconfig.brokerAddress + ":" + quickconfig.brokerPort;

        client = createClient(mContext, url, quickconfig.deviceId + android_id);


        Log.d(TAG, "Cloud Broker URL : " + client.getServerURI());
        Log.d(TAG, "Client ID: " + client.getClientId());
        //MqttConnectOptions options = null;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setPassword(quickconfig.password.toCharArray());
        options.setCleanSession(quickconfig.cleanSession);
        options.setUserName(quickconfig.username);
        String uris[] = {url};
        options.setServerURIs(uris);


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
        } catch (MqttException e) {

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

        if (!client.isConnected()) return;

        List<SensorValue> valuelist = sensor.getSensorValues();
        Iterator<SensorValue> it = valuelist.iterator();
        //Log.d(TAG, "Element: " + "ANDROID_ID" + ":" + android_id);

        JsonObject element = new JsonObject();
        element.addProperty("ANDROID_ID", android_id);
        element.addProperty("SENSOR", sensor.getName());

        while (it.hasNext()) {
            SensorValue sv = it.next();
            SensorValue.TYPE type = sv.getType();
            String value = sv.getValueRepresentation();
            if(value.matches("\\d+(?:\\.\\d+)?")) {
                element.addProperty(type.toString(), Double.parseDouble(value));
            }
            else
            {
                element.addProperty(type.toString(), value);
            }
            //element.addProperty(type.toString(), value.toString());
           // Log.d(TAG, "Element: " + type + ":" + value);

            }







        //check valuelist size
        String json = new Gson().toJson(element);

        try {
            client.publish(quickconfig.publishTopic + android_id, json.getBytes(), 0, false);
            Log.d(TAG, "Published: " + json.toString());
        } catch (MqttException e) {
            Log.d(TAG, e.toString());
        }


    }

    public CloudConfig initPrefsWithIBMFoundation() {


        CloudConfig ibmconfig = CloudConfig.getInstance();
        ibmconfig.brokerAddress = "tcp://abnw49.messaging.internetofthings.ibmcloud.com";
        ibmconfig.brokerPort = 1883;
        //get an uniq id
        ibmconfig.deviceId = "d:abnw49:\"Sensorium\":";
        ibmconfig.password = "?bX7rrJI*JhCie!@(0";
        ibmconfig.username = "use-token-auth";
        ibmconfig.publishTopic = "iot-2/evt/status/fmt/json";
        ibmconfig.service = 0;
        ibmconfig.useSSL = false;
        ibmconfig.cleanSession = true;

        return ibmconfig;
    }

    public CloudConfig initPrefsWithMosquitto() {


        CloudConfig ibmconfig = CloudConfig.getInstance();
        ibmconfig.brokerAddress = "tcp://sysop.go.ro";
        ibmconfig.brokerPort = 1883;
        //get an uniq id
        ibmconfig.deviceId = "d:Sensorium:";
        ibmconfig.password = "sysop";
        ibmconfig.username = "device";
        ibmconfig.publishTopic = "iot-2/evt/status/";
        ibmconfig.service = 0;
        ibmconfig.useSSL = false;
        ibmconfig.cleanSession = false;

        return ibmconfig;
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
        ibmconfig.cleanSession = false;

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

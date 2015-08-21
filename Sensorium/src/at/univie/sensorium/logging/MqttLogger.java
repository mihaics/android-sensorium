package at.univie.sensorium.logging;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;


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

    private static final String TAG = "MQTT" ;
    private List<AbstractSensor> sensors;
    private CloudConfig quickconfig;
    private MqttAndroidClient client;
    private Context  mContext;
    boolean doConnect = true;

    public MqttLogger(){

    }

    public void init(List<AbstractSensor> sensors) {
        this.sensors = sensors;
        init();
    }
    private void init() {
        quickconfig = initPrefsWithIBMQuickStart();
        mContext = SensorRegistry.getInstance().getContext();
        client = createClient(mContext, quickconfig.brokerAddress + ":" + quickconfig.brokerPort, quickconfig.deviceId);




        for (AbstractSensor sensor : sensors) {
            sensor.addListener(this);
        }
    }

    @Override
    public void sensorUpdated(AbstractSensor sensor) {
        sendSensorData(sensor);

    }

    private void sendSensorData(AbstractSensor sensor){
        List<SensorValue> valuelist = sensor.getSensorValues();
        //check valuelist size
        String json = new Gson().toJson(valuelist );
        Log.d(TAG, json.toString());




    }


    public CloudConfig initPrefsWithIBMQuickStart(){
       CloudConfig ibmconfig = CloudConfig.getInstance();
        ibmconfig.brokerAddress = "tcp://quickstart.messaging.internetofthings.ibmcloud.com";
        ibmconfig.brokerPort = 1883;
        //get an uniq id
        ibmconfig.deviceId = "d:quickstart:\"st-app\":abcabc";
        ibmconfig.password = "";
        ibmconfig.username = "";
        ibmconfig.publishTopic = "iot-2/evt/status/fmt/json";
        ibmconfig.brokerAddress = "tcp://quickstart.messaging.internetofthings.ibmcloud.com";
        ibmconfig.service = 1;
        ibmconfig.useSSL = false;
        ibmconfig.cleanSession = true;

       return ibmconfig;
    }


    /**
     * Create a fully initialised <code>MqttAndroidClient</code> for the parameters given
     * @param context The Applications context
     * @param serverURI The ServerURI to connect to
     * @param clientId The clientId for this client
     * @return new instance of MqttAndroidClient
     */
    public MqttAndroidClient createClient(Context context, String serverURI, String clientId)
    {
        MqttAndroidClient client = new MqttAndroidClient(context, serverURI, clientId);
        return client;
    }


}

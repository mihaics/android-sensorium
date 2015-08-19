package mqtt;

/**
 * Sensorium, mqtt
 * Created by mihai on 8/19/15.
 */
public class CloudConfig {
    private static CloudConfig ourInstance = new CloudConfig();

    public Integer service;
    public String username;
    public String password;
    public String deviceId;
    public String brokerAddress;
    public int brokerPort;
    public String publishTopic;
    public boolean cleanSession;
    public boolean useSSL;



    public static CloudConfig getInstance() {
        return ourInstance;
    }

    private CloudConfig() {
    }


    @Override
    public String toString() {
        String s = new String();
        s = "Cloud configuration :\r\n";
        s += "Service : " + service + "\r\n";
        s += "Username : " + username + "\r\n";
        s += "Password : " + password + "\r\n";
        s += "Device ID : " + deviceId + "\r\n";
        s += "Broker Address : " + brokerAddress + "\r\n";
        s += "Proker Port : " + brokerPort + "\r\n";
        s += "Publish Topic : " + publishTopic + "\r\n";
        s += "Clean Session : " + cleanSession + "\r\n";
        s += "Use SSL : " + useSSL + "\r\n";
        return s;
    }


}

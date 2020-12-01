package org.rnn.implementation;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.rnn.AppComponent;
import org.rnn.Identificators.DeviceIdReal;
import org.rnn.Identificators.Identificator;
import org.rnn.json.*;
import org.rnn.Daemon;
import org.rnn.DaemonForwarder;
import org.rnn.NetState;
import org.rnn.UnifiedCommunicationModule;
import org.rnn.service.BasicServiceEndpoint;
import org.rnn.service.IServiceEndpoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.nio.file.FileSystemException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Scanner;

/**
 * @author Piotr Frohlich
 * @version 1.0.1
 * This is a manager for retrieving the information from the config data.
 */
public class ConfigParser
{
    /**
     * String representing the Configuration field in the config file
     */
    private static String CONFIGURATION = "config";
    /**
     * String representing the port field in the config file
     */
    private static String PORT = "port";
    /**
     * String representing the port field value set to local (ovs internal port flag)
     *  in the config file
     */
    private static String LOCAL = "LOCAL";
    /**
     * String representing the device id field in the config file
     */
    private static String DEVICE = "device id";
    /**
     * String representing the ip address field in the config file
     */
    private static String IP_ADDRESS = "ip";
    /**
     * MASTER class providing communication between ONOS internal data and plugin.
     */
    public static UnifiedCommunicationModule MASTER;
    /**
     * First path to the config file.
     */
    private static String CONFIG_PATH = "~/.onos-config/config.json";
    /**
     * Holder for the configurational string (gathered from config data).
     */
    public static String configString = "";

    /**
     * Basic configuration (for the daemons)
     * @return An array with daemons (configured to process and trigger Cognitive
     * Packets).
     */
    static public ArrayList<Daemon> getDaemons() throws FileNotFoundException,FileSystemException
    {
        ArrayList<Daemon> daemons = new ArrayList<Daemon>();
        String configJson = "";

        try
        {
            Scanner scanner = new Scanner(new File(CONFIG_PATH));
            configJson = scanner.useDelimiter("\\A").next();
            scanner.close();
        }
        catch (Exception e)
        {
            UnifiedCommunicationModule.log.error(e.getMessage());
            try {
                Scanner scanner = new Scanner(new File("/home/pfrohlich/.onos-config/config.json"));
                configJson = scanner.useDelimiter("\\A").next();
                scanner.close();
            }
            catch (Exception e2)
            {
                UnifiedCommunicationModule.log.error(e2.getMessage());
                try {
                    Scanner scanner = new Scanner(new File("/home/seriot/.onos-config/config.json"));
                    configJson = scanner.useDelimiter("\\A").next();
                    scanner.close();
                }
                catch (Exception e1)
                {
                    UnifiedCommunicationModule.log.error(e1.getMessage());
                }
            }
        }
        if(configString.equals("")==false)
        {
            configJson = configString;
        }
        if(configJson.equals("ERROR"))
        {
            UnifiedCommunicationModule.log.error("Unable to locate file in : "+CONFIG_PATH);
            throw new FileNotFoundException("Config file "+CONFIG_PATH+" was not found. Waiting for config file provided via the REST interface");
        }
        else
        {
            configString = configJson;
            daemons = getDaemonsFromString();
        }
        return  daemons;
    }

    public static ArrayList<Daemon> getDaemonsFromString() throws FileSystemException
    {
        ArrayList<Daemon> daemons = new ArrayList<>();
        try {
            JSONObject tmp = new JSONObject(configString);

            try {
                NetState.VISUALISATION_IP_ADDRESS = tmp.getString("visualisation ip");
            }
            catch (Exception e){}
            try
            {
                NetState.BLOCKCHAIN_NODE_IP = tmp.getString("blockchain ip");
            }
            catch (Exception e){}

            JSONArray config = tmp.getJSONArray(CONFIGURATION);
            for (int i = 0; i < config.length(); i++) {
                JSONObject daemon = (JSONObject) config.get(i);
                Daemon instance;
                if (daemon.getString(PORT).equals(LOCAL)) {

                    Identificator deviceId = new DeviceIdReal(DeviceId.deviceId(daemon.getString("device id")), AppComponent.getSimpleIDByControllerID(DeviceId.deviceId(daemon.getString("device id"))));
                    String ipAddress = daemon.getString(IP_ADDRESS);
                    MASTER.log.info("NO PROBLEM " + deviceId.toString());
                    instance = new DaemonForwarder(deviceId, ipAddress, -1);
                    daemons.add(instance);

                } else
                {
                    String port = "";
                    Identificator deviceId =new DeviceIdReal(DeviceId.deviceId(daemon.getString("device id")), AppComponent.getSimpleIDByControllerID(DeviceId.deviceId(daemon.getString("device id"))));
                    String ipAddress = daemon.getString(IP_ADDRESS);
                    port = daemon.getString(PORT);
                    long portNumber = Long.parseLong(port);
                    MASTER.log.info("NO PROBLEM " + deviceId.toString());
                    instance = new DaemonForwarder(deviceId, ipAddress, portNumber);
                    daemons.add(instance);

                }
            }
            NetState.CONFIGURATION_PROVIDED = true;
        }
        catch(Exception e)
        {
            String str = e.getLocalizedMessage();
            UnifiedCommunicationModule.log.error(str);
            NetState.CONFIGURATION_PROVIDED = false;
            throw new FileSystemException("File corrupted. "+e.getMessage());

        }
        return daemons;

    }
    /**
     * This method sets every field configured in the configuration file (if not
     * set every parameter will be set to the default value).
     */
    public static void setupParametersOfNetState() throws ParseException
    {
        if(configString.equals(""))
        {
            return;
        }
        JSONArray array = new JSONArray();
        try {
            JSONObject tmp = new JSONObject(configString);
            JSONObject obj2 = tmp.getJSONObject("parameters");
            array = obj2.getJSONArray("list");
        }
        catch (Exception e)
        {
            throw new ParseException("Unable to parse parameters",1);
        }
        for (int i = 0; i <array.length() ; i++)
        {
            JSONObject param = (JSONObject)array.get(i);

            try
            {
                Field[] fields = NetState.class.getDeclaredFields();
                for (int j = 0; j <fields.length; j++)
                {
                    Object obj = null;
                    try {
                        obj = fields[j].get(Class.forName("org.rnn.NetState"));
                    }
                    catch (Exception e){}
                    if(param!= null)
                    {
                        try {
                            if (fields[j].getAnnotatedType().getType() == Double.TYPE) {
                                fields[j].setDouble(obj, param.getDouble(fields[j].getName()));
                            }
                        }
                        catch (Exception ex1){}
                        try {
                            if (fields[j].getAnnotatedType().getType() == Integer.TYPE) {

                                fields[j].setInt(obj, param.getInt(fields[j].getName()));
                            }
                        }
                        catch (Exception ex2){}
                        try
                        {
                            fields[j].set(obj, param.getString(fields[j].getName()));
                        }
                        catch(Exception e)
                        {
                        }
                    }
                }
            }
            catch (Exception e)
            {
            }
        }
    }

    /**
     * This method gets a hashtable for service and the parameters (such as ip,
     * port and device id) of the device on which such service can be started.
     * @param name Name of the service to get configuration for
     * @return Dictionary of String representation of device id (as key) and
     * instance of class implementing the IServiceEndpoint interface.
     */
    public static Dictionary<String, IServiceEndpoint> getServiceHashTable(String name) throws FileNotFoundException
    {
        String configJson = "";

        Dictionary toReturn = new Hashtable();

        try
        {
            Scanner scanner = new Scanner(new File("/home/pfrohlich/.onos-config/"+name+".json"));
            configJson = scanner.useDelimiter("\\A").next();
            scanner.close();
        }
        catch (Exception e)
        {
            UnifiedCommunicationModule.log.error(name+" SERVICE NOT FOUND");
            throw new FileNotFoundException("Config file for "+name+" service was not found");
        }
        try
        {
            JSONObject tmp = new JSONObject(configJson);
            JSONArray deviceServiceMap = tmp.getJSONArray("device service map");
            for (int i = 0; i < deviceServiceMap.length(); i++)
            {
                JSONObject item = (JSONObject)deviceServiceMap.get(i);
                DeviceIdReal realDevice = new DeviceIdReal(DeviceId.deviceId(item.getString("device id")),item.hashCode());
                BasicServiceEndpoint endpoint = new BasicServiceEndpoint();
                String[] serviceIpSplitted = item.getString("service ip").split(":");

                endpoint.setIpAddress(IpAddress.valueOf(serviceIpSplitted[0]));
                if(serviceIpSplitted.length > 1)
                {
                    endpoint.setPortNumber(PortNumber.portNumber(Long.parseLong(serviceIpSplitted[1])));
                }
                endpoint.setRealIpAddress(IpAddress.valueOf(item.getString("management")));
                toReturn.put(realDevice.toString(),endpoint);
            }
        }
        catch (Exception e)
        {
            UnifiedCommunicationModule.log.error(e.getMessage());
        }
        return toReturn;
    }
}

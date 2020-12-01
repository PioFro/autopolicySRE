package org.rnn;

import org.onlab.packet.IpAddress;
import org.onosproject.net.PortNumber;
import org.rnn.Identificators.Identificator;
import org.rnn.json.JSONArray;
import org.rnn.json.JSONObject;
import org.rnn.policy.Criteria;
import org.rnn.policy.Property;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Data on Network State and parameters aggregator.
 * <p>This class stores all data needed for Routing Module. Also it can be called
 * a static class - for other classes to use its resources. This class ensures
 * proper data flow and cohesion.
 *
 * @author Piotr Fr&ouml;hlich
 * @version Beta 2.0.0
 *
 *
 */
public class NetState {
    /**
     * Network topology
     */
    private static ArrayList<ForwardingDevice> topology = new ArrayList<ForwardingDevice>();
    /**
     * Application ID given by ONOS
     */
    private static String appId;
    /**
     * Array of security ratings for hosts in network
     */
    private static ArrayList<SecureHost> secureHosts = new ArrayList<SecureHost>();
    /**
     * Array of all daemons present in this topology;
     */
    private static ArrayList<Daemon> daemons = new ArrayList<Daemon>();
    /**
     * Default security value for hosts
     */
    public static int DEFAULT_SECURITY = 4;
    /**
     * Default sensitivity value for ForwardingDevices
     */
    public static int DEFAULT_SENSITIVITY = 10;
    /**
     * Default number of packets taken into consideration in DelayAggregator
     * average.
     */
    public static int DEFAULT_HISTORICAL_DELAY_PARAMETER = 20;
    /**
     * Default learning parameter of neural network
     */
    public static double DEFAULT_HISTORICAL_LEARNING_PARAMETER = 0.4;
    /**
     * Flag on if params were changed or not. For update purposes.
     */
    public static boolean paramsChanged = false;
    /**
     * Default priority of installed Flow rule concerning Cognitive Packets
     */
    public static int DEFAULT_SMART_PACKET_PRIORITY = 50000;
    /**
     * Default priority of installed Flow rule concerning traffic packets
     */
    public static int DEFAULT_NORMAL_PACKET_PRIORITY = 49999;
    /**
     * Default priority of installed Flow rule concerning Discovery Cognitive Packets
     */
    public static int DEFAULT_DISCOVERY_PACKET_PRIORITY = 50001;
    /**
     * Default priority of installed Flow rule concerning Mitigation rules
     */
    public static int DEFAULT_BLOCK_PRIORITY = 52002;
    /**
     * Parameter concerning verbosity of the log - 0 means lowest verbosity,
     * 4 - means highest verbosity;
     */
    public static int LOG_VERBOSITY = 10;
    /**
     * Default Discovery flow hard timeout
     */
    public static int DEFAULT_DISCOVERY_FLOW_TIMEOUT = 4;

    public static int DEFAULT_SERVICE_FLOW_PRIORITY= 50033;

    /**
     * Default score for node
     */
    public static double DEFAULT_SCORE_VALUE = 0.3;
    /**
     * Threshold value of difference between paths for being changed
     */
    public static double TRESHOLD_OF_SCORE = 0.01;
    /**
     * Default path installation time value
     */
    public static int DEFAULT_PATH_INSTALATION_TIME = 100000;
    /**
     * Max interval between sending SP packet and receiving it in destination node.
     */
    public static int MAX_SMART_PACKET_TIMEOUT = 3000;//
    /**
     * Default punishment for lost packet.
     */
    public static double DEFAULT_PUNISHMENT = 0.5;
    /**
     * Max buffor size for pending packets
     */
    public static int MAX_BUFFOR_SIZE = 300;
    /**
     * Default flow install interval in millis.
     */
    public static int DEFAULT_FLOW_INSTALL_INTERVAL = 1;
    /**
     * Emergency score drop value
     */
    public static double DEFAULT_TRESHOLD = 0.1;
    /**
    * Daemon IP prefix
    */
    protected static String DEFAULT_IP_PREFIX = "192.168.133.";
    /**
     * Address IP of honeypot available in network;
     */
    public static String HONEYPOT_IP = "192.168.133.101";
    /**
     *  IP address of visualisation tool web server.
     */
    public static String VISUALISATION_IP_ADDRESS = "192.168.2.110";

    public static String RSU_1_IP = "192.168.177.15";
    public static String RSU_2_IP= "192.168.177.16";
    public static String CAR_IP ="192.168.177.17";

    public static String BLOCKCHAIN_NODE_IP = "10.0.1.77";
    public static String BLOCKCHAIN_USER_ID = "iitis";

    public static long DEFAULT_MAX_BYTES_THROUGHPUT = 100000;

    public static long DEFAULT_DELTA_BYTES_FOR_FLOW = 1000;

    public static long DEFAULT_TIME_REFRESHING_BYTES_COUNTERS = 1000000;

    public static long lastTimeUpdatedFlowBytesCounters = 0;

    public static String IP_PREFIX_DAEMON = "169.24.246.";

    public static int RANDOMIZE_AFTER_NUMBER_OF_CP = 10;

    public static int DEFAULT_MAX_BYTES_PERCENTAGE = 70;

    public static ArrayList<IpAddress> BLACKLIST= new ArrayList<>();

    /**
     * Parameter of type of data used to learn rnns.
     * To use mean delay to learn the RNN use 0 as a parameter. Otherwise use 1.
     */
    public static int LEARNING_DATA = 0;
    /**
     * Value of revisiting packets creating flow rules. In [ms]
     */
    public static int MULTI_ROUTING_INFO_DEFAULT_TIMEOUT = 5000;

    /**
     * Variable config of used path translator algorithm -
     * 0 -> top with threshold,
     * 1 -> top with threshold with threshold of maximum bytes send via one device,
     * 2 -> full curve of delay progression
     */
    public static int PATH_TRANSLATOR_ALGORITHM = 0;
    public static PathFormat PATH_FORMAT = PathFormat.BASIC;

    public static boolean CONFIGURATION_PROVIDED = false;
    /**
    * List of unused vlans
    */
    protected static ArrayList<Short> restrictedVlans =  new ArrayList<Short>();
    /**
     * RoutingModule delegate
     */
    protected static RoutingModule routingModule;
    /**
     * Semaphore for parameters being set
     */
    public static boolean setupComplete = false;
    public static boolean packetLoss = false;

    public static double DEFAULT_THRESHOLD_OF_SERVICE = 0.95;

    protected static ArrayList<Property> DEFAULT_DEVICE_PROPERITES = new ArrayList<>();

    protected static ArrayList<Property> DEFAULT_LINK_PROPERTIES = new ArrayList<>();


    public static CognitivePacketNotation COGNITIVE_PACKET_NOTATION = CognitivePacketNotation.PATH_ORIENTED;
    public static AppComponent MASTER;

    /**
     * Once every AP_PROFILE_TIMER milliseconds each profile will be checked
     */
    public static long AP_PROFILE_TIMER = 100000;

    public static int AP_PROFILE_PRIOROTY = 55555;

    public static int AP_NOT_IN_PROFILE = 55554;

    public static float AP_DEFAULT_MAX_BITRATE = 0.1f;

    //ms
    public static int CHECK_FLOWS_TIME = 5000;

    public static String AP_PBF_VISUALISATION_IP = "http://localhost:1111/";

    public static String AP_DB_PATH = "/home/seriot/.apdb/";

    /**
     * 24 H
     */
    public static long AP_PROFILE_BUILD_TIME = 86400000;

    public static int MAX_ENERGY_MEASUREMENTS = 100;

    private static Dictionary<String,CurveManager> curves= new Hashtable<>();

    public enum PathFormat
    {
        BASIC,
        HOST_SPECIFIC
    }
    public enum  CognitivePacketNotation
    {
        PATH_ORIENTED,LINK_ORIENTED
    }

    public static boolean policyBasedRouting = true;

    /**
     * Setter for topology.
     *
     * @param topo topology
     */
    public static void setTopology(ArrayList<ForwardingDevice> topo)
    {
        NetState.topology = topo;

        for (int i = 0; i <topology.size() ; i++)
        {
            ArrayList<Daemon> forwardingDeviceDaemons = new ArrayList<Daemon>();

            for (int j = 0; j < NetState.getDaemons().size(); j++)
            {
                if(NetState.getDaemons().get(j).getDeviceId().equals(topology.get(i).getDeviceControllerId()))
                {
                    Daemon daemon = NetState.getDaemons().get(j);

                    forwardingDeviceDaemons.add(daemon);
                }
            }
            topology.get(i).setDaemons(forwardingDeviceDaemons);
            UnifiedCommunicationModule.log.info(forwardingDeviceDaemons.size()+" DAEMONS SET ON "+topology.get(i).getDeviceControllerId().toString());
        }
        restrictedVlans.add((short)134);


    }

    /**
     * Seter for default security parameter
     *
     * @param security Vaule of new default security parameter
     */
    public static void setDefaultSecurity(int security) {
        DEFAULT_SECURITY = security;
    }

    /**
     * Setter for Application ID
     *
     * @param appID Application ID
     */
    public static void setAppId(String appID) {
        NetState.appId = appID;
    }

    /**
     * Getter
     *
     * @param sID Given Simple ID
     * @return Forwarding Device which corresponds to given simple ID
     */
    public static int getForwardingDeviceBySimpleID(int sID) {
        for (int i = 0; i < topology.size(); i++) {
            if (topology.get(i).getSimpleID() == sID)
                return i;
        }
        return -1;
    }

    /**
     * Gets given ForwardingDevice index by full ID of this device
     *
     * @param deviceId Full id of given device
     * @return index of this device in NetState internal list
     */
    public static int getForwardingDeviceByDeviceID(Identificator deviceId) {
        int i = 0;
        for (ForwardingDevice fd : topology) {
            if (fd.getDeviceControllerId().equals(deviceId))
                return i;
            i++;
        }
        return -1;
    }

    /**
     * Setter for secure hosts whole array
     *
     * @param hosts array of secure hosts
     */
    public static void setSecureHosts(ArrayList<SecureHost> hosts) {
        secureHosts = hosts;
    }

    /**
     * Getter for whole array of secure hosts
     *
     * @return array of secure host
     */
    public static ArrayList<SecureHost> getSecureHosts() {
        return secureHosts;
    }

    /**
     * Getter
     *
     * @param Id given simple ID
     * @return Rating for host corresponding to given simple ID or DEFAULT_SECURITY
     * if there is no record of such host.
     */
    public static int getRatingForHost(Identificator Id) {
        for (SecureHost host : secureHosts) {
            if (host.getHostId().equals(Id)) {
                return host.getSecurityLevel();
            }
        }
        return DEFAULT_SECURITY;
    }

    public static int getHigherRatingBetweenTwoHosts(Identificator id, Identificator id2)
    {
        int danger1=DEFAULT_SECURITY, danger2 =DEFAULT_SECURITY;
        for (SecureHost host : secureHosts) {
            if (host.getHostId().equals(id))
            {
                danger1 = host.getSecurityLevel();
            }
            if(host.getHostId().equals(id2))
            {
                danger2 = host.getSecurityLevel();
            }
        }
        if(danger1<danger2)
            return danger2;
        else
            return danger1;
    }

    /**
     * Adds secure host to secure hosts array.
     *
     * @param host host to Add
     */
    public static void addSecureHost(SecureHost host, PortNumber p, byte proto, String type, int value) {
        boolean found = false;
        for (int i = 0; i < secureHosts.size(); i++) {
            if (host.getHostId().equals(secureHosts.get(i).getHostId()))
            {
                secureHosts.get(i).setSecurityLevel(host.getSecurityLevel());
                secureHosts.get(i).addTriplet(p,proto, type, value);
                found = true;
                break;
            }
        }
        if(!found) {
            secureHosts.add(host);
        }
        try {
            routingModule.receiveNewSecurityScoring(host);
        }
        catch (Exception e){}
    }

    /**
     * Getter for default security
     *
     * @return Default security
     */
    public static int getDefaultSecurity() {
        return DEFAULT_SECURITY;
    }

    /**
     * Getter for application id
     *
     * @return application id
     */
    public static String getAppId() {
        return appId;
    }

    /**
     * Getter for whole topology
     *
     * @return whole topology.
     */
    public static ArrayList<ForwardingDevice> getTopology() {
        return topology;
    }

    /**
     * Gets given ForwardingDevice instance by simple ID of this device
     *
     * @param simpleID simple id of given device
     * @return instance of device with given simple ID in NetState internal list
     */
    public static ForwardingDevice getBySimpleID(int simpleID) {
        for (ForwardingDevice fd : topology
        ) {
            if (fd.getSimpleID() == simpleID) {
                return fd;
            }
        }
        return null;
    }

    /**
     * Sets paramsChanged flag to true
     */
    public static void setParamsChanged() {
        paramsChanged = true;
    }

    /**
     * Gets simple id of device by id of connected hosts
     *
     * @param hostId id of given hosts
     * @return simple ID of device to which given host is connected or -1 if host
     * isn't connected to any of known devices.
     */
    public static int getSimpleIDByIdOfConnectedHost(Identificator hostId) {
        for (ForwardingDevice fd : topology) {
            for (Identificator id : fd.connectedHosts) {
                if (id.equals(hostId))
                    return fd.getSimpleID();
            }

        }
        return -1;
    }

    /**
     * Add given device to topology
     * @param fd Instance of ForwardingDevice
     */
    public static void addForwardingDevice(ForwardingDevice fd)
    {
        if(getForwardingDeviceByDeviceID(fd.getDeviceControllerId())!=-1)
        {
            topology.add(fd);
        }
    }
    /**
     * Setter for Daemons list
     * @param daemons List of daemons
     */
    public static void setDaemons(ArrayList<Daemon> daemons)
    {
        NetState.daemons = daemons;
    }
    /**
     * Getter for Daemon list
     * @return List of daemons
     */
    public static ArrayList<Daemon> getDaemons()
    {
        return NetState.daemons;
    }

    public static int getSimpleIdByIpOfDaemon( String ipString )
    {
        String ip = ipString;
        if(ipString.contains(".") == false)
        {
            ip = NetState.IP_PREFIX_DAEMON+ip;
        }
        for (int i = 0; i < topology.size(); i++)
        {
            for (int j = 0; j < topology.get(i).getDaemons().size(); j++)
            {
                if(topology.get(i).getDaemons().get(j).getIpAddress().equals(ip))
                {
                    return topology.get(i).getSimpleID();
                }
            }
        }
        return -1;

    }
    public static int getMaxHostDangerForDevice(int simpleId)
    {
        return topology.get(getForwardingDeviceBySimpleID(simpleId)).getMaximumSecurityFactor();
    }
    public static void updateBytesCountersOnFlows()
    {
        for (int i = 0; i < topology.size(); i++)
        {
            topology.get(i).confirmCurrentBytesPerSecond();
            PathTranslator.updateBytesCountersOnFlows();
        }
    }
    public static void setupHostSpecificRNNs() {
        for (int i = 0; i < topology.size(); i++) {
            topology.get(i).setupHostIdBasedRNNs();
        }
    }

    public static SecureHost getSecureHostById(Identificator id)
    {
        for (int i = 0; i <secureHosts.size(); i++)
        {
            if(secureHosts.get(i).getHostId().equals(id))
            {
                return secureHosts.get(i);
            }
        }
        return null;
    }

    public static String jsonify()
    {
        JSONObject administration = new JSONObject();
        JSONObject topology = new JSONObject();
        JSONArray parameters = new JSONArray();
        JSONObject json = new JSONObject();
        JSONArray arrayTopo = new JSONArray();
        for (int i = 0; i <NetState.topology.size(); i++)
        {
            JSONObject device = NetState.topology.get(i).jsonify();
            arrayTopo.put(device);
        }
        try {
            Field[] fields = NetState.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                try
                {
                    JSONObject param = new JSONObject();
                    param.put(fields[i].getName(),fields[i].get(new Object()).toString());
                    parameters.put(param);
                }
                catch (Exception e)
                {}
            }
        }
        catch (Exception e)
        {}
        administration.put("parameters",parameters);
        topology.put("devices",arrayTopo);
        json.put("administration",administration);
        json.put("topology",topology);
        JSONObject ret = new JSONObject();
        ret.put("cnm",json);
        return ret.toString();
    }

    public static IpAddress getIpById(Identificator hostId)
    {
        for (int i = 0; i <secureHosts.size(); i++)
        {
            if(secureHosts.get(i).getHostId().equals(hostId))
            {
                return IpAddress.valueOf(secureHosts.get(i).getIpAddress());
            }
        }
        return IpAddress.valueOf("0.0.0.0");
    }
    public static void onCNMupdate()
    {
        if(NetState.policyBasedRouting == true)
        {
            for (int i = 0; i <topology.size(); i++)
            {
                topology.get(i).updatePropertiesOnLinks();
            }
        }
    }
    public static void updatePolicyOfHost(Identificator id, Criteria criteria)
    {
        for (int i = 0; i <secureHosts.size(); i++)
        {
            if(secureHosts.get(i).getHostId().equals(id))
            {
                secureHosts.get(i).updatePolicy(criteria);
                return;
            }
        }
    }
    public static void addBlackListItem(IpAddress ip)
    {
        if(!BLACKLIST.contains(ip))
        {
            BLACKLIST.add(ip);
            MASTER.blockIpAddress(ip);
        }
    }
    public static  void delFromBlackList(IpAddress ip)
    {
        if(BLACKLIST.contains(ip))
        {
            BLACKLIST.remove(ip);
            MASTER.unblockAllIpAddresses();
            for(IpAddress blackIP : BLACKLIST)
            {
                MASTER.blockIpAddress(blackIP);
            }
        }
    }
    public static double getEnergyBasedOnLoad(double load, String deviceId)
    {
        CurveManager cm = curves.get(deviceId);
        if(cm!=null)
        {
            return cm.getPredictedDelayValue((int)(load/1024+0.5));
        }
        else
        {
            return new CurveManager().getPredictedDelayValue((int)(load/1024+0.5));
        }
    }
    public static void addCurve(CurveManager curveManager, String deviceId)
    {
        if(curves.get(deviceId)!=null)
        {
            curves.remove(deviceId);
        }
        curves.put(deviceId,curveManager);
        UnifiedCommunicationModule.log.info("Curve for "+deviceId+" set.");
    }
}

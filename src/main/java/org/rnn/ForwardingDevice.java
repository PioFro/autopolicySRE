package org.rnn;
import org.onosproject.net.PortNumber;
import org.rnn.Identificators.DeviceIdReal;
import org.rnn.Identificators.Identificator;
import org.rnn.json.JSONArray;
import org.rnn.json.JSONObject;
import org.rnn.policy.Property;
import java.util.ArrayList;

/**
 * This is container for Device and Random Neural Networks corresponding to it. Every neural network has as much neurons
 * as device has connected ports. It carries all of the external links leading
 * from this device. It also stores list of all hosts connected to this device.
 *
 * @author Piotr Frohlich
 * @version 3.0.0
 *
 */

public class ForwardingDevice {
    /**
     * Set of rnn's created for destination (being eg. border node)
     */
    private ArrayList<RNNDestinationTuple> rnnRoutingModule = new ArrayList<RNNDestinationTuple>();

    private ArrayList<RNNDestinationTuple> hostSpecificRoutingModule =new ArrayList<>();
    /**
     * Number of connected ports
     */
    public int numberOfConnectedPorts = -1;
    /**
     * Device ID assigned to device by controller.
     */
    private Identificator deviceControllerId;
    /**
     * Device ID parsed to simple integer (for smaller networks).
     */
    private int simpleID;
    /**
     * Device parameter set by administrator. Integer value <0,100></0,100>. The higher the value the more sensitive
     * the node is to unsecure traffic.
     */
    private int sensivity;

    /**
     * Is this device active
     */
    private boolean isActive = true;

    /**
     * Array of hosts connected to this device
     */
    public ArrayList<Identificator> connectedHosts = new ArrayList<Identificator>();
    /**
     * This is array which is a representation of links of given instance of device.
     */
    public ArrayList<PortDeviceIdTuple> links = new ArrayList<PortDeviceIdTuple>();

    /**
     * Temporary turned off paths (when device is marked as unavailable)
     */
    private ArrayList<PathFlow> temporaryOffPaths = new ArrayList<PathFlow>();
    /**
     * List of daemons implemented by this device.
     */
    private ArrayList<Daemon> daemons = new ArrayList<Daemon>();
    /**
     * Maximum bytes per second send via this device
     */
    protected long MAX_BYTES_THROUGHPUT = NetState.DEFAULT_MAX_BYTES_THROUGHPUT;

    /**
     * Previous value of bytes send through this device
     */
    protected long previousBytesOverall = 0;
    /**
     * Current sum of bytes per second of all flows send via this device
     */
    protected long currentBytesPerSecond = 0;
    /**
     * Last updated currentBytesPerSecond
     */
    protected long lastUpdated = 0;
    protected CurveManager curveManager;
    protected short defaultMaxBytesPercentage = -1;
    protected int trust = 9;

    public ArrayList<Property> properties =  new ArrayList<>();

    public ArrayList<Double> energyMeasurements = new ArrayList<>();

    /**
     * Constructor which sets up a new device
     *
     * @param ports number of ports this device has connected
     * @param deviceControllerId Id of this device which OpenFlow assigned to it
     * @param sID Simplified id of this device
     */
    public ForwardingDevice(int ports, Identificator deviceControllerId, int sID)
    {
        numberOfConnectedPorts = ports;

        this.deviceControllerId = deviceControllerId;

        this.simpleID = sID;

        this.sensivity = NetState.DEFAULT_SENSITIVITY;

        if(defaultMaxBytesPercentage == -1)
            this.curveManager= new CurveManager(NetState.DEFAULT_MAX_BYTES_PERCENTAGE);
        else
            this.curveManager =new CurveManager(defaultMaxBytesPercentage);
        for (int i = 0; i <NetState.DEFAULT_DEVICE_PROPERITES.size(); i++)
        {
            properties.add(NetState.DEFAULT_DEVICE_PROPERITES.get(i));
        }
    }

    /**
     * An empty Constructor
     */
    public ForwardingDevice() {
    }

    /**
     * Method called when this device receive a smart packet
     * @return number of port which should be used to send the packet
     */
    public int serveSmartPacket(int selectedNeuron, long srcHost, long dstHost) {
        RNNDestinationTuple rnn = getRNNByDestination(srcHost, dstHost);
        if (rnn != null) {
            return rnn.getMaxExcited();
        }

        rnn = new RNNDestinationTuple(numberOfConnectedPorts, srcHost, dstHost);
        rnn.getRecurrentRNN().setSelectedNeuron(rnn.getRecurrentRNN().getNeurons().get(selectedNeuron));
        rnnRoutingModule.add(rnn);
        return rnn.getMaxExcited();
    }


    /**
     * Feedback for RNN when (or if) ack packet returned. Award <0.0 means ack packet
     * didn't come back
     *
     * @param award       amount of award/punishment
     */
    public void serveAckPacket(long srcHost, long dstHost, double award, int output) {
        getRNNByDestination(srcHost, dstHost).giveAward(award, output);
    }
    public void serveAckPacket(Identificator src, Identificator dst, double award, int output)
    {
        getRNNByDestination(src,dst).giveAward(award,output);
    }
    /**
     * Getter for rnn acording to Source and Destination identification. If there
     * isn't such Random Neural Network - such will be created and then returned.
     *
     * @param srcHost Simple id of device to which source host is connected
     * @param dstHost Simple id of device to which destination host is connected
     * @return RNN connected to this source - destination pair.
     **/
    public RNNDestinationTuple getRNNByDestination(long srcHost, long dstHost) {
        try {
            for (RNNDestinationTuple destinationTuple : rnnRoutingModule) {
                if (destinationTuple.getDestinationHost() == dstHost) //Match - we have such rnn already
                {
                    if (destinationTuple.getSourceHost() == srcHost) {
                        return destinationTuple;
                    }
                }
            }
        }catch(Exception e)
        {}
        return createRNN(srcHost, dstHost);
    }
    public RNNDestinationTuple getRNNByDestination(Identificator srcHost, Identificator dstHost)
    {
        for (RNNDestinationTuple destinationTuple : hostSpecificRoutingModule) {
            if (destinationTuple.hostIdDst.equals(dstHost)) //Match - we have such rnn already
            {
                if (destinationTuple.hostIdSrc.equals(srcHost)) {
                    return destinationTuple;
                }
            }
        }
        return createRNN(srcHost,dstHost);
    }



    /**
     * Method called to create RNN for certain destination with number of neurons
     * equal to number of active ports of ForwardingDevice
     *
     * @param srcHost Identification of source device to which this RNN will be
     *                connected
     * @param dstHost Identification of destination device to which this RNN will be
     *      *         connected
     *
     * @return created RNN
     */
    private RNNDestinationTuple createRNN(long srcHost, long dstHost) {
        RNNDestinationTuple rnn = new RNNDestinationTuple(numberOfConnectedPorts, srcHost, dstHost);
        rnnRoutingModule.add(rnn);
        return rnn;
    }
    private RNNDestinationTuple createRNN( Identificator srcHost, Identificator dstHost)
    {
        int source = NetState.getSimpleIDByIdOfConnectedHost(srcHost);
        int destination = NetState.getSimpleIDByIdOfConnectedHost(dstHost);

        RNNDestinationTuple rnn = new RNNDestinationTuple(numberOfConnectedPorts, srcHost, dstHost,source,destination);
        hostSpecificRoutingModule.add(rnn);
        return rnn;
    }


    public void setupHostIdBasedRNNs()
    {
        for (int i = 0; i <this.connectedHosts.size(); i++)
        {
            for (int j = 0; j < NetState.getTopology().size(); j++)
            {
                ForwardingDevice fd = NetState.getTopology().get(j);
                for (int k = 0; k <fd.getConnectedHosts().size(); k++)
                {
                    if(connectedHosts.get(i).equals(fd.getConnectedHosts().get(k))==false && fd.getSimpleID()!=simpleID) {
                        RNNDestinationTuple rnn = new RNNDestinationTuple(numberOfConnectedPorts, connectedHosts.get(i), fd.getConnectedHosts().get(k), this.simpleID, fd.getSimpleID());
                        hostSpecificRoutingModule.add(rnn);
                    }
                }
            }
        }
    }

    /**
     * Getter for DeviceId assigned to this set of rnns
     *
     * @return DeviceId of this device
     */
    public Identificator getDeviceControllerId() {
        return deviceControllerId;
    }

    public ArrayList<String> prepareString() {
        ArrayList<String> parsed = new ArrayList<String>();
        parsed.add(deviceControllerId.toString() + "{\n");
        for (RNNDestinationTuple r : rnnRoutingModule) {
            parsed.addAll(r.prepareString());
        }
        parsed.add("}\n");
        return parsed;
    }



    /**
     * Getter for number of ports in given instance
     * @return Number of ports
     */
    public int getMaxPort()
    {
        return rnnRoutingModule.size();
    }

    /**
     * Getter for simple ID field
     * @return Simple ID
     */
    public int getSimpleID(){return  simpleID;}

    /**
     * Setter for sensitivity field
     * @param sensivity given sensitivity <0,100></0,100>
     */
    public void setSensivity(int sensivity)
    {
        if(sensivity<this.sensivity)
        {
            return;
        }
        if(sensivity>100)
        {
            this.sensivity = 100;
            updateProperties(new Property("device","sensitivity","100"));
            return;
        }
        if(sensivity<0)
        {
            this.sensivity = 0;
            return;
        }
        this.sensivity = sensivity;
        updateProperties(new Property("device","sensitivity",""+sensivity));

    }

    /**
     * Getter for sensitivity field
     * @return Sensitivity of given instance
     */
    public int getSensivity(){return sensivity;}

    /**
     * Searches links of the device and returns Device ID of device connected to given port.
     * @param portNumber Given Port
     * @return Device ID connected to given port. Null if port isn't connected.
     */
    public Identificator getDeviceIdFromPort(long portNumber)
    {
        for (PortDeviceIdTuple tuple: links)
        {
            if(tuple.portNumber ==portNumber)
                return tuple.deviceId;
        }
        return null;
    }
    public Identificator getDeviceIdFromActualPort(long actualPortNumber)
    {
        for (PortDeviceIdTuple tuple: links)
        {
            if(tuple.actualPortNumber ==actualPortNumber)
                return tuple.deviceId;
        }
        return null;
    }

    /**
     * Getter for actual port number mapped with index of most excited neuron
     * @param portNumber index of most excited neuron
     * @return Real port number mapped to given index
     */
    public long getActualPortNumber(int portNumber)
    {
        for (PortDeviceIdTuple tuple: links)
        {
            if(tuple.portNumber ==portNumber)
                return tuple.actualPortNumber;
        }
        return -1;
    }
    /**
     * Getter for index of given real port of this device.
     * @param portNumber  real port number of this device
     * @return Index of this port
     */
    public long getIdOfRealPort(int portNumber)
    {
        for (PortDeviceIdTuple tuple: links)
        {
            if(tuple.actualPortNumber == (portNumber))
                return tuple.portNumber;
        }
        return 0;
    }
    /**
     * Getter for all connected hosts
     * @return List of all connected host or null when there isn't any
     */
    public ArrayList<Identificator> getConnectedHosts()
    {
        return connectedHosts;
    }

    /**
     * Getter for max security factor among all of the connectedHosts.
     * @return Maximum security factor of connected host or 0 when there are no
     * hosts connected.
     */
    public int getMaximumSecurityFactor()
    {
        int max = 0;
        for (Identificator id:connectedHosts)
        {

           if(max < NetState.getRatingForHost(id))
               max = NetState.getRatingForHost(id);
        }
        return max;
    }

    /**
     * Get actual port number by Device ID connected by this port to this device.
     *
     * @param id Full identification of device which is supposedly connected to
     *           this device.
     * @return Actual port number
     */
    public long getPortByDeviceId(Identificator id)
    {
        for (int i = 0; i <links.size() ; i++)
        {
            if(links.get(i).deviceId.equals(id))
                return links.get(i).actualPortNumber;

        }
        return links.get(1).actualPortNumber;
    }

    /**
     * Processes link down to given device. It deletes entry in links as well as
     * deletes neuron corresponding with this link/port
     *
     * @param deviceId Device to which link was put down.
     */
    public void processLinkDownToDevice(Identificator deviceId)
    {
        for (int i = 0; i < links.size(); i++)
        {
            if(links.get(i).deviceId.equals(deviceId))
            {
                for (int j = 0; j < rnnRoutingModule.size(); j++)
                {
                    rnnRoutingModule.get(j).processLinkDownOnNeuron((int)links.get(i).portNumber);
                }
                links.remove(i);
                return;
            }
        }
    }

    /**
     * Processes link up between two forwarders. Creates new entry in links table
     * and creates new neuron corresponding to this link/port.
     * @param linkUp link which was set as active
     */
    public void processLinkUp(PortDeviceIdTuple linkUp)
    {
        int nextIndex = -1;

        for (int i = 0; i <rnnRoutingModule.size(); i++)
        {
            nextIndex = rnnRoutingModule.get(i).processLinkUp(linkUp);
        }
        if(nextIndex != -1)
        {
            links.add(new PortDeviceIdTuple(nextIndex, linkUp.deviceId, linkUp.portNumber));
        }
    }

    /**
     * Action taken when device is marked as unavailable. All paths containing this device
     * are deleted and stored inside this instance. Then, when device is marked
     * as active again, all paths will be re-added.
     */
    protected void changeToUnavailable()
    {
        temporaryOffPaths = PathTranslator.deletePathsWithDeviceId(simpleID);
        isActive = false;
    }

    /**
     * Change this device status to available
     */
    protected void changeToAvailable()
    {
        for (int i = 0; i < temporaryOffPaths.size(); i++)
        {
            PathTranslator.updatePath(temporaryOffPaths.get(i));
        }
        isActive = true;
    }

    protected boolean getIsActive()
    {
        return isActive;
    }

    /**
     * Setter for list of daemons
     * @param daemons List of daemons connected to this device.
     */
    public void setDaemons(ArrayList<Daemon> daemons)
    {
        this.daemons = daemons;
    }
    /**
     * Getter for list of daemons.
     * @return List of daemons.
     */
    public ArrayList<Daemon>getDaemons()
    {
        return daemons;
    }

    public boolean checkLink(int simpleDevice )
    {
        for (int i = 0; i < links.size(); i++)
        {
            //UnifiedCommunicationModule.log.info("Checking :"+NetState.getBySimpleID(simpleDevice).deviceControllerId+", compare: "+links.get(i).deviceId);
            if(links.get(i).deviceId.equals(NetState.getBySimpleID(simpleDevice).deviceControllerId))
            {
                //UnifiedCommunicationModule.log.info("TRUE");
                return true;
            }

        }

        return false;
    }
    public void confirmCurrentBytesPerSecond()
    {
        boolean firstTime = false;
        if(previousBytesOverall == 0)
            firstTime = true;

        if(firstTime)
        {
            previousBytesOverall = UnifiedCommunicationModule.getBytesOnDevice(this.deviceControllerId);
            lastUpdated = System.currentTimeMillis();
            return;
        }
        try {
            currentBytesPerSecond = (UnifiedCommunicationModule.getBytesOnDevice(this.deviceControllerId) - previousBytesOverall) / ((System.currentTimeMillis() - lastUpdated) / 1000);
        }
        catch (Exception e)
        {
            UnifiedCommunicationModule.log.error(e.getMessage());
            currentBytesPerSecond = 1;
        }
        previousBytesOverall = UnifiedCommunicationModule.getBytesOnDevice(this.deviceControllerId);
        lastUpdated = System.currentTimeMillis();
        updateProperties(new Property("device","bytes",""+currentBytesPerSecond));
    }
    public boolean addNewBytes(long bytesPerSecond)
    {
        if (bytesPerSecond+currentBytesPerSecond > MAX_BYTES_THROUGHPUT)
            return false;
        else
        {
            currentBytesPerSecond+=bytesPerSecond;
            return true;
        }
    }
    public double getPredictedDeayValue()
    {
        int p = (int)(currentBytesPerSecond/MAX_BYTES_THROUGHPUT+0.5)*100;

        if(p<=100) {
            return curveManager.getPredictedDelayValue(p);
        }
        return 999.999;
    }

    public void setConnectedHosts(ArrayList<Identificator> hosts)
    {
        this.connectedHosts = hosts;
    }
    public void addConnectedHost(Identificator host)
    {
        connectedHosts.add(host);
    }

    public long getMAX_BYTES_THROUGHPUT() {
        return MAX_BYTES_THROUGHPUT;
    }

    public long getCurrentBytesPerSecond() {
        return currentBytesPerSecond;
    }

    public long getPreviousBytesOverall() {
        return previousBytesOverall;
    }
    public void setTrust(int trust)
    {
        if(trust<this.trust)
            this.trust = trust;
    }
    public JSONObject jsonify()
    {
        JSONObject device = new JSONObject();
        JSONArray arrayLinks = new JSONArray();
        JSONArray arrayHosts = new JSONArray();
        device.put("id",deviceControllerId.toString());
        device.put("sensitivity",sensivity);
        device.put("threat",trust);
        JSONArray arrayProperties = new JSONArray();
        JSONObject p = new JSONObject();
        for (int i = 0; i <links.size(); i++)
        {
            JSONObject link = links.get(i).jsonify(this.getDeviceControllerId());
            arrayLinks.put(link);
        }
        device.put("links",arrayLinks);
        for (int i = 0; i <connectedHosts.size(); i++)
        {
            SecureHost sc = NetState.getSecureHostById(connectedHosts.get(i));
            arrayHosts.put(sc.jsonify());
        }
        device.put("hosts",arrayHosts);

        for (int i = 0; i <properties.size(); i++)
        {
            arrayProperties.put(properties.get(i).jsonify());
        }
        p.put("properties",arrayProperties);
        device.put("property manager",p);
        return device;
    }
    public ArrayList<Property> getProperitesOnDirection(Identificator direction)
    {
        ArrayList<Property> toReturn = this.properties;
        for (int i = 0; i <links.size(); i++)
        {
            if(links.get(i).deviceId.equals(direction))
            {
                for (int j = 0; j <links.get(i).properties.size(); j++)
                {
                    toReturn.add(links.get(i).properties.get(j));
                }
            }
        }
        return toReturn;
    }
    protected void updateProperties(Property property)
    {
        for (int i = 0; i <properties.size(); i++)
        {
            if(properties.get(i).getName().equalsIgnoreCase(property.getName()))
            {
                properties.get(i).setValue(property.getValue());
                return;
            }
        }
        properties.add(property);
    }
    protected void updatePropertiesOnLinks()
    {
        for (int i = 0; i <links.size(); i++)
        {
            PortDeviceIdTuple link = links.get(i);
            long directionSimpleID = NetState.getTopology().get(NetState.getForwardingDeviceByDeviceID(link.deviceId)).getSimpleID();
            double delayValue = RnnRouting.delayAgregator.getDelayInfo(new LinkID(directionSimpleID,this.simpleID));
            links.get(i).updateProperties(new Property("link","qos",""+delayValue));
        }
    }
    protected void updatePropertiesOnLink(Identificator id, Property property)
    {
        for (int i = 0; i <links.size(); i++)
        {
            if(links.get(i).deviceId.equals(id))
            {
                links.get(i).updateProperties(property);
                return;
            }
        }
    }
    public void addNewEnergyMeasurement(double value)
    {
        if(energyMeasurements.size()>NetState.MAX_ENERGY_MEASUREMENTS)
        {
            energyMeasurements.remove(0);
        }
        energyMeasurements.add(value);
    }
    public double getPredictedEnergyConsumption()
    {
        if(energyMeasurements.size()==0)
        {
            return NetState.getEnergyBasedOnLoad(currentBytesPerSecond,((DeviceIdReal)deviceControllerId).getRealDeviceId().toString());
        }
        else
        {
            double sum=0.0;
            long numberofsamples = 0;
            for (int i=0; i<energyMeasurements.size();i++)
            {
                sum+=(i*energyMeasurements.get(i));
                numberofsamples+=i;
            }
            return sum/numberofsamples;
        }
    }
}

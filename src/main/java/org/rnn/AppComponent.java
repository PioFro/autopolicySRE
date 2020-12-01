package org.rnn;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onosproject.cli.net.IpProtocol;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceStore;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkStore;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.rnn.Identificators.DeviceIdReal;
import org.rnn.Identificators.HostIdReal;
import org.rnn.Identificators.Identificator;
import org.rnn.autopolicy.APManager;
import org.rnn.implementation.ConfigParser;
import org.rnn.implementation.PathsLurker;
import org.rnn.implementation.RealCommunicator;
import org.rnn.implementation.RealLogger;
import org.rnn.policy.Property;
import org.rnn.service.ServiceManager;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.nio.file.FileSystemException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;

import static org.onosproject.net.link.LinkEvent.Type.LINK_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;
/**
 * This is master class for all not ONOS-related classes.
 *
 * Disclaimer:
 *
 * <p>
 * - Concerning log:
 * </p><p>
 *       INFO section:</p>
 *       <p>* prefix "@" - log entries concerning flow installation.</p>
 *       <p>* prefix "~" - log entries concerning Smart Packets leaving deamons</p>
 *       <p>* prefix "#" - log entries concerning Path Creation and Award/Punishment</p>
 *       <p>* prefix "&" - log entries concerning Security</p>
 *       <p>* prefix "*" - log entries concerning Setup and other matters.</p>
 *       <p>* prefix "<" - log entries concerning Smart Packet reaching destination</p>
 *       <p>ERROR section:</p>
 *           <p>* No prefixes</p>
 *
 * @author: Piotr Frohlich
 * @version: Beta 2.0.0
 *
 *
 */

@Component(immediate = true)
public class AppComponent {
    /**
     *Agent for topology information
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    /**
     * Agent for processing packets
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    /**
     * Agent for flow deinstallation
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    /**
     * Agent for flow installation and information
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    /**
     * Agent for basic information on app. Also needed for log.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    //@Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    //protected DeviceManager deviceManager;
    /**
     * Agent for gathering information about nodes
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceStore deviceStore;

    /**
     * Agent for information about links
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkStore linkStore;

    /**
     * Agent for information about hosts in the web. Hosts need to be discovered.
     */
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    /**
     * Listener for changes in topology
     */
    private final TopologyListener topologyListener = new InternalTopologyListener();
    /**
     * Listener for packet-in events
     */
    private RNNPacketProcessor packetProcessor = new RNNPacketProcessor();



    //protected  ArpManager arpManager = new ArpManager();

    /**
     * Logger connected with onoses log
     */
    public final Logger log = getLogger(getClass());

    /**
     * Application Id used by the logger
     */
    protected ApplicationId appId;

    /**
     * Number of Forwarding Devices in the net
     */
    private Integer topologySize = 0;

    /**
     * List of available devices (provided by ONOS)
     */
    private Set<DeviceId> deviceList;

    /**
     * Amount of created RNNs
     */
    private short numberOfRNN = 0;

    public static RealCommunicator realCommunicator;

    public static ServiceManager manager;

    /**
     * Listener for changes in hosts
     */
    private InternalHostListener hostListener = new InternalHostListener();


    public static boolean lock = false;

    public static AppComponent tmp;

    private static boolean deamonsConfigured = false;

    public PathsLurker lurker;

    public Timer pathsTimer;

    /**
     * Inner class, implementing PacketProcessor so that it could be notified about
     * packet-in events. It processes only UDP packets arriving on port 5015. Then
     * packets are processed and their UDP destination port is changed to 5025
     * to ensure no returning packets.
     */
    private class RNNPacketProcessor implements PacketProcessor {


        int counter = 0;
        /**
         * Processes packets. Port - 5015 is send to controller as SP (send or received).
         *
         * Then @RnnRouting translates payload of received SP into awards and then
         * reassigns flows (if the timeout is over)
         *
         *
         *
         * @param packetContext this is context of in-out packets
         */
        @Override
        public void
        process(PacketContext packetContext)
        {


            if (!packetContext.isHandled())
            {
                Ethernet inPkt = packetContext.inPacket().parsed();
                if (inPkt.getEtherType() == Ethernet.TYPE_IPV4) {
                    IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
                    if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_UDP) {
                        UDP inPacket = (UDP) ipv4Packet.getPayload();
                        if (inPacket.getDestinationPort() == 5015) {

                            try {
                                realCommunicator.receiveSmartPacket(packetContext);
                            } catch (Exception e) {
                                UnifiedCommunicationModule.log.error(e.getMessage());
                            }
                            packetContext.treatmentBuilder().setUdpDst(TpPort.tpPort(5025));
                            if(counter > 100)
                            {
                                checkFlows();
                                NetState.onCNMupdate();
                                counter = 1;
                            }
                            //Once every DEFAULT_TIME_REFRESHING_BYTES_COUNTERS every device gets reassigned
                            //Bytes per second with 100% accurate value. Note that this method
                            //is massively time-consuming. It
                            if(NetState.lastTimeUpdatedFlowBytesCounters + NetState.DEFAULT_TIME_REFRESHING_BYTES_COUNTERS < System.currentTimeMillis())
                            {
                                NetState.updateBytesCountersOnFlows();
                                NetState.lastTimeUpdatedFlowBytesCounters = System.currentTimeMillis();
                            }
                            counter++;
                            packetContext.treatmentBuilder().drop().build();
                        }
                    }
                    //PathTranslator.addNewTrafficInformation(new MultiRoutingInfo(proto, sport, dport,src, dst, sDevice, dDevice));
                    //PathTranslator.checkTrafficInfo();
                }
                //PathTranslator.checkPaths();
            }

        }

    }

    private class InternalHostListener implements HostListener
    {

        @Override
        public void event(HostEvent event)
        {
            if(NetState.setupComplete == true)
            {
                DefaultHost host = (DefaultHost) event.subject();
                if(host!= null)
                {
                    if (event.type().equals(HostEvent.Type.HOST_ADDED))
                    {

                        if (host.locations().iterator().hasNext())
                        {
                            DeviceId locationDevice = host.locations().iterator().next().deviceId();

                            int indexOfLocationDevice = NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(locationDevice,locationDevice.hashCode()));

                            NetState.getTopology().get(indexOfLocationDevice).connectedHosts.add(new HostIdReal(host.id(),host.hashCode()));

                            if(host.ipAddresses().iterator().hasNext())
                            {
                                //NetState.addSecureHost(new SecureHost(new HostIdReal(host.id(), host.hashCode()), NetState.getDefaultSecurity(), host.ipAddresses().iterator().next().toString(), "SETUP VALUE"));
                            }
                            else
                            {
                                //NetState.addSecureHost(new SecureHost(new HostIdReal(host.id(),host.hashCode()), NetState.getDefaultSecurity(), "SETUP VALUE, NO IP"));
                            }
                        }
                    }
                    log.info("Host event occured"+event.toString());
                    if(event.type().equals(HostEvent.Type.HOST_REMOVED))
                    {

                        log.info("HOST LOST");

                        if (host.locations().iterator().hasNext())
                        {
                            DeviceId locationDevice = host.locations().iterator().next().deviceId();

                            int indexOfLocationDevice = NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(locationDevice,locationDevice.hashCode()));

                            NetState.getTopology().get(indexOfLocationDevice).connectedHosts.remove(host.id());
                            HostId oldDst = host.id();
                            HostId newDst = getHostIdByIpAddress(IpAddress.valueOf(NetState.RSU_2_IP));
                            HostId srcHost = getHostIdByIpAddress(IpAddress.valueOf(NetState.CAR_IP));
                            maskMovemement(oldDst, newDst, srcHost, false);

                        }
                    }
                }

            }
        }


 }


    /**
     * Topology changes listener
     */
    private class InternalTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event)
        {
            List<Event> reasons = event.reasons();
            if(NetState.setupComplete == true)
            {
                for (int i = 0; i < reasons.size(); i++) {
                    try {

                        String reasonName = reasons.get(i).type().name();


                        if (reasonName.equals(LINK_REMOVED.name())) {
                            Link link = (Link) reasons.get(i).subject();

                            int src = getSimpleIDByControllerID(link.src().deviceId());

                            int dst = getSimpleIDByControllerID(link.dst().deviceId());

                            int indexOfSourceDevice = NetState.getForwardingDeviceBySimpleID(src);
                            //Deleting link from link list. Should remove it from paths as well
                            NetState.getTopology().get(indexOfSourceDevice).processLinkDownToDevice(new DeviceIdReal(link.dst().deviceId(),link.hashCode()));

                            PathTranslator.processLinkDown(src, dst);
                        }
/*
                        if (reasonName.equals(LINK_ADDED.name())) {

                            Link link = (Link) reasons.get(i).subject();

                            int src = getSimpleIDByControllerID(link.src().deviceId());

                            int indexOfSourceDevice = NetState.getForwardingDeviceBySimpleID(src);

                            NetState.getTopology().get(indexOfSourceDevice).processLinkUp(link);
                        }
                        //Not sure if this will work properly ( cast may cause exception )
                        if (reasonName.equals(DEVICE_ADDED.name()))
                        {
                            DefaultDevice device = (DefaultDevice) reasons.get(i).subject();

                            DeviceId deviceId = device.id();

                            ForwardingDevice fd = createForwardingDeviceInstanceFromDeviceId(deviceId);

                            NetState.addForwardingDevice(fd);

                        }
                        if( reasonName.equals(DEVICE_AVAILABILITY_CHANGED.name()))
                        {
                            //String strin = reasons.get(i).subject().getClass().toString();
                            DefaultDevice device = (DefaultDevice) reasons.get(i).subject();
                            DeviceId deviceId = device.id();
                            if(NetState.getTopology().size()>deviceStore.getAvailableDeviceCount())
                            {
                                //Device was shut down
                                int indexOfCurrentDevice = NetState.getForwardingDeviceByDeviceID(deviceId);

                                NetState.getTopology().get(indexOfCurrentDevice).changeToUnavailable();
                            }
                            else
                            {
                                //Device was added
                                int indexOfCurrentDevice = NetState.getForwardingDeviceByDeviceID(deviceId);

                                if(indexOfCurrentDevice != -1)
                                {
                                    //Device was in topology but was marked as inactive;
                                    NetState.getTopology().get(indexOfCurrentDevice).changeToAvailable();
                                }
                                else
                                {
                                    DefaultDevice device1 = (DefaultDevice) reasons.get(i).subject();

                                    DeviceId deviceId1 = device1.id();

                                    ForwardingDevice fd = createForwardingDeviceInstanceFromDeviceId(deviceId1);

                                    NetState.addForwardingDevice(fd);
                                }

                            }

                        }*/
                    }
                    catch(Exception e)
                    {
                        log.error("TOPOLOGY MANAGER HAS THROWN EXCEPTION: "+e.getMessage()+"\tWHILE REASON: "+reasons.get(i).type().toString());
                    }
                }
            }
        }
    }


    /***
     * Sets up N*N*p RNNS (where N is number of nodes and p is number of
     * active ports). Sets up whole virtual topology. Also gives ID to all devices
     * which are simplified version of OpenFlow ID.
     */
    public void setupNeuralNetworks()
    {
        //Get list of Devices by their ID (for single controller)
        log.info("SETTING UP NEURAL NETWORK");
        //tmp topology setup
        ArrayList<ForwardingDevice> topology = new ArrayList<>();

        //iterate over all devices
        deviceList = topologyService.getClusterDevices(topologyService.currentTopology(), topologyService.getClusters(topologyService.currentTopology()).iterator().next());

        topologySize = deviceList.size();
        //Add RNN per active, connected port
        for (DeviceId id : deviceList) {

            //fd.macOfDaemon = mac;
            topology.add(createForwardingDeviceInstanceFromDeviceId(id));

            log.info("Creation of device:" + id.uri().toString());

        }
        //Give feedback - number of nodes + number of RNNS
        log.info(appId + ": RNN created for [" + topologySize + "] in total RNNS [" + numberOfRNN + "]");

        //Setting up topology;
        NetState.setTopology(topology);

        PathProviderTree tmp = new PathProviderTree(NetState.getTopology().size()-1);
        PathProviderTree.setup();
        log.info("PATH TREE SETUP READY");
    }

    /**
     * Setting up whole plugin - selecting types of packets listened to, setting up
     * listeners to topology changed and packet in events. Setting up RNNs
     */
    @Activate
    protected void activate() {

        log.info("* Started RNN *");
        NetState.MASTER = this;

        ArrayList<Property> properties = new ArrayList<>();
        Property prop = new Property("device", "abstract location", "SerIoT");
        properties.add(prop);
        prop = new Property("device","geo-location 1","University of Essex");
        properties.add(prop);

        prop = new Property("device","geo-location 2","Great Britain");
        properties.add(prop);
        prop = new Property("device","geo-location 3","Not European Union");
        properties.add(prop);
        prop = new Property("device","software","PicOS");
        properties.add(prop);
        prop = new Property("device","software version","2925");
        properties.add(prop);
        NetState.DEFAULT_DEVICE_PROPERITES = properties;
        properties = new ArrayList<>();
        prop = new Property("link", "abstract location", "SerIoT");
        properties.add(prop);
        prop = new Property("link","geo-location 1","University of Essex");
        properties.add(prop);
        prop = new Property("link","geo-location 2","Great Britain");
        properties.add(prop);
        prop = new Property("link","geo-location 3","Not European Union");
        properties.add(prop);
        prop = new Property("link","wireless","not");
        properties.add(prop);
        prop = new Property("link","vpn","yes");
        properties.add(prop);
        NetState.DEFAULT_LINK_PROPERTIES = properties;
        //hostService.addListener(hostListener);

        this.realCommunicator = new RealCommunicator(this);

        PathTranslator tmp = new PathTranslator(this, 1000);

        ConfigParser.MASTER = new UnifiedCommunicationModule(new RealPathInstaller(this),new RealLogger());

        ArrayList<Daemon> daemons = new ArrayList<>();
        try
        {
            daemons = ConfigParser.getDaemons();
            ConfigParser.setupParametersOfNetState();
            deamonsConfigured = true;
        }
        catch (FileNotFoundException e) {
            log.error(e.getMessage());
        }
        catch (ParseException e2)
        {
            log.error(e2.getMessage());
        }
        catch(FileSystemException e1)
        {
            log.error(e1.getMessage());
        }
        if(deamonsConfigured)
        {
            activateWithProvidedConfig(daemons);
        }
        else
            {
                log.error("Send configuration via the REST interface on controller http://controller ip :8181/onos/rnn/SRE/configuration");
            }
    }
    public void activateWithProvidedConfig(ArrayList<Daemon> daemons)
    {
        NetState.setDaemons(daemons);

        //To invoke event of topology change
        //topologyService.addListener(topologyListener);



        //To register application automatically
        appId = coreService.registerApplication("org.rnn");


        //Store app id in NetState for other classes to have access to it
        NetState.setAppId(appId.toString());

        NetState.setDefaultSecurity(1);

        setupNeuralNetworks();

        //Adding packet processor
        packetService.addProcessor(packetProcessor, PacketProcessor.director(1));
        //packetService.addProcessor(arpManager, PacketProcessor.director(1));


        //Selecting types of packet that Packet Processor will be processing
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        selector.matchEthType(Ethernet.TYPE_IPV4).matchIPProtocol(IPv4.PROTOCOL_UDP).matchUdpDst(TpPort.tpPort(5015));

        //TrafficSelector.Builder selectorArp = DefaultTrafficSelector.builder();

        try {
            setUpSmartPackets();
        }
        catch (Exception e){}


        for (Host host: hostService.getHosts())
        {
            try
            {
                if(host.ipAddresses().iterator().hasNext())
                {
                    SecureHost sh = new SecureHost(new HostIdReal(host.id(), Long.parseLong(host.ipAddresses().iterator().next().toString().split("\\.")[3])), NetState.getDefaultSecurity(), host.ipAddresses().iterator().next().toString(), "SETUP VALUE");
                    NetState.addSecureHost(sh,PortNumber.P0,(byte)0,"t",11);
                }
            }
            catch(Exception e)
            {
                log.info("Invalid Host. Skipping... ");
            }
        }

        setupPingFlows();

        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);

        //packetService.requestPackets(selectorArp.build(), PacketPriority.REACTIVE, appId);

        NetState.setupComplete = true;
        //ConfigParser.setupParametersOfNetState();

        realCommunicator.master = this;

        NetState.MASTER = this;

        RealPathInstaller.master = this;

        VisAppComp.SetForwardingDevices(NetState.getTopology());

        lurker = new PathsLurker();
        pathsTimer = new Timer();
        pathsTimer.scheduleAtFixedRate(lurker,NetState.CHECK_FLOWS_TIME,NetState.CHECK_FLOWS_TIME);
        PathTranslator.reinstallAllBestPaths();
        APManager.init();
    }


    /**
     * It has to be in this class, because only this class can access the ONOSes
     * deeper structure. Simply install objective to given device.
     * @param forwardingObjective Objective (rule, actions, etc.) - given by
     *                            pathTranslator.
     * @param deviceId Given device on which given objective should be installed
     */
    public void installFlowFromPath(ForwardingObjective forwardingObjective, DeviceId deviceId)
    {
        flowObjectiveService.forward(deviceId, forwardingObjective);
    }

    /**
     * Creating of basic routes for first SPs. Routes are based on RNNs' indication
     * or pathService provided by ONOS. Paths are then stored in PathTranslator class
     * where they will be taken into consideration by RNNs decisions
     */
    private void setUpSmartPackets() {


        /**
         * For all devices setup basic flow for SP (redirecting 5004 UDP to
         * deamon on corresponding host)
         */
        for (DeviceId device : deviceList) {

            //IpAddress ipAddress = decideIPOfHop(device);

             ForwardingDevice correspondingForwardingDeviceElement = new ForwardingDevice();
            for(ForwardingDevice fd : NetState.getTopology())
            {
                if(fd.getDeviceControllerId().equals(new DeviceIdReal(device,device.hashCode())))
                {
                    correspondingForwardingDeviceElement = fd;
                    break;
                }
            }
            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_UDP)
                    .matchUdpDst(TpPort.tpPort(5004));

            int numberOfPorts = deviceStore.getPorts(device).size();
            try {

                /**
                 *  Static Smart Packet consument. Currently on node
                 */
                for (int i = 0; i < correspondingForwardingDeviceElement.getDaemons().size(); i++) {
                    TrafficTreatment treatment;
                    if(correspondingForwardingDeviceElement.getDaemons().get(i).getPortNumber() == -1)
                    {
                        treatment = DefaultTrafficTreatment.builder()
                                .setEthDst(MacAddress.BROADCAST)
                                .setIpDst(IpAddress.valueOf(correspondingForwardingDeviceElement.getDaemons().get(i).getIpAddress()))
                                .setOutput(PortNumber.LOCAL)
                                .build();
                    }
                    else {
                        treatment = DefaultTrafficTreatment.builder()
                                .setEthDst(hostService.getHostsByIp(IpAddress.valueOf(correspondingForwardingDeviceElement.getDaemons().get(i).getIpAddress())).iterator().next().mac())
                                .setIpDst(IpAddress.valueOf(correspondingForwardingDeviceElement.getDaemons().get(i).getIpAddress()))
                                .setOutput(PortNumber.portNumber(correspondingForwardingDeviceElement.getDaemons().get(i).getPortNumber()))
                                .build();
                    }
                    ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                            .withSelector(selectorBuilder.build())
                            .withTreatment(treatment)
                            .withPriority(50013)
                            .withFlag(ForwardingObjective.Flag.SPECIFIC)
                            .fromApp(appId)
                            .add();

                    flowObjectiveService.forward(device,
                                                 forwardingObjective);
                }

            }catch (Exception e)
            {
                log.error(e.getMessage());
            }
            /**
             * Redirect UDP 5006 to get to the next hop. Using basic indications
             */
            int counterOfBasicPaths = 0;
            for (DeviceId deviceId: deviceList)
            {
                try {
                    if (device.equals(deviceId) == false) {
                        TrafficSelector.Builder selectorBuilder5006 = DefaultTrafficSelector.builder();
                        long src = Integer.parseInt(  device.toString().charAt(device.toString().length()-1)+"");
                        long dst = Integer.parseInt(  deviceId.toString().charAt(deviceId.toString().length()-1)+"");

                        PathFlow path = null;
                        try {
                            path = PathProviderTree.getPath(src, dst);
                        }
                        catch (TreeProviderException treeException)
                        {
                            log.error(treeException.getMessage());
                        }
                        if (path != null) {
                            counterOfBasicPaths++;
                            PathTranslator.addNewPath(path);
                        }
                    }
                }
                catch(Exception e)
                {

                }
            }
            log.info("NUMBER OF CREATED BASIC PATHS : "+counterOfBasicPaths);

            for (Host host: hostService.getHosts()) {

                try {

                    if (host.location().deviceId().equals(device))
                    {
                        //correspondingForwardingDeviceElement.connectedHosts.add(host.id());
                        //setup path for all hosts connected to a device
                        if(host.ipAddresses().iterator().hasNext() == true)
                        {
                            TrafficSelector.Builder selectorBuilder5006 = DefaultTrafficSelector.builder();
                            selectorBuilder5006
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchIPDst(host.ipAddresses().iterator().next().toIpPrefix());
                            TrafficTreatment treatment5006 = DefaultTrafficTreatment.builder().setOutput(host.location().port()).build();
                            ForwardingObjective forwardingObjective5006 = DefaultForwardingObjective.builder()
                                    .withSelector(selectorBuilder5006.build())
                                    .withTreatment(treatment5006)
                                    .withPriority(NetState.DEFAULT_BLOCK_PRIORITY)
                                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                                    .makePermanent()
                                    .fromApp(appId)
                                    .add();
                            flowObjectiveService.forward(device, forwardingObjective5006);
                        }
                    }
                }
                catch(Exception e)
                {
                }
            }
            /**
             * UDP 5015 send to controller
             */
            TrafficSelector.Builder selectorBuilder5005 = DefaultTrafficSelector.builder();

            selectorBuilder5005
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_UDP)
                    .matchUdpDst(TpPort.tpPort(5015));

            TrafficTreatment treatment5005 = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.CONTROLLER)
                    .build();

            ForwardingObjective forwardingObjective5005 = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder5005.build())
                    .withTreatment(treatment5005)
                    .withPriority(50000)
                    .withFlag(ForwardingObjective.Flag.SPECIFIC)
                    .makePermanent()
                    .fromApp(appId)
                    .add();
            flowObjectiveService.forward(device, forwardingObjective5005);

            TrafficSelector.Builder selectorBuilder5025 = DefaultTrafficSelector.builder();

            selectorBuilder5025
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_UDP)
                    .matchUdpDst(TpPort.tpPort(5025));

            TrafficTreatment treatment5025 = DefaultTrafficTreatment.builder()
                    .drop()
                    .build();

            ForwardingObjective forwardingObjective5025 = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder5025.build())
                    .withTreatment(treatment5025)
                    .withPriority(50003)
                    .withFlag(ForwardingObjective.Flag.SPECIFIC)
                    .makePermanent()
                    .fromApp(appId)
                    .add();
            flowObjectiveService.forward(device, forwardingObjective5025);
        }
    }

    /**
     * Pick simple forward path
     * @param paths set of all paths provided by ONOS
     * @param notToPort port to which path shouldn't head
     * @return chosen path
     */
    private Path pickForwardPathIfPossible(Set<Path> paths, PortNumber notToPort) {
        for (Path path : paths) {
            if (!path.src().port().equals(notToPort)) {
                return path;
            }
        }
        return null;
    }

    /**
     * Deactivation of RNN app.
     */
    @Deactivate
    protected void deactivate() {

        UnifiedCommunicationModule.log.end();
        pathsTimer.purge();
        pathsTimer.cancel();
        packetService.removeProcessor(packetProcessor);
        APManager.deinit();
        //packetService.removeProcessor(arpManager);
        flowRuleService.removeFlowRulesById(appId);

        log.info(appId + ": RNN MODULE STOPPED *");
    }
    /**
     * Checks if argument is of LLDP or BSN type
     *
     * @param eth
     * @return true if argument is a control packet
     */
    private boolean isControlPacket(Ethernet eth)
    {
        short type = eth.getEtherType();

        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }

    /**
     * Returns Device Id of device to which given host is connected
     * @param host Given Host id (MAC/VLAN)
     * @return Device ID of its forwarder
     */
    public DeviceId getLocationOfHost( HostId host)
    {
        return hostService.getHost(host).location().deviceId();
    }
    /**
     * Retruns HostId of host with given MAC
     * @param mac Given MAC
     * @return Host ID of host with given MAC
     */
    public HostId getHostIdByMac(MacAddress mac)
    {
        try
        {
        return hostService.getHostsByMac(mac).iterator().next().id();
    }
        catch(Exception e)
        {
            return HostId.NONE;
        }
    }
    /**
     * Retruns Id of hosts based on IP address of host
     * @param ip Given IP address
     * @return Id of host which has given IP address or NONE (HostId field
     * similar to null) when no such IP address is present in the network.
     */
    public HostId getHostIdByIpAddress(IpAddress ip)
    {
        try {
            return hostService.getHostsByIp(ip).iterator().next().id();
        }
        catch (Exception e)
        {
            return HostId.NONE;
        }
    }
    /**
     * Retruns first IP address of host with given ID
     * @param id Id of given host
     * @return first IP address of host with given ID or null when no such host
     *         is present in the network.
     */
    public IpAddress getIpAddressByHostId(HostId id)
    {
        if(hostService.getHost(id).ipAddresses().iterator().hasNext())
        {
            return hostService.getHost(id).ipAddresses().iterator().next();
        }
        else
            return null;
    }

    /**
     * Returns simple id of device based by Identification of this device given
     * by Openflow Protocol
     * @param id
     * @return
     */
    public static int getSimpleIDByControllerID(DeviceId id)
    {
        return Integer.parseInt(id.toString().charAt(id.toString().length()-1)+"");
    }

    /**
     * Changes path class to PathFlow class
     * @param path instance of Path class of onos
     * @return PathFlow instance which is equivalent to given Path class.
     */
    public PathFlow changePathToPathFlow(Path path)
    {
        PathFlow flow = new PathFlow(getSimpleIDByControllerID(path.src().deviceId()), getSimpleIDByControllerID(path.dst().deviceId()));

        flow.addNewNode(new Node(flow.getSource(), NetState.DEFAULT_SCORE_VALUE, (int)path.links().get(0).src().port().toLong()));

        for (int i = 1; i < path.links().size(); i++)
        {
            flow.addNewNode(new Node(getSimpleIDByControllerID(path.links().get(i).src().deviceId()), NetState.DEFAULT_SCORE_VALUE, (int)path.links().get(i).src().port().toLong()));
        }

        return flow;
    }

    /**
     * Force hard timeout execution.
     */
    private void checkFlows() {
        if (lock == false)
        {
            lock = true;
            Iterator<FlowEntry> iterator= flowRuleService.getFlowEntriesById(appId).iterator();

            while(iterator.hasNext()) {
                FlowEntry flowEntry = iterator.next();
                if (flowEntry.priority() == NetState.DEFAULT_DISCOVERY_PACKET_PRIORITY) {
                    if (flowEntry.life() > 10) {
                        flowRuleService.removeFlowRules(flowEntry);
                    }
                }
            }
            lock = false;
        }
    }

    /**
     * Setting up mechanism for better calculating time delays
     */
    private void setupPingFlows()
    {
        //IP addresses and ports
        for (int i = 0; i < NetState.getTopology().size(); i++)
        {
            ForwardingDevice fd = NetState.getTopology().get(i);
            DeviceIdReal real = (DeviceIdReal)fd.getDeviceControllerId();
            //IpAddress ipOfDevice = IpAddress.valueOf(NetState.DEFAULT_IP_PREFIX+getSimpleIDByControllerID(fd.getDeviceControllerId()));
                for (int k = 0; k < fd.getDaemons().size(); k++) {
                    //IpAddress ipSource = IpAddress.valueOf(NetState.DEFAULT_IP_PREFIX + getSimpleIDByControllerID(fd.links.get(j).deviceId));

                    for (int j = 0; j < fd.links.size(); j++) {
                        ForwardingDevice forwardingDeviceOnTheLink = NetState.getTopology().get(NetState.getForwardingDeviceByDeviceID(fd.links.get(j).deviceId));
                        for (int l = 0; l < forwardingDeviceOnTheLink.getDaemons().size(); l++) {

                            IpAddress ipSource = IpAddress.valueOf(forwardingDeviceOnTheLink.getDaemons().get(l).getIpAddress());

                            TrafficSelector.Builder selectorBuilderPing = DefaultTrafficSelector.builder();
                            TrafficSelector.Builder selectorBuilderPing2 = DefaultTrafficSelector.builder();
                            selectorBuilderPing
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchIPDst(ipSource.toIpPrefix())
                                    .matchIPProtocol(IPv4.PROTOCOL_UDP)
                                    .matchUdpDst(TpPort.tpPort(6004));

                            selectorBuilderPing2
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchIPDst(ipSource.toIpPrefix())
                                    .matchIPProtocol(IPv4.PROTOCOL_UDP)
                                    .matchUdpDst(TpPort.tpPort(6005));

                            TrafficTreatment treatmentPingFlow = DefaultTrafficTreatment.builder()
                                    .setOutput(PortNumber.portNumber(fd.links.get(j).actualPortNumber))
                                    .build();

                            ForwardingObjective forwardingObjective5005 = DefaultForwardingObjective.builder()
                                    .withSelector(selectorBuilderPing.build())
                                    .withTreatment(treatmentPingFlow)
                                    .withPriority(NetState.DEFAULT_BLOCK_PRIORITY + 10)
                                    .withFlag(ForwardingObjective.Flag.SPECIFIC)
                                    .makePermanent()
                                    .fromApp(appId)
                                    .add();


                            flowObjectiveService.forward(real.getRealDeviceId(), forwardingObjective5005);

                            ForwardingObjective forwardingObjective5005_2 = DefaultForwardingObjective.builder()
                                    .withSelector(selectorBuilderPing2.build())
                                    .withTreatment(treatmentPingFlow)
                                    .withPriority(NetState.DEFAULT_BLOCK_PRIORITY + 10)
                                    .withFlag(ForwardingObjective.Flag.SPECIFIC)
                                    .makePermanent()
                                    .fromApp(appId)
                                    .add();
                            flowObjectiveService.forward(real.getRealDeviceId(), forwardingObjective5005_2);
                        }
                    }
                    TrafficSelector.Builder selectorBuilderPingStatic = DefaultTrafficSelector.builder();

                    long port = fd.getDaemons().get(k).getPortNumber();
                    PortNumber portNumber = PortNumber.LOCAL;
                    if(port != -1)
                    {
                        portNumber = PortNumber.portNumber(port);
                    }
                    selectorBuilderPingStatic
                            .matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPDst(IpPrefix.valueOf(fd.getDaemons().get(k).getIpAddress()+"/32"));

                    TrafficTreatment treatmentPingFlowStatic = DefaultTrafficTreatment.builder()
                            .setOutput(portNumber)
                            .build();

                    ForwardingObjective forwardingObjective5005 = DefaultForwardingObjective.builder()
                            .withSelector(selectorBuilderPingStatic.build())
                            .withTreatment(treatmentPingFlowStatic)
                            .withPriority(NetState.DEFAULT_BLOCK_PRIORITY + 10)
                            .withFlag(ForwardingObjective.Flag.SPECIFIC)
                            .makePermanent()
                            .fromApp(appId)
                            .add();
                    flowObjectiveService.forward(real.getRealDeviceId(), forwardingObjective5005);
                }
            }
    }

    /**
     * Creates an instance of ForwardingDevice of given Device Id from controller
     * @param id Device id of device
     * @return ForwardingDevice instance corresponding to given DeviceId
     */
    private ForwardingDevice createForwardingDeviceInstanceFromDeviceId(DeviceId id)
    {
        Set<Link> links = linkStore.getDeviceEgressLinks(id);

        Set<Link> links2 = linkStore.getDeviceIngressLinks(id);

        int tmp = links.size();

        tmp+=links2.size();

        numberOfRNN += tmp;

        int simpleID = -1;

        simpleID = Integer.parseInt(id.toString().charAt(id.toString().length()-1)+"");

        ForwardingDevice fd = new ForwardingDevice(tmp, new DeviceIdReal(id, simpleID), simpleID);

        for (Host host: hostService.getHosts())
        {
            if(host.location().deviceId().equals(id))
            {
                try {
                    if(host.ipAddresses().iterator().hasNext()) {
                        fd.connectedHosts.add(new HostIdReal(host.id(),host.hashCode()));
                    }
                }
                catch(Exception e)
                {
                }
            }
        }
        int j = 0;
        for (Link link: links)
        {
            simpleID =Integer.parseInt(  link.dst().deviceId().toString().charAt(id.toString().length()-1)+"");
            fd.links.add(new PortDeviceIdTuple(j, new DeviceIdReal(link.dst().deviceId(),simpleID), link.src().port().toLong()));
            j++;
        }
        for(Link link: links2)
        {
            boolean addLink = true;
            for (int i = 0; i < fd.links.size(); i++)
            {
                if(fd.links.get(i).deviceId.equals(new DeviceIdReal(link.src().deviceId(),link.hashCode())))
                {
                    addLink =false;
                    break;
                }
            }
            if(addLink) {
                simpleID =Integer.parseInt(  link.src().deviceId().toString().charAt(id.toString().length()-1)+"");
                fd.links.add(new PortDeviceIdTuple(j, new DeviceIdReal(link.src().deviceId(),simpleID), link.dst().port().toLong()));
                j++;
            }
        }
        fd.numberOfConnectedPorts = fd.links.size();

        ArrayList<Daemon> forwardingDeviceDaemons = new ArrayList<>();
        for (int i = 0; i < NetState.getDaemons().size(); i++)
        {
            if(NetState.getDaemons().get(i).getDeviceId().equals(fd.getDeviceControllerId()))
            {
                Daemon daemon = NetState.getDaemons().get(i);

                forwardingDeviceDaemons.add(daemon);
            }
        }
        fd.setDaemons(forwardingDeviceDaemons);
        return fd;
    }

    /**
     * Method for masking movement for source host. After this method is activated
     * source host when trying to connect to previousHosts connects to newHost.
     * @param previousHosts
     * @param newHost
     * @param sourceHost
     * @param tub
     */
    public void maskMovemement( HostId previousHosts, HostId newHost, HostId sourceHost, boolean tub)
    {
        log.info("MASKING HOST");
        DeviceId locationSourceDevice = getLocationOfHost(sourceHost);
        DeviceId locationNewHost = getLocationOfHost(newHost);
        int index1 = NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(locationSourceDevice, getSimpleIDByControllerID(locationSourceDevice)));
        int index2 = NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(locationNewHost, getSimpleIDByControllerID(locationNewHost)));
        PathFlow flow = PathTranslator.getBestPathFromSrcDst(NetState.getTopology().get(index1).getSimpleID(),NetState.getTopology().get(index2).getSimpleID());
        IpAddress IPpreviousHost = null;
        IpAddress IPnewHost = null;
        IpAddress IPsourceHost = null;

        if(tub==true)
        {
            try {
                IPpreviousHost = getIpAddressByHostId(previousHosts);
                IPnewHost = getIpAddressByHostId(newHost);
                IPsourceHost = getIpAddressByHostId(sourceHost);
            }
            catch (Exception e)
            {
                log.error("PROBLEM ACCESSING HOSTS");
            }
        }
        else
        {
            IPpreviousHost = IpAddress.valueOf(NetState.RSU_1_IP);
            IPnewHost = IpAddress.valueOf(NetState.RSU_2_IP);
            IPsourceHost = IpAddress.valueOf(NetState.CAR_IP);
        }


        TrafficSelector.Builder selectorBuilder5005 = DefaultTrafficSelector.builder();

        selectorBuilder5005
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IPpreviousHost.toIpPrefix())
                .matchIPSrc(IPsourceHost.toIpPrefix());

        TrafficTreatment treatment5005 = DefaultTrafficTreatment.builder()
                .setEthDst(newHost.mac())
                .setIpDst(IPnewHost)
                .setOutput(PortNumber.portNumber(flow.getPath().get(0).getOutputPort()))
                .build();

        ForwardingObjective forwardingObjective5005 = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder5005.build())
                .withTreatment(treatment5005)
                .withPriority(50100)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .makePermanent()
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(locationSourceDevice, forwardingObjective5005);

        if(tub == true) {
            TrafficSelector.Builder selectorBuilder5006 = DefaultTrafficSelector.builder();

            selectorBuilder5006
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(IPsourceHost.toIpPrefix())
                    .matchIPSrc(IPnewHost.toIpPrefix());

            TrafficTreatment treatment5006 = DefaultTrafficTreatment.builder()
                    .setIpSrc(IPpreviousHost)
                    .setEthSrc(previousHosts.mac())
                    .setOutput(hostService.getHost(sourceHost).location().port())
                    .build();

            ForwardingObjective forwardingObjective5006 = DefaultForwardingObjective.builder()
                    .withSelector(selectorBuilder5006.build())
                    .withTreatment(treatment5006)
                    .withPriority(50100)
                    .withFlag(ForwardingObjective.Flag.SPECIFIC)
                    .makePermanent()
                    .fromApp(appId)
                    .add();
            flowObjectiveService.forward(locationSourceDevice, forwardingObjective5006);
        }

        int real = NetState.getSimpleIDByIdOfConnectedHost(new HostIdReal(newHost, 0));
        VisAppComp.setHostTwo(NetState.getBySimpleID(real).getDeviceControllerId().toString());
    }

    /**
     * Gets bytes for given MRI unit
     * @param mri Given MRI unit
     * @return number of bytes transferred through the MRI
     */
    protected long getBytesOnMRI(MultiRoutingInfo mri)
    {
        HostId sourceHost = getHostIdByIpAddress(IpAddress.valueOf(mri.getSrc()));
        DeviceId sourceDevice;
        try {
            sourceDevice = getLocationOfHost(sourceHost);
        }
        catch (Exception e)
        {
            try {
                int simpleID = NetState.getSimpleIdByIpOfDaemon(mri.getSrc().toString());
                DeviceIdReal realId = (DeviceIdReal)NetState.getBySimpleID(simpleID).getDeviceControllerId();
                sourceDevice = realId.getRealDeviceId();
            }
            catch (Exception e1)
            {
                return 0;
            }
        }
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(mri.getProto())
                .matchIPDst(IpPrefix.valueOf(mri.getDst()+"/32"))
                .matchIPSrc(IpPrefix.valueOf(mri.getSrc()+"/32"));
        if(mri.getProto() == IPv4.PROTOCOL_UDP)
        {
            selector.matchUdpDst(TpPort.tpPort(mri.getTcpDstPort()));//.matchUdpSrc(TpPort.tpPort(mri.getTcpSrcPort()));
        }
        if(mri.getProto() == IPv4.PROTOCOL_TCP)
        {
            selector.matchTcpDst(TpPort.tpPort(mri.getTcpDstPort()));//.matchTcpSrc(TpPort.tpPort(mri.getTcpSrcPort()));
        }

        log.info("CREATED: "+selector.toString());
        Iterator<FlowEntry> iterator= flowRuleService.getFlowEntries(sourceDevice).iterator();

        int counter = 0;
        while(iterator.hasNext()) {
            counter++;
            FlowEntry flowEntry = iterator.next();
            if (flowEntry.priority() == NetState.DEFAULT_NORMAL_PACKET_PRIORITY+1|| true)
            {
                //log.info(flowEntry.selector().toString()+" EQUALS: "+ selector.build().toString()+" ?");
               if(flowEntry.selector().equals(selector.build()))
               {
                    log.info("BYTES:::");
                   return flowEntry.bytes();
               }
            }
        }
        log.info("ON DEVICE "+sourceDevice.toString()+" DISCOVERED "+counter+" FLOW ENTRIES");
        return 0;
    }

    /**
     * Getter for the number of bytes that went through the given device in 1 second
     * @param deviceId given device
     * @return Value of bytes per seconds on that device
     */
    public long getBytesOnDevice(DeviceId deviceId)
    {
        Iterator<FlowEntry> iterator= flowRuleService.getFlowEntries(deviceId).iterator();

        long bytesOnDevice = 0;

        while(iterator.hasNext())
        {
            bytesOnDevice += iterator.next().bytes();
        }
        return bytesOnDevice;
    }

    /**
     * Gets real number of bytes send via the given flow
     * @param flow given flow
     * @return Number of bytes
     */
    public long getBytesOnFlow(PathFlow flow)
    {
        ForwardingDevice sourceDevice = NetState.getBySimpleID(flow.getSource()), destinationDevice = NetState.getBySimpleID(flow.getDestination());

        DeviceIdReal real = (DeviceIdReal) sourceDevice.getDeviceControllerId();
        Iterator<FlowEntry> iterator = flowRuleService.getFlowEntries(real.getRealDeviceId()).iterator();

        long bytesOnFlow = 0;

        while(iterator.hasNext())
        {
            FlowEntry entry = iterator.next();
            for (int i = 0; i < sourceDevice.getConnectedHosts().size(); i++)
            {
                HostIdReal realHost = (HostIdReal)sourceDevice.getConnectedHosts().get(i);

                IpAddress sourceAddress = getIpAddressByHostId(realHost.getRealHostId());
                for (int j = 0; j <destinationDevice.getConnectedHosts().size(); j++)
                {
                    HostIdReal hostIdReal = (HostIdReal)destinationDevice.getConnectedHosts().get(i);
                    IpAddress destinationAddress = getIpAddressByHostId(hostIdReal.getRealHostId());

                    TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

                    selectorBuilder.matchIPDst(destinationAddress.toIpPrefix()).matchIPSrc(sourceAddress.toIpPrefix());

                    if(entry.selector().getCriterion(Criterion.Type.IPV4_DST).equals(selectorBuilder.build().getCriterion(Criterion.Type.IPV4_DST)))
                    {
                        if(entry.selector().getCriterion(Criterion.Type.IPV4_SRC).equals(selectorBuilder.build().getCriterion(Criterion.Type.IPV4_SRC)))
                        {
                            bytesOnFlow+=entry.bytes();

                            if(NetState.LOG_VERBOSITY > 4)
                            {
                                log.info("@#@# ON FLOW : "+flow.toString()+" BYTES ADDED : "+entry.bytes());
                            }
                        }
                    }
                }
            }
        }
        log.info("@#@# ON FLOW : "+flow.toString()+" BYTES ADDED : "+bytesOnFlow);
        return bytesOnFlow;
    }

    /**
     * Checks if host is in a restricted vlan id network
     * @param realId id of the host to check
     * @return true if it's a restricted vlan
     */
    public static boolean checkVlanId(HostIdReal realId)
    {
        for (int i = 0; i < NetState.restrictedVlans.size(); i++)
        {
            if(realId.getRealHostId().vlanId().equals(VlanId.vlanId(NetState.restrictedVlans.get(i))))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Getter for the application id in the ONOS framework
     * @return the SRE application id from the onos framework
     */
    public ApplicationId getAppId() {
        return appId;
    }

    /**
     * Getter for the port number to which the given host is connected to
     * @param id Id of the given host
     * @return number of the port to which the given host is connected to
     */
    public PortNumber getPortNumberOfHost(HostId id)
    {
        return hostService.getHost(id).location().port();
    }

    /**
     * An implementation of the real path installer.
     */
    public static class RealPathInstaller implements PathInstaller {

        ArrayList<PathFlow> discoveryPaths = new ArrayList<>();
        public static AppComponent master;

        public RealPathInstaller(AppComponent master)
        {
            this.master = master;
        }

        @Override
        public void installPath(PathFlow flow, long specificPort) {

            if (flow.getDstHostId() != null && flow.getSrcHostId() != null) {
                HostIdReal realDstHostId = (HostIdReal) flow.getDstHostId();
                HostIdReal realSrcHostId = (HostIdReal) flow.getSrcHostId();
                IpAddress ipDst = NetState.MASTER.getIpAddressByHostId(realDstHostId.getRealHostId());
                IpAddress ipSrc = NetState.MASTER.getIpAddressByHostId(realSrcHostId.getRealHostId());
                int priority = NetState.DEFAULT_NORMAL_PACKET_PRIORITY;
                if (checkVlanId(realDstHostId) == false && checkVlanId(realSrcHostId) == false) {

                    for (int i = 0; i < flow.getPath().size(); i++) {

                        ForwardingDevice fd = NetState.getBySimpleID(flow.getPath().get(i).getNodeDeviceId());
                        DeviceIdReal realDeviceId = (DeviceIdReal) fd.getDeviceControllerId();

                        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

                        if (specificPort == -1) {
                            selectorBuilder
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchEthDst(realDstHostId.getRealHostId().mac())
                                    .matchEthSrc(realSrcHostId.getRealHostId().mac())
                                    .matchIPDst(ipDst.toIpPrefix())
                                    .matchIPSrc(ipSrc.toIpPrefix())
                                    .build();
                        } else {
                            selectorBuilder
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchEthDst(realDstHostId.getRealHostId().mac())
                                    .matchEthSrc(realSrcHostId.getRealHostId().mac())
                                    .matchIPDst(ipDst.toIpPrefix())
                                    .matchIPSrc(ipSrc.toIpPrefix())
                                    .matchIPProtocol((byte) IpProtocol.UDP.value())
                                    .matchTcpDst(TpPort.tpPort((int) specificPort))
                                    .build();
                            priority = NetState.DEFAULT_SERVICE_FLOW_PRIORITY;
                        }

                        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                .setOutput(PortNumber.portNumber(flow.getPath().get(i).getOutputPort()))
                                .build();

                        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                                .withSelector(selectorBuilder.build())
                                .withTreatment(treatment)
                                .withPriority(priority)
                                .withFlag(ForwardingObjective.Flag.VERSATILE)
                                .makePermanent()
                                .fromApp(RealCommunicator.getMaster().getAppId())
                                .add();

                        master.installFlowFromPath(forwardingObjective, realDeviceId.getRealDeviceId());
                    }
                }
            }
            else
            {
                ForwardingDevice fdsrc = NetState.getBySimpleID(flow.getSource())
                        ,fddst = NetState.getBySimpleID(flow.getDestination());
                if(NetState.LOG_VERBOSITY>4)
                    UnifiedCommunicationModule.log.info("Installing paths for ALL hosts between "+flow.getSource()+" and "+flow.getDestination());
                ArrayList<Identificator> srcHosts = fdsrc.getConnectedHosts();
                ArrayList<Identificator> dstHosts = fddst.getConnectedHosts();
                for (int i = 0; i < flow.getPath().size(); i++) {

                    ForwardingDevice fd = NetState.getBySimpleID(flow.getPath().get(i).getNodeDeviceId());
                    DeviceIdReal realDeviceId = (DeviceIdReal) fd.getDeviceControllerId();
                    TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                    for(Identificator srcid : srcHosts)
                    {
                        IpAddress ipSrc = NetState.MASTER.getIpAddressByHostId(((HostIdReal)srcid).getRealHostId());
                        for(Identificator dstid: dstHosts)
                        {
                            IpAddress ipDst = NetState.MASTER.getIpAddressByHostId(((HostIdReal)dstid).getRealHostId());

                            if(NetState.LOG_VERBOSITY>6)
                                UnifiedCommunicationModule.log.info("Installing path between host "+ipSrc.toString()+" and "+ipDst.toString());

                            selectorBuilder
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchEthDst(((HostIdReal)dstid).getRealHostId().mac())
                                    .matchEthSrc(((HostIdReal)srcid).getRealHostId().mac())
                                    .matchIPDst(ipDst.toIpPrefix())
                                    .matchIPSrc(ipSrc.toIpPrefix())
                                    .build();

                            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                    .setOutput(PortNumber.portNumber(flow.getPath().get(i).getOutputPort()))
                                    .build();
                            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                                    .withSelector(selectorBuilder.build())
                                    .withTreatment(treatment)
                                    .withPriority(NetState.DEFAULT_NORMAL_PACKET_PRIORITY)
                                    .withFlag(ForwardingObjective.Flag.SPECIFIC)
                                    .makePermanent()
                                    .fromApp(RealCommunicator.getMaster().getAppId())
                                    .add();
                            master.installFlowFromPath(forwardingObjective, realDeviceId.getRealDeviceId());
                        }
                    }
                }
            }
        }
        @Override
        public void installCognitivePath(PathFlow flow)
        {
            if(NetState.PATH_FORMAT == NetState.PathFormat.HOST_SPECIFIC)
            {
                ForwardingDevice sourceDevice = NetState.getBySimpleID(flow.getSource()), destinationDevice = NetState.getBySimpleID(flow.getDestination());
                for (int i = 0; i <flow.getPath().size(); i++)
                {
                    for (int j = 0; j < sourceDevice.getDaemons().size(); j++)
                    {
                        for (int k = 0; k < destinationDevice.getDaemons().size(); k++)
                        {
                            ForwardingDevice fd = NetState.getBySimpleID(flow.getPath().get(i).getNodeDeviceId());
                            DeviceIdReal realDeviceId = (DeviceIdReal)fd.getDeviceControllerId();

                            DaemonForwarder destinationDaemon = (DaemonForwarder)destinationDevice.getDaemons().get(k);
                            DaemonForwarder sourceDaemon = (DaemonForwarder)sourceDevice.getDaemons().get(j);
                            TrafficSelector.Builder selectorBuilder5004 = DefaultTrafficSelector.builder();
                            selectorBuilder5004
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchIPProtocol(IPv4.PROTOCOL_UDP)
                                    .matchUdpDst(TpPort.tpPort(5006))
                                    .matchIPDst(IpPrefix.valueOf(destinationDaemon.getIpAddress()+"/32"))
                                    .matchIPSrc(IpPrefix.valueOf(sourceDaemon.getIpAddress()+"/32"))
                                    .build();
                            TrafficTreatment treatment5004 = DefaultTrafficTreatment.builder()
                                    .setUdpDst(TpPort.tpPort(5004))
                                    .setOutput(PortNumber.portNumber(flow.getPath().get(i).getOutputPort()))
                                    .build();

                            ForwardingObjective forwardingObjective5004 = DefaultForwardingObjective.builder()
                                    .withSelector(selectorBuilder5004.build())
                                    .withTreatment(treatment5004)
                                    .withPriority(50000)
                                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                                    .makePermanent()
                                    .fromApp(NetState.MASTER.getAppId())
                                    .add();
                            master.installFlowFromPath(forwardingObjective5004,realDeviceId.getRealDeviceId());
                        }
                    }
                }
            }
            if(NetState.PATH_FORMAT == NetState.PathFormat.BASIC)
            {
                int dest = flow.getDestination();
                int src = flow.getSource();
                for (int i = 0; i<flow.getPath().size(); i++) {

                    ForwardingDevice fd = NetState.getBySimpleID(flow.getPath().get(i).getNodeDeviceId());
                    DeviceIdReal realDeviceId = (DeviceIdReal)fd.getDeviceControllerId();

                    try {
                        ArrayList<Identificator> destinationHosts = NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID(dest)).connectedHosts;
                        ArrayList<Identificator> sourceHosts = NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID(src)).connectedHosts;

                        for (int sourceIter = 0; sourceIter < sourceHosts.size(); sourceIter++) {
                            for (int destinationIter = 0; destinationIter < destinationHosts.size(); destinationIter++) {

                                HostIdReal dstRealHostId = (HostIdReal)destinationHosts.get(destinationIter);
                                HostIdReal srcRealHostId = (HostIdReal)sourceHosts.get(sourceIter);

                                if (master.getIpAddressByHostId(dstRealHostId.getRealHostId()) != null && master.getIpAddressByHostId(srcRealHostId.getRealHostId()) != null) {
                                    //between clients
                                    if(RealCommunicator.getMaster().checkVlanId(dstRealHostId) == false && RealCommunicator.getMaster().checkVlanId(srcRealHostId))
                                    {

                                        TrafficSelector.Builder selectorBuilder5005 = DefaultTrafficSelector.builder();
                                        selectorBuilder5005
                                                .matchEthType(Ethernet.TYPE_IPV4)
                                                .matchEthDst(dstRealHostId.getRealHostId().mac())
                                                .matchEthSrc(srcRealHostId.getRealHostId().mac())
                                                .matchIPSrc(RealCommunicator.getMaster().getIpAddressByHostId(srcRealHostId.getRealHostId()).toIpPrefix())
                                                .matchIPDst(RealCommunicator.getMaster().getIpAddressByHostId(dstRealHostId.getRealHostId()).toIpPrefix())
                                                .build();

                                        TrafficTreatment treatment5005 = DefaultTrafficTreatment.builder()
                                                .setOutput(PortNumber.portNumber(flow.getPath().get(i).getOutputPort()))
                                                .build();

                                        ForwardingObjective forwardingObjective5005 = DefaultForwardingObjective.builder()
                                                .withSelector(selectorBuilder5005.build())
                                                .withTreatment(treatment5005)
                                                .withPriority(49999)
                                                .withFlag(ForwardingObjective.Flag.VERSATILE)
                                                .makePermanent()
                                                .fromApp(RealCommunicator.getMaster().getAppId())
                                                .add();
                                        master.installFlowFromPath(forwardingObjective5005, realDeviceId.getRealDeviceId());
                                    }
                                }
                            }
                        }
                    }
                    catch(Exception e)
                    {

                    }
                    ForwardingDevice sourceDevice = NetState.getBySimpleID(src), destinationDevice = NetState.getBySimpleID(dest);
                    //Between Forwarders

                    for (int j = 0; j < sourceDevice.getDaemons().size(); j++)
                    {
                        for (int k = 0; k < destinationDevice.getDaemons().size(); k++)
                        {
                            DaemonForwarder destinationDaemon = (DaemonForwarder)destinationDevice.getDaemons().get(k);
                            DaemonForwarder sourceDaemon = (DaemonForwarder)sourceDevice.getDaemons().get(j);
                            TrafficSelector.Builder selectorBuilder5004 = DefaultTrafficSelector.builder();
                            selectorBuilder5004
                                    .matchEthType(Ethernet.TYPE_IPV4)
                                    .matchIPProtocol(IPv4.PROTOCOL_UDP)
                                    .matchUdpDst(TpPort.tpPort(5006))
                                    .matchIPDst(IpPrefix.valueOf(destinationDaemon.getIpAddress()+"/32"))
                                    .matchIPSrc(IpPrefix.valueOf(sourceDaemon.getIpAddress()+"/32"))
                                    .build();
                            TrafficTreatment treatment5004 = DefaultTrafficTreatment.builder()
                                    .setUdpDst(TpPort.tpPort(5004))
                                    .setOutput(PortNumber.portNumber(flow.getPath().get(i).getOutputPort()))
                                    .build();

                            ForwardingObjective forwardingObjective5004 = DefaultForwardingObjective.builder()
                                    .withSelector(selectorBuilder5004.build())
                                    .withTreatment(treatment5004)
                                    .withPriority(50000)
                                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                                    .makePermanent()
                                    .fromApp(NetState.MASTER.getAppId())
                                    .add();
                            master.installFlowFromPath(forwardingObjective5004,realDeviceId.getRealDeviceId());
                        }
                    }
                }
                String result;
                result = VisAppComp.SetForwardingDevices(NetState.getTopology());
                if (NetState.LOG_VERBOSITY > 10) {
                    UnifiedCommunicationModule.log.info("@INSTALATION OF PATH: " + flow.toString() + " ENDED");
                    UnifiedCommunicationModule.log.info("VISUALISATION: "+result);
                }
            }

        }

        @Override
        public void installDiscoveryPath(PathFlow flow)
        {
            ForwardingDevice sourceDevice = NetState.getBySimpleID(flow.getSource()), destinationDevice = NetState.getBySimpleID(flow.getDestination());
            for (int i = 0; i <flow.getPath().size(); i++)
            {
                for (int j = 0; j < sourceDevice.getDaemons().size(); j++)
                {
                    for (int k = 0; k < destinationDevice.getDaemons().size(); k++)
                    {
                        ForwardingDevice fd = NetState.getBySimpleID(flow.getPath().get(i).getNodeDeviceId());
                        DeviceIdReal realDeviceId = (DeviceIdReal)fd.getDeviceControllerId();

                        DaemonForwarder destinationDaemon = (DaemonForwarder)destinationDevice.getDaemons().get(k);
                        DaemonForwarder sourceDaemon = (DaemonForwarder)sourceDevice.getDaemons().get(j);
                        TrafficSelector.Builder selectorBuilder5004 = DefaultTrafficSelector.builder();
                        selectorBuilder5004
                                .matchEthType(Ethernet.TYPE_IPV4)
                                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                                .matchUdpDst(TpPort.tpPort(5006))
                                .matchIPDst(IpPrefix.valueOf(destinationDaemon.getIpAddress()+"/32"))
                                .matchIPSrc(IpPrefix.valueOf(sourceDaemon.getIpAddress()+"/32"))
                                .build();
                        TrafficTreatment treatment5004 = DefaultTrafficTreatment.builder()
                                .setUdpDst(TpPort.tpPort(5004))
                                .setOutput(PortNumber.portNumber(flow.getPath().get(i).getOutputPort()))
                                .build();

                        ForwardingObjective forwardingObjective5004 = DefaultForwardingObjective.builder()
                                .withSelector(selectorBuilder5004.build())
                                .withTreatment(treatment5004)
                                .withPriority(NetState.DEFAULT_DISCOVERY_PACKET_PRIORITY)
                                .withFlag(ForwardingObjective.Flag.VERSATILE)
                                .makeTemporary(NetState.DEFAULT_DISCOVERY_FLOW_TIMEOUT)
                                .fromApp(NetState.MASTER.getAppId())
                                .add();
                        master.installFlowFromPath(forwardingObjective5004,realDeviceId.getRealDeviceId());
                    }
                }
            }

            if(!discoveryPaths.contains(flow))
            {
                discoveryPaths.add(flow);
            }

        }

        @Override
        public void installServicePath(PathFlow flow, long specificPort, String serviceStaticIpAddress)
        {
            if (flow.getDstHostId() != null && flow.getSrcHostId() != null) {
                HostIdReal realDstHostId = (HostIdReal) flow.getDstHostId();
                HostIdReal realSrcHostId = (HostIdReal) flow.getSrcHostId();
                IpAddress ipDst = NetState.getIpById(realDstHostId);
                IpAddress ipSrc = NetState.getIpById(realSrcHostId);
                int priority, tmp_port_number = 0;

                if (NetState.MASTER.checkVlanId(realDstHostId) == false && NetState.MASTER.checkVlanId(realSrcHostId) == false) {

                    for (int i = 0; i < flow.getPath().size(); i++) {

                        ForwardingDevice fd = NetState.getBySimpleID(flow.getPath().get(i).getNodeDeviceId());
                        DeviceIdReal realDeviceId = (DeviceIdReal) fd.getDeviceControllerId();

                        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                        selectorBuilder
                                .matchEthType(Ethernet.TYPE_IPV4)
                                .matchEthDst(realDstHostId.getRealHostId().mac())
                                .matchEthSrc(realSrcHostId.getRealHostId().mac())
                                .matchIPDst(ipDst.toIpPrefix())
                                .matchIPSrc(ipSrc.toIpPrefix())
                                .matchIPProtocol((byte) IpProtocol.UDP.value())
                                .matchUdpDst(TpPort.tpPort((int) specificPort))
                                .build();
                        priority = NetState.DEFAULT_SERVICE_FLOW_PRIORITY;
                        TrafficTreatment treatment;
                        treatment = DefaultTrafficTreatment.builder()
                            .setOutput(PortNumber.portNumber(flow.getPath().get(i).getOutputPort()))
                            .build();
                        if(i == 0)
                            tmp_port_number = flow.getPath().get(i).getOutputPort();

                        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                                .withSelector(selectorBuilder.build())
                                .withTreatment(treatment)
                                .withPriority(priority)
                                .withFlag(ForwardingObjective.Flag.VERSATILE)
                                .makePermanent()
                                .fromApp(RealCommunicator.getMaster().getAppId())
                                .add();
                        master.installFlowFromPath(forwardingObjective, realDeviceId.getRealDeviceId());
                    }
                    ForwardingDevice sourceDevice = NetState.getBySimpleID(flow.getSource());
                    TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
                    selectorBuilder
                            .matchEthType(Ethernet.TYPE_IPV4)
                            .matchEthDst(realDstHostId.getRealHostId().mac())
                            .matchEthSrc(realSrcHostId.getRealHostId().mac())
                            .matchIPDst(IpAddress.valueOf(serviceStaticIpAddress).toIpPrefix())
                            .matchIPSrc(ipSrc.toIpPrefix())
                            .matchIPProtocol((byte) IpProtocol.UDP.value())
                            .matchUdpDst(TpPort.tpPort((int) specificPort))
                            .build();
                    priority = NetState.DEFAULT_SERVICE_FLOW_PRIORITY+100;
                    TrafficTreatment treatment;
                    treatment = DefaultTrafficTreatment.builder()
                            .setIpDst(ipDst)
                            .setOutput(PortNumber.portNumber(tmp_port_number))
                            .build();
                    ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                            .withSelector(selectorBuilder.build())
                            .withTreatment(treatment)
                            .withPriority(priority)
                            .withFlag(ForwardingObjective.Flag.VERSATILE)
                            .makePermanent()
                            .fromApp(RealCommunicator.getMaster().getAppId())
                            .add();
                    master.installFlowFromPath(forwardingObjective,((DeviceIdReal)sourceDevice.getDeviceControllerId()).getRealDeviceId());
                    selectorBuilder = DefaultTrafficSelector.builder();
                    selectorBuilder
                            .matchEthType(Ethernet.TYPE_IPV4)
                            .matchEthDst(realDstHostId.getRealHostId().mac())
                            .matchEthSrc(realSrcHostId.getRealHostId().mac())
                            .matchIPDst(ipSrc.toIpPrefix())
                            .matchIPSrc(ipDst.toIpPrefix())
                            .matchIPProtocol((byte) IpProtocol.UDP.value())
                            .matchUdpDst(TpPort.tpPort((int) specificPort))
                            .build();
                    priority = NetState.DEFAULT_SERVICE_FLOW_PRIORITY+100;
                    treatment = DefaultTrafficTreatment.builder()
                            .setIpSrc(ipDst)
                            .setOutput(NetState.MASTER.getPortNumberOfHost(((HostIdReal)flow.getSrcHostId()).getRealHostId()))
                            .build();
                    forwardingObjective = DefaultForwardingObjective.builder()
                            .withSelector(selectorBuilder.build())
                            .withTreatment(treatment)
                            .withPriority(priority)
                            .withFlag(ForwardingObjective.Flag.VERSATILE)
                            .makePermanent()
                            .fromApp(RealCommunicator.getMaster().getAppId())
                            .add();
                    master.installFlowFromPath(forwardingObjective,((DeviceIdReal)sourceDevice.getDeviceControllerId()).getRealDeviceId());
                }
            }

        }

        @Override
        public ArrayList<PathFlow> getInstalledPaths() {
            return null;
        }

        @Override
        public ArrayList<PathFlow> getDiscoveryPaths() {
            return discoveryPaths;
        }


    }

    public Iterable<FlowEntry> getFlowsOnDevice( DeviceId id)
    {
        return flowRuleService.getFlowEntries(id);
    }
    public void addPacketProcessor(PacketProcessor processor, TrafficSelector selector, DeviceId id)
    {
        packetService.addProcessor(processor,PacketProcessor.director(1));
        //packetService.requestPackets(selector, PacketPriority.REACTIVE, appId,Optional.ofNullable(id));
        //packetService.requestPackets(selector, PacketPriority.REACTIVE, appId);
    }
    public void requestsPackets(TrafficSelector selector, DeviceId id)
    {
        packetService.requestPackets(selector,PacketPriority.CONTROL,appId,Optional.ofNullable(id));
    }
    public void delPacketProcessor(PacketProcessor processor, TrafficSelector selector, DeviceId id)
    {
        packetService.removeProcessor(processor);
        packetService.cancelPackets(selector, PacketPriority.REACTIVE, appId, Optional.ofNullable(id));
    }
    public void delPacketProcessor(PacketProcessor processor)
    {
        packetService.removeProcessor(processor);
    }
    public void blockIpAddress(IpAddress ip)
    {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(ip.toIpPrefix());
        treatmentBuilder.drop();
        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withPriority(NetState.AP_PROFILE_PRIOROTY+303)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(NetState.MASTER.getAppId())
                .add();
        for(ForwardingDevice fd : NetState.getTopology())
        {
            installFlowFromPath(forwardingObjective, ((DeviceIdReal)fd.getDeviceControllerId()).getRealDeviceId());
        }
    }
    public void unblockAllIpAddresses()
    {
        for(FlowEntry flow : flowRuleService.getFlowEntriesById(this.appId))
        {
            if(flow.priority() == NetState.DEFAULT_BLOCK_PRIORITY+303)
            {
                flowRuleService.removeFlowRules(flow);
            }
        }
    }
    public void cutBitrateOnDevice(DeviceId id, PortNumber port)
    {

    }
}

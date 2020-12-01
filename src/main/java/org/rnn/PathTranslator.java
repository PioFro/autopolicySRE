package org.rnn;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.cli.net.IpProtocol;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.rnn.Identificators.DeviceIdReal;
import org.rnn.Identificators.HostIdReal;
import org.rnn.Identificators.Identificator;
import org.rnn.implementation.RealCommunicator;
import java.util.ArrayList;
import static org.rnn.AppComponent.checkVlanId;

/**
 * This class is a interface between RoutingModule and it's decisions and
 * controller's actions on particular devices.
 * @author Piotr Frohlich
 * @version Beta 2.0.0
 *
 *
 */
public class PathTranslator{

    /**
     * Lock access to list of New paths and installed path to avoid
     * concurrent modification exception
     */
    private static boolean lock = false;

    /**
     * Array of new paths, waiting to be installed.
     */
    protected static ArrayList<PathFlow> newPaths = new ArrayList<PathFlow>();
    /**
     * Array of RoutingPathUnits being equivalent of installed paths and their
     * competitors.
     */
    private static ArrayList<RoutingPathUnit> installedPaths = new ArrayList<RoutingPathUnit>();
    private static ArrayList<PathFlow> newPathsHostSpecific = new ArrayList<>();
    private static ArrayList<RoutingPathUnit> installedPathsHostSpecific = new ArrayList<>();

    /**
     * Array of MultiRoutingInfo (for multi route flow rules)
     */
    private static ArrayList<MultiRoutingInfo> trafficInformation = new ArrayList<MultiRoutingInfo>();

    protected static long tmp = 0;

    protected static AppComponent master;

    public static long lastCheck = System.currentTimeMillis();
    /**
     * Constructor. Invoked in AppComponent's activate method.
     *
     * @param defaultParam Default flow install interval in millis.
     */
    public PathTranslator(AppComponent master, int defaultParam)
    {
        newPaths = new ArrayList<PathFlow>();
        installedPaths = new ArrayList<RoutingPathUnit>();
        this.master = master;
    }

    /**
     * Installation of given path in devices along it's way.
     */
    public static void installPath(PathFlow flow)
    {
        UnifiedCommunicationModule.pathInstaller.installPath(flow, -1);
        /**
        int specificPort = -1;
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
        String result = VisAppComp.SetForwardingDevices(NetState.getTopology());
        if (NetState.LOG_VERBOSITY > 2) {
            UnifiedCommunicationModule.log.info("@INSTALATION OF PATH: " + flow.toString() + " ENDED");
            UnifiedCommunicationModule.log.info("VISUALISATION: "+result);

        }
    **/
    }

    public static void installCognitivePath(PathFlow flow)
    {
        //UnifiedCommunicationModule.pathInstaller.installCognitivePath(flow);

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
            if (NetState.LOG_VERBOSITY > -2) {
                UnifiedCommunicationModule.log.info("@INSTALATION OF PATH: " + flow.toString() + " ENDED");
                UnifiedCommunicationModule.log.info("VISUALISATION: "+result);
            }
        }

        if (NetState.LOG_VERBOSITY > -2) {
            UnifiedCommunicationModule.log.info("@INSTALATION OF  ForwardingObjective forwardingObjective5005 = DefaultForwardingObjective.builder()\n" +
                                                        "                                        .withSelector(selectorBuilder5005.build())\n" +
                                                        "                                        .withTreatment(treatment5005)\n" +
                                                        "                                        .withPriority(49999)\n" +
                                                        "                                        .withFlag(ForwardingObjective.Flag.VERSATILE)\n" +
                                                        "                                        .makePermanent()\n" +
                                                        "                                        .fromApp(appId)\n" +
                                                        "                                        .add();COGNITIVE PATH: " + flow.toString() + " ENDED");
        }
    }

    /**
     * Installs given path as discovery flow for Cognitive Packets only.
     *
     */
    public static void installDiscoverypath(PathFlow flow, int priority)
    {
        //UnifiedCommunicationModule.pathInstaller.installDiscoveryPath(pathFlow);
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
/**
        if(!discoveryPaths.contains(flow))
        {
            discoveryPaths.add(flow);
        }
**/

        if (NetState.LOG_VERBOSITY > 0) {
            UnifiedCommunicationModule.log.info("@INSTALATION RANDOMIZED OF PATH: " + flow.toString() + " ENDED");
        }
    }

    public static void installServicePath(PathFlow flow, long specificPort, String serviceStaticIpAddress)
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
                        .matchIPDst(IpAddress.valueOf(serviceStaticIpAddress).toIpPrefix())
                        .matchIPSrc(ipSrc.toIpPrefix())
                        .matchIPProtocol((byte) IpProtocol.UDP.value())
                        .matchUdpDst(TpPort.tpPort((int) specificPort))
                        .build();
                priority = NetState.DEFAULT_SERVICE_FLOW_PRIORITY+100;
                TrafficTreatment treatment;
                treatment = DefaultTrafficTreatment.builder()
                        .setIpDst(ipDst)
                        .setEthDst(realDstHostId.getRealHostId().mac())
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
                        .setIpSrc(IpAddress.valueOf(serviceStaticIpAddress))
                        .setOutput(master.getPortNumberOfHost(((HostIdReal)flow.getSrcHostId()).getRealHostId()))
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
    public static void installPathsWithMRI(PathFlow flow)
    {
        ArrayList<MultiRoutingInfo> mris = getMRIForSrcDst(flow.source, flow.destination);
        long bytesSentOverall = 0;
        for (int i = 0; i < mris.size(); i++)
        {
            bytesSentOverall += mris.get(i).getBytesSent();
        }
        RoutingPathUnit rpu = getAllPathsForSrcDst(flow.source, flow.destination);
        double scoreOverall =0, scoreOfBestPath = rpu.getBestPath().checkMeanScore();
        ArrayList<PathFlow> flows = new ArrayList<PathFlow>();
        for (int i = 0; i < rpu.getPaths().size(); i++)
        {
            double tmpScore = rpu.getPaths().get(i).checkMeanScore();
            if(scoreOfBestPath<tmpScore)
            {
                scoreOverall+=tmpScore;
                flows.add(rpu.getPaths().get(i));
            }
            else if(scoreOfBestPath<=(tmpScore+NetState.DEFAULT_TRESHOLD))
            {
                scoreOverall+=tmpScore;
                flows.add(rpu.getPaths().get(i));
            }
        }
        for (int j = 0; j < mris.size(); j++)
        {   double mriFitnessCoverage= 0.5;
            try {
                mriFitnessCoverage = mris.get(j).getBytesSent() / bytesSentOverall;
            }catch (Exception e){}
            int idOfFlow = -1;
            double min = 2.0;
            for (int i = 0; i < flows.size(); i++)
            {
                double flowFitnessPercentage = 0.5;
                try {
                     flowFitnessPercentage = flows.get(i).checkMeanScore() / scoreOverall;
                }catch (Exception e){}
                if (flowFitnessPercentage>=mriFitnessCoverage)
                {
                    if(min > flowFitnessPercentage - mriFitnessCoverage)
                    {
                        min = flowFitnessPercentage - mriFitnessCoverage;
                        idOfFlow = i;
                    }
                }
            }
            if(idOfFlow != -1)
            {
                installPath(flows.get(idOfFlow),mris.get(j));
            }
            else installPath(flows.get(rpu.getBestPathIndex()), mris.get(j));
        }

    }
    public static void installPath(PathFlow flow, MultiRoutingInfo mri)
    {

    }

    /**
     * Adding new path to wait to being ready to be installed or stored for
     * later use.
     *
     * @param path given path.
     */
    public static void addNewPath(PathFlow path) {
        if (newPaths.size() > 0 && NetState.LOG_VERBOSITY > 1)
            UnifiedCommunicationModule.log.info("<><> [" + newPaths.size() + "]");
        if (containsPath(path, newPaths) == false)
        {
            UnifiedCommunicationModule.log.info("Path "+path.toString()+" added to new paths");
            path.setInstallationDueTime(System.currentTimeMillis() + NetState.DEFAULT_FLOW_INSTALL_INTERVAL);
            newPaths.add(path);
            addToRoutingPathUnit(path);
        }
    }
    public static void addNewPathHostSpecific(PathFlow path) {
        if (newPathsHostSpecific.size() > 0 && NetState.LOG_VERBOSITY > 1)
            UnifiedCommunicationModule.log.info("<><> [" + newPathsHostSpecific.size() + "]");
        if (containsHostsTuple(path)==false)
        {
            UnifiedCommunicationModule.log.info("Path "+path.toString()+" added to new paths host specific");
            path.setInstallationDueTime(System.currentTimeMillis() + NetState.DEFAULT_FLOW_INSTALL_INTERVAL);
            newPathsHostSpecific.add(path);
            addToHostSpecificRoutingPathUnit(path);
        }
    }

    public static boolean containsHostsTuple(PathFlow path)
    {
        for (int i = 0; i <newPathsHostSpecific.size(); i++)
        {
            if(newPathsHostSpecific.get(i).dstHostId.equals(path.dstHostId)&&newPathsHostSpecific.get(i).srcHostId.equals(path.srcHostId))
            {
                if(newPathsHostSpecific.get(i).containsNodes(path))
                    return true;
            }
        }
        return false;
    }
    /**
     * Check if given path has a equivalent in given array of paths
     *
     * @param path      given path
     * @param pathFlows given array of paths
     * @return true if in array exists pats that contains the same nodes as
     * given path. Otherwise false.
     */
    private static boolean containsPath(PathFlow path, ArrayList<PathFlow> pathFlows) {
        for (PathFlow pathFlow : pathFlows) {
            if (pathFlow.containsNodes(path))
                return true;
        }
        return false;
    }

    /**
     * Adding path to RoutingPathUnits array. Then RPU decides wheather this path
     * is better or worse than it's currently best.
     *
     * @param flow path to add to RPU
     * @return returns best path for RPU corresponding with given path src-dst
     * tuple
     */
    private static PathFlow addToRoutingPathUnit(PathFlow flow) {
        for (int i = 0; i < installedPaths.size(); i++) {
            if (flow.path.size() >= 1) {
                if (installedPaths.get(i).checkSourceDestination(flow.source, flow.destination)) {
                    PathFlow path = installedPaths.get(i).addPath(flow);
                    return path;
                }
            }
        }
        if (flow.path.size() >= 1) {
            installedPaths.add(new RoutingPathUnit(flow.source, flow.destination, flow, null, null));
            installCognitivePath(flow);
        }
        return flow;
    }
    private static PathFlow addToHostSpecificRoutingPathUnit(PathFlow flow) {
        for (int i = 0; i < installedPathsHostSpecific.size(); i++) {
            if (flow.path.size() >= 1) {
                if (installedPathsHostSpecific.get(i).checkHostSourceDestination(flow.srcHostId, flow.dstHostId))
                {
                    PathFlow path = installedPathsHostSpecific.get(i).addPath(flow);
                    return path;
                }
            }
        }
        if (flow.path.size() >= 1) {
            installedPathsHostSpecific.add(new RoutingPathUnit(flow.source, flow.destination, flow, flow.srcHostId, flow.dstHostId));
            installPath(flow);
        }
        return flow;
    }

    /**
     * Getter for best path corresponding to given src-dst tuple
     *
     * @param src src of src-dst tuple
     * @param dst dst of src-dst tuple
     * @return Best path that corresponds to given src-dst tuple
     */
    public static PathFlow getBestPathFromSrcDst(long src, long dst) {
        for (RoutingPathUnit rpu : installedPaths
        ) {
            if (rpu.checkSourceDestination(src, dst)) {
                return rpu.getBestPath();
            }
        }
        return null;
    }
    public static PathFlow getBestPathFromSrcDst(Identificator src, Identificator dst) {
        for (RoutingPathUnit rpu : installedPathsHostSpecific
        ) {
            if (rpu.checkHostSourceDestination(src, dst)) {
                return rpu.getBestPath();
            }
        }
        PathFlow tmp = getBestPathFromSrcDst(NetState.getSimpleIDByIdOfConnectedHost(src),NetState.getSimpleIDByIdOfConnectedHost(dst));
        if(tmp!= null)
        {
            tmp.setSrcHostId(src);
            tmp.setDstHostId(dst);
            return tmp;
        }
        return null;

    }

    /**
     * Getter for best path corresponding to given src-dst tuple
     *
     * @param flow Path which corresponds to certain src-dst tuple
     * @return Best path that corresponds to src-dst tuple corresponding to given
     * path.
     */
    private static PathFlow getBestPathFromSrcDst(PathFlow flow) {
        for (RoutingPathUnit rpu : installedPaths
        ) {
            if (rpu.checkSourceDestination(flow.source, flow.destination)) {
                return rpu.getBestPath();
            }
        }
        return null;
    }

    /**
     * This is method used to run constantly. In this method firstly all newPaths
     * are checked if they are ready to install (installation time has passed).
     * Then paths are checked for emergency score drop. If their time has passed
     * or emergency drop was discovered they are moved to installation stage.
     * Then all of the paths that should be installed (paths in installation
     * stage, RPU) in this run are installed and the cycle starts again.
     */
    public static void checkPaths() {

        if(lastCheck+NetState.CHECK_FLOWS_TIME > System.currentTimeMillis())
            return;

        //UnifiedCommunicationModule.log.info("Checking paths");

        if (lock == true)
            return;

        lock = true;
        try {
            for (int i = 0; i < newPaths.size(); i++)//Adding paths
            {
                if (newPaths.get(i).getInstallationDueTime() <= System.currentTimeMillis())
                {
                    addToRoutingPathUnit(newPaths.get(i));
                    if (NetState.LOG_VERBOSITY > 1)
                        UnifiedCommunicationModule.log.info("<<Path added to RPUs: " + newPaths.get(i).toString() + " cause timeout");
                    newPaths.remove(i);
                    if (NetState.LOG_VERBOSITY > 2)
                        UnifiedCommunicationModule.log.info("<<Path Removed. NewPathSize: [" + newPaths.size() + "]");

                }
            }
            if(NetState.PATH_FORMAT == NetState.PathFormat.HOST_SPECIFIC) {
                for (int i = 0; i < newPathsHostSpecific.size(); i++)//Adding paths
                {
                    if (newPathsHostSpecific.get(i).getInstallationDueTime() <= System.currentTimeMillis()) {
                        addToHostSpecificRoutingPathUnit(newPathsHostSpecific.get(i));
                        if (NetState.LOG_VERBOSITY > 1)
                            UnifiedCommunicationModule.log.info("<<Path added to RPUs: " + newPathsHostSpecific.get(i).toString() + " cause timeout");
                        newPathsHostSpecific.remove(i);
                        if (NetState.LOG_VERBOSITY > 2)
                            UnifiedCommunicationModule.log.info("<<Path Removed. NewPathSize: [" + newPathsHostSpecific.size() + "]");

                    }
                }
            }
            for (int i = 0; i < installedPaths.size(); i++)//Installing all changed paths
            {
                installedPaths.get(i).calculateBestOption();
                if (installedPaths.get(i).isIdMaxChanged()) {
                    installCognitivePath(installedPaths.get(i).getBestPath());
                    installedPaths.get(i).setIdMaxChanged(false);
                }
            }
            if(NetState.PATH_FORMAT == NetState.PathFormat.HOST_SPECIFIC) {
                for (int i = 0; i < installedPathsHostSpecific.size(); i++)//Installing all changed paths
                {
                    installedPathsHostSpecific.get(i).calculateBestOption();
                    if (installedPathsHostSpecific.get(i).isIdMaxChanged()) {
                        installPath(installedPathsHostSpecific.get(i).getBestPath());
                        installedPathsHostSpecific.get(i).setIdMaxChanged(false);
                    }
                }
            }
            if(tmp<System.currentTimeMillis())
            {
                tmp=System.currentTimeMillis()+100000;
                reinstallAllBestPaths();
            }

            lock = false;
        } catch (Exception e) {
            lock = false;
        }

        String result = VisAppComp.SetForwardingDevices(NetState.getTopology());
        if(NetState.LOG_VERBOSITY>2)
        {
            //UnifiedCommunicationModule.log.info("VISUALISATION: "+ result);
        }
    }

    /**
     * Update info about scores of given path.
     * @param flow New information about path's score
     */
    public static void updatePath(PathFlow flow) {
        if(flow.dstHostId== null && flow.srcHostId==null) {
            for (int i = 0; i < installedPaths.size(); i++) {
                if (installedPaths.get(i).checkSourceDestination(flow.source, flow.destination)) {
                    installedPaths.get(i).updatePathScore(flow);
                    return;
                }
            }
            installedPaths.add(new RoutingPathUnit(flow.source, flow.destination, flow, null, null));
        }
        else
        {
            for (int i = 0; i < installedPathsHostSpecific.size(); i++) {
                if (installedPathsHostSpecific.get(i).checkHostSourceDestination(flow.srcHostId, flow.dstHostId)) {
                    installedPathsHostSpecific.get(i).updatePathScore(flow);
                    return;
                }
            }
            installedPathsHostSpecific.add(new RoutingPathUnit(flow.source, flow.destination, flow, flow.srcHostId, flow.dstHostId));
        }
        //installPath(flow, NetState.getAppId(), NetState.getTopology());
    }
    /**
     * Getter for all the best paths stored in this class
     * @return list of best paths for all source-destination tuple which exists
     */
    public static ArrayList<PathFlow> getPaths() {
        ArrayList<PathFlow> paths = new ArrayList<PathFlow>();

        for (RoutingPathUnit path : installedPaths) {
            paths.add(path.getBestPath());
        }
        return paths;
    }
    public static ArrayList<PathFlow> getHostSpecificPaths()
    {
        ArrayList<PathFlow> paths = new ArrayList<PathFlow>();

        for (RoutingPathUnit path : installedPathsHostSpecific) {
            paths.add(path.getBestPath());
        }
        return paths;
    }
    /**
     * Getter for all the all paths stored in this class
     * @return list of all paths for all source-destination tuple which exists
     */
    public static ArrayList<RoutingPathUnit> getAllPaths()
    {
        return installedPaths;
    }
    public static ArrayList<RoutingPathUnit> getHostSpecificRPU()
    {
        return installedPathsHostSpecific;
    }

    /**
     *
     */
    public static RoutingPathUnit getAllPathsForSrcDst(int src, int dst)
    {
        for (int i = 0; i < installedPaths.size(); i++)
        {
            if(installedPaths.get(i).checkSourceDestination(src, dst))
                return installedPaths.get(i);
        }
        return null;
    }
    /**
     * Getter for exactly the same path (same nodes and outputs)
     * @param flow flow to compare
     * @return Instance of the same path or null if there isn't such flow.
     */
    public static PathFlow getPathFlowConsistingOfNodes(PathFlow flow)
    {
        if(flow.srcHostId== null && flow.dstHostId == null) {
            for (RoutingPathUnit rpu : installedPaths) {
                if (rpu.checkSourceDestination(flow.source, flow.destination)) {
                    for (int i = 0; i < rpu.getPaths().size(); i++) {
                        if (rpu.getPaths().get(i).containsNodes(flow))
                            return rpu.getPaths().get(i);
                    }
                }

            }
        }
        else
        {
            for (RoutingPathUnit rpu : installedPathsHostSpecific) {
                if (rpu.checkHostSourceDestination(flow.srcHostId, flow.dstHostId)) {
                    for (int i = 0; i < rpu.getPaths().size(); i++) {
                        if (rpu.getPaths().get(i).containsNodes(flow))
                            return rpu.getPaths().get(i);
                    }
                }

            }
        }
        return null;
    }

    /**
     * Getter for all of the paths stored in Path Translator module for sake
     * of reinforcing security change impact on Network behaviour.
     * @return List of all paths stored inside Path Translator
     */
    public static ArrayList<PathFlow> getAllPathsForScoring()
    {
        ArrayList<PathFlow> paths = new ArrayList<PathFlow>();
        for (int i = 0; i < installedPaths.size(); i++)
        {
            for (int j = 0; j < installedPaths.get(i).getPaths().size(); j++)
            {
                paths.add(installedPaths.get(i).getPaths().get(j));
            }

        }
        return paths;
    }
    public static ArrayList<PathFlow> getAllPathsForScoring(Identificator host)
    {
        ArrayList<PathFlow> toReturn = new ArrayList<>();
        for (int i = 0; i < installedPathsHostSpecific.size(); i++)
        {
            if(installedPathsHostSpecific.get(i).getDstHost().equals(host))
            {
                for (int j = 0; j <installedPathsHostSpecific.get(i).getPaths().size(); j++)
                {
                    toReturn.add(installedPathsHostSpecific.get(i).getPaths().get(j));
                }
            }
            if(installedPathsHostSpecific.get(i).getSrcHost().equals(host))
            {
                for (int j = 0; j <installedPathsHostSpecific.get(i).getPaths().size(); j++)
                {
                    toReturn.add(installedPathsHostSpecific.get(i).getPaths().get(j));
                }
            }
        }
        return toReturn;
    }


    /**
     * Reinstalls all current best paths for all known flows - in case some of
     * them were malformed.
     */
    public static void reinstallAllBestPaths()
    {
        for (int i = 0; i < getPaths().size() ; i++)
        {
            installCognitivePath(getPaths().get(i));
            installPath(getPaths().get(i));
        }
        for (int i = 0; i < getHostSpecificPaths().size() ; i++)
        {
            installPath(getHostSpecificPaths().get(i));
        }
    }

    /**
     * Deletes all the paths with link that was put down
     * @param src source forwarder of link put down
     * @param dst destination forwarder of link put down
     */
    public static void processLinkDown(int src, int dst)
    {
        for (int i = 0; i < installedPaths.size(); i++)
        {
            for (int j = 0; j < installedPaths.get(i).getPaths().size(); j++)
            {
                if(installedPaths.get(i).getPaths().get(j).hasLinkBetween(src, dst))
                {
                    if(installedPaths.get(i).getPaths().size()>1)
                    {
                        installedPaths.get(i).getPaths().remove(j);
                    }
                }

                installedPaths.get(i).setBestPathIndex(0);
            }
        }
    }

    protected static void addNewTrafficInformation(MultiRoutingInfo mri)
    {
        for (int i = 0; i < trafficInformation.size(); i++)
        {
            if(trafficInformation.get(i).equals(mri))
            {
                trafficInformation.get(i).active = true;
                return;
            }
        }
        trafficInformation.add(mri);
        return;
    }
    /**
     * Deletes all paths which are going through given device
     * @param simpleDeviceId Simple id of given device
     * @return Array with deleted path
     */
    protected static ArrayList<PathFlow> deletePathsWithDeviceId( int simpleDeviceId )
    {
        ArrayList<PathFlow> deletedPaths = new ArrayList<PathFlow>();

        for (int i = 0; i < installedPaths.size(); i++)
        {
            for (int j = 0; j < installedPaths.get(i).getPaths().size(); j++)
            {
                if(installedPaths.get(i).getPaths().get(j).hasSimpleDeviceId(simpleDeviceId))
                {
                    deletedPaths.add(installedPaths.get(i).getPaths().get(j));
                    installedPaths.get(i).getPaths().remove(j);
                }
            }
        }
        return deletedPaths;
    }

    public static void checkTrafficInfo()
    {
        long timeNow = System.currentTimeMillis();

        for (int i = 0; i < trafficInformation.size(); i++)
        {
            if(trafficInformation.get(i).getTime()+NetState.MULTI_ROUTING_INFO_DEFAULT_TIMEOUT < timeNow)
            {
                long bytesSent = UnifiedCommunicationModule.getBytesOnMRI(trafficInformation.get(i));
                if(bytesSent!=trafficInformation.get(i).getBytesSent())
                {
                    trafficInformation.get(i).setBytesSent(bytesSent);
                }
                else
                {
                    //trafficInformation.get(i).active = false;
                }
                trafficInformation.get(i).setTime(System.currentTimeMillis());
            }

        }
    }
    public static ArrayList<MultiRoutingInfo> getTrafficInformation(){return trafficInformation;}
    public static ArrayList<MultiRoutingInfo> getMRIForSrcDst(int src, int dst)
    {
        ArrayList<MultiRoutingInfo> mris = new ArrayList<MultiRoutingInfo>();
        for (int i = 0; i <trafficInformation.size(); i++)
        {
            if(src == trafficInformation.get(i).getSrcDevice()&&dst == trafficInformation.get(i).getDstDevice())
            {
                mris.add(trafficInformation.get(i));
            }
        }
        return mris;
    }
    public static void updateBytesCountersOnFlows()
    {
        for (int i = 0; i <installedPaths.size(); i++)
        {
            for (int j = 0; j <installedPaths.get(i).getPaths().size(); j++)
            {
                installedPaths.get(i).getPaths().get(j).updateBytes();
            }
        }
    }
    public static ArrayList<PathFlow> getAllPathsWithLink(LinkID link)
    {
        ArrayList<PathFlow> allPaths = getAllPathsForScoring(), containingPaths = new ArrayList<>();
        for (int i = 0; i <allPaths.size(); i++)
        {
            PathFlow current = allPaths.get(i);
            for (int j = 0; j <current.getPath().size()-1; j++)
            {
                LinkID linkInCurrentPath = new LinkID(current.getPath().get(j).nodeDeviceId,current.getPath().get(j+1).getNodeDeviceId());
                if(linkInCurrentPath.equals(link))
                {
                    containingPaths.add(current);
                    break;
                }
            }
            LinkID linkInCurrentPath = new LinkID(current.getPath().get(current.getPath().size()-1).nodeDeviceId,current.getDestination());
            if(linkInCurrentPath.equals(link))
            {
                containingPaths.add(current);
            }
        }
        return containingPaths;
    }

}

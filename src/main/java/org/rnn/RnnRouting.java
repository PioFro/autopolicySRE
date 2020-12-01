
package org.rnn;
import org.rnn.Identificators.Identificator;
import org.rnn.service.BasicService;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;

/**
 * This class works as a routing director for plugin. It translates received Smart Packets and Security Indications into
 * flows (PathFlow class). Then they are send to PathTranslator module for storage,
 * installation and further evaluation.
 * @author Piotr Frohlich
 * @version Beta 2.0.0
 */
public class RnnRouting implements RoutingModule
{
    /**
     * All paths created by rnns with scoring, security aware.
     */
    private ArrayList<PathFlow> pathFlows = new ArrayList<PathFlow>();
    /**
     * For randomization. Counting SP corresponding to certain src-dst tuple;
     */
    private ArrayList<SourceDestinationTuple> smartPacketsAggregator= new ArrayList<SourceDestinationTuple>();

    /**
     * Array of send smart packets - used for timing out SPs and punishing packet loss.
     */
    private ArrayList<SourceDestinationTuple> sentPacketsAggregator= new ArrayList<SourceDestinationTuple>();

    public static DelayAgregator delayAgregator= new DelayAgregator();

    private Dictionary<String,Integer> servedPackets= new Hashtable<>();

    //public static ServicePlacementManager servicePlacementManager = new ServicePlacementManager(new BasicService("BasicService"));


    /**
     * This method is used whenever smart packet is sensed by Packet Listener in AppComponent (aka. controller) class.
     * Them it's payload is transformed to either setting up new route, only following old route or giving rewards for
     * corresponding RNNDestinationTuples. What to do depends on previous packets (considering first two options) or if
     * payload has no '~' then awards are calculated - because it is SP that reached it's destination.
     *
     * @param packetContext This is Smart Packet instance containing information if this packet's just
     *                           left or reached destination (then payload contains delays gathered along the way).
     * @throws InvaildPayloadException If smart packet parser is unable to access data in SP payload this exception is
     *                                 thrown. AppComponent then catches this exception and handles it properly.
     */
    @Override
    public void receiveSmartPacket(PacketInfo packetContext) throws InvaildPayloadException
    {
        String payload = packetContext.payload;
        if(payload.contains("|"))
        {
            String packetTimerId = ""+packetContext.payload.split("\\|")[0].split(",")[2];
            if(servedPackets.get(packetTimerId)!=null)
            {
                return;
            }
            delayAgregator.updateDelaysInfo(payload);
            servedPackets.put(packetTimerId,packetContext.hashCode());
            if(servedPackets.size()>NetState.MAX_BUFFOR_SIZE)
            {
                servedPackets = new Hashtable<>();
            }
        }
    }

    /**
     * Iterate over all paths and calculate award or punishment for all nodes
     * concerning passed SecureHost. It does it SECURITY_FACTOR / 10 times.
     * @param secureHost
     */
    @Override
    public void receiveNewSecurityScoring(SecureHost secureHost)
    {
        int simpleID = NetState.getSimpleIDByIdOfConnectedHost(secureHost.getHostId());
        if(simpleID != -1)
        {
            for (int i = 0; i < PathTranslator.getAllPathsForScoring().size(); i++) {
                if (PathTranslator.getAllPathsForScoring().get(i).startsOrEndsWithNodeId(simpleID)) {
                    boolean punished = false;
                    PathFlow scores = new PathFlow(PathTranslator.getAllPathsForScoring().get(i).source, PathTranslator.getAllPathsForScoring().get(i).destination);
                    for (int j = 0; j < PathTranslator.getAllPathsForScoring().get(i).path.size(); j++)
                    {
                        double danger = 0.0;
                        PathFlow tmp = PathTranslator.getAllPathsForScoring().get(i);
                        if(NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID(simpleID)).getMaximumSecurityFactor()>NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID(PathTranslator.getAllPathsForScoring().get(i).destination)).getMaximumSecurityFactor()) {
                            danger = (double)NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID(simpleID)).getMaximumSecurityFactor();
                        }
                        else
                            danger = (double)NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID(PathTranslator.getAllPathsForScoring().get(i).destination)).getMaximumSecurityFactor()*10;
                        double securityAward;

                        scores.addNewNode(new Node(PathTranslator.getAllPathsForScoring().get(i).path.get(j).nodeDeviceId, PathTranslator.getAllPathsForScoring().get(i).path.get(j).score, PathTranslator.getAllPathsForScoring().get(i).path.get(j).outputPort));

                        if((j+1)<PathTranslator.getAllPathsForScoring().get(i).path.size())
                        {
                            double passableDangerFactor = 110 - (double) NetState.getBySimpleID(PathTranslator.getAllPathsForScoring().get(i).path.get(j+1).nodeDeviceId).getSensivity(); // This ranges from 10 to 100 also

                            if(danger<passableDangerFactor)
                                securityAward = 0.0;
                            else
                                securityAward = danger - passableDangerFactor;
                        }
                        else
                        {
                            double passableDangerFactor = 110 - (double) NetState.getBySimpleID(PathTranslator.getAllPathsForScoring().get(i).destination).getSensivity(); // This ranges from 10 to 100 also

                            if(danger<passableDangerFactor)
                                securityAward = 0.0;
                            else
                                securityAward = danger - passableDangerFactor;
                        }

                        //Security award = sensitivity - (max(H(dst), H(src))/9) *100

                        for (int k = 0; k < 10; k++)//Award assigned 10 times;
                        {
                            try {
                                NetState.getBySimpleID(PathTranslator.getAllPathsForScoring().get(i).path.get(j).nodeDeviceId).serveAckPacket(PathTranslator.getAllPathsForScoring().get(i).source, PathTranslator.getAllPathsForScoring().get(i).destination, -(securityAward/100), (int) NetState.getBySimpleID(PathTranslator.getAllPathsForScoring().get(i).getPath().get(j).nodeDeviceId).getIdOfRealPort(PathTranslator.getAllPathsForScoring().get(i).getPath().get(j).outputPort));
                            }
                            catch (Exception e)
                            {
                                UnifiedCommunicationModule.log.error("ONE DEVICE ISN'T CONNECTED");
                            }
                        //scores.path.get(j).score-=securityAward;
                    }
                        if(securityAward!=0.0)
                        {
                            punished = true;
                            scores.path.get(j).score/=(securityAward);
                        }
                    }

                    if(punished)
                    {
                        for (int j = 0; j <10 ; j++)
                        {
                            scores.punishScores();
                            scores.punishScores();
                            scores.punishScores();
                            scores.punishScores();
                            scores.punishScores();
                            scores.punishScores();
                        }

                    }
                    PathTranslator.updatePath(scores);
                }
            }

        }
        ArrayList<PathFlow> tmp = PathTranslator.getAllPathsForScoring(secureHost.getHostId());
        for (int i = 0; i <tmp.size(); i++)
        {
            boolean punished = false;
            PathFlow scores = new PathFlow(tmp.get(i).source, tmp.get(i).destination);
            scores.srcHostId = tmp.get(i).srcHostId;
            scores.dstHostId = tmp.get(i).dstHostId;
            int danger = NetState.getHigherRatingBetweenTwoHosts(tmp.get(i).dstHostId, tmp.get(i).srcHostId)*10;

            for (int j = 0; j <tmp.get(i).path.size(); j++)
            {
                double securityAward = 0.0;
                scores.addNewNode(new Node(tmp.get(i).path.get(j).nodeDeviceId, tmp.get(i).path.get(j).score, tmp.get(i).path.get(j).outputPort));
                if(j+1<tmp.get(i).path.size())
                {
                    double passableDangerFactor = 110 - (double) NetState.getBySimpleID(tmp.get(i).path.get(j+1).nodeDeviceId).getSensivity(); // This ranges from 10 to 100 also

                    if(danger<passableDangerFactor)
                        securityAward = 0.0;
                    else
                        securityAward = danger - passableDangerFactor;
                }
                else
                {
                    double passableDangerFactor = 110 - (double) NetState.getBySimpleID(tmp.get(i).destination).getSensivity(); // This ranges from 10 to 100 also

                    if(danger<passableDangerFactor)
                        securityAward = 0.0;
                    else
                        securityAward = danger - passableDangerFactor;
                }
                for (int k = 0; k < 10; k++)//Award assigned 10 times;
                {
                    try {
                        NetState.getBySimpleID(tmp.get(i).path.get(j).nodeDeviceId).serveAckPacket(tmp.get(i).srcHostId, tmp.get(i).dstHostId, -(securityAward/100), (int) NetState.getBySimpleID(tmp.get(i).getPath().get(j).nodeDeviceId).getIdOfRealPort(tmp.get(i).getPath().get(j).outputPort));
                    }
                    catch (Exception e)
                    {
                        UnifiedCommunicationModule.log.error("ONE DEVICE ISN'T CONNECTED");
                    }
                    //scores.path.get(j).score-=securityAward;
                }

                if(securityAward!=0.0)
                {
                    punished = true;
                    scores.path.get(j).score/=(securityAward);
                }

            }
            if(punished)
            {
                for (int j = 0; j <10 ; j++)
                {
                    scores.punishScores();
                    scores.punishScores();
                    scores.punishScores();
                    scores.punishScores();
                    scores.punishScores();
                    scores.punishScores();
                }

            }
            PathTranslator.updatePath(scores);
        }
    }
    /**
     * Creating path from given SP payload.
     *
     * @param payload String representation of whole path, transported by SP
     */
    private PathFlow createPathFromPayload(String payload)
    {
        PathFlow pathFlow = new PathFlow(payload);

        if(pathFlow.source != pathFlow.destination) {
            int id = getSamePath(pathFlow);
            if (id == -1) {
                if (pathFlow.destination != pathFlow.source) {
                    pathFlows.add(pathFlow);
                }
                return pathFlow;
            } else {
                pathFlows.get(id).setScoresAlongThePath(pathFlow);
                return pathFlows.get(id);
            }
        }
        else return null;
    }
    /**
     * Get id of path given in payload
     *
     * @param path string representation of path
     * @return id of this path or -1 if there isn't a match
     */
    private int getSamePath(PathFlow path)
    {

        for (int i =0; i<pathFlows.size(); i++)
        {
            try {
                if (pathFlows.get(i).containsNodes(path)) {
                    return i;
                }
            }
            catch(Exception e)
            {}
        }
        return -1;
    }

    /**
     * Method gives award for the confirmed paths. Meaning paths which were followed by Smart Packets.
     * Award is calculated based on delay data and security data. Params change
     * percent of influence on scoring paths (more secure or faster)
     */
    private void giveAwardForNewestPath(PathFlow flow)
    {
        //TODO: Add award for every src-dst path (for host-specific cause)
        ForwardingDevice sourceDevice;
        ForwardingDevice destinationDevice;
        double award = 0.0;
        long src = flow.source, dst = flow.destination;
        try {
            sourceDevice = NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID((int) src));
            destinationDevice = NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID((int) dst));
            if(sourceDevice.equals(destinationDevice))
            {
                UnifiedCommunicationModule.log.warn("CATCHED THE SAME DEVICES: "+ sourceDevice.toString());
                return;
            }
            //Assign scores to nodes
        }
        catch(Exception e)
        {
            UnifiedCommunicationModule.log.error("DEVICE NUMBER "+src+" or "+dst+" NOT AVAILABLE");
            return;
        }
        PathFlow scores = new PathFlow(flow.source, flow.destination);
       for(int j = 0; j<flow.path.size(); j++)
       {
           int nextHopDevice;
           if((j+1)<flow.path.size())
           {
               nextHopDevice = flow.path.get(j+1).nodeDeviceId;
           }
           else
               nextHopDevice = flow.destination;

            for (int k=0; k<NetState.getTopology().size(); k++)
            {
                if ( nextHopDevice == NetState.getTopology().get(k).getSimpleID()) {

                    int securityFactor = 0;
                    if(sourceDevice.getMaximumSecurityFactor()>destinationDevice.getMaximumSecurityFactor())
                        securityFactor = sourceDevice.getMaximumSecurityFactor();
                    else
                        securityFactor = destinationDevice.getMaximumSecurityFactor();
                    double factorI = 0.0;

                    double dangerFactor =  (double)securityFactor * 10; //This is from 10 to 90

                    double passableDangerFactor = 110 - (double)NetState.getTopology().get(k).getSensivity(); // This ranges from 10 to 100 also

                    if(dangerFactor<passableDangerFactor)
                        factorI = 0.0;
                    else
                        factorI = dangerFactor - passableDangerFactor;

                    double delayMultiplyFactor = 1.0;

                    if(NetState.PATH_TRANSLATOR_ALGORITHM == 1)
                    {
                        if(flow.checkBytes()==false)
                        {
                            //Setting delay multiplier as high as security meaning
                            delayMultiplyFactor = 500;
                        }
                    }
                    if(NetState.PATH_TRANSLATOR_ALGORITHM == 2)
                    {
                        delayMultiplyFactor = flow.getPredictedDelayValues();
                    }

                    double previousRewardValue = NetState.getTopology().get(k).getRNNByDestination(flow.source, flow.destination).getPreviousAward();

                    //One value is training
                    double factorG = 0.0;
                    if(NetState.LEARNING_DATA == 1)
                    {
                        factorG = flow.getPayloadSum()*delayMultiplyFactor + 500*factorI;
                    }
                    if(NetState.LEARNING_DATA == 0) {
                        //Mean value is training
                        factorG = getMeanDelayValueOnPath(flow)*delayMultiplyFactor + 500 * factorI;
                    }
                    if(factorG== 0.0)
                        factorG = 0.0000001;


                    double currentRewardVaule = 1/factorG;
                    double factorT_L = (NetState.DEFAULT_HISTORICAL_LEARNING_PARAMETER*previousRewardValue) +(1 - NetState.DEFAULT_HISTORICAL_LEARNING_PARAMETER)*currentRewardVaule;

                    if(factorT_L < 0.000001)
                        factorT_L = 0.000001;
                    if(factorT_L > 1.0)
                        factorT_L = 1.0;

                    scores.addNewNode(new Node(flow.path.get(j).nodeDeviceId,factorT_L, flow.path.get(j).outputPort));

                    NetState.getTopology().get(k).getRNNByDestination(flow.source, flow.destination).setPreviousAward(factorT_L);
                    if(previousRewardValue<=currentRewardVaule)
                    {
                        NetState.getTopology().get(k)
                                .serveAckPacket(src, dst, award, (int)NetState.getTopology().get(k).getIdOfRealPort(flow.path.get(j).outputPort));
                    }
                    else
                    {
                        NetState.getTopology()
                                .get(k).
                                serveAckPacket(src, dst, -award, (int)NetState.
                                        getTopology().
                                        get(k).
                                        getIdOfRealPort(flow.path.get(j).outputPort));
                    }
                    break;
                }
            }
        }
        //Confirm packet reached destination
        flow.setConfirmed(false);

        if(NetState.LOG_VERBOSITY>0) {
            UnifiedCommunicationModule.log.info("#@#@ AWARDING PATH " + flow.toString() + " WITH AWARD : " + award);
        }

        PathTranslator.updatePath(scores);
    }

    /**
     * Gives punishment to all Neurons that decided to take this path.
     * @param pathFlow flow to punish
     */
    private void punishPath(PathFlow pathFlow)
    {
        long src = pathFlow.source, dst = pathFlow.destination;
        Identificator hostIdSource = pathFlow.srcHostId;
        Identificator hostIdDestination = pathFlow.dstHostId;


        //Punish all nodes along given path
        for (Node node : pathFlow.getPath()) {
            ForwardingDevice fd = NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID(node.nodeDeviceId));
            if( hostIdDestination != null && hostIdSource != null)
                fd.serveAckPacket(hostIdSource,hostIdDestination,-NetState.DEFAULT_PUNISHMENT, (int) fd.getIdOfRealPort(node.outputPort));
            else
                fd.serveAckPacket(src, dst, -NetState.DEFAULT_PUNISHMENT, (int) fd.getIdOfRealPort(node.outputPort));
        }
        if (NetState.LOG_VERBOSITY > 0)
            UnifiedCommunicationModule.log.info("@##@ PUNISHING PATH: " + pathFlow.toString());

        PathFlow punishedPath = PathTranslator.getPathFlowConsistingOfNodes(pathFlow);
        if (punishedPath != null) {
            punishedPath.punishScores();
            PathTranslator.updatePath(punishedPath);
        }
    }

    /**
     * Sets up the path between source and destination using RNNs indication.
     * Creates a path, which waits from timestamp to timestamp+DEFAULT_FLOW_TIMEOUT (given in seconds)
     * for being awarded (if packets reaches the destination with score better
     * than it done with previous path or being punished with DEFAULT_PUNISHMENT.
     *
     * @param source long representation of source
     * @param destination long representation of destination
     * @param justReturn Shall method try to install a given path?
     *
     * @return Created path
     */
    private PathFlow setupNeuralNetworksAlongThePath(long source, long destination, boolean justReturn)
    {

        setupNeuralNetworksAlongThePathHostSpecific(source,destination,justReturn);
        if(source==-1||destination==-1)
            return null;

        ForwardingDevice sourceDevice = new ForwardingDevice();

        ForwardingDevice destinationDevice = new ForwardingDevice();

        PathFlow pathFlow = new PathFlow((int)source, (int)destination);

        //pathFlow.srcHostId = source;

        //pathFlow.dstHostId = destination;

        for (int i = 0; i<NetState.getTopology().size(); i++)
        {
            /**
            if(fd.getDeviceControllerId().equals(UnifiedCommunicationModule.getLocationOfHost(source)) )
            {
                sourceDevice = fd;
            }
            if(fd.getDeviceControllerId().equals(UnifiedCommunicationModule.getLocationOfHost(destination)))
            {
                destinationDevice = fd;
            }
             */
            if(NetState.getTopology().get(i).getSimpleID()==source)
            {
                sourceDevice = NetState.getTopology().get(i);
            }
            if(NetState.getTopology().get(i).getSimpleID()==destination)
            {
                destinationDevice = NetState.getTopology().get(i);
            }
        }

        int outIter = 0;

        try {

            //Create path between src and dst
            while (sourceDevice.getDeviceControllerId().equals(destinationDevice.getDeviceControllerId()) == false) {
                if (outIter >= NetState.getTopology().size())
                    return pathFlow;
                RNNDestinationTuple rnn = sourceDevice.getRNNByDestination(source, destination);
                int maxExcited = rnn.getMaxExcited();

                int iterations = 0;
                outIter++;
                //Check all RNN indications
                while (true) {
                    int nextHop = getForwardingDeviceByDeviceId(sourceDevice.getDeviceIdFromPort(maxExcited));
                    if (nextHop < 0) {
                        if (NetState.LOG_VERBOSITY > 1)
                            UnifiedCommunicationModule.log.info("@@@Incomplete path");
                        pathFlow.addNewNode(new Node(NetState.getTopology().get(nextHop).getSimpleID(),0.0,maxExcited));
                        punishPath(pathFlow);
                        return pathFlow;
                    }
                    ForwardingDevice tmp = NetState.getTopology().get(nextHop);
                    if (tmp != null) {

                        //Changed!!
                        if (pathFlow.checkIfPathContainsSimpleID(tmp.getSimpleID()) || tmp.getIsActive() == false) {
                            maxExcited += 1;
                            if (sourceDevice.getMaxPort() <= maxExcited) {
                                maxExcited = 0;
                            }
                        } else {
                            pathFlow.addNewNode(new Node(sourceDevice.getSimpleID(), NetState.DEFAULT_SCORE_VALUE, (int) sourceDevice.getActualPortNumber(maxExcited)));
                            sourceDevice = tmp;
                            break;
                        }
                        if (iterations >= sourceDevice.getMaxPort()) {
                            if (justReturn == false)
                                punishPath(pathFlow);
                            return pathFlow;
                        }
                        iterations++;
                    } else {
                        iterations++;
                        maxExcited = 1;
                    }
                }
                //Add chosen, proper Node to path

            }
            if (justReturn == false)
                prepareForInstallation(pathFlow);

            return pathFlow;
        }
        catch(Exception e)
        {
            UnifiedCommunicationModule.log.warn("PATH CREATION FAILED. FALLBACK TO TREE.");
        }
        PathFlow tmp = null;
        try {
            tmp = PathProviderTree.getPath(source, destination);
        }
        catch (TreeProviderException e)
        {
            UnifiedCommunicationModule.log.error("EVEN FALLBACK TO TREE FAILED. "+e.getMessage());
        }
        if(justReturn == false)
            prepareForInstallation(tmp);
        return tmp;
    }

    private ArrayList<PathFlow> setupNeuralNetworksAlongThePathHostSpecific(long source, long destination, boolean justReturn)
    {
        ArrayList<PathFlow> toReturn = new ArrayList<>();
        ArrayList<Identificator> srcHosts = NetState.getBySimpleID((int)source).getConnectedHosts();
        ArrayList<Identificator> dstHosts = NetState.getBySimpleID((int)destination).getConnectedHosts();
        for (int i = 0; i <srcHosts.size(); i++)
        {
            for (int j = 0; j <dstHosts.size(); j++)
            {
                toReturn.add(setupNeuralNetworkAlongThePathSingleHostTuple(srcHosts.get(i),dstHosts.get(j),justReturn,source, destination));
            }

        }
        return toReturn;
    }
    public PathFlow setupNeuralNetworkAlongThePathSingleHostTuple(Identificator hostSrc, Identificator hostDst, boolean justReturn,long source, long destination)
    {
        if(source==-1||destination==-1)
            return null;

        ForwardingDevice sourceDevice = new ForwardingDevice();

        ForwardingDevice destinationDevice = new ForwardingDevice();

        PathFlow pathFlow = new PathFlow((int)source, (int)destination);
        pathFlow.srcHostId = hostSrc;
        pathFlow.dstHostId = hostDst;
        sourceDevice = NetState.getBySimpleID((int)source);
        destinationDevice = NetState.getBySimpleID((int)destination);

        int outIter = 0;

        try {

            //Create path between src and dst
            while (sourceDevice.getDeviceControllerId().equals(destinationDevice.getDeviceControllerId()) == false)
            {
                if (outIter >= NetState.getTopology().size())
                    return pathFlow;
                RNNDestinationTuple rnn = sourceDevice.getRNNByDestination(hostSrc, hostDst);

                int maxExcited = rnn.getMaxExcited();
                int iterations = 0;
                outIter++;
                //Check all RNN indications
                while (true) {
                    int nextHop = getForwardingDeviceByDeviceId(sourceDevice.getDeviceIdFromPort(maxExcited));
                    if (nextHop < 0) {
                        if (NetState.LOG_VERBOSITY > 1)
                            UnifiedCommunicationModule.log.info("@@@ HOST HOST Incomplete path");
                        pathFlow.addNewNode(new Node(NetState.getTopology().get(nextHop).getSimpleID(),0.0,maxExcited));
                        punishPath(pathFlow);
                        return pathFlow;
                    }
                    ForwardingDevice tmp = NetState.getTopology().get(nextHop);
                    if (tmp != null) {

                        //Changed!!
                        if (pathFlow.checkIfPathContainsSimpleID(tmp.getSimpleID()) || tmp.getIsActive() == false) {
                            maxExcited += 1;
                            if (sourceDevice.getMaxPort() <= maxExcited) {
                                maxExcited = 0;
                            }
                        } else {
                            pathFlow.addNewNode(new Node(sourceDevice.getSimpleID(), NetState.DEFAULT_SCORE_VALUE, (int) sourceDevice.getActualPortNumber(maxExcited)));
                            sourceDevice = tmp;
                            break;
                        }
                        if (iterations >= sourceDevice.getMaxPort()) {
                            if (justReturn == false)
                                punishPath(pathFlow);
                            return pathFlow;
                        }
                        iterations++;
                    } else {
                        iterations++;
                        maxExcited = 1;
                    }
                }
                //Add chosen, proper Node to path

            }
            if (justReturn == false)
                prepareForInstallation(pathFlow);

            return pathFlow;
        }
        catch(Exception e)
        {
            UnifiedCommunicationModule.log.error("PATH CREATION ERROR");
        }
        return null;
    }

    /**
     * Prepares created path for installation
     * @param pathFlow created path
     */
    private void prepareForInstallation( PathFlow pathFlow)
    {
        //Set path installation time
        pathFlow.setTimestamp(System.currentTimeMillis());
        //Not yet confirmed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        pathFlow.confirmed = false;
        if(checkIfContainsPath(pathFlow)==false&&pathFlow.path.size()>0) {
            if(NetState.LOG_VERBOSITY > 0)
                UnifiedCommunicationModule.log.info("#NEW PATH: "+pathFlow.toString());
            pathFlows.add(pathFlow);
           // PathTranslator.installPath(pathFlow, NetState.getAppId(), NetState.getTopology());
        }
        if(pathFlow.dstHostId == null || pathFlow.srcHostId == null)
            PathTranslator.addNewPath(pathFlow);
        else
            PathTranslator.addNewPathHostSpecific(pathFlow);
    }
    /**
     * Getter for id of given device in list of all devices.
     *
     * @param id ID (DeviceID) of given device
     * @return id in list of given device
     */
    private int getForwardingDeviceByDeviceId(Identificator id)
    {
        for (int i = 0; i < NetState.getTopology().size(); i++)
        {
            if(NetState.getTopology().get(i).getDeviceControllerId().equals(id))
            {
                return i;
            }
        }
        return -1;
    }

    /**
     *  Routes are randomized similarly to genes in genetic algorithm.
     *  Path is treated like a genom in which one of the chromosomes (nodes) is
     *  changed to a random value and the rest of the path (from this changed
     *  node to destination) is created on normal rules - by RNNs’ decisions.
     *  It’s performed for ⅙ of packets corresponding to certain
     *  source-destination tuple.
     * @param discoveryPathFlow Existing path
     * @return Modified path
     */
    private PathFlow randomizePathFlow(PathFlow discoveryPathFlow)
    {
        if(discoveryPathFlow.path.size()>=1) {
            PathFlow randomizedPathFlow = new PathFlow(discoveryPathFlow.source, discoveryPathFlow.destination);

            Random random = new Random();

            int cuttingPlace = random.nextInt(discoveryPathFlow.getPath().size());

            for (int i = 0; i < cuttingPlace; i++) {
                randomizedPathFlow.addNewNode(discoveryPathFlow.getPath().get(i));
            }

            ForwardingDevice cuttingPlaceDevice = new ForwardingDevice();

            for (ForwardingDevice fd : NetState.getTopology()) {
                if (fd.getSimpleID() == discoveryPathFlow.getPath().get(cuttingPlace).nodeDeviceId) {
                    cuttingPlaceDevice = fd;
                }
            }
            int forbiddenPortNumber = discoveryPathFlow.getPath().get(cuttingPlace).outputPort, randomPortNumber = 0;
            while (true) {
                randomPortNumber = random.nextInt(cuttingPlaceDevice.links.size());
                if ((int)cuttingPlaceDevice.getActualPortNumber(randomPortNumber) != forbiddenPortNumber)
                    break;
            }
            //Add randomized node
            randomizedPathFlow.addNewNode(new Node(cuttingPlaceDevice.getSimpleID(), NetState.DEFAULT_SCORE_VALUE,(int)cuttingPlaceDevice.getActualPortNumber(randomPortNumber)));
            //Take device linked to cuttingPlaceDevice by randomized port
            ForwardingDevice tmp = NetState.getTopology().get(getForwardingDeviceByDeviceId(cuttingPlaceDevice.getDeviceIdFromPort(randomPortNumber)));
            //Creation of second part of path with normal not random terms.
            PathFlow secondPartOfPath = PathTranslator.getBestPathFromSrcDst(tmp.getSimpleID(), discoveryPathFlow.getPath().get(discoveryPathFlow.getPath().size() - 1).getNodeDeviceId());
            //Join paths normal up to cutting point and secondpart
            for (Node node : secondPartOfPath.getPath()) {
                randomizedPathFlow.addNewNode(node);
            }

            randomizedPathFlow.dstHostId = discoveryPathFlow.dstHostId;

            randomizedPathFlow.srcHostId = discoveryPathFlow.srcHostId;

            if (NetState.LOG_VERBOSITY > 1)
                UnifiedCommunicationModule.log.info("RANDOMIZATION ENDED");

            return randomizedPathFlow;
        }
        return discoveryPathFlow;
    }

    /**
     * Method that checks if there is a path composed of given nodes connecting two certain nodes.
     * @param path Set of nodes which are checked in class-wrapper
     * @return true if requirements are met.
     */
    private boolean containsPath(PathFlow path)
    {
        for (PathFlow pathFlow:pathFlows)
        {
            if(pathFlow.containsNodes(path))
                return true;
        }
        return false;
    }

    /**
     * Checks if SP of given source and destination is the 6th thus it's path
     * need to be randomized. Also increments counter or (if it is the 6th packet
     * resets it)
     *
     * @param src Source of the random packet
     * @param dst Destination of the random packet
     * @return True if this packet's the sixth. Otherwise false.
     */
    private boolean checkIfRandomize(long src, long dst)
    {
        if(src==dst)
            return false;

        for (int i=0; i<smartPacketsAggregator.size(); i++)
        {
            if(smartPacketsAggregator.get(i).checkTuple(src, dst))
            {
                return smartPacketsAggregator.get(i).checkCounter();
            }
        }

        smartPacketsAggregator.add(new SourceDestinationTuple(src, dst));

        return false;
    }

    /**
     * Checks if list of all created paths contains given path
     * @param pathFlow given path
     * @return true if contains, false if don't
     */
    boolean checkIfContainsPath( PathFlow pathFlow)
    {
        for (PathFlow flow: pathFlows)
        {
            if(flow.containsNodes(pathFlow))
            {
                return true;
            }

        }
        return false;
    }
    /**
     * Getter for delay aggregator to be visible outside this class.
     * @return Whole aggregated delay wrapped with structure.
     */
    public static DelayAgregator getDelayAgregator(){return delayAgregator;}

    private double getMeanDelayValueOnPath(PathFlow path)
    {
        double sum = 0.0;
        for (int i = 0; i < path.path.size(); i++)
        {
            LinkID link;
            if(i == path.path.size()-1)
            {
                link = new LinkID(path.path.get(i).nodeDeviceId, path.destination);
            }
            else
            {
                link = new LinkID(path.path.get(i).nodeDeviceId, path.path.get(i+1).nodeDeviceId);
            }
            sum += delayAgregator.getDelayInfo(link);
        }
        return sum;
    }
}

package org.rnn;

import org.rnn.json.JSONArray;
import org.rnn.json.JSONObject;

import java.util.ArrayList;

/**
 *This class is used to aggregate data about Delay on given links. It gathers data
 * from Cognitive packets and averages it using last @HISTORICAL_MEANING_PARAMETER packets.
 *
 * @author: Piotr Frohlich
 * @version: Beta 2.0.0
 */

public class DelayAgregator
{
    public DelayAgregator() {
        linkIDDelayInfoHashMap = new ArrayList<DelayLinkIdTuple>();
    }

    class DelayInfo
    {
        long numberOfLostPackets = 0;

        long previousNumberOfLostPackets = 0;
        /**
         * Mean delay value (per HISTORICAL_MEANING_PARAMETER)
         */
        double delay = 0.0;
        /**
         * The higher the value the slower change of mean delay will be noticed
         * in delay field
         */
        long HISTORICAL_MEANING_PARAMETER = NetState.DEFAULT_HISTORICAL_DELAY_PARAMETER;

        /**
         * mean of previous delays updated by newest delay
         * @param newDelay updated info
         */
        public void updateDelayInfo(double newDelay)
        {
            delay = (delay*((double) HISTORICAL_MEANING_PARAMETER -1)+newDelay)/ HISTORICAL_MEANING_PARAMETER;
        }
        public void lostPacket(){numberOfLostPackets++;}
        public boolean checkLostPacketsWarning(int threshold)
        {
            if(numberOfLostPackets>(previousNumberOfLostPackets+threshold))
            {
                previousNumberOfLostPackets = numberOfLostPackets;
                return true;
            }
            previousNumberOfLostPackets = numberOfLostPackets;
            return false;
        }

    }

    public class DelayLinkIdTuple
    {
        LinkID linkID;
       public DelayInfo delayInfo;

        public DelayLinkIdTuple(LinkID link)
        {
            linkID = link;
            delayInfo = new DelayInfo();
        }

        public String toString()
        {
            String string = "( "+linkID.src+" , "+linkID.dst+" ) \t DELAY :"+delayInfo.delay+"\t PACKET LOSS: "+delayInfo.numberOfLostPackets;
            return string;
        }
    }

    /**
     * Tuple src-dst mapped to its mean delay value
     */
    private ArrayList<DelayLinkIdTuple> linkIDDelayInfoHashMap;

    private long deltaTime = 1000;

    private long lastUpdate;

    private int maxDeltaPacketLoss = 3;

    /**
     * Updates all nodes' delay info (nodes' that are in payload)
     * @param payload payload from received SP
     */
    public LinkID updateDelaysInfo(String payload)
    {
        if(NetState.COGNITIVE_PACKET_NOTATION == NetState.CognitivePacketNotation.PATH_ORIENTED) {
            PathFlow pathFlow = new PathFlow(payload);
            for (int i = 0; i < pathFlow.getPath().size(); i++) {
                if (i + 1 < pathFlow.getPath().size()) {
                    Node node = pathFlow.getPath().get(i);
                    Node nodeNext = pathFlow.getPath().get(i + 1);
                    if (NetState.getBySimpleID(node.nodeDeviceId).checkLink(nodeNext.nodeDeviceId)) {
                        LinkID link = new LinkID(node.nodeDeviceId, nodeNext.nodeDeviceId);
                        if (!containsLinkId(link)) {
                            linkIDDelayInfoHashMap.add(new DelayLinkIdTuple(link));
                        }
                        //RnnRouting.MASTER.log.info("UPDATED DELAYS");
                        updateDelayInfoOnLink(link, node.score);
                    }
                } else {
                    Node node = pathFlow.getPath().get(i);
                    Node nodeNext = new Node(pathFlow.destination, 0.0, 0);
                    if (NetState.getBySimpleID(node.nodeDeviceId).checkLink(nodeNext.nodeDeviceId)) {
                        LinkID link = new LinkID(node.nodeDeviceId, nodeNext.nodeDeviceId);
                        if (!containsLinkId(link)) {
                            linkIDDelayInfoHashMap.add(new DelayLinkIdTuple(link));
                        }
                        updateDelayInfoOnLink(link, node.score);
                    }
                }
            }
        }
        else if(NetState.COGNITIVE_PACKET_NOTATION == NetState.CognitivePacketNotation.LINK_ORIENTED)
        {
            String [] slicedPayload = payload.split(":");
            String ipSrc = slicedPayload[0], ipDst = slicedPayload[2], strTime = slicedPayload [1];
            int simpleIdSrc = NetState.getSimpleIdByIpOfDaemon(ipSrc),simpleIdDst = NetState.getSimpleIdByIpOfDaemon(ipDst);
            updateDelayInfoOnLink(new LinkID(simpleIdSrc,simpleIdDst),Double.parseDouble(strTime));
            return new LinkID(simpleIdSrc,simpleIdDst);
        }
        return new LinkID(0,0);
    }

    private boolean containsLinkId(LinkID link)
    {
        for (DelayLinkIdTuple tuple:linkIDDelayInfoHashMap)
        {
            if(tuple.linkID.equals(link))
            {
                return true;
            }
        }
        return false;
    }
    private void updateDelayInfoOnLink( LinkID link, double score)
    {
        for (int i = 0; i < linkIDDelayInfoHashMap.size(); i++)
        {
            if(linkIDDelayInfoHashMap.get(i).linkID.equals(link))
            {
                linkIDDelayInfoHashMap.get(i).delayInfo.updateDelayInfo(score);
                return;
            }
        }
    }

    public void updatePacketLossOnPath(PathFlow path)
    {
        if(lastUpdate+deltaTime<System.currentTimeMillis())
        {
            lastUpdate = System.currentTimeMillis();
            checkDeltaPacketLoss();
        }

        for (int i = 0; i < path.path.size(); i++)
        {
            if(i==path.path.size()-1)
                {
                    LinkID link = new LinkID(path.path.get(i).nodeDeviceId, path.destination);
                    updatePacketLossOnLink(link);
                }
            else
                {
                    LinkID link = new LinkID(path.path.get(i).nodeDeviceId, path.path.get(i+1).nodeDeviceId);
                    updatePacketLossOnLink(link);
                }
        }
    }
    private void checkDeltaPacketLoss()
    {
        for (int i = 0; i <linkIDDelayInfoHashMap.size(); i++)
        {
           // if(linkIDDelayInfoHashMap.get(i).delayInfo.checkLostPacketsWarning(this.maxDeltaPacketLoss))
           //     BlockChainAlertManager.sendAlert("SEVERE PACKET LOSS DISCOVERED ON LINK :"+linkIDDelayInfoHashMap.get(i).linkID.toString(),"CRITICAL ALERT");
        }
    }

    public void updatePacketLossOnLink(LinkID link)
    {
        for (int i = 0; i < linkIDDelayInfoHashMap.size(); i++)
        {
            if(linkIDDelayInfoHashMap.get(i).linkID.equals(link))
            {
                linkIDDelayInfoHashMap.get(i).delayInfo.lostPacket();
            }
        }
    }

    /**
     * Getter for delay info on certain link
     * @param link link (src-dst) tuple
     * @return mean delay value in
     */
    public double getDelayInfo(LinkID link)
    {
        for (int i = 0; i < linkIDDelayInfoHashMap.size(); i++)
        {
            if(linkIDDelayInfoHashMap.get(i).linkID.equals(link))
            {
                return linkIDDelayInfoHashMap.get(i).delayInfo.delay;
            }
        }
        return NetState.DEFAULT_SCORE_VALUE;
    }

    /**
     * Getter for aggregated delay info structure
     * @return aggregated delay info structure
     */
    public ArrayList<DelayLinkIdTuple> getLinkIDDelayInfoHashMap() {
        return linkIDDelayInfoHashMap;
    }
    public double getAverageDelayOverall()
    {
        double sum=0.0;
        for (int i = 0; i <linkIDDelayInfoHashMap.size(); i++)
        {
            sum+=linkIDDelayInfoHashMap.get(i).delayInfo.delay;
        }
        return ((double)sum/linkIDDelayInfoHashMap.size());
    }

    public String jsonify()
    {
        JSONArray delayAgregatorJSON = new JSONArray();

        for (int i = 0; i <linkIDDelayInfoHashMap.size(); i++)
        {
            JSONObject cnmElement = new JSONObject();
            cnmElement.put("enpoint1",NetState.getBySimpleID((int)linkIDDelayInfoHashMap.get(i).linkID.dst).getDeviceControllerId().toString());
            cnmElement.put("enpoint2",NetState.getBySimpleID((int)linkIDDelayInfoHashMap.get(i).linkID.src).getDeviceControllerId().toString());
            cnmElement.put("delay",linkIDDelayInfoHashMap.get(i).delayInfo.delay);
            cnmElement.put("packetLoss",linkIDDelayInfoHashMap.get(i).delayInfo.numberOfLostPackets - linkIDDelayInfoHashMap.get(i).delayInfo.previousNumberOfLostPackets);
            delayAgregatorJSON.put(cnmElement);
        }

        return delayAgregatorJSON.toString();
    }

}

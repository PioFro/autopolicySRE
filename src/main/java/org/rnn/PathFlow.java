package org.rnn;

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.HostId;
import org.rnn.Identificators.DeviceIdReal;
import org.rnn.Identificators.HostIdReal;
import org.rnn.Identificators.Identificator;
import org.rnn.json.JSONArray;
import org.rnn.json.JSONException;
import org.rnn.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This is representation of whole flow/path structure. All nodes in chronological order and information concerning
 * lifetime etc. of given path.
 * @author Piotr Frohlich
 * @version Beta 2.0.0
 *
 */
public class PathFlow
{
    /**
     * Array of nodes which instance of this class consists of.
     */
    ArrayList<Node> path = new ArrayList<Node>();

    /**
     * Is it confirmed by SP?
     */
    boolean confirmed = false;

    /**
     * Time of path being overdue.
     */
    long timestamp = 0;

    /**
     * Security of source host taken from @NetState class.
     */
    int srcSecurity;
    /**
     * Security of destination host taken from @NetState class.
     */
    int dstSecurity;

    /**
     * When path should be installed in millis.
     */
    long installationDueTime;
    /**
     * For host-specific actions
     */
    protected Identificator srcHostId = null;
    /**
     * for Host specific actions
     */
    protected Identificator dstHostId = null;

    /**
     * Simple id of Destination ForwardingDevice
     */
    protected int destination;
    /**
     * Simple id of Source ForwardingDevice
     */
    protected int source;

    protected long actualBytesForFlow = NetState.DEFAULT_DELTA_BYTES_FOR_FLOW;

    protected long previousBytesOverall = 0;

    /**
     * Adding new node
     * @param node node to add
     */
    public void addNewNode(Node node)
    {
        path.add(node);
    }

    /**
     * Checks if route given in packets payload is the same as this instance
     * @param payloadPath   string representation of path
     * @return  true if they are the same
     */
    public boolean compareToString(String payloadPath)
    {
        String [] stringPath = payloadPath.split("\\|");

        if(stringPath.length-1!=path.size())
            return false;

        for (int i = 1; i < stringPath.length; i++)
        {
            if(path.get(i).compareToString(stringPath[i])==false)
            {
                return false;
            }
        }
        return true;
    }
    /**
     * Basic PathFlow constructor. Creates no nodes but marks this instance as
     * path from to certain tuple.
     * @param src Simple id of Source Forwarding device
     * @param dst Simple id of destination Forwarding Device
     */
    public PathFlow (int src, int dst)
    {
        source = src;
        destination =dst;
    }

    public PathFlow(JSONObject json) throws JSONException, Exception
    {
        JSONArray nodes = json.getJSONArray("nodes");
        source = json.getInt("source");
        destination = json.getInt("destination");
        try
        {
            dstHostId = new HostIdReal(HostId.hostId(MacAddress.valueOf(json.getString("host dst id")), VlanId.NONE), json.hashCode());
            srcHostId = new HostIdReal(HostId.hostId(MacAddress.valueOf(json.getString("host src id")), VlanId.NONE), json.hashCode());
        }
        catch (Exception e){}
        for (int i = 0; i <nodes.length(); i++)
        {
            JSONObject jNode = (JSONObject)nodes.get(i);
            Node node = new Node(jNode.getInt("id"),0.0, jNode.getInt("output"));
            path.add(node);
        }
        for (int i = 0; i <path.size()-1; i++)
        {
           ForwardingDevice currentFD = NetState.getBySimpleID(path.get(i).nodeDeviceId);
           DeviceIdReal outDevice = (DeviceIdReal)currentFD.getDeviceIdFromActualPort(path.get(i).outputPort);
           if(outDevice.toLong() != path.get(i+1).nodeDeviceId)
           {
               throw  new Exception("Given path is inpossible. There is no connection between "
                                                 +currentFD.getDeviceControllerId().toString()
                                                 +" and "+outDevice.toString()
                                                 + " through port "+path.get(i).outputPort);
           }
        }
        ForwardingDevice currentFD = NetState.getBySimpleID(path.get(path.size()-1).nodeDeviceId);
        DeviceIdReal outDevice = (DeviceIdReal)currentFD.getDeviceIdFromActualPort(path.get(path.size()-1).outputPort);
        if(outDevice.toLong() != destination)
        {
            throw  new Exception("Given path is inpossible. There is no connection between "
                                              +currentFD.getDeviceControllerId().toString()
                                              +" and "+outDevice.toString()
                                              + " through port "+path.get(path.size()-1).outputPort);
        }
    }

    /**
     * Creates instance of path from it's string representation;
     * @param payloadPath string representation of a single path
     */
    public PathFlow(String payloadPath)
    {
        String [] stringPath = payloadPath.split("\\|");
        String tmp = stringPath[0].split(",")[0];
        if(tmp.contains("."))
        {
            destination = NetState.getSimpleIdByIpOfDaemon(tmp);
        }
        else
            destination = NetState.getSimpleIdByIpOfDaemon(NetState.IP_PREFIX_DAEMON+tmp);
        confirmed = true;
        double scoreSum = 0.0;
        for (int i = 1; i < stringPath.length ; i++)
        {
            String [] nodeStrings = stringPath[i].split(",");

            long port = 0;


            if(i+1<stringPath.length) {
                String[] nodeStrings2 = stringPath[i + 1].split(",");
                double tmp2 =Double.parseDouble(nodeStrings2[1])-scoreSum;
                if(tmp2<0)
                    tmp2 = tmp2*(-1);
                if(tmp2>99990)
                    tmp2 = 1;
                port = NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID(NetState.getSimpleIdByIpOfDaemon(nodeStrings[0]))).getPortByDeviceId(NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID(NetState.getSimpleIdByIpOfDaemon(nodeStrings2[0]))).getDeviceControllerId());
                addNewNode(new Node(NetState.getSimpleIdByIpOfDaemon(nodeStrings[0]),tmp2 , (int) port));
                scoreSum+=Double.parseDouble(nodeStrings2[1]);

            }
        }

        try {
            source = path.get(0).nodeDeviceId;
        }
        catch (Exception e)
        {

        }

    }

    /**
     * Calcuates score of instance of the path.
     *
     * @return score of the path.
     */
    public double calculateScore()
    {
        double overallScore = 0.0;
        boolean flag = true;
        for (Node node: path)
        {
            if(node.score>1)
            {
                flag = false;
                overallScore += node.score;
            }
            else
            {
                flag = true;
                overallScore+=node.score;
            }
        }
        if(flag==false)
            return  1/overallScore;
        else
            return overallScore/((double)NetState.getTopology().size()*((double)path.size()/(double)NetState.getTopology().size()));
    }

    public double checkMeanScore()
    {
        ArrayList<DelayAgregator.DelayLinkIdTuple> tmp = RnnRouting.getDelayAgregator().getLinkIDDelayInfoHashMap();
        double score = 0.0;
        if(source == destination)
            return 0.0;
        if(srcHostId != null && dstHostId!= null) {
            if (srcHostId.equals(dstHostId))
                return 0.0;
        }
        int srcDanger = NetState.getMaxHostDangerForDevice(this.source);
        int dstDanger = NetState.getMaxHostDangerForDevice(this.destination);
        int danger = 0;
        if(srcDanger>dstDanger)
            danger = srcDanger;
        else
            danger = dstDanger;

        if(srcHostId!= null && dstHostId != null)
        {
            danger = NetState.getHigherRatingBetweenTwoHosts(srcHostId,dstHostId);
        }
        double factorI = 0.0;


        for (int i = 0; i < path.size(); i++)
        {
            if(i+1<path.size())
            {
                LinkID link = new LinkID(path.get(i).nodeDeviceId, path.get(i+1).nodeDeviceId);
                double meanScore = RnnRouting.getDelayAgregator().getDelayInfo(link);


                double dangerFactor =  (double)danger * 10; //This is from 10 to 90

                double passableDangerFactor = 110 - (double)NetState.getBySimpleID(path.get(i+1).nodeDeviceId).getSensivity(); // This ranges from 10 to 100 also

                if(dangerFactor<passableDangerFactor)
                    factorI = 0.0;
                else
                    factorI = dangerFactor - passableDangerFactor;

                //factorI = (double)NetState.getBySimpleID(path.get(i+1).nodeDeviceId).getSensivity();

                score +=meanScore;
                score += (factorI)*100;
            }
            else
            {
                LinkID link = new LinkID(path.get(i).nodeDeviceId, this.destination);
                double meanScore = RnnRouting.getDelayAgregator().getDelayInfo(link);
                double dangerFactor =  (double)danger * 10; //This is from 10 to 90

                double passableDangerFactor = 110 - (double)NetState.getBySimpleID(this.destination).getSensivity(); // This ranges from 10 to 100 also

                if(dangerFactor<passableDangerFactor)
                    factorI = 0.0;
                else
                    factorI = dangerFactor - passableDangerFactor;
                //factorI = (double)NetState.getBySimpleID(destination).getSensivity();
                score +=meanScore;
                score += (factorI)*100;
            }
        }

        if(NetState.PATH_TRANSLATOR_ALGORITHM == 1)
        {
            //Flow didn't fit through one of the devices
            if(this.checkBytes() == false)
            {
                UnifiedCommunicationModule.log.info("Flow didn't fit on given device");
                score*=1000;
            }
        }

        return 1/score;

    }

    /**
     * Getter for array of nodes which this path consists of.
     *
     * @return array of nodes
     */
    public ArrayList<Node> getPath (){return this.path;}

    /**
     * Checks if node of given simple id (meaning integer value) in given instance of the path.
     * @param simpleID ID to check.
     * @retur True if such node is present in array of nodes. Otherwise false.
     */

    public boolean checkIfPathContainsSimpleID(int simpleID)
    {
        for (Node node:path)
        {
            if(node.nodeDeviceId == simpleID)
                return true;
        }
        return false;
    }

    /**
     * Setter for when the path should be overdue.
     * @param timestamp time when the path is overdue.
     */
    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }

    /**
     * Parser to string
     * @return PathFlow parsed to string
     */
    public String toString(){
        String string = "{";
        if(dstHostId != null)
        {
            string+=" Host dst: "+dstHostId.toString()+" , ";
        }
        if(srcHostId!=null)
        {
            string+=" Host src: "+srcHostId.toString()+" , ";
        }
        for (Node node:path)
        {
            string +=node.toString()+",";
        }
        string+="} score: "+this.checkMeanScore();
        return string;
    }
    /**
     * Assigns scores of parameter to nodes on this instance of path.
     * @param pathFlow path containing the same nodes but filled with scores.
     */
    public void setScoresAlongThePath(PathFlow pathFlow)
    {
        Iterator<Node> scores = pathFlow.path.iterator();

        for (Node node: path)
        {
            node.score = scores.next().score;
        }
        this.confirmed = true;
    }

    /**
     * Update this instance of Path Flow with new delay info with respect to
     * previous DEFAULT_HISTORICAL_DELAY_PARAMETER from NetState number of packets
     * @param pathFlow Path containing new delay information
     */

    public void updateWithHistoricalData(PathFlow pathFlow)
    {
        Iterator<Node> scores = pathFlow.path.iterator();

        for (Node node: path)
        {
            node.score = (scores.next().score+(node.score*(NetState.DEFAULT_HISTORICAL_DELAY_PARAMETER-1)))/NetState.DEFAULT_HISTORICAL_DELAY_PARAMETER;
        }
        this.confirmed = true;
    }

    /**
     * Check if the path given as a parameter has the same nodes as this instance of path.
     * @param pathFlow path given as a parameter.
     * @return True if paths contain the same nodes. Otherwise false.
     */
    public boolean containsNodes(PathFlow pathFlow)
    {
        if(pathFlow.getPath().size()!=path.size())
            return false;
        else
            for (int i = 0; i < pathFlow.getPath().size(); i++)
            {
                if(path.get(i).nodeDeviceId != pathFlow.getPath().get(i).nodeDeviceId)
                    return false;
            }
        return true;
    }

    /**
     * Checks if this path instance starts or ends with given simple device id
     * @param nodeDeviceId given simple device id
     * @return true if this is starting or ending device. False if it isn't
     */
    public boolean startsOrEndsWithNodeId(int nodeDeviceId)
    {
        return (source==nodeDeviceId||destination==nodeDeviceId);
    }
    /**
     * Setter for confirmed value.
     * @param value Value to set.
     */
    public void setConfirmed(boolean value){confirmed = value;}
    /**
     * Setter for installationDueTime value.
     * @param time Value to set.
     */
    public void setInstallationDueTime( long time)
    {
        installationDueTime = time;
    }

    /**
     * Getter for installationDueTime value
     * @return installationDueTime value
     */
    public long getInstallationDueTime(){return installationDueTime;}

    /**
     * For PathTranslator scores.
     */
    public void punishScores()
    {
        for (int i = 0; i < path.size(); i++)
        {
            path.get(i).score/=1.2;
        }
    }

    /**
     * Checks if score of this path is lower than given score
     * @param score given score
     * @return True if given score is greater than score of this path by treshold.
     */
    public boolean checkIfTresholdCrossed(double score)
    {
        if(score<this.checkMeanScore())
        {
            if((this.checkMeanScore()-score) > NetState.TRESHOLD_OF_SCORE)
                return true;
        }
        return false;
    }

    /**
     * Getter for unmodified sum of scores along the path
     * @return Sum of scores along the path
     */
    public double getPayloadSum()
    {
        double sum = 0.0;
        for (Node node:path)
        {
           sum+=node.score;
        }
        return sum;
    }

    /**
     * Checks if this instance of path has given link.
     * @param src Source of the link
     * @param dst Destination of the link
     * @return true if path has given link, false otherwise.
     */
    public boolean hasLinkBetween(int src, int dst)
    {
        Node previousNode = new Node(-1, 0.0, -1);
        for (Node node : path)
        {
            if(node.nodeDeviceId ==dst&&previousNode.nodeDeviceId == src)
            {
                return true;
            }
            previousNode = node;
        }
        return false;
    }

    /**
     * Checks if given device id exists within this instance of path
     * @param simpleDeviceId Simple device id of device given as parameter
     * @return True if given simple device id exists within this path. False otherwise
     */
    public boolean hasSimpleDeviceId(int simpleDeviceId)
    {
        if(destination==simpleDeviceId)
            return true;
        for (Node node: path)
        {
            if(node.nodeDeviceId == simpleDeviceId)
            {
                return true;
            }

        }
        return false;
    }

    public boolean checkBytes()
    {
        for (int i = 0; i <path.size(); i++)
        {
            if(!NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID(path.get(i).getNodeDeviceId())).addNewBytes(this.actualBytesForFlow))
            {
                return false;
            }
        }
        return true;
    }
    public double getPredictedDelayValues()
    {
        double sum = 0;
        for (int i = 0; i <path.size(); i++)
        {
            sum+=NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID(path.get(i).getNodeDeviceId())).getPredictedDeayValue();
        }
        return sum;
    }

    public void updateBytes()
    {
        long currentBytesOverall =UnifiedCommunicationModule.getBytesOnFlow(this);
        actualBytesForFlow = (currentBytesOverall-previousBytesOverall)/NetState.DEFAULT_TIME_REFRESHING_BYTES_COUNTERS;
        previousBytesOverall = currentBytesOverall;
    }

    public int getSource() {
        return source;
    }

    public int getDestination() {
        return destination;
    }

    public Identificator getDstHostId() {
        return dstHostId;
    }

    public Identificator getSrcHostId() {
        return srcHostId;
    }

    public void setDstHostId(Identificator dstHostId) {
        this.dstHostId = dstHostId;
    }

    public void setSrcHostId(Identificator srcHostId) {
        this.srcHostId = srcHostId;
    }
}

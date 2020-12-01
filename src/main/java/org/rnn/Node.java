package org.rnn;

/**
 * Equivalent of a single node in PathFlow class.
 * @author Piotr Frohlich
 * @version Beta 2.0.0
 */
public class Node
{
    /**
     * Simple id of node
     */
    int nodeDeviceId;
    /**
     * Score assigned to the node by SP (0 if unset)
     */
    double score;
    /**
     * Port to next node in path. This is a real port number value (not an index)
     */
    int outputPort;


    /**
     * Constructor of Node.
     * @param nodeDeviceId Simple Device ID
     * @param score Score received or unset by SP
     */
    public Node(int nodeDeviceId, double score)
    {
        this.nodeDeviceId = nodeDeviceId;
        this.score = score;
    }
    /**
     * Constructor of Node.
     * @param nodeDeviceId Simple Device ID
     * @param score Score received or unset by SP
     * @param outputPort Port to next node in path
     */
    public Node(int nodeDeviceId, double score, int outputPort)
    {
        this.nodeDeviceId = nodeDeviceId;
        this.score = score;
        this.outputPort = outputPort;
    }

    /**
     * Setter for output port
     * @param outputPort given output port
     */
    public void setOutputPort(int outputPort)
    {
        this.outputPort = outputPort;
    }

    /**
     * This method is used to compare if this the path taken by the SP
     * @param node String equivalent of node
     * @return  true if they are the same as deviceId of this node
     */
    public boolean compareToString(String node)
    {

        String [] nodeList = node.split(",");


        return Integer.parseInt(nodeList[0])==nodeDeviceId;

    }

    /**
     * Getter for nodeID field.
     * @return Simple ID of device
     */
    public int getNodeDeviceId(){return nodeDeviceId;}

    public int getOutputPort() {
        return outputPort;
    }

    /**
     * Getter for score field.
     * @return Score of this instance of node in path.
     */
    public double getScore(){return score;}

    /**
     * Parser to string.
     * @return String representation of this instance of node.
     */
    public String toString()
    {
        return "[OF:"+nodeDeviceId+", OUT: "+outputPort+" ]";
    }

}
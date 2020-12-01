package org.rnn;

import org.onosproject.net.PortNumber;
import org.rnn.Identificators.HostIdReal;
import org.rnn.Identificators.Identificator;
import org.rnn.implementation.FlowEndpoint;
import org.rnn.json.JSONArray;
import org.rnn.json.JSONObject;
import org.rnn.policy.Criteria;
import org.rnn.policy.Policy;

import java.util.ArrayList;

/**
 * Data container. Contains Tuples of Host and assigned to them: IP, Security Score, explanation and simple ID of device
 * to which host is connected.
 * @author Piotr Fr&ouml;hlich
 * @version Beta 2.0.0
 *
 */
public class SecureHost
{
    /**
     * ID of host. Assigned by controller.
     */
    private Identificator hostId;

    /**
     * Level of security. Takes values <0,9></0,9> where 0 means completely safe, 9 means completely unsafe.
     */
    private int securityLevel;

    /**
     * IP address of corresponding host.
     */
    private String ipAddress;

    /**
     *Simple id of device to which host is connected to.
     */
    private int simpleID;

    /**
     * Verbose explanation of security rating. Given by PVT, administrator or whoever has access to REST interface of
     * controller.
     */
    private String explanation;

    private ArrayList<FlowEndpoint> scoredTriplets =new ArrayList<>();

    private Policy policy = new Policy();



    /**
     *
     */

    /**
     * Contructor - all fields are filled with given parameters.
     * @param host Id of this host
     * @param lvl level of danger of this host
     * @param ipString primary IP of this host
     * @param explanation String explanation of value of score
     */
    public SecureHost(Identificator host, int lvl, String ipString, String explanation)
    {
        hostId = host;
        securityLevel = lvl;
        ipAddress = ipString;
        String[] array = ipString.split("\\.");
        this.explanation = explanation;
        simpleID = Integer.parseInt(array[array.length-1]);

    }

    /**
     * Constructor for endpoints without IP address. Notice that routing won't
     * consider them as endpoints but it will consider them as danger indicatiors.
     * @param host Id of this host
     * @param lvl level of danger of this host
     * @param explanation String explanation of value of score
     */
    public SecureHost(Identificator host, int lvl, String explanation)
    {
        hostId = host;
        securityLevel = lvl;
        this.explanation = explanation;
        simpleID = host.hashCode();
    }

    @Deprecated
    /**
     * Check if given security level is higher than this of this instance.
     * @param lvl security level to check
     * @return True if given security level is higher. Otherwise false.
     */
    public boolean checkSecurityLevel(int lvl)
    {
        return (securityLevel<lvl);
    }

    /**
     * Parser to string.
     * @return String representation of class.
     */
    public String toString()
    {
        String ret = hostId.toString()+"     IP:   "+ipAddress.toString()+"     SECURITY LEVEL: "+securityLevel+"  EXPLANATION:    "+ explanation+" PORTS : \n";
        for (int i = 0; i <scoredTriplets.size(); i++)
        {
            ret+="PORT : "+scoredTriplets.get(i).port+",PROTOCOL : "+scoredTriplets.get(i).protocol+" ,DANGER : "+scoredTriplets.get(i).value+" ,TRUST: "+scoredTriplets.get(i).value+"\n";
        }
        ret += policy.toString();
        return ret;
    }

    /**
     * Getter for simpleID field
     * @return simple id of device to which this instance is connected.
     */
    public int getSimpleID(){return simpleID;}

    /**
     * Getter for security level field.
     * @return Integer value of security level of this instance.
     */
    public int getSecurityLevel(){return securityLevel;}

    /**
     * Getter for Host id field
     * @return id of this host
     */
    public Identificator getHostId(){return hostId;}

    public void setSecurityLevel(int securityLevel) {
        this.securityLevel = securityLevel;
    }

    public void addTriplet(PortNumber p, byte proto, String type, int value)
    {
        for (int i = 0; i < scoredTriplets.size(); i++)
        {
            if(scoredTriplets.get(i).port.equals(p)&&scoredTriplets.get(i).protocol == proto&&type.equalsIgnoreCase(scoredTriplets.get(i).type))
            {
                if(type.equalsIgnoreCase("trust")&&scoredTriplets.get(i).value>value)
                {
                    scoredTriplets.get(i).value = value;
                    return;
                }
                else if(type.equalsIgnoreCase("danger")&&scoredTriplets.get(i).value<value)
                {
                    scoredTriplets.get(i).value = value;
                    return;
                }
            }

        }
        scoredTriplets.add(new FlowEndpoint(p,proto,value,type));
    }

    public ArrayList<FlowEndpoint> getScoredTriplets(){return scoredTriplets;}

    public String getIpAddress() {
        return ipAddress;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public void updatePolicy(Criteria criteria)
    {
        this.policy.addCriterium(criteria);
    }

    public JSONObject jsonify()
    {
        JSONObject obj = new JSONObject();
        JSONObject scores = new JSONObject();
        JSONArray arrayScores = new JSONArray();
        JSONObject policy = new JSONObject();

        if(getScoredTriplets().size() < 1)
        {
            JSONObject triplet = new JSONObject();
            triplet
                    .put("port", PortNumber.ALL.toString())
                    .put("protocol","all")
                    .put("type of score","danger")
                    .put("value",NetState.DEFAULT_SECURITY)
                    .put("ip address","0.0.0.0");
            arrayScores.put(triplet);
        }


        for (int i = 0; i <scoredTriplets.size(); i++)
        {
            arrayScores.put(scoredTriplets.get(i).jsonify());
        }
        scores.put("entries",arrayScores);

        obj
                .put("host id",((HostIdReal)hostId).getRealHostId().toString())
                .put("host ip",this.getIpAddress())
                .put("host traffic scores",scores)
                .put("policy manager",this.policy.jsonify());

        return obj;
    }
}
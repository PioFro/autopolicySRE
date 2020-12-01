package org.rnn.implementation;

import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onosproject.net.PortNumber;
import org.rnn.UnifiedCommunicationModule;
import org.rnn.json.JSONObject;

/**
 * @author Piotr Frohlich
 * @version 1.0.1
 * This class is a representation of the flow endpoint. It gathers all information
 * about the endpoint (given in fields). If any field is left uninitiated it will
 * be set to a default (ALL) value.
 */

public class FlowEndpoint
{
    /**
     * Port of this instance of the flow endpoint.
     */
    public PortNumber port;
    /**
     * Protocol of this instance of the flow endpoint.
     */
    public byte protocol;
    /**
     * Value of the score assigned to that endpoint.
     */
    public int value;
    /**
     * It specifies what type is the score assigned to this endpoint.
     */
    public String type;
    /**
     * IP address assigned to this flow endpoint.
     */
    public IpAddress ipAddress;

    /**
     * Basic constructor. Initializes every field with the given value.
     * @param p Port number
     * @param proto Protocol (IPv4)
     * @param value Value of the score
     * @param type Type of the score.
     */
    public FlowEndpoint(PortNumber p, byte proto, int value, String type)
    {
        port = p;
        protocol = proto;
        this.value = value;
        this.type = type.toLowerCase();
    }

    /**
     * Constructor parses the string to the endpoint instance.
     * @param str Notation of the string "PROTOCOL:IP_ADDRESS:PORT_NUMBER"
     */
    public FlowEndpoint(String str)
    {
        String tmp[] = str.split(":");
        if(tmp.length != 3)
        {
            UnifiedCommunicationModule.log.error("Given flow endpoint is wrong");
            port = null;
            return;
        }
        if(tmp[0].equalsIgnoreCase("udp"))
        {
            protocol = IPv4.PROTOCOL_UDP;
        }
        else if(tmp[0].equalsIgnoreCase("tcp"))
        {
            protocol = IPv4.PROTOCOL_TCP;
        }
        else if(tmp[0].equalsIgnoreCase("ALL"))
        {
            protocol = 0;
        }

        ipAddress = IpAddress.valueOf(tmp[1]);
        if(tmp[2].equalsIgnoreCase("ALL"))
        {
            port = PortNumber.ALL;
        }
        else
            port = PortNumber.portNumber(Long.parseLong(tmp[2]));

    }

    public static String getHumanReadableProtocol(byte proto)
    {
        if(proto == IPv4.PROTOCOL_UDP)
        {
            return "UDP";
        }
        if(proto == IPv4.PROTOCOL_TCP)
        {
            return "TCP";
        }
        return "ALL";

    }

    public JSONObject jsonify()
    {
        JSONObject endpoint = new JSONObject();

        endpoint.put("port",port.toString())
                .put("protocol",getHumanReadableProtocol(protocol))
                .put("type of score",type)
                .put("value",this.value)
                .put("ip destination address",ipAddress.toString());

        return endpoint;
    }
}

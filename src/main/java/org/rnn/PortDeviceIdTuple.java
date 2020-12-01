package org.rnn;
import org.rnn.Identificators.*;
import org.rnn.json.JSONArray;
import org.rnn.json.JSONObject;
import org.rnn.policy.Property;
import java.util.ArrayList;

/**
 * Data container. Contains tuples of port number and device id connected to this port of certain device. Used to
 * speed up links search.
 * @author Piotr Frohlich
 * @Version Beta 2.0.0
 *
 *
 */
public class PortDeviceIdTuple
{
    protected ArrayList<Property> properties = new ArrayList<>();
    /**
     * Long representation of port number on certain device.
     */
    public long portNumber;

    /**
     * Real port number on certain device
     */
    public long actualPortNumber;

    /**
     * Device Id of device connected to this port.
     */
    public Identificator deviceId;

    /**
     * Constructor
     * @param port Port of certain device.
     * @param id Id of device connected to given port.
     */
    public PortDeviceIdTuple(long port, Identificator id, long realPort)
    {
        portNumber = port;
        deviceId = id;
        actualPortNumber = realPort;
        for (int i = 0; i <NetState.DEFAULT_LINK_PROPERTIES.size(); i++)
        {
            properties.add(NetState.DEFAULT_LINK_PROPERTIES.get(i));
        }
    }
    public void updateProperties(Property property)
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
    public JSONObject jsonify(Identificator deviceId)
    {
        JSONObject obj = new JSONObject();
        JSONArray arrayProperites = new JSONArray();
        for (int i = 0; i <properties.size(); i++)
        {
            arrayProperites.put(properties.get(i).jsonify());
        }

        JSONObject tmp = new JSONObject();
        tmp.put("properties",arrayProperites);
        obj
                .put("device id",this.deviceId.toString())
                .put("port",actualPortNumber)
                .put("qos parameter",RnnRouting.delayAgregator.getDelayInfo(new LinkID(deviceId.toLong(),this.deviceId.toLong())))
                .put("property manager",tmp);
        return obj;

    }

}

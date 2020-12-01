package org.rnn.policy;

import org.rnn.ForwardingDevice;
import org.rnn.NetState;
import org.rnn.PathFlow;
import java.util.ArrayList;

/**
 * @author Piotr Frohlich
 * @version 1.0.1
 * This class is a representation of the Properties available in the network.
 */

public class PropertiesManager
{
    /**
     * A getter for the properties that are available on a given path flow.
     * @param flow A given path flow
     * @return A list of properties on the given path flow.
     */
    public static ArrayList<Property> getPropertiesOnPath(PathFlow flow)
    {
        ArrayList<Property> toReturn = new ArrayList<>();
        for (int i = 0; i <flow.getPath().size()-1; i++)
        {
            ForwardingDevice
                    fd = NetState.getBySimpleID(flow.getPath().get(i).getNodeDeviceId()),
                    direction = NetState.getBySimpleID(flow.getPath().get(i+1).getNodeDeviceId());

            ArrayList<Property> tmp = fd.getProperitesOnDirection(direction.getDeviceControllerId());
            for (int j = 0; j <tmp.size(); j++)
            {
                toReturn.add(tmp.get(j));
            }
        }
        return toReturn;
    }

}

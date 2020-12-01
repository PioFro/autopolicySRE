package org.rnn;

import org.rnn.Identificators.Identificator;

import java.util.Dictionary;
import java.util.Hashtable;

public class PathProviderTreeLeaf
{
    private Identificator myID;
    private Dictionary<Integer,PathProviderTreeLeaf> children = new Hashtable<>();
    private int depth;

    public PathProviderTreeLeaf(PathFlow pathUpToNow, int depth)
    {
        this.depth = depth;
        if(this.depth>=PathProviderTree.MAX_DEPTH)
            return;

        if(pathUpToNow.path.size()>0) {
            if(pathUpToNow.path.get(pathUpToNow.path.size()-1).nodeDeviceId == pathUpToNow.destination)
            {
                myID = NetState.getBySimpleID(pathUpToNow.path.get(pathUpToNow.path.size() - 1).nodeDeviceId).getDeviceControllerId();
                PathFlow tmp = new PathFlow(pathUpToNow.source, pathUpToNow.destination);
                for (int i = 0; i <pathUpToNow.path.size(); i++) {
                    if(pathUpToNow.path.get(i).nodeDeviceId == pathUpToNow.destination)
                    {
                        tmp.addNewNode(new Node(pathUpToNow.path.get(i).nodeDeviceId, 0.0, 9999));
                    }
                    else {
                        tmp.addNewNode(new Node(pathUpToNow.path.get(i).nodeDeviceId, 0.0, pathUpToNow.path.get(i).outputPort));
                    }
                }

                PathProviderTree.addPath(pathUpToNow.source, pathUpToNow.destination, tmp);
                return;
            }
            this.myID = NetState.getBySimpleID(pathUpToNow.path.get(pathUpToNow.path.size() - 1).nodeDeviceId).getDeviceControllerId();
        }
        else
            {
                this.myID = NetState.getBySimpleID(pathUpToNow.source).getDeviceControllerId();
            }
        ForwardingDevice thisDevice = NetState.getTopology().get(NetState.getForwardingDeviceByDeviceID(myID));

        for (int i = 0; i <thisDevice.links.size(); i++)
        {
            if(!pathUpToNow.checkIfPathContainsSimpleID((int)thisDevice.links.get(i).deviceId.toLong()))
            {
                PathFlow flow = new PathFlow(pathUpToNow.source,pathUpToNow.destination);
                if(pathUpToNow.path.size()<1)
                {
                    flow.path.add(new Node((int) myID.toLong(), 0.0, (int) thisDevice.links.get(i).actualPortNumber));
                }
                for (int j = 0; j <pathUpToNow.path.size(); j++)
                {
                    flow.path.add(new Node(pathUpToNow.path.get(j).nodeDeviceId, 0.0,pathUpToNow.path.get(j).outputPort));
                }
                flow.path.add(new Node((int)thisDevice.links.get(i).deviceId.toLong(),0.0,(int)thisDevice.links.get(i).actualPortNumber));
                children.put((int)thisDevice.links.get(i).actualPortNumber,new PathProviderTreeLeaf(flow, depth+1));
            }
        }

    }
}

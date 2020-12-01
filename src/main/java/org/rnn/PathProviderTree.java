package org.rnn;
import java.util.Dictionary;
import java.util.Hashtable;

public class PathProviderTree
{
    private static Dictionary<String,PathProviderTreeLeaf> children = new Hashtable<>();
    private static Dictionary<String,PathProviderStorage> createdPaths = new Hashtable<>();
    public static int MAX_DEPTH;


    public PathProviderTree(int max_depth)
    {
        MAX_DEPTH = max_depth;
    }

    public static void setup()
    {
                   for (int i = 1; i <= NetState.getTopology().size(); i++) {
                for (int j = 1; j <= NetState.getTopology().size(); j++) {
                    if (i != j) {
                        try {
                            String idString = "" + i + " " + j;
                            PathProviderTreeLeaf tmp = new PathProviderTreeLeaf(new PathFlow(i, j), 0);
                            children.put(idString, tmp);
                        }
                        catch(Exception e) {
                            UnifiedCommunicationModule.log.info("Exception for "+i+", "+j);
                        }
                    }
                }
            }
            UnifiedCommunicationModule.log.info("Setup of the paths completed");
    }

    public static void addPath(long src, long dst, PathFlow flow)
    {
        UnifiedCommunicationModule.log.info("Created path :"+flow.toString());
        String idString = ""+src+" "+dst;
        try
        {
            ((PathProviderStorage)createdPaths.get(idString)).addPath(flow);
        }
        catch (Exception e)
        {
            createdPaths.put(idString,new PathProviderStorage(flow));
            flow.setInstallationDueTime(System.currentTimeMillis());
            PathTranslator.addNewPath(flow);
        }
    }
    public static PathFlow getPath(long src, long dst) throws TreeProviderException
    {
        String idString = ""+src+" "+dst;
        PathFlow path = null;
        try {
            path = ((PathProviderStorage) createdPaths.get(idString)).getBest();
        }
        catch (Exception e)
        {
            UnifiedCommunicationModule.log.error("Unable to provide the best path between src: "+src+" and dst: "+dst);
            throw new TreeProviderException(src, dst);
        }
        return path;

    }
}

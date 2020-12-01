package org.rnn;

import org.rnn.Identificators.Identificator;

import java.util.ArrayList;

/**
 * RouitngPathUnit class is responsible for managing different paths leading
 * from and to the same nodes. It's tool used for aggregation of previous
 * solutions (paths) as well as deciding which is currently the best.
 * Basically it can be treated as a class inside of PathTranstalor
 * @author Piotr Frohlich
 * @version Beta 2.0.0
 */
public class RoutingPathUnit
{
    /**
     * Set of paths leading to and from given src-dst tuple.
     */
    private ArrayList<PathFlow> paths = new ArrayList<PathFlow>();
    /**
     * Source part of src-dst tuple.
     */
    private long source;
    /**
     * Destination part of src-dst tuple
     */
    private long destination;
    /**
     * Index of a current best path.
     */
    private int bestPathIndex;
    /**
     * Did value of bestPathIndex change?
     */
    private boolean idMaxChanged = false;

    /**
     * Change of path will take place in timeToChange [ms]
     */
    private long timeToChange = 0;

    /**
     * Tmp field
     */
    private long semaphore = 0;

    /**
     * Host of source traffic
     */
    private Identificator srcHost;
    /**
     * Host of destination traffic
     */
    private Identificator dstHost;

    /**
     * Constructor.
     * @param source Source of the paths.
     * @param destination Destination of the paths.
     * @param flow First path to match src-dst tuple.
     */
    public RoutingPathUnit(long source, long destination, PathFlow flow, Identificator src, Identificator dst)
    {
        this.source = source;
        this.destination = destination;
        flow.dstHostId = dst;
        flow.srcHostId = src;
        idMaxChanged = true;
        srcHost = src;
        dstHost = dst;
        addPath(flow);

    }

    /**
     * Calculate which path, from current array of paths has the best score.
     * Assign it's index to bestPathIndex field and if it is different from previous
     * bestPathIndex value - change idMaxChanged to true.
     */
    public void calculateBestOption()
    {
            int idMax = -1, i = 0;
            double max = paths.get(bestPathIndex).checkMeanScore();
            double bestScore = paths.get(bestPathIndex).checkMeanScore();
            for (PathFlow path : paths
            ) {
                if (path.checkMeanScore() >= max) {
                    max = path.checkMeanScore();
                    idMax = i;
                }
                i++;
            }

            if (bestPathIndex == idMax || idMax == -1)
                idMaxChanged = false;
            else
            {
                if(paths.get(idMax).checkIfTresholdCrossed(bestScore)||timeToChange<System.currentTimeMillis())
                {
                    if(semaphore <System.currentTimeMillis()) {
                        semaphore = System.currentTimeMillis() + 10;
                        timeToChange = System.currentTimeMillis() + NetState.DEFAULT_PATH_INSTALATION_TIME;
                        idMaxChanged = true;
                        this.bestPathIndex = idMax;
                    }
                }
            }
            return;
    }

    /**
     * Check if given source-destination tuple matches source-destination tuple of
     * this instance.
     * @param source Given source
     * @param destination Given destination
     * @return True if both source and destination matches. Otherwise false.
     */
    public boolean checkSourceDestination(long source, long destination)
    {
        boolean tmp = (this.source==source&&this.destination==destination);
        return tmp;
    }
    /**
     * Checks if given parameters are the same hosts as hosts assigned to this instance
     * @param src Source host parameter
     * @param dst Destination host parameter
     * @return True if both source and destination hosts parameters are the same as
     *         hosts in this instance.
     */
    public boolean checkHostSourceDestination(Identificator src, Identificator dst)
    {
        return(src.equals(srcHost)&&dst.equals(dstHost));
    }
    /**
     * Add path to current array of paths. Recalculate bestPathIndex.
     * @param flow Path/Flow to add
     * @return best Path
     */
    public PathFlow addPath(PathFlow flow)
    {
        flow.srcHostId = srcHost;
        flow.dstHostId =dstHost;
        int tmp =checkPath(flow);
        if(tmp ==-1)
        {
            paths.add(flow);
        }
        else
        {
            //paths.get(tmp).setScoresAlongThePath(flow);
            paths.get(tmp).updateWithHistoricalData(flow);
        }
        calculateBestOption();
        return paths.get(bestPathIndex);
    }

    /**
     * Check if in array of paths of this instance is a path which contains
     * given nodes
     * @param flow Given array of nodes in form of a path
     * @return Index of the same path or -1 if there are no such paths.
     */
    private int checkPath(PathFlow flow)
    {
        for (int i = 0; i < paths.size(); i++)
        {
           if(paths.get(i).containsNodes(flow))
           {
               return i;
           }
        }
        return -1;
    }
    /**
     * Getter for idMaxChanged field.
     * @return idMaxChanged.
     */
    public boolean isIdMaxChanged() {
        return idMaxChanged;
    }

    /**
     * Setter for flag for changed best path index
     * @param flag Set to value
     */

    public void setIdMaxChanged(boolean flag){this.idMaxChanged = flag;}

    /**
     * Getter for bestPathIndexFiled.
     * @return bestPathIndex
     */
    public int getBestPathIndex() {
        return bestPathIndex;
    }

    /**
     * Getter for the instance of the best path.
     * @return Instance of the best path from array of paths.
     */
    public PathFlow getBestPath()
    {
        return paths.get(bestPathIndex);
    }

    /**
     * Setter for bestPathIndex field
     * @param bestPathIndex index of the best path.
     */
    public void setBestPathIndex(int bestPathIndex) {
        this.bestPathIndex = bestPathIndex;
    }

    /**
     * Getter for all paths connecting given source and destination
     * @return List of all the paths
     */
    public ArrayList<PathFlow> getPaths(){return paths;}

    /**
     * Update score of given path with respect of previous values.
     * @param path Path containing score to update
     */
    public void updatePathScore( PathFlow path)
    {
        for (int i = 0; i < paths.size(); i++)
        {
            if(paths.get(i).containsNodes(path))
            {
                paths.get(i).updateWithHistoricalData(path);
                return;
            }
        }
        this.addPath(path);
    }

    /**
     * Lurker for new possibilities of paths to avoid static-like situation
     */
    @Deprecated
    public void checkForNewPossibilities()
    {
        for (int i = 0; i < paths.size(); i++)
        {
            if(paths.get(i).getPath().get(0).score ==0.0)
            {
                if(paths.get(i).source == paths.get(i).path.get(0).nodeDeviceId)
                {
                    ForwardingDevice lastDevice = NetState.getBySimpleID(paths.get(i).getPath().get(paths.get(i).getPath().size()-1).nodeDeviceId);
                    int realPort = paths.get(i).getPath().get(paths.get(i).getPath().size()-1).outputPort;
                    Identificator destinationDevice = lastDevice.getDeviceIdFromPort(realPort);
                    if(paths.get(i).destination == Integer.parseInt(destinationDevice.toString().charAt(destinationDevice.toString().length()-1)+""))
                    {
                        PathTranslator.installDiscoverypath(paths.get(i), 50002);
                    }
                }
            }

        }
    }

    public Identificator getDstHost() {
        return dstHost;
    }

    public Identificator getSrcHost() {
        return srcHost;
    }
}
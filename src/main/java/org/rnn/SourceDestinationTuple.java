package org.rnn;
import org.rnn.Identificators.Identificator;

/**
 * This is a data aggregator for received smart packets.
 *
 * @author pfrohlich
 * @version Beta 2.0.0.
 */
public class SourceDestinationTuple
{
    /**
     * Destination of smart packet
     */
    Identificator destination;
    /**
     * Source of smart packet
     */
    Identificator source;
    /**
     * Number of smart packets of corresponding src-dst tuple
     */
    short count =0;

    /**
     * SP-specific timestamp (provided by deamon)
     */
    long idTimestamp;

    /**
     * Path this SP took
     */
    PathFlow path=new PathFlow(0,0);

    /**
     * Contructor for randomization purposes
     * @param source Source of SP
     * @param destination Destination of SP
     */
   public SourceDestinationTuple(long source, long destination)
    {
        this.path.source = (int)source;
        this.path.destination = (int)destination;
    }

    /**
     * Constructor for timeout purposes.
     * @param idTimestamp ID by deamon
     * @param path path taken
     */

    public SourceDestinationTuple(long idTimestamp, PathFlow path, long timestamp)
    {
        this.idTimestamp = idTimestamp;
        this.path = path;
        this.path.timestamp = timestamp;
        this.path.setInstallationDueTime(System.currentTimeMillis());
    }

    /**
     * Check if this is corresponding to src-dst tuple
     * @param source src
     * @param destination dst
     * @return true if it is corresponding to src-dst tuple
     */
    public boolean checkTuple(long source, long destination)
    {
        return(this.path.source == source&&this.path.destination== destination);
    }

    /**
     * Check counter
     * @return true if it was 10
     */
    public boolean checkCounter()
    {
        count++;
        if(count == 10)
        {
            count=0;
            return true;
        }
        return false;
    }

    /**
     * Compare this instance to payload of SP
     * @param payload String payload of SP
     * @return true if they are corresponding
     */
    public boolean compareToString(String payload)
    {
        try {
            PathFlow pathflow = new PathFlow(payload);

            if(pathflow.containsNodes(this.path))
            {
                return true;
            }
            else return false;

        }
        catch(Exception e)
        {
            return false;
        }

    }
}

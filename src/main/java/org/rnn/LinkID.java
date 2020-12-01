package org.rnn;

/**
 *This is representation of src destination identified link. Used to store
 * links between ForwardingDevices.
 *
 * @author: Piotr Frohlich
 * @version: Beta 2.0.0
 */
public class LinkID
{
        /**
        * Simple id of source device
        */
        public long src;
        /**
        * Simple ID of destination device
        */
        public long dst;

        /**
        * Simple construcotr which assigns values to fields
        * @param source Source simple id value
        * @param destination Destination Simple Id value
        */
        public LinkID(long source, long destination){src=source;dst = destination;}

        @Override
        public boolean equals(Object o)
        {
                if(o.getClass() == LinkID.class)
                {
                        LinkID link = (LinkID)o;
                        return (link.dst == this.dst && link.src == this.src) || (link.dst == this.src && link.src == this.dst);
                }
                return super.equals(o);
        }
        @Override
        public String toString()
        {
                return "SRC :"+src+", DST : "+dst;
        }
}

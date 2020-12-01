package org.rnn.Identificators;

import org.onosproject.net.HostId;

/**
 * @author Piotr Frohlich
 * @version 1.0.1
 * This class is a dedicated identificator class which implements the Identificator interface.
 * This implementation is ONOS-dependent.
 */
public class HostIdReal implements Identificator
{
    /**
     * Real identificator of a host provided by ONOS (notation - MAC:VLAN)
     */
    protected HostId realHostId;
    /**
     * Readable form of id of the host. Assigned by the controller.
     */
    protected long simpleId;

    /**
     * This is a basic constructor. It assigns a value for each field needed to
     * create an instance of this class.
     * @param id An instance of an ONOS - provided id of the host.
     * @param simpleId Readable form of id of the host
     */
    public HostIdReal( HostId id, long simpleId)
    {
        realHostId = id;
        this.simpleId = simpleId;
    }

    /**
     * A comparison method between two instances of Host ids.
     * @param o Object to compare
     * @return true if o and this instance are the same.
     */
    @Override
    public boolean equals(Object o)
    {
        HostIdReal tmp = (HostIdReal)o;
        if(tmp == null)
            return false;
        else
        {
            return (tmp.realHostId.equals(this.realHostId));
        }
    }

    /**
     * This method returns a string representation of this instance
     * @return a string representation
     */
    @Override
    public String toString()
    {
        return realHostId.toString();
    }
    /**
     * This method returns a long representation of this instance
     * @return a long representation
     */
    @Override
    public long toLong() {
        return simpleId;
    }
    /**
     * This method returns the value of the real Host Id field of this instance
     * @return A value of the real Host Id field.
     */
    public HostId getRealHostId() {
        return realHostId;
    }
}

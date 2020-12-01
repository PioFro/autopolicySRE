package org.rnn.Identificators;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.rnn.UnifiedCommunicationModule;
/**
 * @author Piotr Frohlich
 * @version 1.0.1
 * This class is a dedicated identificator class which implements the Identificator interface.
 * This implementation is ONOS-dependent.
 */
public class DeviceIdReal implements Identificator
{
    /**
     * Real, assigned by the OpenFlow protocol id of the device.
     */
    protected DeviceId realDeviceId;
    /**
     * Assigned by the controller readable by human id of the device
     */
    private long simpleId;

    /**
     * A basic constructor of the real implementation of the device identificator.
     * @param deviceId A class provided by ONOS, contains the real id of the device.
     * @param id A readable representation of the id of the device
     */
    public DeviceIdReal (DeviceId deviceId, long id)
    {
        realDeviceId = deviceId;
        this.simpleId = id;
    }

    /**
     * This method returns true only when o is a RealDeviceId class and the
     * Device Id fields are equal.
     * @param o An object to compare
     * @return True if the device id is equal
     */
    @Override
    public boolean equals(Object o)
    {
        DeviceIdReal tmp = (DeviceIdReal)o;
        if(tmp == null)
        {
            DeviceId tmp_2 = (DeviceId)o;
            if(tmp_2 != null)
            {
                UnifiedCommunicationModule.log.error("DEVICE ID COMPARED TO DEVICE ID REAL !!!!!!");
            }
            return false;
        }
        else
        {
            return (tmp.realDeviceId.equals(this.realDeviceId));
        }
    }

    /**
     * This method returns a string representation of this instance
     * @return a string representation
     */
    @Override
    public String toString()
    {
        return realDeviceId.toString();
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
     * This method returns the value of the real Device Id field of this instance
     * @return A value of the real Device ID field.
     */
    public DeviceId getRealDeviceId() {
        return realDeviceId;
    }
}

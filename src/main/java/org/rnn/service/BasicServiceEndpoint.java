package org.rnn.service;

import org.onlab.packet.IpAddress;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.rnn.Identificators.HostIdReal;
import org.rnn.Identificators.Identificator;
import org.rnn.implementation.RealCommunicator;
/**
 * @author Piotr Frohlich
 * @version 1.0.1
 * This class is a representation of the single (Basic) service endpoint (a machine
 * able to instantiate the service) - running the Service Manager.
 */

public class BasicServiceEndpoint implements IServiceEndpoint
{
    /**
     * Real IP address (not the static, service-based) of the endpoint.
     */
    private IpAddress ipAddress;
    /**
     * Real IP address (not the static, service-based) of the endpoint.
     */
    private IpAddress realIpAddress;
    /**
     * Real id (onos provided) of the machine
     */
    private HostId hostId;
    /**
     * Abstract id (onos independent) of the machine
     */
    private HostIdReal hostIdReal;
    /**
     * Port number under which the service will be instantiated.
     */
    private PortNumber portNumber;


    @Override
    public String getIPAddress() {
        return ipAddress.toString();
    }

    @Override
    public long getPortNumber() {
        return portNumber.toLong();
    }

    @Override
    public Identificator getId() {
        return hostIdReal;
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    public void setHostIdReal(HostIdReal hostIdReal) {
        this.hostIdReal = hostIdReal;
    }

    public void setRealIpAddress(IpAddress realIpAddress) {
        this.realIpAddress = realIpAddress;
    }

    public IpAddress getRealIpAddress() {
        return realIpAddress;
    }

    public void setIpAddress(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
        hostId = RealCommunicator.getMaster().getHostIdByIpAddress(ipAddress);
        hostIdReal = new HostIdReal(hostId, hostId.hashCode());
    }

    public void setPortNumber(PortNumber portNumber) {
        this.portNumber = portNumber;
    }
    public PortNumber getRealPortNumber(){return portNumber;}

    public HostId getHostId() {
        return hostId;
    }
    public void setHostId(HostId hostId) {
        this.hostId = hostId;
    }
}

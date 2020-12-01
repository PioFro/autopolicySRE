package org.rnn;


import org.rnn.Identificators.Identificator;
/**
 * Handler for rest input. Reveives information about host and devices security
 * @author Piotr Frohlich
 * @version 1.0.0
 */

public class DaemonForwarder implements Daemon
{
    private Identificator deviceId;

    private String ipAddress;

    private long portNumber;

    public DaemonForwarder(Identificator id, String ip, long port)
    {
        deviceId = id;
        ipAddress = ip;
        portNumber = port;
    }


    @Override
    public Identificator getDeviceId()
    {
        return deviceId;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public long getPortNumber() {
        return portNumber;
    }

    public void setDeviceId(Identificator deviceId) {
        this.deviceId = deviceId;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPortNumber(long portNumber) {
        this.portNumber = portNumber;
    }

    @Override
    public String toString()
    {
        return "{  "+deviceId.toString()+"  ,  "+ipAddress.toString()+"  ,  PORT: "+portNumber+"  }";
    }

}

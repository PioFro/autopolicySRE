package org.rnn;

public class MultiRoutingInfo
{
    private byte proto;
    private int tcpSrcPort;
    private int tcpDstPort;
    private String src;
    private String dst;
    private long time;
    private long bytesSent;
    protected boolean active = false;
    private int srcDevice;
    private int dstDevice;

    public MultiRoutingInfo(byte protocol, int srcPort,int dstPort, String deviceSrc, String deviceDst, int sDevice, int dDevice)
    {
        proto = protocol;
        tcpSrcPort = srcPort;
        tcpDstPort = dstPort;
        src = deviceSrc;
        dst = deviceDst;
        time = System.currentTimeMillis();
        bytesSent = 0;
        srcDevice = sDevice;
        dstDevice = dDevice;
    }

    @Override
    public boolean equals(Object o) {
        try {
            MultiRoutingInfo toCompare = (MultiRoutingInfo) o;
            if (toCompare.src.equals(this.src) && toCompare.tcpSrcPort == this.tcpSrcPort && toCompare.dst.equals(this.dst) && this.proto == toCompare.proto&& toCompare.tcpDstPort == this.tcpDstPort)
            {
                return true;
            }
            return false;
        }
        catch (Exception e)
        {
            return super.equals(o);
        }
    }

    public boolean isUsable()
    {
        return (active&&bytesSent>0);
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }

    public int getDstDevice() {
        return dstDevice;
    }

    public int getSrcDevice() {
        return srcDevice;
    }

    public byte getProto() {
        return proto;
    }

    public String getDst() {
        return dst;
    }

    public String getSrc() {
        return src;
    }

    public int getTcpSrcPort() {
        return tcpSrcPort;
    }

    public int getTcpDstPort() {
        return tcpDstPort;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public long getTime() {
        return time;
    }
    @Override
    public String toString()
    {
        return "PROTO : "+proto+" IP SRC: "+src.toString()+" IP DST: "+dst.toString()+" PORTS (S,D) : ("+tcpSrcPort+","+tcpDstPort+") BYTES: "+bytesSent+ " TIME: "+time;
    }
}

package org.rnn.autopolicy;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.rnn.NetState;
import org.rnn.PathFlow;
import org.rnn.PathTranslator;

public class APFlow
{
    public int protocol;
    public int startPort;
    public int endPort;

    public IpAddress dst;
    public MacAddress mac;

    public int simpleSrc;
    public int simpleDst;

    public float maxBitrate = NetState.AP_DEFAULT_MAX_BITRATE;

    public long previousBytes = 0;

    public APFlow(int protocol, int startPort, int endPort, IpAddress dst, MacAddress mac)
    {
        this.protocol = protocol;
        this.startPort = startPort;
        this.endPort = endPort;
        this.dst = dst;
        this.mac = mac;
    }

    public APFlow(int protocol, int startPort, IpAddress dst, MacAddress mac)
    {
        this.protocol = protocol;
        this.startPort = startPort;
        this.endPort = startPort;
        this.dst = dst;
        this.mac = mac;
    }
    public enum ProfileResponse
    {
        GOOD, BAD, NOT_MINE
    }
    public ProfileResponse check(FlowEntry entry)
    {
        if(entry.selector().equals(getForwardingObjectiveForAPFlow().selector()))
        {
            int secondsTillLastUpdate = (int)NetState.AP_PROFILE_TIMER/1000;
            float actualBR = (float)((float)entry.bytes() - (float)previousBytes)/(float)secondsTillLastUpdate;
            previousBytes = entry.bytes();
            if(actualBR > this.maxBitrate) return ProfileResponse.BAD;
            else return ProfileResponse.GOOD;
        }
        return ProfileResponse.NOT_MINE;
    }

    public ForwardingObjective getForwardingObjectiveForAPFlow()
    {
        TrafficSelector.Builder selectorBuilderPing2 = DefaultTrafficSelector.builder();
        selectorBuilderPing2
            .matchEthType(Ethernet.TYPE_IPV4);
        if(!dst.equals(IpAddress.valueOf("0.0.0.0")))
            selectorBuilderPing2.matchIPDst(dst.toIpPrefix());
        if(protocol == 17)
        {
            selectorBuilderPing2.matchIPProtocol(IPv4.PROTOCOL_UDP);
            if(endPort!=-1)
                selectorBuilderPing2.matchUdpDst(TpPort.tpPort(endPort));
        }
        else if( protocol == 6)
            {
                selectorBuilderPing2.matchIPProtocol(IPv4.PROTOCOL_TCP);
                if(endPort!=-1)
                    selectorBuilderPing2.matchTcpDst(TpPort.tpPort(endPort));
            }
        else
            {
                if(protocol!=-1)
                    selectorBuilderPing2.matchIPProtocol((byte)protocol);
            }
        selectorBuilderPing2.matchEthSrc(mac);
        PathFlow flow = PathTranslator.getBestPathFromSrcDst(simpleSrc, simpleDst);
        int outport = 0;
        PortNumber outPortNumber;
        try
        {
            outport = flow.getPath().get(0).getOutputPort();
            outPortNumber = PortNumber.portNumber(outport);
        }
        catch (Exception e)
        {
            outPortNumber = PortNumber.ALL;
        }

        TrafficTreatment treatmentPingFlow = DefaultTrafficTreatment.builder()
                .setOutput(outPortNumber)
                .build();

        ForwardingObjective forwardingObjective5005 = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilderPing2.build())
                .withTreatment(treatmentPingFlow)
                .withPriority(NetState.AP_PROFILE_PRIOROTY)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .makePermanent()
                .fromApp(NetState.MASTER.getAppId())
                .add();

        return forwardingObjective5005;
    }
    public String toString()
    {
        String dest="", proto="", ports="";
        if(dst.equals(IpAddress.valueOf("0.0.0.0")))
            dest = "ALL IPS";
        else
            dest = dst.toString();
        if(protocol == -1)
            proto = "ALL PROTOCOLS";
        else
            proto = ""+protocol;
        if(startPort == -1)
            ports="ALL PORTS";
        else
            ports=""+startPort+" - "+endPort;


        return "Destination: "+dest+
                ", Protocol: "+proto+
                ", Ports: "+ports+
                ", Avg bitrate: "+maxBitrate;
    }

}
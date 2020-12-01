package org.rnn.implementation;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.rnn.Identificators.DeviceIdReal;
import org.rnn.Identificators.HostIdReal;
import org.rnn.NetState;
import org.rnn.PathFlow;
import org.rnn.PathTranslator;

public class StringObjectiveInformation
{
    String IP;
    String protocol;
    String port;

    public IpAddress ipAddress;
    int parsedPort;
    int parsedPortEnd;
    int parsedProtocol;

    public boolean isUsable=false;

    public void validateStringObjective(String obj) throws IncorectStringObjectiveException
    {
        if(obj.equals(""))
            return;
        String[] src;
        try
        {
            src = obj.split(" ");
            protocol = src[0];
            IP = src[1].split(":")[0];
            port = src[1].split(":")[1];
        }
        catch (Exception e)
        {
            throw  new IncorectStringObjectiveException("String Objective in incorrect format. " +
                                                                "Provided: "+obj+" should be in format PROTOCOL ip:port");
        }
    }
    private void validateIP() throws IncorectStringObjectiveException
    {
        if(IP!=null)
        {
            if(IP.equals("*"))
            {
                ipAddress = IpAddress.valueOf("0.0.0.0");
                return;
            }

            try
            {
                ipAddress = IpAddress.valueOf(IP);
            }
            catch (Exception e)
            {
                throw  new IncorectStringObjectiveException("IP address incorect. Provided: "+IP+" caused parser to exit with an error: "+e.getMessage());
            }
        }
    }
    private void validatePort() throws IncorectStringObjectiveException
    {
        if(port != null)
        {
            if(port.equals("*"))
            {
                parsedPort=-1;
                return;
            }

            try
            {
                if(port.contains("-"))
                {
                    parsedPort = Integer.parseInt(port.split("-")[0]);
                    parsedPortEnd = Integer.parseInt(port.split("-")[1]);
                }
                else {
                    parsedPort = Integer.parseInt(port);
                    parsedPortEnd = parsedPort;
                }
            }
            catch (Exception e)
            {
                throw  new IncorectStringObjectiveException("Port incorect. Provided: "+port+" caused parser to exit with an error: "+e.getMessage());
            }
        }
    }
    private void validateProtocol() throws IncorectStringObjectiveException
    {
        if(protocol!=null) {
            switch (protocol.toUpperCase()) {
                case "UDP":
                    parsedProtocol = IPv4.PROTOCOL_UDP;
                    break;
                case "TCP":
                    parsedProtocol = IPv4.PROTOCOL_TCP;
                    break;
                case "ICMP":
                    parsedProtocol = IPv4.PROTOCOL_ICMP;
                    break;
                case "IGMP":
                    parsedProtocol = IPv4.PROTOCOL_IGMP;
                    break;
                case "PIM":
                    parsedProtocol = IPv4.PROTOCOL_PIM;
                    break;
                case "*":
                    parsedProtocol = -1;
                    break;
                default:
                    throw new IncorectStringObjectiveException("Provided protocol " + protocol + " doesn't exists or is not supported.");
            }
        }
    }
    public void validateAll() throws IncorectStringObjectiveException
    {
        validateIP();
        validatePort();
        validateProtocol();
        if(IP!=null)
            isUsable = true;
    }
    //this is treated as source but can be ommited if the value of the isUsable == false
    public ForwardingObjective createForwardingObjective(StringObjectiveInformation dst, boolean block)
    {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV4);
        if(this.isUsable)
        {
            selectorBuilder.matchIPSrc(ipAddress.toIpPrefix());
            if(this.parsedProtocol != -1)
                selectorBuilder.matchIPProtocol((byte)parsedProtocol);
            if(this.parsedPort != -1) {
                if (this.parsedProtocol == IPv4.PROTOCOL_UDP) {
                    for (int i = 0; i <= (parsedPortEnd - parsedPort); i++) {
                        selectorBuilder.matchUdpSrc(TpPort.tpPort(parsedPort + i));
                    }
                }
                if (this.parsedProtocol == IPv4.PROTOCOL_TCP) {
                    for (int i = 0; i <= (parsedPortEnd - parsedPort); i++) {
                        selectorBuilder.matchTcpSrc(TpPort.tpPort(parsedPort + i));
                    }
                }
            }
        }
        if(dst.isUsable)
        {
            selectorBuilder.matchIPDst(dst.ipAddress.toIpPrefix());
            if(dst.parsedProtocol != -1)
                selectorBuilder.matchIPProtocol((byte)dst.parsedProtocol);
            if(dst.parsedPort != -1) {
                if (dst.parsedProtocol == IPv4.PROTOCOL_UDP) {
                    for (int i = 0; i <= (dst.parsedPortEnd - dst.parsedPort); i++) {
                        selectorBuilder.matchUdpDst(TpPort.tpPort(dst.parsedPort + i));
                    }
                }
                if (dst.parsedProtocol == IPv4.PROTOCOL_TCP) {
                    for (int i = 0; i <= (dst.parsedPortEnd - dst.parsedPort); i++) {
                        selectorBuilder.matchTcpDst(TpPort.tpPort(dst.parsedPort + i));
                    }
                }
            }
        }
        PortNumber out;
        PathFlow flow=null;
        try
        {
            HostId srcHostId = NetState.MASTER.getHostIdByIpAddress(ipAddress),dstHostId = NetState.MASTER.getHostIdByIpAddress(dst.ipAddress);
            long simpleSrc = NetState.getSimpleIDByIdOfConnectedHost(new HostIdReal(srcHostId,srcHostId.hashCode())),
                    simpleDst = NetState.getSimpleIDByIdOfConnectedHost(new HostIdReal(dstHostId,dstHostId.hashCode()));
            if(this.isUsable)
            {
                DeviceIdReal dri = (DeviceIdReal)NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID((int)simpleSrc)).getDeviceControllerId();
                ForwardingObjectiveTranslator.srcId =dri.getRealDeviceId();
                flow =PathTranslator.getBestPathFromSrcDst(simpleSrc,simpleDst);
            }
            else if(dst.isUsable)
            {
                DeviceIdReal dri = (DeviceIdReal)NetState.getTopology().get(NetState.getForwardingDeviceBySimpleID((int)simpleDst)).getDeviceControllerId();
                ForwardingObjectiveTranslator.srcId =dri.getRealDeviceId();
                flow = PathTranslator.getBestPathFromSrcDst(simpleSrc,simpleDst);
            }

            out = PortNumber.portNumber(flow.getPath().get(0).getOutputPort());
            //treatmentBuilder.setOutput(out);
        }
        catch (Exception e)
        {
            out = PortNumber.ALL;
        }
        if(block)
        {
            treatmentBuilder.drop();
        }
        else
            treatmentBuilder.setOutput(out);
        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withPriority(NetState.DEFAULT_BLOCK_PRIORITY)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(NetState.MASTER.getAppId())
                .add();
        return forwardingObjective;
    }

}
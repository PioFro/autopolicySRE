package org.rnn.autopolicy;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TCP;
import org.onlab.packet.UDP;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.rnn.Identificators.DeviceIdReal;
import org.rnn.NetState;
import org.rnn.UnifiedCommunicationModule;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimerTask;

public class APModelBuilder extends TimerTask
{
    public TrafficSelector trafficSelector;
    public DeviceId id;
    public MacAddress mac;
    public APProfile profile;
    public APPacketProcessorBuilder processor;


    public APModelBuilder(MacAddress mac, DeviceId device, IpAddress ip)
    {
        this.mac = mac;
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder()
                .matchEthSrc(mac)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(ip.toIpPrefix());
        trafficSelector = selectorBuilder.build();
        id = device;
        //empty profile
        profile = new APProfile(new DeviceIdReal(device, device.hashCode()),ip, mac, new ArrayList<>());
        //TODO: Protect from using an empty profile?

        processor = new APPacketProcessorBuilder(this.mac);
        NetState.MASTER.addPacketProcessor(processor,trafficSelector,device);
    }

    public class APPacketProcessorBuilder implements PacketProcessor
    {
        public Dictionary<String,APFlow> gatheredAPFlows = new Hashtable<>();
        public MacAddress mac;
        public APPacketProcessorBuilder(MacAddress mac)
        {
            this.mac = mac;
        }
        @Override
        public void process(PacketContext context)
        {
            Ethernet inPkt = context.inPacket().parsed();
            try {
                IPv4 ipPacket = (IPv4) inPkt.getPayload();
                int proto=0, port=0;

                if(ipPacket.getProtocol() == IPv4.PROTOCOL_UDP)
                {
                    UDP inPacket = (UDP) ipPacket.getPayload();
                    proto = IPv4.PROTOCOL_UDP;
                    port = inPacket.getDestinationPort();
                }
                else if(ipPacket.getProtocol() == IPv4.PROTOCOL_TCP)
                {
                    TCP inPacket = (TCP)ipPacket.getPayload();
                    proto = IPv4.PROTOCOL_TCP;
                    port = inPacket.getDestinationPort();
                }
                try
                {
                    String k = ""+ipPacket.getDestinationAddress()+";"+proto+";"+port;
                    APFlow flow = gatheredAPFlows.get(k);
                    if(flow == null)
                    {
                        APFlow apflow = new APFlow(proto, port, IpAddress.valueOf(ipPacket.getDestinationAddress()),mac);
                        gatheredAPFlows.put(k,apflow);
                    }
                }
                catch (Exception e1)
                {

                }
            }
            catch (Exception e)
            {

            }
        }
        @Override
        public boolean equals(Object o)
        {
           try
           {
               APPacketProcessorBuilder obj = (APPacketProcessorBuilder)o;
               return obj.mac.equals(this.mac);
           }
           catch (Exception e)
           {
               return false;
           }
        }
    }

    @Override
    public void run() {
        try {
            Enumeration<String> e = processor.gatheredAPFlows.keys();
            while (e.hasMoreElements()) {
                String k = e.nextElement();
                profile.apFlows.add(processor.gatheredAPFlows.get(k));
            }
            APManager.addProfile(mac, profile);
        }
        catch (Exception e)
        {
            UnifiedCommunicationModule.log.warn("For the "+mac.toString()+" profiling ended empty. There will be no traffic allowed from that device from now on.");
        }
        NetState.MASTER.delPacketProcessor(processor,trafficSelector,id);
    }

}

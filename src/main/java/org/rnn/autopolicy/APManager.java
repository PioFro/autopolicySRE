package org.rnn.autopolicy;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TCP;
import org.onlab.packet.UDP;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.rnn.AppComponent;
import org.rnn.Identificators.DeviceIdReal;
import org.rnn.Identificators.HostIdReal;
import org.rnn.NetState;
import org.rnn.UnifiedCommunicationModule;
import sun.nio.ch.Net;

import javax.ws.rs.NotFoundException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Timer;

/**
 * Manager class for all of the AP-profiles stored in the controller. This class
 * adds new profiles, schedules the checking of the profiles fulfillment. Use
 * the parameter NetState.AP_PROFILE_TIMER parameter to change how often
 * the ap-profiles should be checked.
 */
public class APManager
{
    /**
     * Timer used for the scheduling
     */
    public static Timer timer = new Timer();
    /**
     * Key is the MAC address of the IoT device, Profile is the instance of the
     * APProfile class with all channels declared.
     */
    public static Dictionary<String, APProfile> profiles = new Hashtable<>();
    public static APManagerProcessor processor;

    public static class APManagerProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context)
        {
            if (!context.isHandled())
            {
                Ethernet inPkt = context.inPacket().parsed();
                if (inPkt.getEtherType() == Ethernet.TYPE_IPV4 && inPkt.getDestinationMAC().equals(MacAddress.ZERO)) {
                    //log.info("#*#*# PACKET IN")
                    IPv4 ipv4Packet = (IPv4) inPkt.getPayload();
                    MacAddress srcMac = inPkt.getSourceMAC();
                    if(profiles.get(srcMac.toString())==null)
                        return;
                    IpAddress dstIP = IpAddress.valueOf(ipv4Packet.getDestinationAddress());
                    int protocol = ipv4Packet.getProtocol();
                    int port = -1;
                    if(protocol == 17)
                    {
                        UDP inPacket = (UDP) ipv4Packet.getPayload();
                        port = inPacket.getDestinationPort();
                    }
                    if(protocol == 6)
                    {
                        TCP inPacket = (TCP) ipv4Packet.getPayload();
                        port = inPacket.getDestinationPort();
                    }
                    APFlow flow = new APFlow(protocol, port, dstIP,srcMac);
                    alert(srcMac, flow);
                    context.treatmentBuilder().drop();
                }
            }
        }
    }



    public static void init()
    {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        processor = new APManagerProcessor();
        NetState.MASTER.addPacketProcessor(processor, selectorBuilder.build(),null);
    }
    public static void deinit()
    {
        timer.purge();
        timer.cancel();
        NetState.MASTER.delPacketProcessor(processor);
    }

    /**
     * Adds new profile to a new MAC address - id of the profiled IoT
     * @param mac MAC address of the profiled IoT device
     * @param profile profile of the IoT device
     */
    public static void addProfile(MacAddress mac, APProfile profile)
    {
        try {
            if (profiles.get(mac.toString()) == null)
            {
                profiles.put(mac.toString(), profile);
                profile.installProfile();
                // Schedule checking of the profile. Note - there is no ending condition.
                timer.scheduleAtFixedRate(profile, NetState.AP_PROFILE_TIMER, NetState.AP_PROFILE_TIMER);
            }
        }
        catch (Exception e)
        {
            UnifiedCommunicationModule.log.error("Unable to add profile for "+mac.toString()+" IoT device. Reason: "+e.toString());
        }
    }

    public static void alert(MacAddress mac, APFlow flow)
    {
        try
        {
            APProfile ap = profiles.get(mac.toString());
            ap.mitigate(false, flow);
        }
        catch (Exception e)
        {
            UnifiedCommunicationModule.log.info("Unable to invoke alert. No profiled device with mac: "+mac.toString());
        }
    }

    public static void postAnAlert(String subject, String explanation, int severity, String details)
    {
        try {
            URL realURL = new URL(NetState.AP_PBF_VISUALISATION_IP + "alert/");
            HttpURLConnection connection = (HttpURLConnection) realURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");

            connection.connect();
            String jsonInputString = "{ \"subject\":\""+subject+
                    "\",\"explanation\":\""+explanation+
                    "\",\"severity\":"+severity+","+
                    "\"details\":\""+details+"\"}";
            try(OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try(BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                UnifiedCommunicationModule.log.info("RQ :"+response.toString());
            }
        }
        catch (Exception e)
        {
            UnifiedCommunicationModule.log.error(e.getMessage());
        }
    }

    public static void startProfiling(MacAddress mac, DeviceId id, IpAddress ip) throws NotFoundException
    {
        HostId hostId = NetState.MASTER.getHostIdByMac(mac);
        if(hostId.equals(HostId.NONE))
            throw new NotFoundException("No such mac address in the network");
        int sid = NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(id, id.hashCode()));
        if(sid == -1)
            throw new NotFoundException("No such device id in the network");
        hostId = NetState.MASTER.getHostIdByIpAddress(ip);
        if(hostId.equals(HostId.NONE))
            throw new NotFoundException("No such IP address in the network");

        APModelBuilder apmodel = new APModelBuilder(mac, id, ip);
        // Gather info for the AP_PROFILE_BUILD_TIME time in ms and then run (aggregate) it once and discard.
        timer.schedule(apmodel, NetState.AP_PROFILE_BUILD_TIME);
    }
}

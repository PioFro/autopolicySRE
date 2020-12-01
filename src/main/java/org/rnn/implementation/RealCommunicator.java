package org.rnn.implementation;

import org.onlab.packet.Data;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.UDP;
import org.onosproject.net.DeviceId;
import org.onosproject.net.packet.PacketContext;
import org.rnn.AppComponent;
import org.rnn.InvaildPayloadException;
import org.rnn.PacketInfo;
import org.rnn.RnnRouting;
/**
 * @author Piotr Frohlich
 * @version 1.0.1
 *
 * This class enables the communcation between ONOS-independent and
 * ONOS-dependent parts of the plugin.
 */
public class RealCommunicator
{
    /**
     * MASTER class which provides this instance with communication with ONOS
     * internal methods.
     */
    public static AppComponent master;
    /**
     * Instance of RNN based routing module.
     */
    public static RnnRouting rnnRouting;

    public static boolean rnnLock = false;

    /**
     * This constructor assigns the MASTER class and initializes the new routing
     * manager. Call this constructor only once in the plugin lifetime.
     * @param master MASTER class.
     */
    public RealCommunicator(AppComponent master)
    {
        RealCommunicator.master = master;
        RealCommunicator.rnnRouting = new RnnRouting();
    }

    /**
     * Getter for the MASTER class
     * @return The MASTER class
     */
    public static AppComponent getMaster() {
        return master;
    }

    /**
     * This method is called to send the data from the packet received by the packet
     * processor to the RNN Routing module for processing.
     * @param context Packet data
     * @throws InvaildPayloadException Thrown whenever a Cognitive Packets payload
     * isn't compatible with established notation.
     */
    public void receiveSmartPacket(PacketContext context) throws InvaildPayloadException
    {
        Ethernet inPkt = context.inPacket().parsed();

        IPv4 ipv4Packet = (IPv4) inPkt.getPayload();

        UDP inPacket = (UDP) ipv4Packet.getPayload();

        DeviceId inID = context.inPacket().receivedFrom().deviceId();

        Data data = (Data) inPacket.getPayload();
        String payload = "";
        try
        {
            //Convert to string
            payload = new String(data.getData(), "UTF-8");
            rnnRouting.receiveSmartPacket(new PacketInfo(payload));
        }
        catch (Exception ex)
        {
            //If payload isn't right throw exception
            master.log.error("Cannot unpack packet's payload");
            throw new InvaildPayloadException();
        }

    }
}

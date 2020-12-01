package org.rnn;

import org.rnn.Identificators.Identificator;
import org.rnn.implementation.ConfigParser;

import java.util.ArrayList;

public class UnifiedCommunicationModule
{
    public static UnifiedLogger log;
    protected static RoutingModule routingModule;
    protected static PathTranslator pathTranslator;
    public static PathInstaller pathInstaller;


    public static long getBytesOnDevice(Identificator id){return 1;}
    public static long getBytesOnFlow(PathFlow flow){return 1;}
    public static long getBytesOnMRI(MultiRoutingInfo mri){return 1;}


    public UnifiedCommunicationModule(PathInstaller pathInstaller,UnifiedLogger logger)
    {
        log =logger;
        ConfigParser.MASTER = this;
        //ArrayList<Daemon> daemons =ConfigParser.getDaemons();
        //NetState.setDaemons(daemons);
        //log.info("DAEMONS SET");
        //ConfigParser.setupParametersOfNetState();
        UnifiedCommunicationModule.pathInstaller = pathInstaller;
    }
    public static void setupTopology(ArrayList<ForwardingDevice> topology)
    {
        NetState.setTopology(topology);
        routingModule = new RnnRouting();
        //pathTranslator = new PathTranslator(1000);
        NetState.setupHostSpecificRNNs();
        NetState.routingModule = routingModule;
    }
    public static void receivePacketInfo( PacketInfo packetInfo)
    {
        try {
            routingModule.receiveSmartPacket(packetInfo);
        }
        catch (Exception e)
        {
            log.error(e.getMessage());
        }
    }
    public static void receiveSecurityInfo(SecureHost host)
    {
        try {
            //NetState.addSecureHost(host);
        }
        catch (Exception e)
        {
            log.error(e.getMessage());
        }
    }
    public static void receiveSecurityInfo(Identificator id,int value)
    {
        NetState.getTopology().get(NetState.getForwardingDeviceByDeviceID(id)).setSensivity(value);
    }
    public static ArrayList<PathFlow> getPaths()
    {
        return PathTranslator.getPaths();
    }
    public static void cycleNetwork()
    {
        PathTranslator.checkPaths();
        PathTranslator.updateBytesCountersOnFlows();
        NetState.updateBytesCountersOnFlows();
    }

}

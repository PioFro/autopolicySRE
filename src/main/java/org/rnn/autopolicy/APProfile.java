package org.rnn.autopolicy;

import com.google.common.base.Objects;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.rnn.Identificators.DeviceIdReal;
import org.rnn.NetState;
import org.rnn.UnifiedCommunicationModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TimerTask;

/**
 * A profile gathered for the IoT device instance. This implements the TimerTask
 * interface so it can be scheduled to lurk through the flows on device and check
 * if the profile is being fulfilled.
 */
public class APProfile extends TimerTask
{
    /**
     * Forwarder on which the IoT device is deployed. Note that this CAN be changed
     */
    public DeviceIdReal id;
    /**
     * IpAddress connected to the IoT device identified by the mac address.
     */
    public IpAddress ip;
    /**
     * Identificatior of the IoT device which is profiled by this profile.
     */
    public MacAddress mac;
    /**
     * List of flows in the profile for the IoT device
     */
    public ArrayList<APFlow> apFlows = new ArrayList<>();

    public ForwardingObjective defaultForwardingObjective;

    public String manufacturer = "SerIoT";
    public String typeDevice = "IoT automatically profiled";
    public String revision = "242";

    /**
     * Was alerted and not mitigated?
     */
    public boolean alert = false;

    public APProfile(DeviceIdReal ID, IpAddress IP, MacAddress MAC, ArrayList<APFlow> apFlows)
    {
        id = ID;
        ip=IP;
        mac=MAC;
        this.apFlows = apFlows;
        TrafficSelector.Builder selectorB = DefaultTrafficSelector.builder()
                .matchEthSrc(MAC)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IP.toIpPrefix());
        TrafficTreatment treat = DefaultTrafficTreatment.builder()
                .setEthDst(MacAddress.ZERO)
                .setOutput(PortNumber.CONTROLLER)
                .build();
        ForwardingObjective fwdo = org.onosproject.net.flowobjective.DefaultForwardingObjective.builder()
                .withSelector(selectorB.build())
                .withTreatment(treat)
                .withPriority(NetState.AP_NOT_IN_PROFILE)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .makePermanent()
                .fromApp(NetState.MASTER.getAppId())
                .add();
        defaultForwardingObjective = fwdo;
    }

    @Override
    public void run()
    {
        Iterable<FlowEntry> realFlows = NetState.MASTER.getFlowsOnDevice(id.getRealDeviceId());
        Iterator<FlowEntry> iterFlow = realFlows.iterator();
        while(iterFlow.hasNext())
        {
            FlowEntry entry = iterFlow.next();
            // Check if flows are mine
            if(NetState.MASTER.getAppId().id() == entry.appId())
            {
                if(NetState.AP_PROFILE_PRIOROTY == entry.priority())
                {
                    for(Iterator<APFlow> flows = this.apFlows.iterator();flows.hasNext();)
                    {
                        APFlow flow = flows.next();
                        APFlow.ProfileResponse response = flow.check(entry);
                       if( response == APFlow.ProfileResponse.GOOD )
                       {
                           break;
                       }
                       else if ( response == APFlow.ProfileResponse.BAD )
                       {
                           this.mitigate(true, flow);
                           break;
                       }
                    }
                }
                else if (NetState.AP_NOT_IN_PROFILE == entry.priority())
                {
                    if( entry.bytes() != 0 )
                    {
                        UnifiedCommunicationModule.log.warn("Flow connected with the " +
                                                                    "profile on "+this.ip.toString()+"" +
                                                                    "has bytes != 0. Check your alerts.");
                    }
                }
            }
        }
    }
    public void mitigate(boolean bitrate, APFlow flow)
    {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        this.alert = true;

        if(bitrate)
            APManager.postAnAlert(mac.toString(),
                                  mac.toString()+" crossed bitrate in the profile.",
                                  5,
                                  "The device "+mac.toString()+" crossed the flow rule of " +flow.toString()+
                                          ". Reported by "+id.toString()+
                                          ". Event took place: "+ dtf.format(now)+
                                          ". Information provided by: SRE."+NetState.getAppId()
                                  );
        else
        {
            APManager.postAnAlert(mac.toString(),
                                  mac.toString()+" tried to connect to flow outside of his profile.",
                                  5,
                                  "The device "+mac.toString()+
                                          " tried to connect to"+
                                          ". Reported by "+id.toString()+
                                          ". Event took place: "+ dtf.format(now)+
                                          ". Information provided by: SRE."+NetState.getAppId()
            );

        }
        NetState.addBlackListItem(ip);
        UnifiedCommunicationModule.log.info("Mitigating crossing the profile mac:"+mac.toString()+"ip: "+ip.toString()+" on forwarder: "+id.toString());

    }

    public void installProfile()
    {
        for( Iterator<APFlow> flows = apFlows.iterator();flows.hasNext();)
        {
            APFlow flow = flows.next();
            NetState.MASTER.installFlowFromPath(flow.getForwardingObjectiveForAPFlow(),id.getRealDeviceId());
        }
        NetState.MASTER.installFlowFromPath(this.defaultForwardingObjective, id.getRealDeviceId());
    }

    @Override
    public String toString()
    {
        String toRet = "Profile for device: "+mac.toString()+" - "+ip.toString()+"\n";
        for(APFlow flow : apFlows)
        {
            toRet+=flow.toString()+"\n";
        }
        return toRet;
    }
}

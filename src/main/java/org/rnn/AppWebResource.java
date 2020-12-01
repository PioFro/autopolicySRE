/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rnn;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.rest.AbstractWebResource;
import org.rnn.Identificators.DeviceIdReal;
import org.rnn.Identificators.HostIdReal;
import org.rnn.autopolicy.APFlow;
import org.rnn.autopolicy.APManager;
import org.rnn.autopolicy.APProfile;
import org.rnn.implementation.ConfigParser;
import org.rnn.implementation.CurveInterpolation;
import org.rnn.implementation.FlowEndpoint;
import org.rnn.implementation.ForwardingObjectiveTranslator;
import org.rnn.implementation.IncorectStringObjectiveException;
import org.rnn.implementation.RealCommunicator;
import org.rnn.implementation.StringObjectiveInformation;
import org.rnn.json.JSONException;
import org.rnn.json.JSONObject;
import org.rnn.policy.Criteria;
import org.rnn.policy.Property;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.FileSystemException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

/**
 * Handler for rest input. Reveives information about host and devices security
 * @author Piotr Frohlich
 * @version 3.0.0
 */
@Path("SRE")
public class AppWebResource extends AbstractWebResource {

    String VALUE = "value";
    String IP = "client_ip";
    String PORT = "client_port";
    String EXPLANATION = "explanation";
    String DEVICE_ID = "node_id";
    String SENSITIVITY = "sensitivity";
    String ACTION = "action";
    String TYPE = "type";

    String FLOW_SRC = "flow_src";
    String FLOW_DST = "flow_dst";
    String DEFLECTION_DESTINATION = "deflection-dest";
    String LOG_OF_EVENT = "Event";


    String ID = "id";
    String SUBJECT = "subject";
    String NAME = "name";
    String CRITERIA = "criteria";

    /**
     * POST security info about host.
     *
     * JSON file must consist of:
     * <p>IP address (key "ip")</p>
     * <p>Security Metric (key "metric")</p>
     * <p>Host ID (key "id")</p>
     * <p>Explanation (key "explanation")</p>
     * @return 200 OK
     */
    @POST
    @Path("securityclient")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getGreeting(InputStream stream) {
        ObjectNode node;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

            JsonNode hostIP = jsonTree.get(IP);
            JsonNode value = jsonTree.get(VALUE);
            JsonNode hostPortInfo = jsonTree.get(PORT);

            String hostPortInfoString = hostPortInfo.asText();
            String tmp [] = hostPortInfoString.split(":");
            byte protocol = 0;
            PortNumber port = PortNumber.ALL;
            try
            {
                if(tmp[0].equalsIgnoreCase("udp"))
                {
                    protocol = IPv4.PROTOCOL_UDP;
                }
                if(tmp[0].equalsIgnoreCase("tcp"))
                {
                    protocol = IPv4.PROTOCOL_TCP;
                }
                if(!tmp[1].equals("ALL"))
                {
                    port = PortNumber.portNumber(Long.parseLong(tmp[1]));
                }
            }
            catch (Exception e)
            {

            }

            //JsonNode hostID = jsonTree.get(ONOS_HOST);\
            JsonNode explanation = null;
            try {
                explanation = jsonTree.get(EXPLANATION);
            } catch (Exception e)
            {}

            JsonNode type = jsonTree.get(TYPE);

            HostId hostId = RealCommunicator.getMaster().getHostIdByIpAddress(IpAddress.valueOf(hostIP.asText()));

            UnifiedCommunicationModule.log.info("### NEW SECURITY RECORD FOR " + hostIP.asText() + " TYPE: "+type.asText()+" VALUE: " + value.asText());
            if(explanation!=null)
                NetState.addSecureHost(new SecureHost(new HostIdReal(hostId,hostId.hashCode()), value.asInt(), hostIP.asText(), explanation.asText()), port, protocol, type.asText(),value.asInt());
            else
                NetState.addSecureHost(new SecureHost(new HostIdReal(hostId,hostId.hashCode()), value.asInt(), hostIP.asText(), "NO EXPLANATION PROVIDED"), port, protocol, type.asText(),value.asInt());



            NetState.setParamsChanged();




            node = mapper().createObjectNode().put("OK","OK");
        } catch (Exception e) {
            node = mapper().createObjectNode().put("ERROR", e.getMessage());
        }
        return ok(node).build();
    }

    /**
     * POST sensitivity information about given device. JSON file must contain:
     * <p>Device id (key "device id")</p>
     * <p>Sensitivity of given device (key "sensitivity")</p>
     *
     * @return 200 OK
     */
    @POST
    @Path("securitynode")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getDevicesSensitivity(InputStream stream) {
        ObjectNode node;
        try {

            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

            JsonNode device = jsonTree.get(DEVICE_ID);
            JsonNode value = jsonTree.get(VALUE);
            JsonNode type = jsonTree.get(TYPE);

            UnifiedCommunicationModule.log.info("### NEW SENSITIVITY RECORD FOR " + device.asText() +" TYPE :"+type.asText()+" VALUE: " + value.asText());
            if(type.asText().equalsIgnoreCase("sensitivity"))
                NetState.getTopology().get(NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(DeviceId.deviceId(device.asText()), device.hashCode()))).setSensivity(value.asInt());
            else
                {
                    if(type.asText().equalsIgnoreCase("trust"))
                    {
                        NetState.getTopology().get(NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(DeviceId.deviceId(device.asText()), device.hashCode()))).setTrust(value.asInt());
                    }
                    else {
                        UnifiedCommunicationModule.log.warn("Device doesn't have field in type " + type.asText());
                        node = mapper().createObjectNode().put("Device doesn't have field in type ",type.asText());
                        return ok(node).build();
                    }
                }
            NetState.setParamsChanged();

            node = mapper().createObjectNode().put(device.asText(), value.asText()).put(type.asText(),"OK");
        } catch (Exception e) {
            node = mapper().createObjectNode().put("ERROR", e.getMessage());
        }
        return ok(node).build();
    }

    /**
     * Javadoc
     * @param stream stream
     * @return response
     */
    @POST
    @Path("mitigation")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteDevice(InputStream stream) {
        ObjectNode node;
        node = mapper().createObjectNode().put("Received mitigation query","Version 2.3.3");
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode action = jsonTree.get(ACTION);
            if(action.asText().equalsIgnoreCase("deflect"))
            {
                try
                {
                    String reason = "";
                    JsonNode destinationDevice = jsonTree.get(FLOW_DST);
                    JsonNode attackingDevice = jsonTree.get(FLOW_SRC);
                    JsonNode deflectionDevice = jsonTree.get(DEFLECTION_DESTINATION);
                    try {
                        JsonNode logReason = jsonTree.get(LOG_OF_EVENT);
                        reason = logReason.asText();
                    }
                    catch ( Exception e)
                    {
                        reason = "Reason not stated";
                    }
                    UnifiedCommunicationModule.log.info("Device: "+destinationDevice.asText() + " is attacked by: "+attackingDevice.asText()+ " discovered reason: "+reason+ " taken action: "+action.asText());
                    HostId destinationHost = RealCommunicator.master.getHostIdByIpAddress(IpAddress.valueOf(destinationDevice.asText()));
                    HostId attackingHost = RealCommunicator.master.getHostIdByIpAddress(IpAddress.valueOf(attackingDevice.asText()));
                    HostId honeypotHostId = RealCommunicator.master.getHostIdByIpAddress(IpAddress.valueOf(deflectionDevice.asText()));

                    if(destinationHost.equals(HostId.NONE))
                    {
                        node.put("Destination host unknown", destinationDevice.asText());
                    }
                    if(attackingHost.equals(HostId.NONE))
                    {
                        node.put("Attacking host unknown", attackingDevice.asText());
                    }
                    if(honeypotHostId.equals(HostId.NONE))
                    {
                        node.put("Honeypot host unknown", deflectionDevice.asText());
                    }
                    //VisAppComp.setHostTwo(NetState.getBySimpleID(NetState.getSimpleIDByIdOfConnectedHost(new HostIdReal(honeypotHostId,honeypotHostId.hashCode()))).getDeviceControllerId().toString());
                    try {
                        RealCommunicator.master.maskMovemement(destinationHost, honeypotHostId, attackingHost, true);
                    }
                    catch (Exception exc)
                    {
                        node.put("Error with installing rule", action.asText());
                        node.put("dst",destinationDevice.asText());
                        node.put("src", attackingDevice.asText());
                        node.put("honeypot", honeypotHostId.toString());
                    }
                    node.put("Installing rule ended successfully", action.asText());
                    node.put("dst",destinationDevice.asText());
                    node.put("src", attackingDevice.asText());
                    node.put("honeypot", honeypotHostId.toString());
                }
                catch (Exception e)
                {
                    node.put("ERROR", e.getLocalizedMessage());
                }


                return  ok(node).build();
            }
            if(action.asText().equalsIgnoreCase("block"))
            {
                String src= "", dst="",retInfo = "Blocking all flows ";
                FlowEndpoint srcEndpoint, dstEndpoint;
                try
                {
                    JsonNode jsonNode = jsonTree.get(FLOW_SRC);
                    src = jsonNode.asText();
                }
                catch (Exception e)
                {
                    node.put("src wasn't provided","blocking all flows leading to DST!");
                    src = "";
                }
                try
                {
                    JsonNode jsonNode = jsonTree.get(FLOW_DST);
                    dst = jsonNode.asText();
                }
                catch (Exception e)
                {
                    node.put("dst wasn't provided","blocking all flows leading from SRC!");
                    dst = "";
                }
                if(!src.equals(""))
                {
                    retInfo+="from "+src+" ";
                }
                if(!dst.equals(""))
                {
                    retInfo+="to "+dst;
                }
                try {
                    ForwardingObjective fwd = ForwardingObjectiveTranslator.translateStringToForwardingObjective(src, dst, true);
                    if(ForwardingObjectiveTranslator.srcId != null)
                    {
                        NetState.MASTER.installFlowFromPath(fwd, ForwardingObjectiveTranslator.srcId);
                        node.put("OF RULE INSTALLED ON",
                                  ForwardingObjectiveTranslator.srcId.toString());
                    }
                    else
                    {
                        node.put("OF RULE INSTALLED ON","ALL DEVICES!!!");
                        for(ForwardingDevice fd: NetState.getTopology())
                        {
                            NetState.MASTER.installFlowFromPath(fwd,((DeviceIdReal)fd.getDeviceControllerId()).getRealDeviceId());
                        }
                    }
                    node
                            .put("SRC",src)
                            .put("DST",dst)
                            .put("STATUS CODE","OK");
                }
                catch (Exception e)
                {
                    node.put("ERROR",e.getMessage()).put("STATUS CODE","NOT OK");
                    return ok(node).build();
                }


                return ok(node).build();
            }
            if(action.asText().equalsIgnoreCase("blacklist")||action.asText().equalsIgnoreCase("whitelist"))
            {
                try {
                    JsonNode jsonNode = jsonTree.get(FLOW_SRC);
                    String src = jsonNode.asText();
                    StringObjectiveInformation soi = new StringObjectiveInformation();
                    soi.validateStringObjective(src);
                    soi.validateAll();
                    if(action.asText().equalsIgnoreCase("blacklist"))
                        NetState.addBlackListItem(soi.ipAddress);
                    else
                        NetState.delFromBlackList(soi.ipAddress);
                }
                catch (IncorectStringObjectiveException e)
                {
                    node.put("ERROR: ",e.getMessage());
                    return ok(node).build();
                }
            }
        }
        catch (Exception e)
        {
            node.put("ERROR","parsing json");
            return ok(node).build();
        }
        return ok(node).build();
    }

    /**
     * Method used to change changable parameters in the NetState class.
     * @param stream json file
     * @return Response to the administrator
     */
    @POST
    @Path("parameters")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setParameters(InputStream stream)
    {
        //BlockChainAlertManager.sendAlert("TRY THIS", "TRY");
        ObjectNode node;
        node = mapper().createObjectNode().put("Changing parameters","Version 2.0.3");
        try
        {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

            Field[] fields = NetState.class.getDeclaredFields();
            for (int i = 0; i <fields.length; i++)
            {
                JsonNode param = jsonTree.get(fields[i].getName());
                if(param!= null)
                {
                    Object obj = fields[i].get(Class.forName("org.rnn.NetState"));

                    if(fields[i].getAnnotatedType().getType()==Double.TYPE)
                    {
                        fields[i].setDouble(obj, param.asDouble());
                    }
                    if(fields[i].getAnnotatedType().getType()==Integer.TYPE)
                    {
                        fields[i].setInt(obj, param.asInt());
                    }
                    try
                    {
                        fields[i].set(obj, param.asText());
                    }
                    catch(Exception e)
                    {
                        node.put("EXCEPTION",e.getMessage());
                    }
                    node.put(fields[i].getName(), param.asText());

                    if(fields[i].getName().equals("HONEYPOT_IP"))
                    {
                        try {
                            NetState.HONEYPOT_IP = jsonTree.get("HONEYPOT_IP").asText();
                        }
                        catch (Exception exc)
                        {}
                    }
                }
            }
        }
        catch (Exception e)
        {
            node.put("ERROR", e.getLocalizedMessage());

        }

        return  ok(node).build();
    }

    /**
     * Method used to post the criteria or property of a given subject.
     * @param stream json file
     * @return response to the administrator
     */
    @POST
    @Path("policy")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setPolicy(InputStream stream) {
        ObjectNode node;
        try
        {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode subject = jsonTree.get(SUBJECT);
            String sSubject = subject.asText();
            if(sSubject.equalsIgnoreCase("link"))
            {
                JsonNode id = jsonTree.get(ID);

                String [] devicesTuple = id.asText().split("/");

                DeviceId sourceDevice = DeviceId.deviceId(devicesTuple[0]),
                        destinationDevice = DeviceId.deviceId(devicesTuple[1]);

                int indexOfSource = NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(sourceDevice,sourceDevice.hashCode()));

                JsonNode c = jsonTree.get(CRITERIA);
                JsonNode name = c.get(NAME);
                JsonNode value = c.get(VALUE);
                Property prop = new Property("LINK",name.asText(),value.asText());

                NetState.getTopology().get(indexOfSource).updatePropertiesOnLink(new DeviceIdReal(destinationDevice, destinationDevice.hashCode()),prop);

            }
            if(sSubject.equalsIgnoreCase("device"))
            {
                JsonNode id = jsonTree.get(ID);

                String device = id.asText();

                DeviceId sourceDevice = DeviceId.deviceId(device);

                int indexOfSource = NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(sourceDevice,sourceDevice.hashCode()));

                JsonNode c = jsonTree.get(CRITERIA);
                JsonNode name = c.get(NAME);
                JsonNode value = c.get(VALUE);
                Property prop = new Property("DEVICE",name.asText(),value.asText());

                NetState.getTopology().get(indexOfSource).updateProperties(prop);
            }
            if(sSubject.equalsIgnoreCase("host"))
            {
                JsonNode id = jsonTree.get(ID);

                String host = id.asText();

                HostIdReal hostId = new HostIdReal(HostId.hostId(host),0);

                JsonNode c = jsonTree.get(CRITERIA);
                JsonNode name = c.get(NAME);
                JsonNode value = c.get(VALUE);
                JsonNode subjectCriteria = c.get(SUBJECT);
                JsonNode type = c.get(TYPE);

                Criteria criteria = new Criteria();
                criteria.setTYPE(Criteria.CRITERIA_TYPE.valueOf(type.asText()));
                criteria.setSubject(subjectCriteria.asText());
                criteria.setName(name.asText());
                criteria.setValue(value.asText());
                NetState.updatePolicyOfHost(hostId,criteria);
            }
            node = mapper().createObjectNode().put("OK","OK");
        }
        catch (Exception e)
        {
            node = mapper().createObjectNode().put("ERROR", e.getMessage());
        }
        return ok(node).build();
    }

    /**
     * Method used to send the configuration via the REST iface
     * @param stream json stream
     * @return Response to administrator
     */
    @POST
    @Path("configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setConfig(InputStream stream) {
        ObjectNode node;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            ConfigParser.configString = jsonTree.toString();
            NetState.setDaemons(ConfigParser.getDaemonsFromString());
            node = mapper().createObjectNode().put("OK","OK") ;
            NetState.MASTER.activateWithProvidedConfig(NetState.getDaemons());
        }
        catch (FileSystemException e)
        {
            node = mapper().createObjectNode().put("error","provided json was not correct. "+e.getMessage());
        }
        catch (Exception e)
        {
            node = mapper().createObjectNode().put("error","other error");
        }
        return ok(node).build();
    }
    @GET
    @Path("cnm")
    public Response getCNM()
    {
        ObjectNode node,resp;
        try {
            node = (ObjectNode)mapper().readTree(NetState.jsonify());
        }
        catch (Exception e)
        {
            node = mapper().createObjectNode().put("error","error");
        }
        return ok(node).build();
    }
    @POST
    @Path("path")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postNewPath(InputStream stream)
    {
        ObjectNode node = mapper().createObjectNode().put("CODE","OK. Path was added. ");
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JSONObject json = new JSONObject(jsonTree.toString());
            try
            {
                PathFlow flow = new PathFlow(json);
                PathTranslator.addNewPath(flow);
                node = mapper().createObjectNode().put("CODE","OK. Path was added. "+flow.toString());
            }
            catch (JSONException jException)
            {
                node =mapper().createObjectNode().put("error","provided json was not correct. "+jException.getMessage());
            }
            catch (Exception vException)
            {
                node = mapper().createObjectNode().put("error","Provided data was incorrect. "+vException.getMessage());
            }
        }
        catch (Exception e)
        {
            node = mapper().createObjectNode().put("error","provided json was not correct. "+e.getMessage());
        }
        return ok(node).build();
    }
    @POST
    @Path("autopolicy")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response apProfile(InputStream stream)
    {
        ObjectNode node = mapper().createObjectNode().put("code","ok");
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode mac, forwarderID, srcIP, allow;
            MacAddress realMac;
            String currentCheck = "";
            APProfile profile;
            ArrayList<APFlow> flows = new ArrayList<>();
            IpAddress realSrc;
            DeviceId deviceId;
            try {
                currentCheck = "mac";
                mac = jsonTree.get(currentCheck);
                realMac = MacAddress.valueOf(mac.asText());
                currentCheck = "id";
                forwarderID = jsonTree.get(currentCheck);
                deviceId =DeviceId.deviceId(forwarderID.asText());
                currentCheck="srcIP";
                srcIP = jsonTree.get(currentCheck);
                realSrc = IpAddress.valueOf(srcIP.asText());
                currentCheck ="from device";
                allow = jsonTree.get(currentCheck);
                currentCheck="allow";
                allow = allow.get(currentCheck);
                if(allow.isArray())
                {
                    for(int i=0; i<allow.size();i++)
                    {
                        String apflow = allow.get(i).asText();
                        String [] apflowSplit = apflow.split(" ");
                        currentCheck = "ip address";
                        IpAddress ip = IpAddress.valueOf("0.0.0.0");
                        if(!apflowSplit[1].equals("*"))
                            ip = IpAddress.valueOf(apflowSplit[1]);
                        currentCheck = "protocol";
                        int protocol = -1;
                        if (!apflowSplit[2].equals("*"))
                            protocol = Integer.parseInt(apflowSplit[2]);
                        currentCheck = "dport";
                        int dport = -1;
                        if(!apflowSplit[3].equals("*"))
                            dport= Integer.parseInt(apflowSplit[3]);
                        currentCheck = "bitrate";
                        float bitrate = Float.parseFloat(apflowSplit[4]);
                        APFlow flow = new APFlow(protocol,dport, ip, realMac);
                        HostId src = NetState.MASTER.getHostIdByIpAddress(realSrc);
                        HostId dst = NetState.MASTER.getHostIdByIpAddress(ip);
                        int simpleSrc = NetState.getSimpleIDByIdOfConnectedHost(new HostIdReal(src,src.hashCode()));
                        int simpleDst = NetState.getSimpleIDByIdOfConnectedHost(new HostIdReal(dst,dst.hashCode()));
                        flow.simpleDst = simpleDst;
                        flow.simpleSrc =simpleSrc;
                        flow.maxBitrate = bitrate*(1024);
                        flows.add(flow);
                    }
                }
                profile = new APProfile(new DeviceIdReal(deviceId,deviceId.hashCode()),realSrc, realMac,flows);
                APManager.addProfile(realMac, profile);
            }
            catch (Exception e)
            {
                return Response.notModified("wrong "+currentCheck).build();
            }
        }
        catch (Exception e)
        {
            node = mapper().createObjectNode().put("error","provided json was not correct. "+e.getMessage());
            return Response.notModified().build();
        }
        return Response.ok(node).build();
    }

    @POST
    @Path("autopolicy/{mac}/{deviceid}/{ip}")
    public Response profile(@PathParam("mac") String mac, @PathParam("deviceid") String deviceid,@PathParam("ip") String ip)
    {
        ObjectNode node = mapper().createObjectNode().put("Started profiling time",System.currentTimeMillis());
        String test = "mac";
        try {

            MacAddress macaddr = MacAddress.valueOf(mac);
            test = "device id";
            DeviceId device = DeviceId.deviceId(deviceid);
            test = "ip";
            IpAddress realIp = IpAddress.valueOf(ip);
            APManager.startProfiling(macaddr, device, realIp);
        }
        catch (NotFoundException e1)
        {
            node = mapper()
                    .createObjectNode()
                    .put("Error while cross validating data" +
                                 " against the network state",e1.getMessage());
        }
        catch (Exception e)
        {
            node = mapper().createObjectNode()
                    .put("error parsing field",test)
                    .put("Reason",e.getLocalizedMessage());
            return Response.ok(node).build();
        }
        node
                .put("For the device",mac+" / "+ip)
                .put("For time ",NetState.AP_PROFILE_BUILD_TIME/(3600000)+" hours");
        return Response.ok(node).build();
    }
    @GET
    @Path("autopolicy/data/{id}")
    public Response profile(@PathParam("id") String id)
    {
        ObjectNode node = mapper().createObjectNode().put("Data from the id",id);
        // Decide what kind of the id is it:
        // of: marker for the forwarder
        // - for the link
        // more than 5 ':' for the host
        if(id.contains("of:")&& !id.contains("-"))
        {
            DeviceId deviceId = DeviceId.deviceId(id);
            ForwardingDevice fd = NetState.getTopology().get(NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(deviceId,deviceId.hashCode())));
            ArrayNode properties = node.putArray("properties");
            for (Property property:fd.properties
                 )
            {
                JsonNode prop = mapper().createObjectNode()
                        .put("name",property.getName())
                        .put("value",property.getValue());
                properties.add(prop);
            }
            return ok(node).build();
        }
        else if(id.split(":").length>2 && !id.contains("-"))
        {
            HostId host = HostId.NONE;
            try {
                host = HostId.hostId(MacAddress.valueOf(id));
            }
            catch (Exception e)
            {
                node.put("Fail Reason","Provided MAC was incorrect");
                ok(node).build();
            }
            SecureHost sc = NetState.getSecureHostById(new HostIdReal(host, host.hashCode()));

            ArrayNode criteria = node.putArray("criteria manager");
            ArrayList<Criteria> policy = sc.getPolicy().getPolicy();
            for(Criteria criterion:policy)
            {
                JsonNode arr = mapper().createObjectNode()
                        .put("name",criterion.getName())
                        .put("subject",criterion.getSubject())
                        .put("value",criterion.getValue());
                criteria.add(arr);
            }
            APProfile profile = APManager.profiles.get(id);
            JsonNode ap;
            if(profile == null)
            {
                JsonNode ap2 = mapper().createObjectNode().put("allow","Everything");
                ap = mapper().createObjectNode()
                        .put("from device",ap2);
                node.put("autopolicy profile",ap);
                JsonNode identity = mapper().createObjectNode()
                        .put("manufacturer","SerIoT")
                        .put("device","Desktop PC - unprofiled")
                        .put("revision","1000");

                node.put("autopolicy identity",identity);
            }
            else
            {
                ObjectNode ap2 = mapper().createObjectNode();
                ArrayNode array = ap2.putArray("allow");
                for(APFlow flow: profile.apFlows)
                {
                    //dst 10.0.1.1/24 tcp 80,443,8888 0.5
                    String proto = "None";
                    if(flow.protocol==17)
                        proto = "udp";
                    if(flow.protocol==6)
                        proto="tcp";

                    array.add("dst "+flow.dst.toString()+" "+proto+" "+flow.endPort+" "+flow.maxBitrate);
                }
                ap = mapper().createObjectNode().put("from device",ap2);
                node.put("autopolicy profile",ap);
                JsonNode identity = mapper().createObjectNode()
                        .put("manufacturer","SerIoT")
                        .put("device","Desktop PC - unprofiled")
                        .put("revision","1000");

                node.put("autopolicy identity",identity);
            }

            return ok(node).build();
        }
        else if(id.contains("-"))
        {
            DeviceId deviceIdSrc = DeviceId.deviceId(id.split("-")[0]),deviceIdDst =DeviceId.deviceId(id.split("-")[1]);

            ForwardingDevice fd = NetState.getTopology().get(NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(deviceIdSrc,deviceIdSrc.hashCode())));
            ArrayList<Property> properties = fd.getProperitesOnDirection(new DeviceIdReal(deviceIdDst,deviceIdDst.hashCode()));
            ArrayNode propertiesJSON = node.putArray("properties");
            for (Property property:properties
            )
            {
                JsonNode prop = mapper().createObjectNode()
                        .put("name",property.getName())
                        .put("value",property.getValue());
                propertiesJSON.add(prop);
            }
            return ok(node).build();
        }

        return ok(node).build();
    }

    @GET
    @Path("autopolicy/getprofile/{manufacturer}/{device}/{revision}/{version}/")
    public Response getProfile(@PathParam("manufacturer") String manufacturer,
                               @PathParam("device") String device,
                               @PathParam("revision") String revision,
                               @PathParam("version") String version)
    {
        String configJson = "";
        //ObjectNode node = mapper().createObjectNode();
        try
        {
            Scanner scanner = new Scanner(new File(NetState.AP_DB_PATH+manufacturer+"/"+device+"/"+revision+"/"+version+"/profile.json"));
            configJson = scanner.useDelimiter("\\A").next();
            scanner.close();
            JsonNode json = mapper().readTree(configJson);
            return Response.ok(json).build();

        }
        catch (Exception e)
        {
            return Response.notAcceptable(new ArrayList<>()).build();
        }
    }
    @GET
    @Path("path/getbest/{src}/{dst}/")
    public Response getBestPath(@PathParam("src") String src,
                               @PathParam("dst") String dst)
    {
        ObjectNode ret = mapper().createObjectNode();
        try {
            ForwardingDevice srcId = NetState.getTopology().get(NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(DeviceId.deviceId(src),this.hashCode())));
            ForwardingDevice dstId = NetState.getTopology().get(NetState.getForwardingDeviceByDeviceID(new DeviceIdReal(DeviceId.deviceId(dst),this.hashCode())));

            PathFlow path = PathTranslator.getBestPathFromSrcDst(srcId.getSimpleID(),dstId.getSimpleID());
            ArrayNode p = ret.putArray("path");

            for(Node node: path.getPath()) {
                p.add(node.nodeDeviceId);
            }
            return Response.ok(ret).build();
        }
        catch (Exception e)
        {
            return Response.noContent().build();
        }
    }
    @POST
    @Path("energy")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postEnergy(InputStream stream)
    {
        ObjectNode node = mapper().createObjectNode().put("Start parsing","energy data");
        try
        {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            Iterator<String> iter= jsonTree.fieldNames();
            while(iter.hasNext())
            {
                String name = iter.next();
                Double value = jsonTree.get(name).asDouble();
                try {
                    DeviceIdReal id = new DeviceIdReal(DeviceId.deviceId(name), name.hashCode());
                    ForwardingDevice fd = NetState.getTopology().get(NetState.getForwardingDeviceByDeviceID(id));
                    fd.addNewEnergyMeasurement(value);
                    UnifiedCommunicationModule.log.info("TO FILE "+fd.getPredictedEnergyConsumption());
                }
                catch (Exception e)
                {
                    node.put("ERROR","No such forwarder "+name);
                }
            }
        }
        catch (Exception e)
        {
            node.put("ERROR", "Incorrect json");
        }
        return Response.ok(node).build();
    }
    @POST
    @Path("energy/curve/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postEnergyCurve(InputStream stream,@PathParam("id") String id)
    {
        ObjectNode node = mapper().createObjectNode().put("Start parsing","energy curve");
        DeviceIdReal deviceId = new DeviceIdReal(DeviceId.deviceId(id),0);
        if(NetState.getForwardingDeviceByDeviceID(deviceId) == -1)
        {
            node.put("error","no such device in the network");
            return Response.ok(node).build();
        }
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode x = jsonTree.get("x");
            JsonNode y = jsonTree.get("y");
            if(x.isArray() && y.isArray())
            {
                Iterator<JsonNode> xiter = x.elements();
                Iterator<JsonNode> yiter = y.elements();
                ArrayList<Double> xDouble = new ArrayList<>();
                ArrayList<Double> yDouble = new ArrayList<>();
                while(xiter.hasNext() && yiter.hasNext())
                {
                    xDouble.add(xiter.next().asDouble());
                    yDouble.add(yiter.next().asDouble());
                }
                CurveInterpolation ci = new CurveInterpolation(0,1024,1,xDouble,yDouble);
                node.put("ineterpolated to",ci.iY.toString());
                CurveManager cm = new CurveManager(ci.iY);
                NetState.addCurve(cm,id);
                return Response.ok(node).build();
            }
            else
            {
                node.put("error","x and y must be arrays of double");
            }

        }
        catch (Exception e)
        {
            node.put("error","Parsing json");
            return Response.ok(node).build();
        }
        return Response.ok(node).build();
    }
}
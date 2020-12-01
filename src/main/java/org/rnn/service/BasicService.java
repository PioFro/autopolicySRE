package org.rnn.service;

import org.onlab.packet.IpAddress;
import org.onosproject.net.Path;
import org.rnn.Identificators.DeviceIdReal;
import org.rnn.Identificators.HostIdReal;
import org.rnn.Identificators.Identificator;
import org.rnn.NetState;
import org.rnn.PathFlow;
import org.rnn.PathTranslator;
import org.rnn.UnifiedCommunicationModule;
import org.rnn.implementation.ConfigParser;
import org.rnn.implementation.RealCommunicator;
import org.rnn.implementation.RealLogger;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;

/**
 * @author Piotr Frohlich
 * @version 1.0.1
 * This class is a representation of the single (Basic) service.
 */
public class BasicService implements IService
{
    /**
     * Name of the service
     */
    private String name;
    /**
     * A hashmap, mapping the device to the flowendpoint information.
     */
    private Dictionary<String, IServiceEndpoint> map;
    /**
     * A list of installed paths leading from clients/users to instances of the
     * base service.
     */
    private ArrayList<PathFlow> installedPaths = new ArrayList<>();
    /**
     * Network-wide ip address of the base service (instance independent).
     */
    public IpAddress staticIpAddressOfService;

    /**
     * This constructor takes the name of the service to get the configuration.
     * @param name
     */
    public BasicService(String name)
    {
        this.staticIpAddressOfService = IpAddress.valueOf("10.0.2.253");
        this.name = name;
        try {
            this.map = ConfigParser.getServiceHashTable(name);
        }
        catch (FileNotFoundException e)
        {
            UnifiedCommunicationModule.log.error(e.getMessage());
            this.map = null;
        }
        for (int i = 0; i <NetState.getTopology().size(); i++)
        {
            DeviceIdReal realId = (DeviceIdReal)NetState.getTopology().get(i).getDeviceControllerId();
            BasicServiceEndpoint endpoint = (BasicServiceEndpoint)map.get(realId.toString());
            String str = "http://"+endpoint.getRealIpAddress().toString()+":7474/setup/";
            connectToUrl(str);
        }

    }

    /**
     * This methods checks the service specific hash map and returns the real id
     * of corresponding to the given simple id.
     * @param simpleId The given simple id
     * @return The real id of the service instance corresponding to the given simple id
     */
    @Override
    public Identificator getIdentificatorOfServiceOnDevice(int simpleId)
    {
        BasicServiceEndpoint endpoint = (BasicServiceEndpoint)map.get(NetState.getBySimpleID(simpleId).getDeviceControllerId().toString());
        return endpoint.getId();
    }

    /**
     * Install a flow rule connecting given user and given service instance.
     * @param idHost Given user's id
     * @param idService Given service id (real id, provided by the hash map)
     */
    @Override
    public void installPathFromClientToService(Identificator idHost, Identificator idService) {
        PathFlow flow;
        if (idHost.equals(idService))
        {
            flow = new PathFlow(NetState.getSimpleIDByIdOfConnectedHost(idHost),NetState.getSimpleIDByIdOfConnectedHost(idHost));
            flow.setDstHostId(idService);
            flow.setSrcHostId(idHost);
        }
        else {
            flow = PathTranslator.getBestPathFromSrcDst(idHost, idService);
            if (flow == null) {
                flow = PathTranslator.getBestPathFromSrcDst(NetState.getSimpleIDByIdOfConnectedHost(idHost), NetState.getSimpleIDByIdOfConnectedHost(idService));
                flow.setDstHostId(idService);
                flow.setSrcHostId(idHost);
            }
        }
        for (int i = 0; i <installedPaths.size(); i++)
        {
            try {
                if (installedPaths.get(i).getSrcHostId().equals(idHost))
                {
                    installedPaths.remove(i);
                }
            }
            catch (Exception e){}
        }
        installedPaths.add(flow);
        DeviceIdReal real = new DeviceIdReal(((DeviceIdReal)NetState.getBySimpleID(NetState.getSimpleIDByIdOfConnectedHost((HostIdReal)idService)).getDeviceControllerId()).getRealDeviceId(),flow.hashCode());
        BasicServiceEndpoint endpoint = (BasicServiceEndpoint)map.get(real.toString());
        PathTranslator.installServicePath(flow,7777,this.staticIpAddressOfService.toString());
    }

    /**
     * Getter for the service name
     * @return the string representation of the service name
     */
    @Override
    public String serviceName() {
        return name;
    }

    /**
     * Getter for all of the installed paths (connecting users to specific
     * instances of the services.
     * @return the list of paths
     */
    @Override
    public ArrayList<PathFlow> getInstalledPath() {
        return installedPaths;
    }

    /**
     * Getter for static (not instance-dependent but service as a whole dependent)
     * IP address of service
     * @return The service ip address
     */
    @Override
    public String getStaticIpAddress()
    {
        return staticIpAddressOfService.toString();
    }

    /**
     * Method starting the instance of the service by sending the information to
     * the ServiceManager running on machine able to initialize the service on it.
     * Do not use this method if the ServiceManager isn't running - it'll cause
     * malfunction of the SRE
     * @param id Id of the machine on which the service instance can be started.
     */
    @Override
    public void startService(Identificator id)
    {
        /**
        //Starting Service
        BasicServiceEndpoint endpoint = (BasicServiceEndpoint)map.get(RealCommunicator.master.getLocationOfHost(((HostIdReal)id).getRealHostId()).toString());
        UnifiedCommunicationModule.log.info("STARTING SERVICE ON " + endpoint.getIPAddress().toString());
        String url ="http://"+endpoint.getRealIpAddress().toString()+":7474/start/"+name+"/"+endpoint.getPortNumber()+"/"+endpoint.getPortNumber()+"/";
        connectToUrl(url);
         **/
    }
    /**
     * Method stopping the instance of the service by sending the information to
     * the ServiceManager running on machine able to end the service on it.
     * Do not use this method if the ServiceManager isn't running - it'll cause
     * malfunction of the SRE.
     * @param id Id of the machine on which the service instance can be started.
     */
    @Override
    public void stopService(Identificator id)
    {
        //Deleting stopped Id of destination service
        try {
            for (int i = 0; i < installedPaths.size(); i++) {
                if (installedPaths.get(i).getDstHostId().equals(id)) {
                    installedPaths.remove(i);
                }
            }
        }
        catch (Exception e){}

        /**
        //Stopping Service
        BasicServiceEndpoint endpoint = (BasicServiceEndpoint)map.get(RealCommunicator.master.getLocationOfHost(((HostIdReal)id).getRealHostId()).toString());
        UnifiedCommunicationModule.log.info("STOPPING SERVICE ON " + endpoint.getIPAddress().toString());
        String url ="http://"+endpoint.getRealIpAddress().toString()+":7474/stop/"+name+"/"+endpoint.getPortNumber()+"/";
        connectToUrl(url);
         **/
    }

    /**
     * This method will connect to given url.
     * @param url Url to which the connection must be made.
     */
    private void connectToUrl(String url)
    {
        try {
            URL realURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) realURL.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.connect();
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

}

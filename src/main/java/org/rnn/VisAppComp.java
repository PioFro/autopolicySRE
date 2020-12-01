package org.rnn;

import org.rnn.Identificators.HostIdReal;
import org.rnn.Identificators.Identificator;
import org.rnn.implementation.RealCommunicator;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;


public class VisAppComp {
    private static String hostOne = "";
    private static String hostTwo = "";
    private static String USER_AGENT = "Mozilla/5.0";
    private Integer someNumber;

    VisAppComp(){
        someNumber = 0;
    }

    public static void setHostOne(String hostOnetmp){
        hostOne = hostOnetmp;
        String result;
        result = VisAppComp.SetForwardingDevices(NetState.getTopology());
    }

    public static void setHostTwo(String hostTwotmp){
        hostTwo = hostTwotmp;
        String result;
        result = VisAppComp.SetForwardingDevices(NetState.getTopology());
    }

    private String run(){
        String string;
        try {
            TimeUnit.SECONDS.sleep(1);
            string = someNumber.toString();
        } catch(InterruptedException ex) {
            string = someNumber + ":  " +  ex.getMessage();
        }
        someNumber++;
        return string;
    }

    public static String SetForwardingDevices(ArrayList<ForwardingDevice> tmpForwardingDevices){
        ArrayList<String> simpleSwitchIDs = new ArrayList<>();
        ArrayList<String> hostIDs = new ArrayList<>();
        ArrayList<String> deviceToDeviceLinks = new ArrayList<>();
        ArrayList<String> switchHostLinks = new ArrayList<>();

        for (ForwardingDevice forwardingDevice: tmpForwardingDevices)
        {
            String tmp = Integer.toString(forwardingDevice.getSimpleID());
            if (!simpleSwitchIDs.contains(tmp)) {
                simpleSwitchIDs.add(tmp);
                for (PortDeviceIdTuple portDeviceIdTuple: forwardingDevice.links){
                    String tempDevice = portDeviceIdTuple.deviceId.toString();
                    for (ForwardingDevice forwardingDevice2: tmpForwardingDevices){
                        if (portDeviceIdTuple.deviceId.toString().equals(forwardingDevice2.getDeviceControllerId().toString())){
                            tempDevice = Integer.toString(forwardingDevice2.getSimpleID());
                        }
                    }

                    String temporString = tmp + ";" + tempDevice;
                    if (!deviceToDeviceLinks.contains(temporString))
                        deviceToDeviceLinks.add(temporString);
                }
            }
            for (Identificator hostId: forwardingDevice.getConnectedHosts()){
                try {
                    HostIdReal host = (HostIdReal)hostId;
                    hostIDs.add(RealCommunicator.master.getIpAddressByHostId(host.getRealHostId()).toString());
                    switchHostLinks.add(tmp + ";" +RealCommunicator.master.getIpAddressByHostId(host.getRealHostId()).toString());
                }catch(Exception e){}
            }
        }
        StringBuilder nodes = new StringBuilder();
        for (String tmp: simpleSwitchIDs){
            nodes.append(tmp);
            nodes.append(" ");
        }
        for (String tmp: hostIDs){
            nodes.append(tmp);
            nodes.append(" ");
        }
        nodes.deleteCharAt(nodes.length() - 1);

        StringBuilder links = new StringBuilder();
        for (String tmp: deviceToDeviceLinks){
            links.append(tmp);
            links.append(" ");
        }
        for (String tmp: switchHostLinks){
            links.append(tmp);
            links.append(" ");
        }
        links.deleteCharAt(links.length() - 1);

        StringBuilder selectedFlow = new StringBuilder();
        if (!hostOne.equals("") && !hostTwo.equals("")) {
            PathFlow pathFlow;
            long hostOneLong = 0;
             long hostTwoLong = 0;
            for (ForwardingDevice forwardingDevice: tmpForwardingDevices){
                if (forwardingDevice.getDeviceControllerId().toString().equals(hostOne)){
                    hostOneLong = forwardingDevice.getSimpleID();
                }
                if (forwardingDevice.getDeviceControllerId().toString().equals(hostTwo)){
                    hostTwoLong = forwardingDevice.getSimpleID();
                }
            }
            try {
                pathFlow = PathTranslator.getBestPathFromSrcDst(hostOneLong, hostTwoLong);
                if (pathFlow != null) {
                    for (Node node : pathFlow.getPath()) {
                        selectedFlow.append(node.getNodeDeviceId());
                        selectedFlow.append(" ");
                    }
                    selectedFlow.append(pathFlow.getDestination());
                }
            } catch (NullPointerException ex) {
                if (selectedFlow.length() > 0 ) {
                    selectedFlow.delete(0, selectedFlow.length() - 1);
                    selectedFlow.append("");
                }
            }
        }




        String result = "";
        if(NetState.VISUALISATION_IP_ADDRESS.equals("0.0.0.0") == false)
        {
            result = sendPOST(nodes.toString(), links.toString(), selectedFlow.toString());
        }
        return result;
    }
    static private String sendPOST(String nodes, String links, String selectedFlow) {
        USER_AGENT = "Mozilla/5.0";
        String url = null;
        try {
            //url = "http://192.168.100.36:5000/";
            url = "http://"+NetState.VISUALISATION_IP_ADDRESS+":5000/";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();


            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Language", "en-US,en;q-0.5");

            java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());

            String urlParameters;
            urlParameters = "nodes=" + nodes +
                    "&edges=" + links +
                    "&selectedFlow=" + selectedFlow +
                    "&timeStamp=" + currentTimestamp.toString();
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            UnifiedCommunicationModule.log.info(urlParameters);
            wr.flush();
            wr.close();

            Integer responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

            return response.toString();

        } catch (IOException ex) {
            return ex.getMessage();
        }
    }
}

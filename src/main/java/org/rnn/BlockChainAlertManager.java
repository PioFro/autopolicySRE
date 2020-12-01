package org.rnn;
import org.rnn.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
/**
 * Handler for the blockchain based logging of critical events.
 * @author Piotr Frohlich
 * @version 3.0.0
 */

public class BlockChainAlertManager
{

    /**
     * Send an allert for the storage in the blockchain
     * @param description Description of an event
     * @param type type of the event
     * @return Response if it was possible to post such an event in the blockchain
     * structure.
     */
    static public boolean sendAlert(String description, String type)
    {
        if(NetState.BLOCKCHAIN_USER_ID == null)
        {
            return false;
        }
        UnifiedCommunicationModule.log.info("SENDING REQUEST");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("service", "SerCon");
        jsonObject.put("type", type);
        jsonObject.put("description",description);
        UnifiedCommunicationModule.log.info("RQ: "+jsonObject.toString());
        try {
            //StringEntity stringEntity = new StringEntity(jsonObject.toString());
            String url ="http://"+NetState.BLOCKCHAIN_NODE_IP+":5000/api/v1/alerts/"+NetState.BLOCKCHAIN_USER_ID;
            URL realURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)realURL.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            String jsonString = jsonObject.toString();
            try(OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonString.getBytes("utf-8");
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
            UnifiedCommunicationModule.log.info("EXCEPTION : "+ e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Handshake method to start the coordination with the blockchain daemon.
     * @return True if communication was established. False otherwise (e.g. server
     * didn't respond).
     */
    static public boolean startSendingAlerts()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user_id",NetState.BLOCKCHAIN_USER_ID);
        try {

            //StringEntity stringEntity = new StringEntity(jsonObject.toString());
            String url =NetState.BLOCKCHAIN_NODE_IP+":5000/api/v1/admin/alerts/";

        }
        catch (Exception e)
        {
            return false;
        }
        return true;

    }
}

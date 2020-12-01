package org.rnn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;


@Path("Vis")
public class VisAppREST extends AbstractWebResource {

    String HOST = "host";

    @POST
    @Path("HostOne")
    public Response getHostOne(InputStream stream){
        ObjectNode node;
        try{
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode host = jsonTree.get(HOST);
            VisAppComp.setHostOne(host.asText());
             node = mapper().createObjectNode().put("OK","host added. Pay attention there is no testing of host absence.");
        } catch (Exception ex) {
            node = mapper().createObjectNode().put("ERROR", ex.getMessage());
        }

        return ok(node).build();
    }

    @POST
    @Path("HostTwo")
    public Response getHostTwo(InputStream stream){
        ObjectNode node;
        try{
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode host = jsonTree.get(HOST);
            VisAppComp.setHostTwo(host.asText());
            node = mapper().createObjectNode().put("OK","host added. Pay attention there is no testing of host absence.");
        } catch (Exception ex) {
            node = mapper().createObjectNode().put("ERROR", ex.getMessage());
        }

        return ok(node).build();
    }
}

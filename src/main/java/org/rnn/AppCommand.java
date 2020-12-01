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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.rnn.Identificators.Identificator;
import org.rnn.autopolicy.APFlow;
import org.rnn.autopolicy.APManager;
import org.rnn.autopolicy.APProfile;
import org.rnn.implementation.RealCommunicator;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * @author Piotr Frohlich
 * @version 3.0.0
 * CLI Manager Class
 */
@Command(scope = "onos", name = "dump-rnn-module",
         description = "Dump whole rnns' structures")
public final class AppCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "parameter", description = "Decide what do you want " +
            "to be listed. D for devices, P for paths, H for hosts and T for delay info." +
            "To see all paths (not only best ones) type +. To see all" +
            " output type DP+HT ",
            required = false, multiValued = false)
    String parameter = null;

    /**
     * Executes cli command dump-rnn-module, which prints all data on the console.
     */
    @Override
    protected void execute() {


        if(parameter!=null) {

            ArrayList<MultiRoutingInfo> tmp = PathTranslator.getTrafficInformation();
            if(parameter.contains("M") || parameter.contains("m")) {
                for (int i = 0; i < tmp.size(); i++) {
                    if (tmp.get(i).isUsable()) {
                        print(tmp.get(i).toString());
                    }
                }
            }

            if(parameter.contains("S")||parameter.contains("s"))
            {
                ArrayList<PathFlow> flows = AppComponent.manager.getService().getInstalledPath();
                print("SERVICE "+AppComponent.manager.getService().serviceName()+" PRODUCED "+flows.size()+" PATHS");
                print("SERVICE HAS "+AppComponent.manager.getActiveServices());
                for (int i = 0; i <flows.size(); i++)
                {
                    print("@@ "+flows.get(i).toString());
                }
            }

            if (parameter.contains("H") || parameter.contains("h")) {
                for (SecureHost sHost : NetState.getSecureHosts()) {
                    print(sHost.toString());

                }
            }
            if(parameter.contains("D")|| parameter.contains("d")) {
                //print("DEVICES:\n_________________________________________________\n");
                for (ForwardingDevice fd : NetState.getTopology()) {
                    print("DEVICE: [" + fd.getDeviceControllerId().toString()+" , simpleID: "+fd.getSimpleID());
                    print("HOSTS: [");
                    String hosts = "";
                    for (Identificator id : fd.connectedHosts) {
                        hosts+=(id.toString() + " , ");
                    }
                    print(hosts+"]");
                    print("LINKS: [ ");
                    String links = "";
                    for (PortDeviceIdTuple tuple : fd.links) {
                        links +="(PORT :" + tuple.portNumber + "/ " + tuple.deviceId.toString() + "/" + tuple.actualPortNumber + ")  ,  ";
                    }
                    print(links +"] ");
                    print("SENSITIVITY: " + fd.getSensivity() + "\nDAEMONS: ");
                    String daemons = "";
                    for (int i = 0; i < fd.getDaemons().size(); i++) {
                        daemons+=(fd.getDaemons().get(i).toString()+" , ");
                    }
                    print(daemons+"]");
                    print("FULLNESS: " + fd.getPreviousBytesOverall()+ "/" + fd.getMAX_BYTES_THROUGHPUT());
                    String latest= "N/A";
                    if(fd.energyMeasurements.size()>1)
                    {
                        latest = ""+fd.energyMeasurements.get(fd.energyMeasurements.size()-1);
                    }
                    print("AVG ENERGY USAGE: "+fd.getPredictedEnergyConsumption()+" - latest measurement: "+latest);

                }
            }
            if(parameter.contains("P")||parameter.contains("p")) {
                int j = 0;
                if(NetState.PATH_FORMAT == NetState.PathFormat.BASIC || parameter.contains("c") || parameter.contains("C"))
                {
                for (RoutingPathUnit rpu : PathTranslator.getAllPaths()) {
                    //print("\t\t[ " + j + " ]\n");
                    j++;
                    int i = 0;

                        for (PathFlow path : rpu.getPaths()) {
                            if (i == rpu.getBestPathIndex())
                                print("**(" + path.getSource() + "," + path.getDestination() + ")" + "    PATH: " + path.toString());
                            else if (parameter.contains("+")) {
                                print("\t(" + path.getSource() + "," + path.getDestination() + ")" + "    PATH: " + path.toString());
                            }
                            i++;
                        }
                        print("\n");
                    }
                }
                if(NetState.PATH_FORMAT == NetState.PathFormat.HOST_SPECIFIC)
                {
                    for (RoutingPathUnit rpu : PathTranslator.getHostSpecificRPU())
                    {
                        //print("\t\t[ " + j + " ]\n");
                        j++;
                        int i = 0;
                        for (PathFlow path : rpu.getPaths())
                        {
                            if (i == rpu.getBestPathIndex())
                                print("##(" + path.getSource() + "," + path.getDestination() + ")" + "    PATH: " + path.toString());
                            else if (parameter.contains("+")) {
                                print("\t(" + path.getSource() + "," + path.getDestination() + ")" + "    PATH: " + path.toString());
                            }
                            i++;
                        }
                        print("\n");
                    }
                }
            }
            if(parameter.contains("T")||parameter.contains("t"))
            {
                ArrayList<DelayAgregator.DelayLinkIdTuple> tmp_2 = RnnRouting.getDelayAgregator().getLinkIDDelayInfoHashMap();
                for (int i = 0; i < tmp_2.size(); i++)
                {
                    print(tmp_2.get(i).toString() + "");
                }
            }

            if(parameter.equals("n"))
            {
                try {
                    Field[] fields = NetState.class.getDeclaredFields();
                    for (int i = 0; i < fields.length; i++) {
                        try {
                            print(fields[i].getName() + "   :   " + fields[i].get(new Object()).toString());
                        }
                        catch (Exception e)
                        {}
                    }
                }
                catch (Exception e)
                {}
            }
            if (parameter.contains("B") || parameter.contains("b")) {
                print("BLACKLIST: ");
                for(IpAddress ip : NetState.BLACKLIST)
                {
                    print(ip.toString());
                }
            }
            if (parameter.contains("A") || parameter.contains("a")) {
                print("Autopolicy Enforced Profiles");
                Enumeration<APProfile> apProfiles = APManager.profiles.elements();
                while(apProfiles.hasMoreElements())
                {
                    print(apProfiles.nextElement().toString());
                }
            }

        }
        print("DEVICES: "+ NetState.getTopology().size());
        //print(NetState.jsonify());
        //print(RealCommunicator.rnnRouting.delayAgregator.jsonify());
        //print(RealCommunicator.rnnRouting.servicePlacementManager.toString());
    }
}

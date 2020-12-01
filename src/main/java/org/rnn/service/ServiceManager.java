package org.rnn.service;

import org.rnn.Identificators.Identificator;
import org.rnn.NetState;
import org.rnn.PathFlow;
import org.rnn.PathTranslator;
import org.rnn.UnifiedCommunicationModule;
import org.rnn.rnnLibrary.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * @author Piotr Frohlich
 * @version 1.0.1
 *
 * This is the manager of the services. It utilizes the RNN based solution.
 */

public class ServiceManager implements IServiceManager
{
    /**
     * The RNN created as a decisive module
     */
    private RecurrentRNN rnn;

    /**
     * Stored value of a previous reward assigned to the previous decision.
     */
    private double previousAward = 0.0;

    /**
     * List of pending clients/users (wanting to connect to the service)
     */
    private ArrayList<Identificator> clientsList = new ArrayList<>();

    /**
     * Active services instances (on machines with Idenficators)
     */
    private ArrayList<Identificator> activeServiceList = new ArrayList<>();

    /**
     * Used service template (with it's own hashmap etc.)
     */
    private IService service;

    /**
     * Basic constructor. It takes the service template and the number of the
     * available machines (for instantiating the services).
     * @param size The number of the available machines (for instantiating the
     *             services).
     * @param service The service template.
     */
    public ServiceManager(int size, IService service)
    {
        ArrayList<Neuron> neuronList = new ArrayList<Neuron>();

        for(int i = 0; i<size; i++)
        {
            neuronList.add(new Neuron(false, false, NetState.getTopology().get(i).getSimpleID()));
        }

        Random rnd = new Random();

        rnn = new RecurrentRNN(neuronList, neuronList.get(rnd.nextInt(size)));

        rnn.updateRnn(true, 0);

        this.service = service;
    }
    public ArrayList<PathFlow> getFlows() {
        return service.getInstalledPath();
    }

    public double getPreviousAward() {
        return previousAward;
    }

    public void setPreviousAward(double previousAward)
    {
        this.previousAward = previousAward;
    }

    /**
     * This method adds the user to the list of users requesting connection to the
     * service.
     * @param client
     */
    public void addClient(Identificator client)
    {
        clientsList.add(client);
    }

    /**
     * Method periodically updating the (instantiating new service instances, checking
     * the decisions of the RNN and rewarding the RNN)
     */
    public void updateServicesList()
    {
        /**
        try {
            double maxValue = Double.MIN_VALUE;
            int i = 0;
            for (int j = 0; j < clientsList.size(); j++) {
                for (int k = 0; k < activeServiceList.size(); k++) {
                    double score = 5.0;
                    try {
                        score = PathTranslator.getBestPathFromSrcDst(clientsList.get(j), activeServiceList.get(k)).checkMeanScore();
                    } catch (Exception e) {
                        UnifiedCommunicationModule.log.error(e.getMessage());
                    }
                    if (maxValue < score) {
                        i = k;
                        maxValue = score;
                    }
                }
                try {
                    service.installPathFromClientToService(clientsList.get(j), activeServiceList.get(i));
                }
                catch (Exception e1)
                {}
            }
        }
        catch (Exception e){}
**/
        for (int i = 0; i <activeServiceList.size(); i++)
        {
            if(activeServiceList.get(i) == null)
            {
                activeServiceList.remove(i);
            }
        }
        try {
            ArrayList<Integer> tmp = rnn.selectTopNeurons((int)(Math.log(NetState.getTopology().size())+0.5));
            if(activeServiceList.size() > 0)
            {
                for (int i = 0; i <activeServiceList.size(); i++)
                {
                    boolean isService = false;
                    for (int j = 0; j <tmp.size(); j++)
                    {
                        if(activeServiceList.get(i).equals(service.getIdentificatorOfServiceOnDevice(tmp.get(j))))
                        {
                            isService = true;
                        }
                    }
                    if(!isService)
                    {
                        service.stopService(activeServiceList.get(i));
                    }
                }
                for (int i = 0; i < tmp.size(); i++) {
                    boolean notActivate = false;
                    for (int j = 0; j < activeServiceList.size(); j++) {
                        if(activeServiceList.get(j).equals(service.getIdentificatorOfServiceOnDevice(tmp.get(i))))
                        {
                            notActivate = true;
                        }
                    }
                    if(!notActivate)
                    {
                        service.startService(service.getIdentificatorOfServiceOnDevice(tmp.get(i)));
                    }
                }
            }
            activeServiceList = new ArrayList<>();
            for (int i = 0; i < tmp.size(); i++)
            {
                activeServiceList.add(service.getIdentificatorOfServiceOnDevice(tmp.get(i)));
            }
            int i = 0;
            tmp = new ArrayList<>();
            for (int j = 0; j < activeServiceList.size(); j++)
            {
                tmp.add(0);
            }
            double maxValue = Double.MIN_VALUE;
            for (int j = 0; j < clientsList.size(); j++)
            {
                for (int k = 0; k < activeServiceList.size(); k++)
                {
                    double score = 5.0;
                    try {
                        score = PathTranslator.getBestPathFromSrcDst(clientsList.get(j), activeServiceList.get(k)).checkMeanScore();
                    }
                    catch (Exception e)
                    {
                        UnifiedCommunicationModule.log.error(e.getMessage());
                    }
                    if(maxValue < score)
                    {
                        i = k;
                        maxValue = score;
                    }
                }
                tmp.add(tmp.get(i)+1);
                service.installPathFromClientToService(clientsList.get(j), activeServiceList.get(i));
            }
            for (int j = 0; j <tmp.size(); j++)
            {
                if(tmp.get(j)==0)
                {
                    activeServiceList.remove(j);
                }
            }
        }
        catch (Exception e)
        {
            UnifiedCommunicationModule.log.error(e.getMessage());
        }
    }

    /**
     * Method called every update of the CNM. It updates the RNN.
     */
    public void updateScores()
    {
        try {
            double variation = 0.0, meanDelayOnRoute = 0.0, meanNumberOfClientsOnSingleService = (double) ((double) clientsList.size() / (double) activeServiceList.size());
            ArrayList<PathFlow> pathsToServices = service.getInstalledPath();

            for (int i = 0; i < pathsToServices.size(); i++) {
                meanDelayOnRoute += (1 / pathsToServices.get(i).checkMeanScore());
            }
            meanDelayOnRoute /= (double) pathsToServices.size();
            for (int i = 0; i < pathsToServices.size(); i++) {
                variation = variation + (((1 / pathsToServices.get(i).checkMeanScore()) - meanDelayOnRoute) * ((1 / pathsToServices.get(i).checkMeanScore()) - meanDelayOnRoute));
            }
            variation = variation / (double) pathsToServices.size();
            variation = Math.sqrt(variation);
            double goal = (1 - (NetState.DEFAULT_HISTORICAL_LEARNING_PARAMETER*2)) * (10 * variation + meanDelayOnRoute + 100 * activeServiceList.size()) + (NetState.DEFAULT_HISTORICAL_LEARNING_PARAMETER*2) * previousAward;
            double r = 1 / goal;
            //String toFile = " TO FILE\t"+variation+"\t"+meanDelayOnRoute+"\t"+100 * ((10*activeServiceList.size())-clientsList.size())+"\n";
            //UnifiedCommunicationModule.log.info(toFile);

            if (r >= previousAward) {
                rnn.updateRnn(true, r);
                previousAward = r;
            } else {
                rnn.updateRnn(false, r);
                previousAward = r;
            }
        }
        catch (Exception e)
        {
            UnifiedCommunicationModule.log.error(e.getMessage());
        }
    }

    /**
     * Method called every time the CNM is updated it provides the Manager with the work cycle.
     */
    @Override
    public void onCNMupdate()
    {
        updateServicesList();
        updateScores();
    }

    /**
     * Every time a new host requests the service, this method is called.
     * @param hostId An id of the new host.
     */
    @Override
    public void onHostUpdate(Identificator hostId)
    {
        addClient(hostId);
    }


    public IService getService() {
        return service;
    }

    /**
     * Getter for the active service instances.
     * @return String representation of the list of the active service instances.
     */
    public String getActiveServices()
    {
        String str= activeServiceList.size()+" ACTIVE SERVICES: [ ";
        for (int i = 0; i <activeServiceList.size(); i++)
        {
            str+=(activeServiceList.get(i).toString()+" , ");
        }
        str+="]";
        return str;
    }


}

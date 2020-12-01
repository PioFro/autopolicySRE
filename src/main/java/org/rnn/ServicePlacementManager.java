package org.rnn;

import org.rnn.Identificators.Identificator;
import org.rnn.NetState;
import org.rnn.PathFlow;
import org.rnn.rnnLibrary.Neuron;
import org.rnn.rnnLibrary.RecurrentRNN;
import org.rnn.service.IService;
import org.rnn.service.IServiceManager;

import java.util.ArrayList;

public class ServicePlacementManager implements IServiceManager
{
    public IService service;
    public int change = 0;

    public ServicePlacementManager(IService service)
    {
        this.service = service;
    }


    public enum ServiceEventType
    {
        ADD,REMOVE
    }
    private class ClientServiceRnn
    {
        Identificator hostId;
        IService service;
        RecurrentRNN rnn;
        double previousReward = 0.0;

        public ClientServiceRnn(Identificator id, IService service, int numberOfActiveServices)
        {
            hostId = id;
            this.service = service;
            ArrayList<Neuron> neurons = new ArrayList<>();
            for (int i = 0; i <numberOfActiveServices; i++)
            {
                neurons.add(new Neuron(false, false, i));
            }
            rnn = new RecurrentRNN(neurons, neurons.get(0));
            previousReward = 0.0;
        }
        protected void addNeuron()
        {
            rnn.addNeuron(new Neuron(false,false,rnn.getFirstPossibleIndex()));
        }
        protected int getMaxExcited()
        {
            rnn.updateRnn(true, 0.0);
            return rnn.getSelectedNeuron().id;
        }
        protected void updateRnn(double reward)
        {
            double factorT_L = NetState.DEFAULT_HISTORICAL_LEARNING_PARAMETER*reward + (1-NetState.DEFAULT_HISTORICAL_DELAY_PARAMETER)*this.previousReward;
            if(factorT_L < reward)
            {
                rnn.updateRnn(true, reward);
            }
            else
            {
                rnn.updateRnn(false, reward);
            }
            previousReward = reward;
        }
    }

    protected ArrayList<ClientServiceRnn> clients = new ArrayList<>();
    protected ArrayList<Identificator> activeServices = new ArrayList<>();

    /**
     * Method invoked every time CNM is updated. It recalculates the optimal or
     * sub-optimal routes for clients to respective services.
     */
    @Override
    public void onCNMupdate()
    {
        ArrayList<PathFlow> installedServicesPaths = service.getInstalledPath();

        for (int i = 0; i <clients.size(); i++)
        {
            for (int j = 0; j <installedServicesPaths.size(); j++)
            {
                if(installedServicesPaths.get(j).getSrcHostId().equals(clients.get(i).hostId))
                {
                    clients.get(i).updateRnn(installedServicesPaths.get(j).checkMeanScore());
                    if(!activeServices.get(clients.get(i).getMaxExcited()).equals(installedServicesPaths.get(j).getDstHostId()))
                    {
                        if(change > 2)
                        {
                            change = 0;
                            clients.get(i).service.installPathFromClientToService(clients.get(i).hostId, activeServices.get(clients.get(i).getMaxExcited()));
                        }
                        change +=1;
                    }
                }
            }
            if(installedServicesPaths.size() == 0)
            {
                service.installPathFromClientToService(clients.get(i).hostId,activeServices.get(0));
            }
        }

    }

    public String toString()
    {
        String toRet = "";
        ArrayList<PathFlow> tmp = service.getInstalledPath();
        for (int i = 0; i <tmp.size(); i++)
        {
            toRet+=tmp.get(i).toString()+"\n";
        }

        return toRet;
    }

    /**
     * Method invoked every time a new client requires a connection to a given
     * service.
     * @param hostId Id of connected client
     */
    @Override
    public void onHostUpdate(Identificator hostId)
    {
        clients.add(new ClientServiceRnn(hostId,service,activeServices.size()));
        service.installPathFromClientToService(hostId,activeServices.get(0));
    }
    /**
     * This method is rarely invoked when the placement of service is changed
     * @param serviceId Id of a given service
     * @param type Type of action for given service
     */
    public void onServiceUpdate(Identificator serviceId, ServiceEventType type)
    {
        if(type == ServiceEventType.ADD)
        {
            if(activeServices.contains(serviceId))
                return;
            else
                activeServices.add(serviceId);
        }
        if(type == ServiceEventType.REMOVE)
        {
            activeServices.remove(serviceId);
        }
    }
}
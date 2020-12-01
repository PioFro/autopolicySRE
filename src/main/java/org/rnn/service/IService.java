package org.rnn.service;

import org.rnn.Identificators.Identificator;
import org.rnn.PathFlow;

import java.util.ArrayList;

public interface IService
{
    public Identificator getIdentificatorOfServiceOnDevice(int simpleId);
    public void installPathFromClientToService(Identificator identificatorHost, Identificator identificatorService);
    public String serviceName();
    public ArrayList<PathFlow> getInstalledPath();
    public String getStaticIpAddress();
    public void startService(Identificator id);
    public void stopService(Identificator id);
}

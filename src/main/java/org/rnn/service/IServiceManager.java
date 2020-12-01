package org.rnn.service;

import org.rnn.Identificators.Identificator;

public interface IServiceManager
{
    public void onCNMupdate();
    void onHostUpdate(Identificator hostId);
}

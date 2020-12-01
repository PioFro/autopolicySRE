package org.rnn.service;

import org.rnn.Identificators.Identificator;

public interface IServiceEndpoint
{
    public String getIPAddress();
    public long getPortNumber();
    public Identificator getId();

}

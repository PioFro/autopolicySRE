package org.rnn;

import org.rnn.Identificators.Identificator;

public interface Daemon
{
    public Identificator getDeviceId();
    public String getIpAddress();
    public long getPortNumber();
}

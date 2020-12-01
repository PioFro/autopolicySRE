package org.rnn;

import java.util.ArrayList;

public interface PathInstaller
{
    void installPath(PathFlow flow, long specificPort);
    void installCognitivePath(PathFlow flow);
    void installDiscoveryPath(PathFlow flow);
    void installServicePath(PathFlow flow,long specificPort, String serviceStaticIpAddress);
    ArrayList<PathFlow> getInstalledPaths();
    ArrayList<PathFlow> getDiscoveryPaths();

}

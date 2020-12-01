package org.rnn;

import java.util.ArrayList;

public class PathProviderStorage
{
    private ArrayList<PathFlow> flows = new ArrayList<>();
    private PathFlow best;

    public PathProviderStorage(PathFlow flow)
    {
        best = flow;
        flows.add(flow);
    }
    public void addPath(PathFlow flow)
    {
        flows.add(flow);
        if(flow.checkMeanScore()>best.checkMeanScore())
        {
            best = flow;
        }
    }
    public PathFlow getBest(){return best;}
}

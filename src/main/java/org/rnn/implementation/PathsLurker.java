package org.rnn.implementation;

import org.rnn.PathTranslator;

import java.util.TimerTask;

public class PathsLurker extends TimerTask {

    @Override
    public void run()
    {
        PathTranslator.checkPaths();
    }
}

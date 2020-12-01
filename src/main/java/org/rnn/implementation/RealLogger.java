package org.rnn.implementation;

import org.rnn.AppComponent;
import org.rnn.UnifiedLogger;

import java.io.FileWriter;

/**
 * @author Piotr Frohlich
 * @version 1.0.1
 * This class connects the ONOS-dependent and ONOS-independent logger mechanism.
 */
public class RealLogger implements UnifiedLogger {

    /**
     * The MASTER class
     */
    AppComponent MASTER;
    /**
     * This provides a way out of the ONOS environment. It's used to get data
     * for experiments etc. Normally uninitialized.
     */
    FileWriter file;

    /**
     * This basic constructor initializes MASTER class as well as (if parameter
     * EXPERIMENT from the NetState class is set to true) initializes the way out
     * of the ONOS environment.
     */
    public RealLogger()
    {
        MASTER = RealCommunicator.master;
        try {
            file = new FileWriter("/home/pfrohlich/outputSerivces.txt");
        }
        catch (Exception e)
        {}
    }

    /**
     * Log on the INFO level
     * @param str String to log
     */
    @Override
    public void info(String str)
    {
        MASTER.log.info(str);
        if(str.contains("TO FILE"))
        {
            if(file!= null)
            {
                try {
                    file.write(str);
                }
                catch (Exception e ){}
            }

        }

    }

    /**
     * Log on the ERROR level
     * @param err String to log
     */
    @Override
    public void error(String err)
    {
        MASTER.log.error(err);
    }

    /**
     * Log on the WARN level
     * @param warn String to log
     */
    @Override
    public void warn(String warn)
    {
        MASTER.log.warn(warn);

    }

    /**
     * Closes all required connections (in and out). Called at the end of the
     * plugin lifecycle.
     */
    @Override
    public void end()
    {
        try {
            file.close();
        }
        catch (Exception e){}
    }
}

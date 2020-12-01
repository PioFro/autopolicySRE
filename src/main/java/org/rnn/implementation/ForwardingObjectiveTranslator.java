package org.rnn.implementation;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.ForwardingObjective;

public class ForwardingObjectiveTranslator
{
    public static DeviceId srcId;
    public static ForwardingObjective translateStringToForwardingObjective
            (
                    String stringObjectiveSrc,
                    String stringObjectiveDst,
                    boolean block
            )
            throws IncorectStringObjectiveException
    {
        StringObjectiveInformation src = new StringObjectiveInformation();
        StringObjectiveInformation dst = new StringObjectiveInformation();
        dst.validateStringObjective(stringObjectiveDst);
        dst.validateAll();
        src.validateStringObjective(stringObjectiveSrc);
        src.validateAll();

        return src.createForwardingObjective(dst,block);
    }
}

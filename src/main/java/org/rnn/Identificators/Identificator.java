package org.rnn.Identificators;

/**
 * @author Piotr Frohlich
 * @version 1.0.1
 * This class is a dedicated identificator class which implements the Identificator interface.
 * This implementation is ONOS-dependent.
 */

public interface Identificator
{
    /**
     * Most important method of this interface. Compares two Identidficators
     * @param o Object to compare
     * @return true if objects are referring to the same identificators.
     */
    @Override
    boolean equals(Object o);

    /**
     * Method returning readable (in form of string) representation of the instance
     * @return String representation of the instance
     */
    @Override
    String toString();

    /**
     * Method returning the long representation of the instance
     * @return Long representation of the instance
     */
    long toLong();
}

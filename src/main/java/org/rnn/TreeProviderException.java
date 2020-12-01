package org.rnn;

public class TreeProviderException extends Exception
{
    public TreeProviderException(long src, long dst)
    {
        super("Unable to create the src: "+src+" and dst: "+dst+" path.");
    }
}

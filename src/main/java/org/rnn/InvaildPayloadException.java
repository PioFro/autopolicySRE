package org.rnn;


/**
 * Invalid Smart Packet Payload Exception
 * @Author Piotr Fr&ouml;hlich
 * @version Beta 2.0.0
 */

public class InvaildPayloadException extends Exception
{
    /**
     * Constructor for invalid Smart Packet Payload exception.
     */
    public InvaildPayloadException()
    {
        super("INVALID SMART PACKET PAYLOAD RECEIVED");
    }
}

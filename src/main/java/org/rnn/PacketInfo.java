package org.rnn;

public class PacketInfo
{
    String payload = "";
    public PacketInfo(String payload)
    {
        this.payload = payload;
    }
    @Override
    public String toString()
    {
        return payload;
    }
}

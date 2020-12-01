package org.rnn;


/**
 * Interface class for Routing Module element.
 */
public interface RoutingModule
{
    /**
     * Method which takes Cognitive Packet (parsed in onos class) and processes it
     * @param packetContext Cognitive Packet parsed in onos class
     * @throws InvaildPayloadException If payload is malformed or tempered with
     *                                  this exception will occur.
     */
    void receiveSmartPacket(PacketInfo packetContext) throws InvaildPayloadException;

    /**
     * This method takes new security scoring for endpoint and punishes or rewards
     * neural networks accordingly.
     * @param secureHost Scoring and Identification of given endpoint.
     */
    void receiveNewSecurityScoring(SecureHost secureHost);

}

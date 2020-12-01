package org.rnn;
import org.rnn.Identificators.Identificator;
import org.rnn.rnnLibrary.Neuron;
import org.rnn.rnnLibrary.RecurrentRNN;

import java.util.ArrayList;
import java.util.Random;

/**
 * Data container. Contains Tuples of Random Neural Networks assigned to Source Node - Destination Node tuple.
 * @author Piotr Frohlich
 * @version Beta 2.0.0
 *
 *
 */

public class RNNDestinationTuple
{

    /**
     * Source Host ID
     * */
    protected long sourceHost;
    /**
     * Destination Host ID
     */
    protected  long destinationHost;


    /***
     * Neural Network connected to this destination
     */
    private RecurrentRNN recurrentRNN = new RecurrentRNN();

    /**
     * Previous reward value
     */
    private double previousAward = 0.0;

    /**
     * Id of src host
     */
    Identificator hostIdSrc;
    /**
     * Id of dst host
     */
    Identificator hostIdDst;

    /***
     * Constructor which sets up RNN of given size with random values
     * and makes neurons emit their first signal
     * @param size size of neural network to create (number of connected ports of
     *             a device).
     * @param dstHost Simple id of device to which destination host is connected
     * @param srcHost Simple id of device to which source host is connected
     */
    public RNNDestinationTuple(int size, long srcHost, long dstHost)
    {


        sourceHost = srcHost;

        destinationHost = dstHost;

        ArrayList<Neuron> neuronList = new ArrayList<Neuron>();

        for(int i = 0; i<size; i++)
        {
            neuronList.add(new Neuron(false, false, i));
        }

        Random rnd = new Random();

        recurrentRNN = new RecurrentRNN(neuronList, neuronList.get(rnd.nextInt(size)));

        recurrentRNN.updateRnn(true, 0);
    }

    /***
     * An empty constructor
     */
    public RNNDestinationTuple(int size, Identificator hostIdSrc, Identificator hostIdDst,int sourceDevice, int destinationDevice)
    {
        this.hostIdDst = hostIdDst;
        this.hostIdSrc = hostIdSrc;
        sourceHost = sourceDevice;
        destinationHost = destinationDevice;
        ArrayList<Neuron> neuronList = new ArrayList<Neuron>();

        for(int i = 0; i<size; i++)
        {
            neuronList.add(new Neuron(false, false, i));
        }

        Random rnd = new Random();

        recurrentRNN = new RecurrentRNN(neuronList, neuronList.get(rnd.nextInt(size)));

        recurrentRNN.updateRnn(true, 0);
    }

    /**
     * Getter for whole RNN Class
     * @return whole RNN Class
     */
    public RecurrentRNN getRecurrentRNN() {
        return recurrentRNN;
    }

    /**
     * Give award to index'th neuron in RNN accordingly to Reinforcement Learning
     * algorithm
     *
     *
     * @param award award that will be given to that neuron. If award is greater
     *              than 0 this will be an award otherwise this will be punishment
     * @param output index of neuron which corresponded to this decision (index of
     *               port)
     */
    public void giveAward(double award, int output)
    {
        Neuron previousNeuron = recurrentRNN.getSelectedNeuron();

        try {
            recurrentRNN.setSelectedNeuron(recurrentRNN.getNeurons().get(output));
        }
        catch(Exception e)
        {
            return;
        }
        if(award>0)
            recurrentRNN.updateRnn(true, award);
        else {
            if(award <= -1) {
                this.previousAward *= 1.2;
                if (this.previousAward > 1) {
                    this.previousAward = 1;
                }
            }
            recurrentRNN.updateRnn(false, -award);
        }

        recurrentRNN.setSelectedNeuron(previousNeuron);

    }

    /**
     * Getter for simple id of source device.
     * @return simple id of source device
     */
    public long getSourceHost(){return sourceHost;}

    /**
     * Getter for simple id of destination device.
     * @return simple id of source device
     */
    public long getDestinationHost(){return destinationHost;}
    /***
     * getter for the most excited neuron in the whole RNN
     * @return
     */
    public int getMaxExcited()
    {
        recurrentRNN.updateRnn(true, 0.0);
        return this.recurrentRNN.getSelectedNeuron().id;
    }

    /**
     * parser to string
     * @return ArrayList of strings being representation of border node and whole
     * RNN
     */
    public ArrayList<String> prepareString()
    {
        ArrayList<String> parsed = new ArrayList<String>();

        return parsed;
    }
    /**
     * Parser to string
     * @return whole string consisitng of all neurons and weights parsed to one string.
     */
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (String str:prepareString()) {
            stringBuilder.append(str+" ");
        }
        return stringBuilder.toString();
    }

    public double getPreviousAward() {
        return previousAward;
    }

    public void setPreviousAward(double previousAward) {
        this.previousAward = previousAward;
    }
    public void processLinkDownOnNeuron( int index )
    {
        recurrentRNN.deleteNeuronOnIndex(index);
    }
    public int processLinkUp(PortDeviceIdTuple link)
    {
        int index = recurrentRNN.getFirstPossibleIndex();
        recurrentRNN.addNeuron(new Neuron(false, false, index));
        return index;

    }

}

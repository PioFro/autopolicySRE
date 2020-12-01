package org.rnn.rnnLibrary;

import org.rnn.NetState;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class RecurrentRNN {
    //private DatapathId datapathId;
    private ArrayList<Neuron> neuronsList;
    private int tNeurons=0;
    private Neuron selectedNeuron;
    private static double probabilityExploratory=0.3;
    private static int maxIterations=1000;
    private static double maxConvergenceError=Math.pow(10, -4);


    public RecurrentRNN(){
        //this.datapathId=datapathId;
        this.neuronsList = new ArrayList<Neuron>();
    }

    public RecurrentRNN(ArrayList<Neuron> neuronsList, Neuron selectedNeuron){
        //log.info("creating rnn");
        try{
            this.neuronsList = neuronsList;

            this.tNeurons = neuronsList.size();
            double weight=1;
            if(this.tNeurons >2){
                weight=1/(2*(this.tNeurons-2));
            }


            //log.info("rnn total neurons: {}", this.tNeurons);
            for(int neuronIndex=0;neuronIndex< this.tNeurons;neuronIndex++){
                for(int neuronIndex1=0;neuronIndex1< this.tNeurons;neuronIndex1++){
                    this.neuronsList.get(neuronIndex).posWeights.add(weight);
                    this.neuronsList.get(neuronIndex).negWeights.add(weight);
                }
            }

            if (selectedNeuron == null){
                this.selectProbeNeuron();//this method returns a Neuron AND assigns it to field in this.
            }
            else{
                int neuronIndex=this.getNeuronIndex(selectedNeuron);
                //log.info("rnn constr, neuron ide:", neuronIndex);
                this.selectedNeuron=this.neuronsList.get(neuronIndex);
            }
        }
        catch(Exception ex){

        }
        //log.info("created rnn ok");
    }

    public boolean addNeuron(Neuron neuron){

        if(!neuronsList.contains(neuron)) { // if neuron isn't already in the list add it and return false
            // if not present then add
            this.tNeurons += 1;
            this.neuronsList.add(neuron);
            return false;
        }
        return true;						// neuron is already in the list, return true
    }

    private List<Double> calFiringRates(boolean updateNeurons){
        List<Double> tmpFiringRates = new ArrayList<Double>();

        for(int neuronSrcIndex=0;neuronSrcIndex<this.tNeurons; neuronSrcIndex++){
            double firingRate=0;
            for(int neuronDstIndex=0;neuronDstIndex<this.tNeurons; neuronDstIndex++){
                if (neuronSrcIndex != neuronDstIndex){
                    firingRate+=this.neuronsList.get(neuronSrcIndex).posWeights.get(neuronDstIndex) +
                            this.neuronsList.get(neuronSrcIndex).negWeights.get(neuronDstIndex);
                }
            }
            tmpFiringRates.add(firingRate);
        }

        if ((updateNeurons==true) | (this.neuronsList.get(0).firingRate==-1)){
            for (int neuronIndex=0;neuronIndex<this.neuronsList.size();neuronIndex++){
                this.neuronsList.get(neuronIndex).firingRate=tmpFiringRates.get(neuronIndex);
            }
        }

        return tmpFiringRates;
    }

    private void normalizeWeights(){
        List<Double> tmpFiringRates=this.calFiringRates(false);
        for (int neuronIndex=0;neuronIndex<this.neuronsList.size();neuronIndex++){
            for (int neuronIndex1=0;neuronIndex1<this.neuronsList.size();neuronIndex1++){
                if ((neuronIndex != neuronIndex1) & (tmpFiringRates.get(neuronIndex)!=0) ){
                    double newWeight= this.neuronsList.get(neuronIndex).posWeights.get(neuronIndex1)*
                            this.neuronsList.get(neuronIndex).firingRate/tmpFiringRates.get(neuronIndex);
                    this.neuronsList.get(neuronIndex).posWeights.set(neuronIndex1, newWeight);
                    newWeight= this.neuronsList.get(neuronIndex).negWeights.get(neuronIndex1)*
                            this.neuronsList.get(neuronIndex).firingRate/tmpFiringRates.get(neuronIndex);
                    this.neuronsList.get(neuronIndex).negWeights.set(neuronIndex1, newWeight);
                }
            }
        }
        this.calFiringRates(true);
    }

    private class NeuronListsPair{
        List<Neuron> bestNeuronsList=new ArrayList<Neuron>();
        List<Neuron> nonBestNeuronsList=new ArrayList<Neuron>();

        public NeuronListsPair(List<Neuron> bestNeuronsList, List<Neuron> nonBestNeuronsList){
            this.bestNeuronsList=bestNeuronsList;
            this.nonBestNeuronsList=nonBestNeuronsList;
        }
    }

    private NeuronListsPair findBestNeurons(){
        List<Neuron> bestNeuronsList= new ArrayList<Neuron>();
        double bestPotential=0;
        List<Neuron> nonBestNeuronsList= new ArrayList<Neuron>();

        for (int neuronIndex=0;neuronIndex<this.neuronsList.size();neuronIndex++){
            if ((this.neuronsList.get(neuronIndex).invalidOutput==true) || (this.neuronsList.get(neuronIndex).terminating==true)){
                continue;
            }

            if (this.neuronsList.get(neuronIndex).potential > bestPotential){
                bestPotential=this.neuronsList.get(neuronIndex).potential;
            }
        }

        for (int neuronIndex=0;neuronIndex<this.neuronsList.size();neuronIndex++){
            if ((this.neuronsList.get(neuronIndex).invalidOutput==true) || (this.neuronsList.get(neuronIndex).terminating==true)){
                continue;
            }

            if (this.neuronsList.get(neuronIndex).potential == bestPotential){
                bestNeuronsList.add(this.neuronsList.get(neuronIndex));
            }
            else{
                nonBestNeuronsList.add(this.neuronsList.get(neuronIndex));
            }
        }

        NeuronListsPair neuronListsPair= new NeuronListsPair(bestNeuronsList, nonBestNeuronsList);
        return neuronListsPair;
    }

    public ArrayList<Integer> selectTopNeurons(int numberOfTopNeurons)
    {
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<Neuron> n = new ArrayList<>();
        for (int i = 0; i <neuronsList.size(); i++)
        {
            if(neuronsList.get(i).potential > NetState.DEFAULT_THRESHOLD_OF_SERVICE)
            {
                if(n.size() == 0)
                {
                    n.add(neuronsList.get(i));
                }
                else {
                    boolean added = false;
                    for (int j = 0; j < n.size(); j++) {
                        if (n.get(j).potential <= neuronsList.get(i).potential) {
                            n.add(j,neuronsList.get(i));
                            added = true;
                            break;
                        }
                    }
                    if (!added)
                    {
                        n.add(neuronsList.get(i));
                    }
                }
            }
        }
        if(n.size() >= 1) {
            for (int i = 0; i < numberOfTopNeurons; i++)
            {
                try {
                    ids.add(n.get(i).id);
                }
                catch (Exception e){}
            }
            if (ids.size() >= 1) {
                return ids;
            }
        }
        else
        {
            selectProbeNeuron();
            ids.add(selectedNeuron.id);
            return ids;
        }
        return null;
    }

    public Neuron selectProbeNeuron(){
        double randomDouble=ThreadLocalRandom.current().nextDouble(0, 1);
        NeuronListsPair neuronListsPair = this.findBestNeurons();
        Neuron neuron = null;

        if (neuronListsPair.bestNeuronsList.size() == 0)
        {
            neuron = new Neuron(false, false, ThreadLocalRandom.current().nextInt(0,neuronsList.size()));
            return neuron;
        }

        if (randomDouble <= probabilityExploratory){
            if (neuronListsPair.nonBestNeuronsList.size() > 0){
                int randomIndex=ThreadLocalRandom.current().nextInt(0,neuronListsPair.nonBestNeuronsList.size());
                this.selectedNeuron = neuronListsPair.nonBestNeuronsList.get(randomIndex);
                return neuronListsPair.nonBestNeuronsList.get(randomIndex);
            }
            else{
                int randomIndex=ThreadLocalRandom.current().nextInt(0,neuronListsPair.bestNeuronsList.size());
                this.selectedNeuron = neuronListsPair.bestNeuronsList.get(randomIndex);
                return neuronListsPair.bestNeuronsList.get(randomIndex);
            }
        }

        else{
            if (neuronListsPair.bestNeuronsList.size() > 0){
                int randomIndex=ThreadLocalRandom.current().nextInt(0,neuronListsPair.bestNeuronsList.size());
                this.selectedNeuron=neuronListsPair.bestNeuronsList.get(randomIndex);
                return neuronListsPair.bestNeuronsList.get(randomIndex);
            }
            else{
                int randomIndex=ThreadLocalRandom.current().nextInt(0,neuronListsPair.nonBestNeuronsList.size());
                this.selectedNeuron=neuronListsPair.nonBestNeuronsList.get(randomIndex);
                return neuronListsPair.nonBestNeuronsList.get(randomIndex);
            }
        }
    }

    private int getNeuronIndex(Neuron neuron){

        for (int neuronIndex=0;neuronIndex<this.neuronsList.size();neuronIndex++){
            if (this.neuronsList.get(neuronIndex).equals(neuron)){
                return neuronIndex;
            }
        }

        return -1;
    }

    private void updateWeights(boolean success, double reward){

        int selectedNeuronIndex = this.getNeuronIndex(this.selectedNeuron);
        int weightShare = this.tNeurons-2;
        if (weightShare<1){
            weightShare=1;
        }

        if (success == true){
            //log.info("neuron list size: {} and index {}", new Object []{this.neuronsList.size(),selectedNeuronIndex});
            for (int neuronSrcIndex=0;neuronSrcIndex<this.neuronsList.size();neuronSrcIndex++){
                //log.info("pos weight:{}",this.neuronsList.get(neuronSrcIndex).posWeights.size());
                if (neuronSrcIndex != selectedNeuronIndex){
                    double newWeight = this.neuronsList.get(neuronSrcIndex).posWeights.get(selectedNeuronIndex) + reward;
                    this.neuronsList.get(neuronSrcIndex).posWeights.set(selectedNeuronIndex, newWeight);
                }
                //log.info("neg weight:{}",this.neuronsList.get(neuronSrcIndex).negWeights.size());
                for (int neuronDstIndex=0;neuronDstIndex<this.neuronsList.size();neuronDstIndex++){
                    if ((neuronDstIndex != selectedNeuronIndex) && ( neuronDstIndex != neuronSrcIndex)){
                        double newWeight = this.neuronsList.get(neuronSrcIndex).negWeights.get(neuronDstIndex) + (reward/weightShare);
                        this.neuronsList.get(neuronSrcIndex).negWeights.set(neuronDstIndex, newWeight);
                    }
                }
            }
        }
        else{
            for (int neuronSrcIndex=0;neuronSrcIndex<this.neuronsList.size();neuronSrcIndex++){
                if (neuronSrcIndex != selectedNeuronIndex){
                    double newWeight = this.neuronsList.get(neuronSrcIndex).negWeights.get(selectedNeuronIndex) + reward;
                    this.neuronsList.get(neuronSrcIndex).negWeights.set(selectedNeuronIndex, newWeight);
                }

                for (int neuronDstIndex=0;neuronDstIndex<this.neuronsList.size();neuronDstIndex++){
                    if ((neuronDstIndex != selectedNeuronIndex) && ( neuronDstIndex != neuronSrcIndex)){
                        double newWeight = this.neuronsList.get(neuronSrcIndex).posWeights.get(neuronDstIndex) + (reward/weightShare);
                        this.neuronsList.get(neuronSrcIndex).posWeights.set(neuronDstIndex, newWeight);
                    }
                }
            }
        }
        //this.normalizeWeights();
    }

    private void calPotentials(){

        int iterations=0;

        for (int neuronIndex=0; neuronIndex<this.tNeurons;neuronIndex++){
            this.neuronsList.get(neuronIndex).potential=Math.pow(10, 6);//static number is better pow works very slow
        }

        while (iterations <= maxIterations){
            iterations++;

            for (int neuronSrcIndex=0; neuronSrcIndex<this.tNeurons;neuronSrcIndex++){
                double tmpNominator=0.25;
                double tmpDenominator=this.neuronsList.get(neuronSrcIndex).firingRate;

                for (int neuronDstIndex=0; neuronDstIndex<this.tNeurons;neuronDstIndex++){
                    if(neuronSrcIndex!=neuronDstIndex){
                        tmpNominator+=this.neuronsList.get(neuronDstIndex).potential*
                                this.neuronsList.get(neuronDstIndex).posWeights.get(neuronSrcIndex);
                        tmpDenominator+=this.neuronsList.get(neuronDstIndex).potential*
                                this.neuronsList.get(neuronDstIndex).negWeights.get(neuronSrcIndex);
                    }
                }

                if (tmpDenominator !=0){
                    this.neuronsList.get(neuronSrcIndex).tmpPotential=(tmpNominator/tmpDenominator);
                }
                else{
                    this.neuronsList.get(neuronSrcIndex).tmpPotential=Math.pow(10, 6);
                }
            }

            int numPotentialsConverged = 0;
            for (int neuronIndex=0; neuronIndex<this.tNeurons;neuronIndex++){
                double convergenceDifference=0;
                if(this.neuronsList.get(neuronIndex).potential > this.neuronsList.get(neuronIndex).tmpPotential){
                    convergenceDifference = this.neuronsList.get(neuronIndex).potential -
                            this.neuronsList.get(neuronIndex).tmpPotential;
                }
                else{
                    convergenceDifference = this.neuronsList.get(neuronIndex).tmpPotential-
                            this.neuronsList.get(neuronIndex).potential;
                }

                if (convergenceDifference<maxConvergenceError){
                    numPotentialsConverged++;
                }

                this.neuronsList.get(neuronIndex).potential = this.neuronsList.get(neuronIndex).tmpPotential;
            }

            if (numPotentialsConverged == this.tNeurons){
                break;
            }
        }

        if (iterations == maxIterations){
        }
    }

    public Neuron updateRnn(boolean success, double reward){
        //log.info("updateRNN started");
        this.updateWeights(success, reward);

        //log.info("updated link weights");
        this.calPotentials();
        //log.info("updated calpotentials");
        selectedNeuron=this.selectProbeNeuron();
        //log.info("selected neuron");
        this.normalizeWeights();
        //log.info("normalized weights");

        return selectedNeuron;
    }

    public void setSelectedNeuron(Neuron neuron){
        for(int neuronIndex=0; neuronIndex< this.neuronsList.size(); neuronIndex++){
            if(neuron.equals(this.neuronsList.get(neuronIndex))){
                this.selectedNeuron = this.neuronsList.get(neuronIndex);
                break;
            }
        }
    }

    public void deleteNeuronOnIndex( int index )
    {
        if(selectedNeuron.equals(neuronsList.get(index)))
        {
            if(index-1 < 0&&index+1<neuronsList.size())
            {
                setSelectedNeuron(neuronsList.get(index+1));
            }
            else
            {
                setSelectedNeuron(neuronsList.get(index-1));
            }
        }
        for (int i = 0; i < neuronsList.size(); i++)
        {
            if(i!=index)
                neuronsList.get(i).deleteWeightsOnIndex(index);
        }
        neuronsList.remove(index);
    }

    public Neuron getSelectedNeuron(){
        return this.selectedNeuron;
    }

    public int getFirstPossibleIndex()
    {
        boolean success = true;
        int i = 0;
        while(true)
        {
            for (int j = 0; j < neuronsList.size(); j++)
            {
                if(i == neuronsList.get(j).id)
                {
                    success= false;
                    break;
                }
            }
            if(success == true)
                return i;
            success = true;
            i++;
        }
    }

    public List<Neuron> getNeurons(){ return  this.neuronsList;}
}

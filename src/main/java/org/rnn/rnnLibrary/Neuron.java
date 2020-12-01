package org.rnn.rnnLibrary;

import java.util.ArrayList;
import java.util.List;

    public class Neuron{

        public String eqSymbol;
        public double potential=0.5;
        public double tmpPotential=0.5;
        public List<Double> posWeights;
        public List<Double> negWeights;
        public double firingRate=1;
        public boolean terminating = false;
        public boolean invalidOutput=false;
        public int id;


        public Neuron(boolean terminating, boolean invalidOutput, int id){

            this.id = id;
            this.terminating = terminating;
            this.invalidOutput = invalidOutput;
            this.posWeights = new ArrayList<Double>();
            this.negWeights = new ArrayList<Double>();
            //this.eqSymbol="N"+String.valueOf(this.port.getPortNumber());
        }


        public void deleteWeightsOnIndex(int index)
        {
            posWeights.remove(index);
            negWeights.remove(index);
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Neuron other = (Neuron) obj;
            return true;
        }

        @Override
        public String toString() {
         return ""+id;
        }
    }